package com.fsdetc.fsdstudio.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fsdetc.fsdstudio.R
import com.fsdetc.fsdstudio.data.Api
import com.fsdetc.fsdstudio.data.DetailRequest
import com.fsdetc.fsdstudio.data.DownloadOutcome
import com.fsdetc.fsdstudio.data.ImageSaver
import com.fsdetc.fsdstudio.data.SkinBitmaps
import com.fsdetc.fsdstudio.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderDetailScreen(
    mainVm: MainViewModel,
    onBack: () -> Unit,
    onOpenPro: () -> Unit,
    onOpenEditor: () -> Unit,
) {
    val req by mainVm.detailRequest.collectAsState()
    val current = req

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (current) {
                            is DetailRequest.RemotePose -> current.poseLabel
                            is DetailRequest.LocalFile -> current.title
                            is DetailRequest.MobRender -> current.mobLabel
                            null -> ""
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            when (current) {
                null -> LaunchedEffect(Unit) { onBack() }
                is DetailRequest.RemotePose -> RemoteDetail(mainVm, current, onOpenPro, onOpenEditor)
                is DetailRequest.LocalFile -> LocalDetail(mainVm, current, onBack, onOpenPro, onOpenEditor)
                is DetailRequest.MobRender -> MobDetail(mainVm, current, onOpenPro)
            }
        }
    }
}

/**
 * "Edit skin" entry into the Skin Editor, shown when the current character's
 * skin can be resolved (username/library skins via their texture URL, uploads
 * via their local file) — the editor itself loads the actual pixels.
 */
@Composable
private fun EditSkinButton(mainVm: MainViewModel, onOpenEditor: () -> Unit) {
    val character by mainVm.character.collectAsState()
    val canEdit = character?.let { ch ->
        when (ch.source) {
            com.fsdetc.fsdstudio.data.CharacterSource.UPLOAD -> ch.localSkinPath != null
            com.fsdetc.fsdstudio.data.CharacterSource.USERNAME ->
                ch.skinUrl != null || ch.renderId != null
        }
    } ?: false
    if (!canEdit) return
    OutlinedButton(onClick = onOpenEditor, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.Brush, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.detail_edit_skin))
    }
}

