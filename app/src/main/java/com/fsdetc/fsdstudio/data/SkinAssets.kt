package com.fsdetc.fsdstudio.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import kotlin.math.abs

/**
 * Outfit slots. Each slot holds at most ONE premade asset at a time; applying a
 * second asset to the same slot replaces the first (the asset library's own
 * compositing rules — see the generator's NOTES.md).
 */
enum class AssetSlot { PANTS, SHOES, TOP, JEWELRY, BACK, HEAD, GLASSES }

object AssetSlots {
    /** Fixed compositing order: later slots draw OVER earlier ones. */
    val ORDER: List<AssetSlot> = listOf(
        AssetSlot.PANTS,
        AssetSlot.SHOES,
        AssetSlot.TOP,
        AssetSlot.JEWELRY,
        AssetSlot.BACK,
        AssetSlot.HEAD,
        AssetSlot.GLASSES,
    )

    /**
     * Slot for a catalog entry. Hats and masks share the HEAD slot; wings, bags,
     * capes (and the seasonal vampire cape) share the BACK slot; the other
     * seasonal pieces behave as their garment kind.
     */
    fun slotFor(section: String, id: String): AssetSlot = when (section) {
        "pants" -> AssetSlot.PANTS
        "shoes" -> AssetSlot.SHOES
        "tops" -> AssetSlot.TOP
        "jewelry" -> AssetSlot.JEWELRY
        "wings", "bags", "capes" -> AssetSlot.BACK
        "hats", "masks" -> AssetSlot.HEAD
        "glasses" -> AssetSlot.GLASSES
        "seasonal" -> when (id) {
            "vampire_cape" -> AssetSlot.BACK
            "santa_coat", "christmas_sweater" -> AssetSlot.TOP
            else -> AssetSlot.HEAD // pumpkin_hat, bunny_ears
        }
        else -> AssetSlot.TOP
    }

    /** Section ids in browser display order ("all" is a UI-only filter). */
    val SECTIONS: List<String> = listOf(
        "hats", "glasses", "masks", "tops", "pants", "shoes",
        "wings", "bags", "capes", "jewelry", "seasonal",
    )
}

/** One premade asset from the bundled catalog (assets/skin_assets/catalog.json). */
data class SkinAsset(
    val id: String,
    val section: String,
    val name: String,
    val free: Boolean,
    val tintable: Boolean,
    /** ARGB of the asset's primary colour family (the BASE shade). */
    val defaultColor: Int,
    val patchFile: String,
) {
    val slot: AssetSlot get() = AssetSlots.slotFor(section, id)
}

/** A worn asset: which one, and an optional user tint (ARGB base colour). */
data class OutfitEntry(val assetId: String, val tint: Int? = null)

/**
 * Loads and caches the bundled premade-asset library and everything derived
 * from it: patch bitmaps, per-tint recolours and full outfit composites.
 */
class SkinAssetRepository(private val context: Context) {

    @Volatile
    private var catalog: List<SkinAsset>? = null

    private val patchCache = LruCache<String, Bitmap>(160)
    private val tintCache = LruCache<String, Bitmap>(80)

