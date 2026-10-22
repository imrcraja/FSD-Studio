package com.fsdetc.fsdstudio.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fsdetc.fsdstudio.FsdStudioApp
import com.fsdetc.fsdstudio.data.Api
import com.fsdetc.fsdstudio.data.AssetSlot
import com.fsdetc.fsdstudio.data.AssetSlots
import com.fsdetc.fsdstudio.data.BlankSkins
import com.fsdetc.fsdstudio.data.CharacterInfo
import com.fsdetc.fsdstudio.data.CharacterSource
import com.fsdetc.fsdstudio.data.ImageSaver
import com.fsdetc.fsdstudio.data.OutfitEntry
import com.fsdetc.fsdstudio.data.Plan
import com.fsdetc.fsdstudio.data.SkinAsset
import com.fsdetc.fsdstudio.data.SkinBitmaps
import com.fsdetc.fsdstudio.data.SkinLayer
import com.fsdetc.fsdstudio.data.SkinPart
import com.fsdetc.fsdstudio.data.SkinTemplate
import com.fsdetc.fsdstudio.data.WardrobeEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.ArrayDeque

/** Curated paint palette: FSD purples, skin tones, then the working basics. */
val EDITOR_PALETTE: List<Int> = listOf(
    0xFF6E4CAD, 0xFF9D6BFF, 0xFF5B38A3, 0xFF22123F, 0xFF160A2A,
    0xFFF1C27D, 0xFFE0AC69, 0xFFC68642, 0xFF8D5524, 0xFF5C3317,
    0xFFFFFFFF, 0xFFD9D9D9, 0xFF9E9E9E, 0xFF555555, 0xFF111111,
    0xFFD0342C, 0xFFE07B2A, 0xFFF0B41E, 0xFF3FA34D, 0xFF2AA198,
    0xFF2E7FC2, 0xFF274690, 0xFFF28BB4, 0xFF7A4A21,
).map { it.toInt() }

/** One-tap tint choices for tintable assets (also the random-outfit tint pool). */
val EDITOR_TINTS: List<Int> = listOf(
    0xFFD0342C, 0xFFE07B2A, 0xFFF0B41E, 0xFF3FA34D, 0xFF2AA198,
    0xFF2E7FC2, 0xFF7A4FD0, 0xFFF28BB4, 0xFF8D5524, 0xFF30303A,
).map { it.toInt() }

/**
 * State and operations for the Skin Editor.
 *
 * The model is deliberately non-destructive: [paint] is the user's own 64×64
 * painting, the worn premade assets live separately in [outfit] (one per
 * slot), and every preview/export composites the two on demand. Painting is
 * ALWAYS free — no editor operation here consults the plan except the single
 * asset-lock check in [assetLocked].
 */
class SkinEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FsdStudioApp
    private val container = app.container
    private val assetsRepo = container.skinAssets

    val assets = MutableStateFlow<List<SkinAsset>>(emptyList())
    val initialized = MutableStateFlow(false)
    val loading = MutableStateFlow(false)
    val loadFailed = MutableStateFlow(false)

    /** Bumped on every paint change — previews key their renders off this. */
    val paintRevision = MutableStateFlow(0)
    val outfit = MutableStateFlow<Map<AssetSlot, OutfitEntry>>(emptyMap())

    val paintColor = MutableStateFlow(EDITOR_PALETTE[0])
    val recentColors = MutableStateFlow<List<Int>>(emptyList())

    val undoCount = MutableStateFlow(0)
    val redoCount = MutableStateFlow(0)

    val wardrobe = MutableStateFlow<List<WardrobeEntry>>(emptyList())

    private var paint: Bitmap? = null
    private val undoStack = ArrayDeque<Bitmap>()
    private val redoStack = ArrayDeque<Bitmap>()

    private var compositeKey: String? = null
    private var compositeCache: Bitmap? = null
    private val cellCache = HashMap<String, Bitmap>()

    init {
        viewModelScope.launch {
            runCatching { assetsRepo.loadCatalog() }
            assets.value = assetsRepo.catalogOrEmpty()
            refreshWardrobe()
        }
    }

    fun paintBitmap(): Bitmap? = paint

    // ------------------------------------------------------------ sources

    /** Auto-start from the current character when the editor first opens. */
    fun bootstrap(character: CharacterInfo?) {
        if (initialized.value || loading.value) return
        if (character == null || !isResolvable(character)) return
        loadFromCharacter(character)
    }

    fun isResolvable(character: CharacterInfo): Boolean = when (character.source) {
        CharacterSource.UPLOAD -> character.localSkinPath != null
        CharacterSource.USERNAME -> character.skinUrl != null || character.renderId != null
    }

    fun loadFromCharacter(character: CharacterInfo) {
        loading.value = true
        loadFailed.value = false
        viewModelScope.launch {
            val bmp = runCatching {
                when (character.source) {
                    CharacterSource.UPLOAD -> character.localSkinPath?.let {
                        SkinBitmaps.decodeSkinFile(File(it))
                    }
                    CharacterSource.USERNAME -> {
                        val url = character.skinUrl
                            ?: character.renderId?.let { Api.renderUrl(it, "skin", 64) }
                        url?.let { SkinBitmaps.decodeSkinBytes(container.skinRepository.downloadBytes(it)) }
                    }
                }
            }.getOrNull()
            loading.value = false
            if (bmp != null) setPaint(bmp) else loadFailed.value = true
        }
    }

    fun loadUpload(uri: Uri) {
        loading.value = true
        loadFailed.value = false
        viewModelScope.launch {
            val bmp = runCatching {
                val bytes = withContext(Dispatchers.IO) {
                    app.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: error("unreadable")
                SkinBitmaps.decodeSkinBytes(bytes)
            }.getOrNull()
            loading.value = false
            if (bmp != null) setPaint(bmp) else loadFailed.value = true
        }
    }

    fun loadBlank(kind: BlankSkins.Kind) {
        setPaint(BlankSkins.create(kind))
    }

    /** Replace the whole painting (new source / wardrobe load). Outfit + history reset. */
    fun setPaint(bmp: Bitmap) {
        paint = bmp.copy(Bitmap.Config.ARGB_8888, true)
        outfit.value = emptyMap()
        undoStack.clear()
        redoStack.clear()
        syncHistoryCounts()
        initialized.value = true
        loadFailed.value = false
        touch()
    }

    // ------------------------------------------------------------ painting

    /** Snapshot before a stroke/fill/clear so one gesture = one undo step. */
    fun beginStroke() {
        val p = paint ?: return
        undoStack.addLast(p.copy(Bitmap.Config.ARGB_8888, false))
        while (undoStack.size > MAX_UNDO) undoStack.removeFirst()
        redoStack.clear()
        syncHistoryCounts()
    }

    fun undo() {
        val p = paint ?: return
        val prev = undoStack.pollLast() ?: return
        redoStack.addLast(p.copy(Bitmap.Config.ARGB_8888, false))
        paint = prev
        syncHistoryCounts()
        touch()
    }

    fun redo() {
        val p = paint ?: return
        val next = redoStack.pollLast() ?: return
        undoStack.addLast(p.copy(Bitmap.Config.ARGB_8888, false))
        paint = next
        syncHistoryCounts()
        touch()
    }

    private fun syncHistoryCounts() {
        undoCount.value = undoStack.size
        redoCount.value = redoStack.size
    }

    /** Paint (or erase) one pixel, honouring the layer mask. Returns true if it changed. */
    fun paintDot(x: Int, y: Int, layer: SkinLayer, argb: Int, eraser: Boolean): Boolean {
        val p = paint ?: return false
        if (x !in 0 until 64 || y !in 0 until 64) return false
        if (!SkinTemplate.maskFor(layer)[y * 64 + x]) return false
        val target = if (eraser) Color.TRANSPARENT else argb
        if (p.getPixel(x, y) == target) return false
        p.setPixel(x, y, target)
        touch()
        return true
    }

    /** Paint a straight run of pixels (drag strokes stay continuous). */
    fun paintLine(x0: Int, y0: Int, x1: Int, y1: Int, layer: SkinLayer, argb: Int, eraser: Boolean) {
        val dx = kotlin.math.abs(x1 - x0)
        val dy = kotlin.math.abs(y1 - y0)
        val steps = maxOf(dx, dy)
        if (steps == 0) {
            paintDot(x1, y1, layer, argb, eraser)
            return
        }
        for (i in 0..steps) {
            val x = x0 + (x1 - x0) * i / steps
            val y = y0 + (y1 - y0) * i / steps
            paintDot(x, y, layer, argb, eraser)
        }
    }

    /** 4-connected exact-colour flood fill, constrained to the layer mask. */
    fun floodFill(x: Int, y: Int, layer: SkinLayer, argb: Int): Boolean {
        val p = paint ?: return false
        if (x !in 0 until 64 || y !in 0 until 64) return false
        val mask = SkinTemplate.maskFor(layer)
        if (!mask[y * 64 + x]) return false
        val pixels = IntArray(64 * 64)
        p.getPixels(pixels, 0, 64, 0, 0, 64, 64)
        val target = pixels[y * 64 + x]
        if (target == argb) return false
        beginStroke()
        val stack = ArrayDeque<Int>()
        stack.addLast(y * 64 + x)
        while (!stack.isEmpty()) {
            val idx = stack.removeLast()
            if (pixels[idx] != target || !mask[idx]) continue
            pixels[idx] = argb
            val px = idx % 64
            val py = idx / 64
            if (px > 0) stack.addLast(idx - 1)
            if (px < 63) stack.addLast(idx + 1)
            if (py > 0) stack.addLast(idx - 64)
            if (py < 63) stack.addLast(idx + 64)
        }
        p.setPixels(pixels, 0, 64, 0, 0, 64, 64)
        touch()
        return true
    }

    /** Eyedropper reads the PAINT bitmap (not the outfit composite). */
    fun pickColor(x: Int, y: Int): Int? {
        val p = paint ?: return null
        if (x !in 0 until 64 || y !in 0 until 64) return null
        return p.getPixel(x, y)
    }

    fun noteColorUsed(argb: Int) {
        if (Color.alpha(argb) == 0) return
        val list = recentColors.value.toMutableList()
        list.remove(argb)
        list.add(0, argb)
        recentColors.value = list.take(12)
    }

    fun setColor(argb: Int) {
        paintColor.value = argb
    }

    /**
     * Clear-part: base regions are reset to the blank template's pixels,
     * overlay regions to transparent. [parts] empty + both layers = whole skin.
     */
    fun clearRegions(parts: Set<SkinPart>, layers: Set<SkinLayer>) {
        val p = paint ?: return
        beginStroke()
        val blank = BlankSkins.create(BlankSkins.Kind.SKIN_TONE)
        for (part in parts) {
            for (layer in layers) {
                for (rect in SkinTemplate.faceRects(part, layer)) {
                    for (y in rect.y until rect.y + rect.h) {
                        for (x in rect.x until rect.x + rect.w) {
                            p.setPixel(
                                x, y,
                                if (layer == SkinLayer.BASE) blank.getPixel(x, y)
                                else Color.TRANSPARENT,
                            )
                        }
                    }
                }
            }
        }
        touch()
    }

    fun clearWholeSkin() {
        paint ?: return
        beginStroke()
        paint = BlankSkins.create(BlankSkins.Kind.SKIN_TONE)
        touch()
    }

    // ------------------------------------------------------------ outfit

    /** The ONLY plan check in the editor: premade assets marked non-free. */
    fun assetLocked(asset: SkinAsset, plan: Plan): Boolean = !asset.free && plan == Plan.FREE

    /** Apply an asset to its slot (replacing the slot's previous asset). */
    fun applyAsset(asset: SkinAsset) {
        val current = outfit.value.toMutableMap()
        val existing = current[asset.slot]
        current[asset.slot] = OutfitEntry(
            assetId = asset.id,
            tint = if (existing?.assetId == asset.id) existing.tint else null,
        )
        outfit.value = current
        touch()
    }

    fun removeSlot(slot: AssetSlot) {
        outfit.value = outfit.value - slot
        touch()
    }

    fun setTint(slot: AssetSlot, tint: Int?) {
        val entry = outfit.value[slot] ?: return
        outfit.value = outfit.value + (slot to entry.copy(tint = tint))
        touch()
    }

    /** One tap, a full random outfit — plan locks respected (free users get free assets). */
    fun randomOutfit(plan: Plan) {
        val all = assets.value
        if (all.isEmpty()) return
        val next = mutableMapOf<AssetSlot, OutfitEntry>()
        for (slot in AssetSlots.ORDER) {
            val pool = all.filter { it.slot == slot && !assetLocked(it, plan) }
            if (pool.isEmpty()) continue
            // Leave the glasses slot empty now and then — not every look needs them.
            if (slot == AssetSlot.GLASSES && (0..2).random() == 0) continue
            val pick = pool.random()
            val tint = if (pick.tintable && (0..9).random() < 7) EDITOR_TINTS.random() else null
            next[slot] = OutfitEntry(pick.id, tint)
        }
        outfit.value = next
        touch()
    }

    // ------------------------------------------------------------ composites

    private fun outfitKey(): String = outfit.value.entries
        .sortedBy { it.key.ordinal }
        .joinToString("|") { "${it.key.ordinal}=${it.value.assetId}:${it.value.tint}" }

    /** The final skin: paint + outfit in slot order. Cached per revision+outfit. */
    fun compositeBitmap(): Bitmap? {
        val p = paint ?: return null
        val key = "${paintRevision.value}#${outfitKey()}"
        if (compositeKey == key) return compositeCache
        val out = assetsRepo.composite(p, outfit.value)
        compositeKey = key
        compositeCache = out
        return out
    }

    /**
     * Small front-view preview of [candidate] worn over the current painting
     * (its slot's current occupant is replaced), for the asset grid cells.
     */
    fun cellPreview(candidate: SkinAsset): Bitmap? {
        val p = paint ?: return null
        val tentative = outfit.value.toMutableMap()
        val existing = tentative[candidate.slot]
        tentative[candidate.slot] = OutfitEntry(
            candidate.id,
            if (existing?.assetId == candidate.id) existing.tint else null,
        )
        val key = "${paintRevision.value}#${candidate.id}#" +
            tentative.entries.sortedBy { it.key.ordinal }
                .joinToString("|") { "${it.key.ordinal}=${it.value.assetId}:${it.value.tint}" }
        cellCache[key]?.let { return it }
        val composite = assetsRepo.composite(p, tentative)
        val front = SkinBitmaps.renderFrontScaled(composite, CELL_SCALE)
        if (cellCache.size > 90) cellCache.clear()
        cellCache[key] = front
        return front
    }

    // ------------------------------------------------------------ actions

    /** Save the composited skin as the current character (an upload-style skin). */
    fun useAsMySkin(onDone: () -> Unit) {
        val composite = compositeBitmap() ?: return
        viewModelScope.launch {
            val info = SkinBitmaps.importEditedSkin(app, composite, "Edited skin")
            container.characterStore.save(info)
            onDone()
        }
    }

    /**
     * Download the composited 64×64 skin PNG to the gallery. FREE by the
     * owner's rule (making your own skin is core) — this deliberately does NOT
     * go through DownloadGate. The save IS recorded in History as kind "SKIN".
     */
    fun downloadSkinPng(onResult: (Boolean) -> Unit) {
        val composite = compositeBitmap() ?: return onResult(false)
        viewModelScope.launch {
            val bytes = SkinBitmaps.bitmapToPngBytes(composite)
            val dir = File(app.cacheDir, "shared").apply { mkdirs() }
            val tmp = File(dir, "skin_${System.currentTimeMillis()}.png")
            tmp.writeBytes(bytes)
            val ok = ImageSaver.saveToGallery(app, tmp, "FSD_Skin.png")
            if (ok) {
                runCatching {
                    container.historyRepository.recordBytes(
                        bytes,
                        kind = "SKIN",
                        title = "Edited skin",
                        subtitle = "Skin Editor",
                        sourceName = "Skin Editor",
                        uniqueKey = "skin:" + SkinBitmaps.sha256Hex(bytes),
                    )
                }
            }
            onResult(ok)
        }
    }

    // ------------------------------------------------------------ wardrobe

    fun refreshWardrobe() {
        viewModelScope.launch { wardrobe.value = container.wardrobe.list() }
    }

    fun saveToWardrobe(name: String) {
        val composite = compositeBitmap() ?: return
        viewModelScope.launch {
            container.wardrobe.save(name, composite)
            refreshWardrobe()
        }
    }

    fun loadWardrobe(entry: WardrobeEntry) {
        val bmp = container.wardrobe.loadBitmap(entry) ?: return
        if (bmp.width == 64 && bmp.height == 64) setPaint(bmp)
    }

    /** Small front-view thumbnail of a wardrobe entry (null if unreadable). */
    fun wardrobeThumb(entry: WardrobeEntry): Bitmap? {
        val bmp = container.wardrobe.loadBitmap(entry) ?: return null
        if (bmp.width != 64 || bmp.height != 64) return null
        return SkinBitmaps.renderFrontScaled(bmp, 4)
    }

    fun deleteWardrobe(entry: WardrobeEntry) {
        viewModelScope.launch {
            container.wardrobe.delete(entry.id)
            refreshWardrobe()
        }
    }

    private fun touch() {
        paintRevision.value += 1
    }

    companion object {
        private const val MAX_UNDO = 40
        private const val CELL_SCALE = 5
    }
}
