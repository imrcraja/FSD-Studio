package com.fsdetc.fsdstudio.ui

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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fsdetc.fsdstudio.R
import com.fsdetc.fsdstudio.data.CharacterSource
import com.fsdetc.fsdstudio.data.DownloadOutcome
import com.fsdetc.fsdstudio.data.ImageSaver
import com.fsdetc.fsdstudio.data.Plan
import com.fsdetc.fsdstudio.viewmodel.AiViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AiScreen(aiVm: AiViewModel, onOpenPro: () -> Unit) {
    val character by aiVm.character.collectAsState()
    val remaining by aiVm.remaining.collectAsState()
    val plan by aiVm.plan.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { aiVm.refreshCount() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.ai_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        val ch = character
        if (ch == null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Text(
                    text = stringResource(R.string.ai_need_character),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else if (ch.source == CharacterSource.USERNAME) {
            // Edit mode: the current character's render is the edit source.
            Text(
                text = stringResource(R.string.ai_editing_this),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            aiVm.sourceRenderUrl(ch)?.let { sourceUrl ->
                FramedImage(
                    model = sourceUrl,
                    bg = BgOption.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f),
                )
            }
        } else {
            // Uploaded skins have no public render to edit — prompt-only, said honestly.
            Text(
                text = stringResource(R.string.ai_reference_upload_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = aiVm.prompt,
            onValueChange = { aiVm.prompt = it },
            label = { Text(stringResource(R.string.ai_prompt_label)) },
            placeholder = { Text(stringResource(R.string.ai_prompt_hint)) },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = if (plan == Plan.MAX) "AI edits: Unlimited" else stringResource(R.string.ai_remaining, remaining),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = { aiVm.generate() },
            enabled = !aiVm.generating && ch != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.ai_generate))
        }

        if (aiVm.generating) {
            Row {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.ai_generating),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        aiVm.error?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            if (!aiVm.generating) {
                OutlinedButton(onClick = { aiVm.generate() }) {
                    Text(stringResource(R.string.ai_retry))
                }
            }
        }

        aiVm.resultPath?.let { path ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            ) {
                FramedImage(
                    model = File(path),
                    bg = BgOption.Transparent,
                    modifier = Modifier.fillMaxSize(),
                )
                // Preview-only branding; the download below stays clean.
                WatermarkOverlay(Modifier.fillMaxSize())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            val outcome = aiVm.downloadGate.saveWithGate {
                                ImageSaver.saveToGallery(
                                    context,
                                    File(path),
                                    "FSD_AI_${System.currentTimeMillis()}.png",
                                )
                            }
                            when (outcome) {
                                DownloadOutcome.Saved ->
                                    aiVm.postSnackbar(context.getString(R.string.saved_to_gallery))
                                DownloadOutcome.Failed ->
                                    aiVm.postSnackbar(context.getString(R.string.saved_failed))
                                DownloadOutcome.LimitReached -> {
                                    aiVm.postSnackbar(context.getString(R.string.dl_limit_reached))
                                    onOpenPro()
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_download))
                }
                OutlinedButton(
                    onClick = { ImageSaver.shareImage(context, File(path)) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_share))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
