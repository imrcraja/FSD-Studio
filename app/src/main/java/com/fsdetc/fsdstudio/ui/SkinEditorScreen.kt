package com.fsdetc.fsdstudio.ui

import android.graphics.Color as AndroidColor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.fsdetc.fsdstudio.R
import com.fsdetc.fsdstudio.data.AssetSlot
import com.fsdetc.fsdstudio.data.AssetSlots
import com.fsdetc.fsdstudio.data.BlankSkins
import com.fsdetc.fsdstudio.data.SkinAsset
import com.fsdetc.fsdstudio.data.SkinBitmaps
import com.fsdetc.fsdstudio.data.SkinLayer
import com.fsdetc.fsdstudio.data.SkinPart
import com.fsdetc.fsdstudio.data.SkinTemplate
import com.fsdetc.fsdstudio.viewmodel.EDITOR_PALETTE
import com.fsdetc.fsdstudio.viewmodel.EDITOR_TINTS
import com.fsdetc.fsdstudio.viewmodel.MainViewModel
import com.fsdetc.fsdstudio.viewmodel.SkinEditorViewModel
import kotlin.math.roundToInt

private enum class EditorTool { PENCIL, ERASER, FILL, PICKER }

private fun slotLabelRes(slot: AssetSlot): Int = when (slot) {
    AssetSlot.PANTS -> R.string.slot_pants
    AssetSlot.SHOES -> R.string.slot_shoes
    AssetSlot.TOP -> R.string.slot_top
    AssetSlot.JEWELRY -> R.string.slot_jewelry
    AssetSlot.BACK -> R.string.slot_back
    AssetSlot.HEAD -> R.string.slot_head
    AssetSlot.GLASSES -> R.string.slot_glasses
}