    suspend fun loadCatalog(): List<SkinAsset> = withContext(Dispatchers.IO) {
        catalog?.let { return@withContext it }
        val text = context.assets.open("skin_assets/catalog.json")
            .bufferedReader().use { it.readText() }
        val arr = JSONArray(text)
        val out = ArrayList<SkinAsset>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += SkinAsset(
                id = o.getString("id"),
                section = o.getString("section"),
                name = o.getString("name"),
                free = o.getBoolean("free"),
                tintable = o.getBoolean("tintable"),
                defaultColor = runCatching { Color.parseColor(o.getString("defaultColor")) }
                    .getOrDefault(Color.MAGENTA),
                patchFile = o.getString("patchFile"),
            )
        }
        catalog = out
        out
    }

    fun catalogOrEmpty(): List<SkinAsset> = catalog ?: emptyList()

    fun assetById(id: String): SkinAsset? = catalog?.firstOrNull { it.id == id }

    /** The asset's raw patch (64×64, transparent except its own pixels). */
    fun patch(asset: SkinAsset): Bitmap? {
        patchCache.get(asset.id)?.let { return it }
        val bmp = runCatching {
            context.assets.open("skin_assets/${asset.patchFile}").use {
                BitmapFactory.decodeStream(it)
            }
        }.getOrNull() ?: return null
        patchCache.put(asset.id, bmp)
        return bmp
    }

    /**
     * The asset's patch recoloured to [tintArgb]: every pixel whose hue sits
     * within ±25° of the asset's default-colour hue (and is saturated enough to
     * be part of the primary family) is rotated to the tint's hue, with its
     * saturation/value scaled by the tint's ratio to the default — exactly the
     * generator's family-remap rule, so neutral details (blacks, whites, gold
     * thread, fur) never shift.
     */
    fun tintedPatch(asset: SkinAsset, tintArgb: Int): Bitmap? {
        val key = "${asset.id}@$tintArgb"
        tintCache.get(key)?.let { return it }
        val src = patch(asset) ?: return null
        val out = tintBitmap(src, asset.defaultColor, tintArgb)
        tintCache.put(key, out)
        return out
    }

    fun patchFor(asset: SkinAsset, tint: Int?): Bitmap? =
        if (tint == null || !asset.tintable) patch(asset) else tintedPatch(asset, tint)

    /**
     * Composite [base] + the outfit's patches in slot order. [base] is never
     * mutated; the result is a fresh 64×64 bitmap.
     */
    fun composite(base: Bitmap, outfit: Map<AssetSlot, OutfitEntry>): Bitmap {
        val out = base.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        for (slot in AssetSlots.ORDER) {
            val entry = outfit[slot] ?: continue
            val asset = assetById(entry.assetId) ?: continue
            val patchBmp = patchFor(asset, entry.tint) ?: continue
            canvas.drawBitmap(patchBmp, 0f, 0f, null)
        }
        return out
    }

    companion object {
        /** Pure tint remap (also used to preview tints without the repository). */
        fun tintBitmap(src: Bitmap, defaultArgb: Int, tintArgb: Int): Bitmap {
            val out = src.copy(Bitmap.Config.ARGB_8888, true)
            val dh = FloatArray(3).also {
                Color.RGBToHSV(Color.red(defaultArgb), Color.green(defaultArgb), Color.blue(defaultArgb), it)
            }
            val th = FloatArray(3).also {
                Color.RGBToHSV(Color.red(tintArgb), Color.green(tintArgb), Color.blue(tintArgb), it)
            }
            val sRatio = if (dh[1] > 0.01f) th[1] / dh[1] else 1f
            val vRatio = if (dh[2] > 0.01f) th[2] / dh[2] else 1f
            val pixels = IntArray(src.width * src.height)
            out.getPixels(pixels, 0, src.width, 0, 0, src.width, src.height)
            val hsv = FloatArray(3)
            for (i in pixels.indices) {
                val px = pixels[i]
                if (Color.alpha(px) == 0) continue
                Color.RGBToHSV(Color.red(px), Color.green(px), Color.blue(px), hsv)
                if (hsv[1] <= 0.15f) continue // neutral detail — never shifts
                var diff = abs(hsv[0] - dh[0]) % 360f
                if (diff > 180f) diff = 360f - diff
                if (diff > 25f) continue // not part of the primary family
                hsv[0] = th[0]
                hsv[1] = (hsv[1] * sRatio).coerceIn(0f, 1f)
                hsv[2] = (hsv[2] * vRatio).coerceIn(0f, 1f)
                pixels[i] = Color.HSVToColor(Color.alpha(px), hsv)
            }
            out.setPixels(pixels, 0, src.width, 0, 0, src.width, src.height)
            return out
        }
    }
}

/**
 * The two programmatic blank skins the editor can start from. Both are painted
 * with the template's face map (top lit, front base, sides/back shaded) so they
 * read as proper characters and behave correctly when painted over.
 */
object BlankSkins {

    enum class Kind { SKIN_TONE, MANNEQUIN }

    fun create(kind: Kind): Bitmap = when (kind) {
        Kind.SKIN_TONE -> skinTone()
        Kind.MANNEQUIN -> mannequin()
    }

    /** Face shading multipliers per face index (top, bottom, right, front, left, back). */
    private val SHADE = floatArrayOf(1.10f, 0.55f, 0.80f, 1.00f, 0.88f, 0.70f)

    private fun shade(argb: Int, factor: Float): Int {
        fun ch(v: Int) = (v * factor).toInt().coerceIn(0, 255)
        return Color.rgb(ch(Color.red(argb)), ch(Color.green(argb)), ch(Color.blue(argb)))
    }

    /** Fill every face of [part]'s base layer with [base] shaded per face; overlay stays transparent. */
    private fun paintPart(bmp: Bitmap, part: SkinPart, base: Int) {
        for (face in 0..5) {
            val rect = SkinTemplate.faceRect(part, SkinLayer.BASE, face)
            val color = shade(base, SHADE[face])
            for (y in rect.y until rect.y + rect.h) {
                for (x in rect.x until rect.x + rect.w) {
                    bmp.setPixel(x, y, color)
                }
            }
        }
    }

    private fun fillRect(bmp: Bitmap, rect: PxRect, argb: Int) {
        for (y in rect.y until rect.y + rect.h) {
            for (x in rect.x until rect.x + rect.w) {
                bmp.setPixel(x, y, argb)
            }
        }
    }

