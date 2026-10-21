package com.fsdetc.fsdstudio.ui

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import com.fsdetc.fsdstudio.data.ImageSaver
import com.fsdetc.fsdstudio.data.SkinBitmaps
import com.fsdetc.fsdstudio.ui.theme.FsdBackground
import com.fsdetc.fsdstudio.ui.theme.FsdError
import com.fsdetc.fsdstudio.ui.theme.FsdOnSurfaceMuted
import com.fsdetc.fsdstudio.viewmodel.MainViewModel
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
                            null -> ""
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FsdBackground),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            when (current) {
                null -> LaunchedEffect(Unit) { onBack() }
                is DetailRequest.RemotePose -> RemoteDetail(mainVm, current, onOpenPro)
                is DetailRequest.LocalFile -> LocalDetail(mainVm, current, onBack)
            }
        }
    }
}

@Composable
private fun RemoteDetail(
    mainVm: MainViewModel,
    req: DetailRequest.RemotePose,
    onOpenPro: () -> Unit,
) {
    val isPro by mainVm.isPro.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var bg by remember { mutableStateOf(BgOption.Transparent) }
    var size by remember { mutableStateOf(512) }
    var recordedUrl by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val url = Api.renderUrl(req.renderId, "body", size, pose = req.poseId, bgHex = bg.hex)
    val title = "Pose render"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FramedImage(
            model = url,
            bg = bg,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            onSuccess = {
                if (recordedUrl != url) {
                    recordedUrl = url
                    mainVm.recordRemoteInBackground(url, title, req.poseLabel, req.charName)
                }
            },
        )
        Text(
            text = "${req.charName} · ${req.poseLabel}",
            style = MaterialTheme.typography.bodyMedium,
            color = FsdOnSurfaceMuted,
        )

        Text(stringResource(R.string.detail_background), style = MaterialTheme.typography.labelLarge)
        BgSelector(selected = bg, onSelect = { bg = it })

        Text(stringResource(R.string.detail_size), style = MaterialTheme.typography.labelLarge)
        Row {
            FilterChip(
                selected = size == 512,
                onClick = { size = 512 },
                label = { Text("512") },
                modifier = Modifier.padding(end = 8.dp),
            )
            FilterChip(
                selected = size == 1024,
                onClick = { if (isPro) size = 1024 else onOpenPro() },
                label = { Text(if (isPro) "1024" else "1024 🔒") },
            )
        }
        if (!isPro) {
            Text(
                text = stringResource(R.string.pro_locked_size),
                style = MaterialTheme.typography.bodySmall,
                color = FsdOnSurfaceMuted,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    if (busy) return@Button
                    busy = true
                    scope.launch {
                        val ok = runCatching {
                            val item = mainVm.ensureRemoteHistory(url, title, req.poseLabel, req.charName)
                            ImageSaver.saveToGallery(
                                context,
                                File(item.filePath),
                                "FSD_${req.charName}_${req.poseId}_$size.png",
                            )
                        }.getOrDefault(false)
                        busy = false
                        Toast.makeText(
                            context,
                            if (ok) R.string.saved_to_gallery else R.string.saved_failed,
                            Toast.LENGTH_SHORT,
                        ).show()
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
private fun LocalDetail(
    mainVm: MainViewModel,
    req: DetailRequest.LocalFile,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bg by remember { mutableStateOf(BgOption.Transparent) }
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
        FramedImage(
            model = srcFile,
            bg = bg,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
        Text(
            text = listOf(req.title, req.subtitle, req.sourceName)
                .filter { it.isNotBlank() }
                .joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = FsdOnSurfaceMuted,
        )

        Text(stringResource(R.string.detail_background), style = MaterialTheme.typography.labelLarge)
        BgSelector(selected = bg, onSelect = { bg = it })

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    if (busy) return@Button
                    busy = true
                    scope.launch {
                        val ok = runCatching {
                            val out = outputFile()
                            ImageSaver.saveToGallery(
                                context,
                                out,
                                "FSD_" + req.title.replace(" ", "_") + ".png",
                            )
                        }.getOrDefault(false)
                        busy = false
                        Toast.makeText(
                            context,
                            if (ok) R.string.saved_to_gallery else R.string.saved_failed,
                            Toast.LENGTH_SHORT,
                        ).show()
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

        if (req.deletableHistory && req.historyId != null) {
            OutlinedButton(
                onClick = {
                    mainVm.deleteHistoryItem(req.historyId)
                    onBack()
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FsdError),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_delete))
            }
        }
    }
}