private fun sectionLabelRes(section: String): Int = when (section) {
    "hats" -> R.string.section_hats
    "glasses" -> R.string.section_glasses
    "masks" -> R.string.section_masks
    "tops" -> R.string.section_tops
    "pants" -> R.string.section_pants
    "shoes" -> R.string.section_shoes
    "wings" -> R.string.section_wings
    "bags" -> R.string.section_bags
    "capes" -> R.string.section_capes
    "jewelry" -> R.string.section_jewelry
    "seasonal" -> R.string.section_seasonal
    else -> R.string.section_all
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SkinEditorScreen(
    mainVm: MainViewModel,
    editorVm: SkinEditorViewModel,
    onBack: () -> Unit,
    onOpenPro: () -> Unit,
) {
    val context = LocalContext.current
    val character by mainVm.character.collectAsState()
    val plan by mainVm.plan.collectAsState()
    val assets by editorVm.assets.collectAsState()
    val initialized by editorVm.initialized.collectAsState()
    val loading by editorVm.loading.collectAsState()
    val loadFailed by editorVm.loadFailed.collectAsState()
    val rev by editorVm.paintRevision.collectAsState()
    val outfit by editorVm.outfit.collectAsState()
    val paintColor by editorVm.paintColor.collectAsState()
    val recentColors by editorVm.recentColors.collectAsState()
    val undoCount by editorVm.undoCount.collectAsState()
    val redoCount by editorVm.redoCount.collectAsState()
    val wardrobe by editorVm.wardrobe.collectAsState()

    var tool by remember { mutableStateOf(EditorTool.PENCIL) }
    var layer by remember { mutableStateOf(SkinLayer.BASE) }
    var showGrid by remember { mutableStateOf(true) }
    var showLabels by remember { mutableStateOf(false) }
    var sectionFilter by remember { mutableStateOf<String?>(null) }
    var selectedSlot by remember { mutableStateOf<AssetSlot?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showTintDialog by remember { mutableStateOf(false) }
    var pendingSource by remember { mutableStateOf<(() -> Unit)?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { editorVm.loadUpload(it) } }

    LaunchedEffect(character) { editorVm.bootstrap(character) }

    fun requestSource(action: () -> Unit) {
        if (initialized) pendingSource = action else action()
    }

    val composite = remember(rev, outfit) { editorVm.compositeBitmap() }
    // The canvas paints the raw paint bitmap so every stroke lands exactly
    // where the user sees it; the panes above show the composited result.
    val paintBmp = remember(rev) { editorVm.paintBitmap() }
    val frontRender = remember(composite) {
        composite?.let { SkinBitmaps.renderFrontScaled(it, 9) }
    }
    val backRender = remember(composite) {
        composite?.let { SkinBitmaps.renderBackScaled(it, 9) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.editor_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (loading) {
                Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(10.dp))
                        Text(stringResource(R.string.editor_loading))
                    }
                }
            }

            if (!initialized && !loading) {
                // ------------------------------------------------ start panel
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.editor_start_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val ch = character
                        if (ch != null && editorVm.isResolvable(ch)) {
                            Button(
                                onClick = { editorVm.loadFromCharacter(ch) },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(stringResource(R.string.editor_source_character) + " · " + ch.name) }
                        }
                        OutlinedButton(
                            onClick = { picker.launch(arrayOf("image/png")) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.editor_source_upload))
                        }
                        OutlinedButton(
                            onClick = { editorVm.loadBlank(BlankSkins.Kind.SKIN_TONE) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.editor_blank_tone)) }
                        OutlinedButton(
                            onClick = { editorVm.loadBlank(BlankSkins.Kind.MANNEQUIN) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.editor_blank_mannequin)) }
                        if (loadFailed) {
                            Text(
                                text = stringResource(R.string.error_load_failed),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            if (initialized && composite != null && paintBmp != null) {
                // ------------------------------------------------ preview
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PreviewPane(
                        label = stringResource(R.string.editor_front),
                        bitmap = frontRender,
                        modifier = Modifier.weight(1f),
                    )
                    PreviewPane(
                        label = stringResource(R.string.editor_back),
                        bitmap = backRender,
                        modifier = Modifier.weight(1f),
                    )
                }

                // ------------------------------------------------ sources
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val ch = character
                    if (ch != null && editorVm.isResolvable(ch)) {
                        item {
                            OutlinedButton(onClick = {
                                requestSource { editorVm.loadFromCharacter(ch) }
                            }) { Text(stringResource(R.string.editor_source_character)) }
                        }
                    }
                    item {
                        OutlinedButton(onClick = {
                            requestSource { picker.launch(arrayOf("image/png")) }
                        }) { Text(stringResource(R.string.editor_source_upload)) }
                    }
                    item {
                        OutlinedButton(onClick = {
                            requestSource { editorVm.loadBlank(BlankSkins.Kind.SKIN_TONE) }
                        }) { Text(stringResource(R.string.editor_blank_tone)) }
                    }
                    item {
                        OutlinedButton(onClick = {
                            requestSource { editorVm.loadBlank(BlankSkins.Kind.MANNEQUIN) }
                        }) { Text(stringResource(R.string.editor_blank_mannequin)) }
                    }
                }
                if (loadFailed) {
                    Text(
                        text = stringResource(R.string.error_load_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                // ------------------------------------------------ canvas + tools
                SectionTitle(stringResource(R.string.editor_tools_title))
                SkinCanvas(
                    bitmap = paintBmp,
                    showGrid = showGrid,
                    showLabels = showLabels,
                    partLabels = mapOf(
                        SkinPart.HEAD to stringResource(R.string.part_head),
                        SkinPart.TORSO to stringResource(R.string.part_torso),
                        SkinPart.ARM_RIGHT to stringResource(R.string.part_arms),
                        SkinPart.ARM_LEFT to stringResource(R.string.part_arms),
                        SkinPart.LEG_RIGHT to stringResource(R.string.part_legs),
                        SkinPart.LEG_LEFT to stringResource(R.string.part_legs),
                    ),
                    tool = tool,
                    layer = layer,
                    paintColor = paintColor,
                    editorVm = editorVm,
                    onPicked = { tool = EditorTool.PENCIL },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ToolButton(
                        icon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        label = stringResource(R.string.tool_pencil),
                        selected = tool == EditorTool.PENCIL,
                        modifier = Modifier.weight(1f),
                    ) { tool = EditorTool.PENCIL }
                    ToolButton(
                        icon = { Icon(Icons.Filled.Backspace, contentDescription = null) },
                        label = stringResource(R.string.tool_eraser),
                        selected = tool == EditorTool.ERASER,
                        modifier = Modifier.weight(1f),
                    ) { tool = EditorTool.ERASER }
                    ToolButton(
                        icon = { Icon(Icons.Filled.FormatColorFill, contentDescription = null) },
                        label = stringResource(R.string.tool_fill),
                        selected = tool == EditorTool.FILL,
                        modifier = Modifier.weight(1f),
                    ) { tool = EditorTool.FILL }
                    ToolButton(
                        icon = { Icon(Icons.Filled.Colorize, contentDescription = null) },
                        label = stringResource(R.string.tool_picker),
                        selected = tool == EditorTool.PICKER,
                        modifier = Modifier.weight(1f),
                    ) { tool = EditorTool.PICKER }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(onClick = { editorVm.undo() }, enabled = undoCount > 0) {
                        Icon(Icons.Filled.Undo, contentDescription = stringResource(R.string.editor_undo))
                    }
                    IconButton(onClick = { editorVm.redo() }, enabled = redoCount > 0) {
                        Icon(Icons.Filled.Redo, contentDescription = stringResource(R.string.editor_redo))
                    }
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(onClick = { showClearDialog = true }) {
                        Text(stringResource(R.string.editor_clear))
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = layer == SkinLayer.BASE,
                        onClick = { layer = SkinLayer.BASE },
                        label = { Text(stringResource(R.string.editor_layer_base)) },
                    )
                    FilterChip(
                        selected = layer == SkinLayer.OVERLAY,
                        onClick = { layer = SkinLayer.OVERLAY },
                        label = { Text(stringResource(R.string.editor_layer_overlay)) },
                    )
                    FilterChip(
                        selected = showGrid,
                        onClick = { showGrid = !showGrid },
                        label = { Text(stringResource(R.string.editor_grid)) },
                    )
                    FilterChip(
                        selected = showLabels,
                        onClick = { showLabels = !showLabels },
                        label = { Text(stringResource(R.string.editor_labels)) },
                    )
                }

                // ------------------------------------------------ colour
                SectionTitle(stringResource(R.string.editor_color_title))
                ColorPanel(
                    color = paintColor,
                    onColorChange = { editorVm.setColor(it) },
                    recent = recentColors,
                )

                // ------------------------------------------------ outfit
                SectionTitle(stringResource(R.string.editor_outfit_title))
                if (outfit.isEmpty()) {
                    Text(
                        text = stringResource(R.string.editor_outfit_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        outfit.entries.sortedBy { it.key.ordinal }.forEach { (slot, entry) ->
                            val asset = assets.firstOrNull { it.id == entry.assetId }
                            OutfitChip(
                                slotLabel = stringResource(slotLabelRes(slot)),
                                assetName = asset?.name ?: entry.assetId,
                                tint = entry.tint,
                                selected = selectedSlot == slot,
                                onClick = { selectedSlot = slot },
                                onRemove = {
                                    editorVm.removeSlot(slot)
                                    if (selectedSlot == slot) selectedSlot = null
                                },
                            )
                        }
                    }
                    // Tint panel for the selected, tintable outfit piece.
                    val sel = selectedSlot
                    val selEntry = sel?.let { outfit[it] }
                    val selAsset = selEntry?.let { e -> assets.firstOrNull { it.id == e.assetId } }
                    if (sel != null && selEntry != null && selAsset != null && selAsset.tintable) {
                        TintPanel(
                            currentTint = selEntry.tint,
                            onTint = { editorVm.setTint(sel, it) },
                            onCustom = { showTintDialog = true },
                        )
                    }
                }

                // ------------------------------------------------ assets
                SectionTitle(stringResource(R.string.editor_assets_title))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = sectionFilter == null,
                            onClick = { sectionFilter = null },
                            label = { Text(stringResource(R.string.section_all)) },
                        )
                    }
                    items(AssetSlots.SECTIONS, key = { it }) { section ->
                        FilterChip(
                            selected = sectionFilter == section,
                            onClick = { sectionFilter = section },
                            label = { Text(stringResource(sectionLabelRes(section))) },
                        )
                    }
                }
                OutlinedButton(
                    onClick = { editorVm.randomOutfit(plan) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.editor_random))
                }
                val shownAssets = remember(assets, sectionFilter) {
                    if (sectionFilter == null) assets else assets.filter { it.section == sectionFilter }
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxWidth().height(400.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(shownAssets, key = { it.id }) { asset ->
                        AssetCell(
                            asset = asset,
                            preview = remember(asset.id, rev, outfit) {
                                editorVm.cellPreview(asset)
                            },
                            locked = editorVm.assetLocked(asset, plan),
                            applied = outfit.values.any { it.assetId == asset.id },
                            onClick = {
                                if (editorVm.assetLocked(asset, plan)) {
                                    onOpenPro()
                                } else {
                                    editorVm.applyAsset(asset)
                                    selectedSlot = asset.slot
                                }
                            },
                        )
                    }
                }

                // ------------------------------------------------ actions
                Button(
                    onClick = {
                        editorVm.useAsMySkin {
                            mainVm.postSnackbar(context.getString(R.string.editor_set_done))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.editor_use_as_skin)) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            editorVm.downloadSkinPng { ok ->
                                mainVm.postSnackbar(
                                    context.getString(
                                        if (ok) R.string.saved_to_gallery else R.string.saved_failed,
                                    ),
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.editor_download_skin))
                    }
                    OutlinedButton(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.editor_save_wardrobe))
                    }
                }

                // ------------------------------------------------ wardrobe
                SectionTitle(stringResource(R.string.editor_wardrobe_title))
                if (wardrobe.isEmpty()) {
                    Text(
                        text = stringResource(R.string.editor_wardrobe_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    wardrobe.forEach { entry ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val thumb = remember(entry.id) { editorVm.wardrobeThumb(entry) }
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (thumb != null) {
                                        Image(
                                            bitmap = thumb.asImageBitmap(),
                                            contentDescription = entry.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit,
                                            filterQuality = FilterQuality.None,
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = entry.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                TextButton(onClick = {
                                    requestSource {
                                        editorVm.loadWardrobe(entry)
                                        mainVm.postSnackbar(
                                            context.getString(R.string.editor_loaded_wardrobe),
                                        )
                                    }
                                }) { Text(stringResource(R.string.editor_load)) }
                                IconButton(onClick = {
                                    editorVm.deleteWardrobe(entry)
                                    mainVm.postSnackbar(context.getString(R.string.editor_deleted))
                                }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.action_delete),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    // ------------------------------------------------------------ dialogs

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.editor_clear_title)) },
            text = {
                Column {
                    ClearOption(stringResource(R.string.part_head)) {
                        editorVm.clearRegions(setOf(SkinPart.HEAD), SkinLayer.entries.toSet())
                        showClearDialog = false
                    }
                    ClearOption(stringResource(R.string.part_torso)) {
                        editorVm.clearRegions(setOf(SkinPart.TORSO), SkinLayer.entries.toSet())
                        showClearDialog = false
                    }
                    ClearOption(stringResource(R.string.part_arms)) {
                        editorVm.clearRegions(
                            setOf(SkinPart.ARM_RIGHT, SkinPart.ARM_LEFT),
                            SkinLayer.entries.toSet(),
                        )
                        showClearDialog = false
                    }
                    ClearOption(stringResource(R.string.part_legs)) {
                        editorVm.clearRegions(
                            setOf(SkinPart.LEG_RIGHT, SkinPart.LEG_LEFT),
                            SkinLayer.entries.toSet(),
                        )
                        showClearDialog = false
                    }
                    ClearOption(stringResource(R.string.part_overlay)) {
                        editorVm.clearRegions(SkinPart.entries.toSet(), setOf(SkinLayer.OVERLAY))
                        showClearDialog = false
                    }
                    ClearOption(stringResource(R.string.part_all)) {
                        editorVm.clearWholeSkin()
                        showClearDialog = false
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showSaveDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text(stringResource(R.string.editor_save_name_title)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.editor_name_hint)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            editorVm.saveToWardrobe(name.trim())
                            mainVm.postSnackbar(context.getString(R.string.editor_saved_wardrobe))
                        }
                        showSaveDialog = false
                    },
                ) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (pendingSource != null) {
        AlertDialog(
            onDismissRequest = { pendingSource = null },
            title = { Text(stringResource(R.string.editor_switch_title)) },
            text = { Text(stringResource(R.string.editor_switch_body)) },
            confirmButton = {
                TextButton(onClick = {
                    val action = pendingSource
                    pendingSource = null
                    action?.invoke()
                }) { Text(stringResource(R.string.editor_switch_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingSource = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    run {
        val sel = selectedSlot
        val entry = sel?.let { outfit[it] }
        val asset = entry?.let { e -> assets.firstOrNull { it.id == e.assetId } }
        if (showTintDialog && sel != null && entry != null && asset != null) {
            var draft by remember(asset.id) {
                mutableStateOf(entry.tint ?: asset.defaultColor)
            }
            AlertDialog(
                onDismissRequest = { showTintDialog = false },
                title = { Text(stringResource(R.string.editor_tint_custom)) },
                text = {
                    HsvPicker(color = draft, onColorChange = { draft = it })
                },
                confirmButton = {
                    TextButton(onClick = {
                        editorVm.setTint(sel, draft)
                        showTintDialog = false
                    }) { Text(stringResource(R.string.action_save)) }
                },
                dismissButton = {
                    TextButton(onClick = { showTintDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
            )
        }
    }
}

@Composable
private fun ClearOption(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PreviewPane(label: String, bitmap: android.graphics.Bitmap?, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Checkerboard(Modifier.fillMaxSize()) {}
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.None,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ToolButton(
    icon: @Composable () -> Unit,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            icon()
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** HSV sliders + hex field bound to one ARGB colour. */
@Composable
private fun HsvPicker(color: Int, onColorChange: (Int) -> Unit) {
    val hsv = remember(color) {
        FloatArray(3).also {
            AndroidColor.RGBToHSV(
                AndroidColor.red(color), AndroidColor.green(color), AndroidColor.blue(color), it,
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        HsvSlider(stringResource(R.string.editor_hue), hsv[0], 360f) { v ->
            onColorChange(AndroidColor.HSVToColor(floatArrayOf(v, hsv[1], hsv[2])))
        }
        HsvSlider(stringResource(R.string.editor_saturation), hsv[1], 1f) { v ->
            onColorChange(AndroidColor.HSVToColor(floatArrayOf(hsv[0], v, hsv[2])))
        }
        HsvSlider(stringResource(R.string.editor_value), hsv[2], 1f) { v ->
            onColorChange(AndroidColor.HSVToColor(floatArrayOf(hsv[0], hsv[1], v)))
        }
    }
}

@Composable
private fun HsvSlider(label: String, value: Float, max: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(86.dp))
        Slider(value = value, onValueChange = onChange, valueRange = 0f..max, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ColorPanel(color: Int, onColorChange: (Int) -> Unit, recent: List<Int>) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(color))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
                )
                Spacer(Modifier.width(12.dp))
                var hexText by remember(color) {
                    mutableStateOf("#%06X".format(color and 0xFFFFFF))
                }
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { raw ->
                        hexText = raw
                        val cleaned = raw.trim().removePrefix("#")
                        if (cleaned.length == 6) {
                            runCatching { AndroidColor.parseColor("#$cleaned") }
                                .getOrNull()?.let { onColorChange(it) }
                        }
                    },
                    label = { Text(stringResource(R.string.editor_hex_label)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            HsvPicker(color = color, onColorChange = onColorChange)
            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                modifier = Modifier.fillMaxWidth().height(104.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(EDITOR_PALETTE, key = { it }) { swatch ->
                    ColorSwatch(
                        argb = swatch,
                        selected = swatch == color,
                        modifier = Modifier.size(30.dp),
                    ) { onColorChange(swatch) }
                }
            }
            if (recent.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.editor_recent),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(recent, key = { it }) { swatch ->
                        ColorSwatch(
                            argb = swatch,
                            selected = swatch == color,
                            modifier = Modifier.size(30.dp),
                        ) { onColorChange(swatch) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(argb: Int, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(argb))
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
    )
}

@Composable
private fun OutfitChip(
    slotLabel: String,
    assetName: String,
    tint: Int?,
    selected: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$slotLabel · $assetName",
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
            if (tint != null) {
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(tint)),
                )
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(15.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TintPanel(currentTint: Int?, onTint: (Int?) -> Unit, onCustom: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.editor_tint_title), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = currentTint == null,
                    onClick = { onTint(null) },
                    label = { Text(stringResource(R.string.editor_tint_original)) },
                )
                EDITOR_TINTS.forEach { tint ->
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(tint))
                            .border(
                                width = if (currentTint == tint) 3.dp else 1.dp,
                                color = if (currentTint == tint) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                shape = CircleShape,
                            )
                            .clickable { onTint(tint) },
                    )
                }
                FilterChip(
                    selected = false,
                    onClick = onCustom,
                    label = { Text(stringResource(R.string.editor_tint_custom)) },
                )
            }
        }
    }
}

@Composable
private fun AssetCell(
    asset: SkinAsset,
    preview: android.graphics.Bitmap?,
    locked: Boolean,
    applied: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.62f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(
                    if (applied) Modifier.border(
                        2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp),
                    ) else Modifier,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (preview != null) {
                Image(
                    bitmap = preview.asImageBitmap(),
                    contentDescription = asset.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.None,
                )
            }
            if (locked) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = asset.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!asset.free) {
            Text(
                text = stringResource(R.string.pro_badge),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * The painting surface: the composited 64×64 skin drawn large over a
 * checkerboard. One finger paints (per the active tool/layer); two fingers
 * pinch-zoom and pan. Painting edits the paint bitmap underneath the outfit —
 * the canvas shows the final composite, so what you see is the saved skin.
 */
@Composable
private fun SkinCanvas(
    bitmap: android.graphics.Bitmap,
    showGrid: Boolean,
    showLabels: Boolean,
    partLabels: Map<SkinPart, String>,
    tool: EditorTool,
    layer: SkinLayer,
    paintColor: Int,
    editorVm: SkinEditorViewModel,
    onPicked: () -> Unit,
) {
    var zoom by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }

    val toolState = rememberUpdatedState(tool)
    val layerState = rememberUpdatedState(layer)
    val colorState = rememberUpdatedState(paintColor)
    val onPickedState = rememberUpdatedState(onPicked)
    val image = remember(bitmap) { bitmap.asImageBitmap() }

    val partColors = remember {
        mapOf(
            SkinPart.HEAD to Color(0xFFF0B41E),
            SkinPart.TORSO to Color(0xFF2E7FC2),
            SkinPart.ARM_RIGHT to Color(0xFF3FA34D),
            SkinPart.ARM_LEFT to Color(0xFF2AA198),
            SkinPart.LEG_RIGHT to Color(0xFF9D6BFF),
            SkinPart.LEG_LEFT to Color(0xFFF28BB4),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var transformMode = false
                        var lastPixel: Pair<Int, Int>? = null

                        fun toPixel(pos: Offset): Pair<Int, Int>? {
                            val cell = size.width / 64f * zoom
                            val x = ((pos.x - panX) / cell).toInt()
                            val y = ((pos.y - panY) / cell).toInt()
                            return if (x in 0 until 64 && y in 0 until 64) x to y else null
                        }

                        fun handlePaint(pos: Offset, isMove: Boolean) {
                            val px = toPixel(pos) ?: run {
                                lastPixel = null
                                return
                            }
                            when (toolState.value) {
                                EditorTool.PICKER -> {
                                    if (!isMove) {
                                        editorVm.pickColor(px.first, px.second)?.let {
                                            editorVm.setColor(it)
                                            onPickedState.value()
                                        }
                                    }
                                }
                                EditorTool.FILL -> {
                                    if (!isMove) {
                                        if (editorVm.floodFill(
                                                px.first, px.second, layerState.value, colorState.value,
                                            )
                                        ) {
                                            editorVm.noteColorUsed(colorState.value)
                                        }
                                    }
                                }
                                EditorTool.PENCIL, EditorTool.ERASER -> {
                                    val eraser = toolState.value == EditorTool.ERASER
                                    if (!isMove) {
                                        editorVm.beginStroke()
                                        if (!eraser) editorVm.noteColorUsed(colorState.value)
                                        editorVm.paintDot(
                                            px.first, px.second, layerState.value,
                                            colorState.value, eraser,
                                        )
                                    } else {
                                        val from = lastPixel
                                        if (from != null) {
                                            editorVm.paintLine(
                                                from.first, from.second, px.first, px.second,
                                                layerState.value, colorState.value, eraser,
                                            )
                                        } else {
                                            editorVm.paintDot(
                                                px.first, px.second, layerState.value,
                                                colorState.value, eraser,
                                            )
                                        }
                                    }
                                }
                            }
                            lastPixel = px
                        }

                        handlePaint(down.position, isMove = false)
                        var event = awaitPointerEvent()
                        while (event.changes.any { it.pressed }) {
                            if (event.changes.size >= 2) {
                                transformMode = true
                                val centroid = event.calculateCentroid()
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                val oldZoom = zoom
                                val newZoom = (zoom * zoomChange).coerceIn(1f, 12f)
                                val ratio = if (oldZoom != 0f) newZoom / oldZoom else 1f
                                panX = centroid.x - (centroid.x - panX) * ratio + panChange.x
                                panY = centroid.y - (centroid.y - panY) * ratio + panChange.y
                                zoom = newZoom
                                // Keep the bitmap on screen.
                                val disp = size.width * zoom
                                panX = panX.coerceIn(size.width - disp, 0f)
                                panY = panY.coerceIn(size.height - disp, 0f)
                                if (zoom == 1f) {
                                    panX = 0f
                                    panY = 0f
                                }
                            } else if (!transformMode) {
                                val change = event.changes.first()
                                if (change.pressed) handlePaint(change.position, isMove = true)
                            }
                            event = awaitPointerEvent()
                        }
                    }
                },
        ) {
            val w = size.width
            // Checkerboard behind the (partly transparent) template.
            val sq = 16.dp.toPx()
            var row = 0
            var y = 0f
            while (y < size.height) {
                var col = 0
                var x = 0f
                while (x < size.width) {
                    drawRect(
                        color = if ((row + col) % 2 == 0) Color(0xFFC9C9C9) else Color(0xFF8F8F8F),
                        topLeft = Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(sq, sq),
                    )
                    x += sq
                    col++
                }
                y += sq
                row++
            }
            val cell = w / 64f * zoom
            drawImage(
                image = image,
                dstOffset = IntOffset(panX.roundToInt(), panY.roundToInt()),
                dstSize = IntSize((w * zoom).roundToInt(), (w * zoom).roundToInt()),
                filterQuality = FilterQuality.None,
            )
            if (showLabels) {
                for (part in SkinPart.entries) {
                    val tint = partColors[part] ?: Color.White
                    for (layerKind in SkinLayer.entries) {
                        for (rect in SkinTemplate.faceRects(part, layerKind)) {
                            drawRect(
                                color = tint.copy(alpha = 0.13f),
                                topLeft = Offset(panX + rect.x * cell, panY + rect.y * cell),
                                size = androidx.compose.ui.geometry.Size(rect.w * cell, rect.h * cell),
                            )
                        }
                    }
                }
                // Part names on the front faces (native canvas text).
                with(drawContext.canvas.nativeCanvas) {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = cell * 1.5f
                        isFakeBoldText = true
                        setShadowLayer(3f, 0f, 0f, android.graphics.Color.BLACK)
                    }
                    for (part in SkinPart.entries) {
                        val label = partLabels[part] ?: continue
                        for (layerKind in SkinLayer.entries) {
                            val front = SkinTemplate.faceRect(
                                part, layerKind, SkinTemplate.FACE_FRONT,
                            )
                            drawText(
                                label,
                                panX + (front.x + front.w / 2f) * cell,
                                panY + (front.y + front.h / 2f) * cell + cell * 0.5f,
                                paint,
                            )
                        }
                    }
                }
            }
            if (showGrid && cell >= 4f) {
                val gridColor = Color.Black.copy(alpha = 0.18f)
                for (i in 0..64) {
                    val gx = panX + i * cell
                    drawLine(gridColor, Offset(gx, panY), Offset(gx, panY + 64 * cell))
                    val gy = panY + i * cell
                    drawLine(gridColor, Offset(panX, gy), Offset(panX + 64 * cell, gy))
                }
            }
        }
    }
}