/** "4 downloads left today" / "Unlimited downloads" status line for detail screens. */
@Composable
private fun DownloadsLeftLine(mainVm: MainViewModel) {
    val downloadsLeft by mainVm.downloadsLeft.collectAsState()
    Text(
        text = downloadsLeft?.let { stringResource(R.string.dl_left_today, it) }
            ?: stringResource(R.string.dl_unlimited),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Shared outcome handling for every gated download button (bottom snackbar). */
private fun CoroutineScope.launchGatedDownload(
    mainVm: MainViewModel,
    context: android.content.Context,
    onOpenPro: () -> Unit,
    onBusyChange: (Boolean) -> Unit,
    save: suspend () -> Boolean,
) {
    onBusyChange(true)
    launch {
        val outcome = mainVm.downloadGate.saveWithGate(save)
        onBusyChange(false)
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
}

@Composable
private fun RemoteDetail(
    mainVm: MainViewModel,
    req: DetailRequest.RemotePose,
    onOpenPro: () -> Unit,
    onOpenEditor: () -> Unit,
) {
    val plan by mainVm.plan.collectAsState()
    val defaultBgName by mainVm.defaultBgName.collectAsState()
    val defaultSize by mainVm.defaultRenderSize.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val maxSize = mainVm.entitlements.maxRenderSize(plan)
    var bg by remember(req.renderId, req.poseId) { mutableStateOf(BgOption.fromName(defaultBgName)) }
    var size by remember(req.renderId, req.poseId) {
        mutableIntStateOf(defaultSize.coerceAtMost(maxSize))
    }
    var recordedUrl by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }

    val effectiveSize = size.coerceAtMost(maxSize)
    val url = Api.renderUrl(
        req.renderId, req.renderType, effectiveSize,
        bgHex = bg.hex, extra = req.extraParams,
    )
    val title = "Pose render"
    val loadFailedText = stringResource(R.string.error_load_failed)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        key(attempt) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                FramedImage(
                    model = url,
                    bg = bg,
                    modifier = Modifier.fillMaxSize(),
                    onSuccess = {
                        loadError = null
                        if (recordedUrl != url) {
                            recordedUrl = url
                            mainVm.recordRemoteInBackground(url, title, req.poseLabel, req.charName)
                        }
                    },
                    onError = { loadError = loadFailedText },
                )
                // Preview-only branding; the download below stays clean.
                WatermarkOverlay(Modifier.fillMaxSize())
            }
        }
        if (loadError != null) {
            Text(
                text = loadError!!,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            OutlinedButton(onClick = { loadError = null; attempt++ }) {
                Text(stringResource(R.string.action_retry))
            }
        }
        Text(
            text = "${req.charName} · ${req.poseLabel}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        EditSkinButton(mainVm, onOpenEditor)

        Text(stringResource(R.string.detail_background), style = MaterialTheme.typography.labelLarge)
        BgSelector(selected = bg, onSelect = { bg = it })

        Text(stringResource(R.string.detail_size), style = MaterialTheme.typography.labelLarge)
        Row {
            FilterChip(
                selected = effectiveSize == 512,
                onClick = { size = 512 },
                label = { Text("512") },
                modifier = Modifier.padding(end = 8.dp),
            )
            FilterChip(
                selected = effectiveSize == 1024,
                onClick = { if (maxSize >= 1024) size = 1024 else onOpenPro() },
                label = { Text(if (maxSize >= 1024) "1024" else "1024 🔒") },
            )
        }
        if (maxSize < 1024) {
            Text(
                text = stringResource(R.string.pro_locked_size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        DownloadsLeftLine(mainVm)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    if (busy) return@Button
                    scope.launchGatedDownload(mainVm, context, onOpenPro, { busy = it }) {
                        val item = mainVm.ensureRemoteHistory(url, title, req.poseLabel, req.charName)
                        ImageSaver.saveToGallery(
                            context,
                            File(item.filePath),
                            "FSD_${req.charName}_${req.poseId}_$effectiveSize.png",
                        )
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_download))
            }
            OutlinedButton(
                onClick = {
                    scope.launch {
                        runCatching {
                            val item = mainVm.ensureRemoteHistory(url, title, req.poseLabel, req.charName)
                            ImageSaver.shareImage(context, File(item.filePath))
                        }
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_share))
            }
        }
    }
}

@Composable
private fun MobDetail(
    mainVm: MainViewModel,
    req: DetailRequest.MobRender,
    onOpenPro: () -> Unit,
) {
    val defaultBgName by mainVm.defaultBgName.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var poseId by remember(req.mobId) { mutableStateOf(req.poses.firstOrNull()?.id ?: "default") }
    var bg by remember(req.mobId) { mutableStateOf(BgOption.fromName(defaultBgName)) }
    var recordedUrl by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }

    val poseLabel = req.poses.firstOrNull { it.id == poseId }?.label ?: poseId
    val url = Api.mobRenderUrl(req.mobId, "body", 512, pose = poseId, bgHex = bg.hex)
    val title = "Mob render"
    val loadFailedText = stringResource(R.string.error_load_failed)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        key(attempt) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                FramedImage(
                    model = url,
                    bg = bg,
                    modifier = Modifier.fillMaxSize(),
                    onSuccess = {
                        loadError = null
                        if (recordedUrl != url) {
                            recordedUrl = url
                            mainVm.recordRemoteInBackground(
                                url, title, poseLabel, req.mobLabel, kind = "MOB",
                            )
                        }
                    },
                    onError = { loadError = loadFailedText },
                )
                // Preview-only branding; the download below stays clean.
                WatermarkOverlay(Modifier.fillMaxSize())
            }
        }
        if (loadError != null) {
            Text(
                text = loadError!!,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            OutlinedButton(onClick = { loadError = null; attempt++ }) {
                Text(stringResource(R.string.action_retry))
            }
        }
        Text(
            text = "${req.mobLabel} · $poseLabel",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(stringResource(R.string.detail_pose), style = MaterialTheme.typography.labelLarge)
        Row {
            req.poses.forEach { pose ->
                FilterChip(
                    selected = poseId == pose.id,
                    onClick = { poseId = pose.id },
                    label = { Text(pose.label) },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }

        Text(stringResource(R.string.detail_background), style = MaterialTheme.typography.labelLarge)
        BgSelector(selected = bg, onSelect = { bg = it })

        DownloadsLeftLine(mainVm)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    if (busy) return@Button
                    scope.launchGatedDownload(mainVm, context, onOpenPro, { busy = it }) {
                        val item = mainVm.ensureRemoteHistory(
                            url, title, poseLabel, req.mobLabel, kind = "MOB",
                        )
                        ImageSaver.saveToGallery(
                            context,
                            File(item.filePath),
                            "FSD_${req.mobLabel}_$poseId.png",
                        )
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_download))
            }
            OutlinedButton(
                onClick = {
                    scope.launch {
                        runCatching {
                            val item = mainVm.ensureRemoteHistory(
                                url, title, poseLabel, req.mobLabel, kind = "MOB",
                            )
                            ImageSaver.shareImage(context, File(item.filePath))
                        }
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_share))
            }
        }
    }
}

