package com.fsdetc.fsdstudio.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fsdetc.fsdstudio.R
import com.fsdetc.fsdstudio.data.Api
import com.fsdetc.fsdstudio.data.CharacterSource
import com.fsdetc.fsdstudio.data.DownloadOutcome
import com.fsdetc.fsdstudio.data.ImageSaver
import com.fsdetc.fsdstudio.data.PoseCatalog
import com.fsdetc.fsdstudio.data.PoseDef
import com.fsdetc.fsdstudio.data.SkinBitmaps
import com.fsdetc.fsdstudio.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The Studio tab (free): model your own character — pick any pose from the
 * library, then adjust camera, position and body joints, and export the
 * result to the gallery. The app's normal render/download limits apply.
 *
 * API discipline: sliders edit a local value while dragging; the committed
 * state (and therefore the render URL / Coil key) changes ONLY on slider
 * release or on chip/toggle taps — never one request per drag frame.
 */
@Composable
fun StudioScreen(
    mainVm: MainViewModel,
    onOpenPro: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenLibrary: () -> Unit,
) {
    val character by mainVm.character.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.studio_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(12.dp))

        val ch = character
        if (ch == null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.poses_no_character),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onOpenHome) {
                            Text(stringResource(R.string.studio_go_home))
                        }
                        OutlinedButton(onClick = onOpenLibrary) {
                            Text(stringResource(R.string.lib_button))
                        }
                    }
                }
            }
            return@Column
        }

        if (ch.source == CharacterSource.UPLOAD) {
            // Honest flat preview: the Studio's pose/camera model needs an
            // online (username) skin, so uploaded skins only preview here.
            Text(
                text = stringResource(R.string.studio_upload_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            val sha = ch.skinHash
            if (sha != null) {
                val front = SkinBitmaps.flatFile(context, sha, "front")
                FramedImage(
                    model = front,
                    bg = BgOption.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(5f / 8f),
                )
            }
            return@Column
        }

        val renderId = ch.renderId
        if (renderId == null) {
            Text(
                text = stringResource(R.string.poses_no_character),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        StudioEditor(
            mainVm = mainVm,
            renderId = renderId,
            charName = ch.name,
            onOpenPro = onOpenPro,
        )
    }
}

@Composable
private fun StudioEditor(
    mainVm: MainViewModel,
    renderId: String,
    charName: String,
    onOpenPro: () -> Unit,
) {
    val plan by mainVm.plan.collectAsState()
    val downloadsLeft by mainVm.downloadsLeft.collectAsState()
    val defaultBgName by mainVm.defaultBgName.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Committed render state — the ONLY values the preview URL is built from.
    var poseDef by remember(renderId) { mutableStateOf(PoseCatalog.POSES.first()) }
    var yaw by remember(renderId) { mutableFloatStateOf(35f) }
    var pitch by remember(renderId) { mutableFloatStateOf(12f) }
    var roll by remember(renderId) { mutableFloatStateOf(0f) }
    var zoom by remember(renderId) { mutableFloatStateOf(1f) }
    var offsetX by remember(renderId) { mutableFloatStateOf(0f) }
    var offsetY by remember(renderId) { mutableFloatStateOf(0f) }
    var headYaw by remember(renderId) { mutableFloatStateOf(0f) }
    var headPitch by remember(renderId) { mutableFloatStateOf(0f) }
    var armLeft by remember(renderId) { mutableFloatStateOf(0f) }
    var armRight by remember(renderId) { mutableFloatStateOf(0f) }
    var legLeft by remember(renderId) { mutableFloatStateOf(0f) }
    var legRight by remember(renderId) { mutableFloatStateOf(0f) }
    var shadow by remember(renderId) { mutableStateOf(false) }
    var cape by remember(renderId) { mutableStateOf(true) }
    var overlay by remember(renderId) { mutableStateOf(true) }
    var bg by remember(renderId, defaultBgName) { mutableStateOf(BgOption.fromName(defaultBgName)) }
    var advancedOpen by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    fun applyPose(def: PoseDef) {
        poseDef = def
        yaw = def.params["yaw"]?.toFloatOrNull() ?: 35f
        pitch = def.params["pitch"]?.toFloatOrNull() ?: 12f
        roll = 0f
        zoom = 1f
        offsetX = 0f
        offsetY = 0f
        headYaw = def.params["headYaw"]?.toFloatOrNull() ?: 0f
        headPitch = def.params["headPitch"]?.toFloatOrNull() ?: 0f
        armLeft = 0f
        armRight = 0f
        legLeft = 0f
        legRight = 0f
        shadow = def.params["shadow"] == "true"
        cape = true
        overlay = true
    }

    fun currentParams(): Map<String, String> {
        val p = LinkedHashMap(poseDef.params)
        p["yaw"] = fmt(yaw)
        p["pitch"] = fmt(pitch)
        if (roll != 0f) p["roll"] = fmt(roll)
        if (zoom != 1f) p["zoom"] = fmt(zoom)
        if (offsetX != 0f) p["offsetX"] = fmt(offsetX)
        if (offsetY != 0f) p["offsetY"] = fmt(offsetY)
        p["headYaw"] = fmt(headYaw)
        p["headPitch"] = fmt(headPitch)
        if (armLeft != 0f) p["leftArmPitch"] = fmt(armLeft)
        if (armRight != 0f) p["rightArmPitch"] = fmt(armRight)
        if (legLeft != 0f) p["leftLegPitch"] = fmt(legLeft)
        if (legRight != 0f) p["rightLegPitch"] = fmt(legRight)
        p["shadow"] = shadow.toString()
        p["cape"] = cape.toString()
        p["overlay"] = overlay.toString()
        return p
    }

    val poseLabel = stringResource(poseDef.labelRes)
    val previewUrl = Api.renderUrl(renderId, "body", 512, bgHex = bg.hex, extra = currentParams())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ---------------------------------------------------------- preview
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f),
        ) {
            FramedImage(
                model = previewUrl,
                bg = bg,
                modifier = Modifier.fillMaxSize(),
            )
            // Preview-only branding; the export below stays clean.
            WatermarkOverlay(Modifier.fillMaxSize())
        }
        BgSelector(selected = bg, onSelect = { bg = it })

        // ---------------------------------------------------------- poses
        SectionTitle(stringResource(R.string.studio_pose))
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            PoseCatalog.POSES.forEach { def ->
                val locked = !mainVm.entitlements.poseUnlocked(def, plan)
                val label = stringResource(def.labelRes)
                FilterChip(
                    selected = poseDef.id == def.id,
                    onClick = { if (locked) onOpenPro() else applyPose(def) },
                    label = { Text(if (locked) "$label 🔒" else label) },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }

        // ---------------------------------------------------------- camera
        SectionTitle(stringResource(R.string.studio_camera))
        StudioSlider(stringResource(R.string.studio_yaw), yaw, -180f..180f) { yaw = it }
        StudioSlider(stringResource(R.string.studio_pitch), pitch, -60f..60f) { pitch = it }
        StudioSlider(stringResource(R.string.studio_roll), roll, -45f..45f) { roll = it }
        StudioSlider(
            stringResource(R.string.studio_zoom), zoom, 0.5f..2.5f,
            format = { String.format(Locale.US, "%.1f", it) },
        ) { zoom = it }

        // ---------------------------------------------------------- position
        SectionTitle(stringResource(R.string.studio_position))
        StudioSlider(
            stringResource(R.string.studio_offset_x), offsetX, -0.5f..0.5f,
            format = { String.format(Locale.US, "%.2f", it) },
        ) { offsetX = it }
        StudioSlider(
            stringResource(R.string.studio_offset_y), offsetY, -0.5f..0.5f,
            format = { String.format(Locale.US, "%.2f", it) },
        ) { offsetY = it }

        // ---------------------------------------------------------- advanced joints
        TextButton(onClick = { advancedOpen = !advancedOpen }) {
            Text(stringResource(R.string.studio_advanced))
            Icon(
                if (advancedOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
            )
        }
        if (advancedOpen) {
            StudioSlider(stringResource(R.string.studio_head_yaw), headYaw, -90f..90f) { headYaw = it }
            StudioSlider(stringResource(R.string.studio_head_pitch), headPitch, -90f..90f) { headPitch = it }
            StudioSlider(stringResource(R.string.studio_arm_left), armLeft, -120f..120f) { armLeft = it }
            StudioSlider(stringResource(R.string.studio_arm_right), armRight, -120f..120f) { armRight = it }
            StudioSlider(stringResource(R.string.studio_leg_left), legLeft, -90f..90f) { legLeft = it }
            StudioSlider(stringResource(R.string.studio_leg_right), legRight, -90f..90f) { legRight = it }
        }

        // ---------------------------------------------------------- toggles
        ToggleRow(stringResource(R.string.studio_shadow), shadow) { shadow = it }
        ToggleRow(stringResource(R.string.studio_cape), cape) { cape = it }
        ToggleRow(stringResource(R.string.studio_layers), overlay) { overlay = it }

        // ---------------------------------------------------------- actions
        Text(
            text = downloadsLeft?.let { stringResource(R.string.dl_left_today, it) }
                ?: stringResource(R.string.dl_unlimited),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    if (busy) return@Button
                    busy = true
                    scope.launch {
                        val exportSize = mainVm.entitlements.maxRenderSize(plan)
                        val exportUrl = Api.renderUrl(
                            renderId, "body", exportSize,
                            bgHex = bg.hex, extra = currentParams(),
                        )
                        val outcome = mainVm.downloadGate.saveWithGate {
                            val item = mainVm.ensureRemoteHistory(
                                exportUrl, "Studio — $poseLabel", charName, charName,
                                kind = "STUDIO",
                            )
                            ImageSaver.saveToGallery(
                                context,
                                File(item.filePath),
                                "FSD_${charName}_studio_$exportSize.png",
                            )
                        }
                        busy = false
                        when (outcome) {
                            DownloadOutcome.Saved ->
                                mainVm.postSnackbar(context.getString(R.string.saved_to_gallery))
                            DownloadOutcome.Failed ->
                                mainVm.postSnackbar(context.getString(R.string.saved_failed))
                            DownloadOutcome.LimitReached -> {
                                mainVm.postSnackbar(context.getString(R.string.dl_limit_reached))
                                onOpenPro()
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.studio_export))
            }
            OutlinedButton(
                onClick = { applyPose(PoseCatalog.POSES.first()) },
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.studio_reset))
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/**
 * A slider that reports its value ONLY when the drag finishes, so the render
 * URL (and the network) changes once per adjustment, not once per frame.
 */
@Composable
private fun StudioSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    format: (Float) -> String = { it.roundToInt().toString() },
    onCommit: (Float) -> Unit,
) {
    var local by remember(value) { mutableFloatStateOf(value) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = format(local),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onCommit(local) },
            valueRange = range,
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** Compact number formatting for URL parameters: 35, -40, 0.5, 1.25. */
private fun fmt(v: Float): String {
    val rounded = v.roundToInt()
    return if (kotlin.math.abs(v - rounded) < 0.001f) {
        rounded.toString()
    } else {
        String.format(Locale.US, "%.2f", v).trimEnd('0').trimEnd('.')
    }
}