    /** Simple face features on the head's front face: [eyes] colour, [mouth] colour. */
    private fun paintFace(bmp: Bitmap, eyes: Int, mouth: Int, pupils: Int? = null) {
        val front = SkinTemplate.faceRect(SkinPart.HEAD, SkinLayer.BASE, SkinTemplate.FACE_FRONT)
        fun px(lx: Int, ly: Int, c: Int) = bmp.setPixel(front.x + lx, front.y + ly, c)
        // Eyes: two 2-px eyes on row 4; optional white+pupil split.
        if (pupils != null) {
            px(1, 4, pupils); px(2, 4, eyes)
            px(5, 4, eyes); px(6, 4, pupils)
        } else {
            px(1, 4, eyes); px(2, 4, eyes)
            px(5, 4, eyes); px(6, 4, eyes)
        }
        // Mouth: short line on row 6.
        px(3, 6, mouth); px(4, 6, mouth)
    }

    /** A starter character: warm skin tones, simple hair and shorts shading. */
    private fun skinTone(): Bitmap {
        val bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val skin = Color.rgb(0xC6, 0x88, 0x5A)
        val skinDark = Color.rgb(0x9C, 0x66, 0x3E)
        paintPart(bmp, SkinPart.HEAD, skin)
        paintPart(bmp, SkinPart.TORSO, skin)
        paintPart(bmp, SkinPart.ARM_RIGHT, skin)
        paintPart(bmp, SkinPart.ARM_LEFT, skin)
        paintPart(bmp, SkinPart.LEG_RIGHT, skin)
        paintPart(bmp, SkinPart.LEG_LEFT, skin)
        // Hair: top rows of the head (top face + upper rows of every side face).
        val hair = Color.rgb(0x4A, 0x2E, 0x1B)
        val headTop = SkinTemplate.faceRect(SkinPart.HEAD, SkinLayer.BASE, SkinTemplate.FACE_TOP)
        fillRect(bmp, headTop, shade(hair, 1.1f))
        for (face in intArrayOf(
            SkinTemplate.FACE_FRONT, SkinTemplate.FACE_BACK,
            SkinTemplate.FACE_LEFT, SkinTemplate.FACE_RIGHT,
        )) {
            val r = SkinTemplate.faceRect(SkinPart.HEAD, SkinLayer.BASE, face)
            fillRect(bmp, PxRect(r.x, r.y, r.w, 2), shade(hair, SHADE[face]))
            if (face == SkinTemplate.FACE_FRONT) {
                // Side wisps framing the face.
                fillRect(bmp, PxRect(r.x, r.y + 2, 1, 2), shade(hair, SHADE[face]))
                fillRect(bmp, PxRect(r.x + r.w - 1, r.y + 2, 1, 2), shade(hair, SHADE[face]))
            }
        }
        // Shorts: upper rows of both legs in a deep denim.
        val shorts = Color.rgb(0x2E, 0x44, 0x6E)
        for (leg in listOf(SkinPart.LEG_RIGHT, SkinPart.LEG_LEFT)) {
            for (face in 0..5) {
                val r = SkinTemplate.faceRect(leg, SkinLayer.BASE, face)
                if (face == SkinTemplate.FACE_TOP || face == SkinTemplate.FACE_BOTTOM) continue
                fillRect(bmp, PxRect(r.x, r.y, r.w, 4), shade(shorts, SHADE[face]))
            }
        }
        paintFace(bmp, eyes = Color.WHITE, mouth = skinDark, pupils = Color.rgb(0x2A, 0x1C, 0x12))
        return bmp
    }

    /** The neutral warm-gray mannequin (same tones as the asset library previews). */
    private fun mannequin(): Bitmap {
        val bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        paintPart(bmp, SkinPart.HEAD, Color.rgb(0xD9, 0xCF, 0xC0))
        paintPart(bmp, SkinPart.TORSO, Color.rgb(0xC9, 0xC9, 0xC9))
        paintPart(bmp, SkinPart.ARM_RIGHT, Color.rgb(0xC9, 0xC9, 0xC9))
        paintPart(bmp, SkinPart.ARM_LEFT, Color.rgb(0xC9, 0xC9, 0xC9))
        paintPart(bmp, SkinPart.LEG_RIGHT, Color.rgb(0xB2, 0xB2, 0xB2))
        paintPart(bmp, SkinPart.LEG_LEFT, Color.rgb(0xB2, 0xB2, 0xB2))
        paintFace(bmp, eyes = Color.rgb(0x3A, 0x32, 0x30), mouth = Color.rgb(0x8A, 0x7E, 0x74))
        return bmp
    }
}