@Composable
private fun LocalDetail(
    mainVm: MainViewModel,
    req: DetailRequest.LocalFile,
    onBack: () -> Unit,
    onOpenPro: () -> Unit,
    onOpenEditor: () -> Unit,
) {
    val defaultBgName by mainVm.defaultBgName.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bg by remember(req.path) { mutableStateOf(BgOption.fromName(defaultBgName)) }
    var busy by remember { mutableStateOf(false) }
    val srcFile = remember(req.path) { File(req.path) }

    // Viewing a local render records it into history once (unless it came from there).
    LaunchedEffect(req.path) {
        if (req.historyId == null) {
            runCatching { mainVm.ensureLocalHistory(srcFile, req.title, req.subtitle, req.sourceName) }
        }
    }

    /** The file to download/share: the original for Transparent, a composited copy otherwise. */
    suspend fun outputFile(): File {
        if (bg == BgOption.Transparent || bg.previewColor == null) return srcFile
        return withContext(Dispatchers.IO) {
            val bmp = BitmapFactory.decodeFile(srcFile.absolutePath)
            val composited = SkinBitmaps.compositeOver(bmp, bg.previewColor!!.toArgb())
            val dir = File(context.cacheDir, "shared").apply { mkdirs() }
            val out = File(dir, "bg_" + srcFile.name)
            out.writeBytes(SkinBitmaps.bitmapToPngBytes(composited))
            out
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            FramedImage(
                model = srcFile,
                bg = bg,
                modifier = Modifier.fillMaxSize(),
            )
            // Preview-only branding; the download below stays clean.
            WatermarkOverlay(Modifier.fillMaxSize())
        }
        Text(
            text = listOf(req.title, req.subtitle, req.sourceName)
                .filter { it.isNotBlank() }
                .joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(stringResource(R.string.detail_background), style = MaterialTheme.typography.labelLarge)
        BgSelector(selected = bg, onSelect = { bg = it })

        DownloadsLeftLine(mainVm)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    if (busy) return@Button
                    scope.launchGatedDownload(mainVm, context, onOpenPro, { busy = it }) {
                        ImageSaver.saveToGallery(
                            context,
                            outputFile(),
                            "FSD_" + req.title.replace(" ", "_") + ".png",
                        )
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_download))
            }
            OutlinedButton(
                onClick = {
                    scope.launch {
                        runCatching { ImageSaver.shareImage(context, outputFile()) }
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_share))
            }
        }

        EditSkinButton(mainVm, onOpenEditor)

        if (req.deletableHistory && req.historyId != null) {
            OutlinedButton(
                onClick = {
                    mainVm.deleteHistoryItem(req.historyId)
                    onBack()
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_delete))
            }
        }
    }
}
