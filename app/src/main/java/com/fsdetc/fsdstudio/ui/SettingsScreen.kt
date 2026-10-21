package com.fsdetc.fsdstudio.ui

import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.imageLoader
import com.fsdetc.fsdstudio.FsdStudioApp
import com.fsdetc.fsdstudio.R
import com.fsdetc.fsdstudio.data.ImageSaver
import com.fsdetc.fsdstudio.ui.theme.FsdOnSurface
import com.fsdetc.fsdstudio.ui.theme.FsdOnSurfaceMuted
import com.fsdetc.fsdstudio.ui.theme.FsdSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun SettingsScreen(onOpenPro: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as FsdStudioApp

    var cacheSizeBytes by remember { mutableStateOf<Long?>(null) }
    var clearedNote by remember { mutableStateOf(false) }

    fun cacheBytes(): Long =
        ImageSaver.folderSizeBytes(File(context.cacheDir, "http_cache")) +
            ImageSaver.folderSizeBytes(File(context.cacheDir, "coil_cache"))

    LaunchedEffect(Unit) { cacheSizeBytes = cacheBytes() }

    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall,
            color = FsdOnSurface,
        )

        // ------------------------------------------------ language
        SettingsCard(title = stringResource(R.string.settings_language)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = true, onClick = { /* English is the active language. */ })
                Text(stringResource(R.string.language_english), color = FsdOnSurface)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = false, onClick = {}, enabled = false)
                Text(
                    text = stringResource(R.string.language_hindi) + " · " +
                        stringResource(R.string.coming_soon),
                    color = FsdOnSurfaceMuted,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = false, onClick = {}, enabled = false)
                Text(
                    text = stringResource(R.string.language_bangla) + " · " +
                        stringResource(R.string.coming_soon),
                    color = FsdOnSurfaceMuted,
                )
            }
            Text(
                text = stringResource(R.string.settings_theme_note),
                style = MaterialTheme.typography.bodySmall,
                color = FsdOnSurfaceMuted,
            )
        }

        // ------------------------------------------------ pro
        SettingsCard(title = stringResource(R.string.pro_title)) {
            TextButton(onClick = onOpenPro) {
                Text(stringResource(R.string.pro_title))
            }
        }

        // ------------------------------------------------ cache
        SettingsCard(title = stringResource(R.string.settings_cache)) {
            Text(
                text = stringResource(
                    R.string.settings_cache_size,
                    cacheSizeBytes?.let { Formatter.formatShortFileSize(context, it) } ?: "0 B",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = FsdOnSurfaceMuted,
            )
            OutlinedButton(
                onClick = {
                    context.imageLoader.memoryCache?.clear()
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            runCatching { context.imageLoader.diskCache?.clear() }
                            runCatching { app.container.skinRepository.httpClient.cache?.evictAll() }
                        }
                        cacheSizeBytes = 0L
                        clearedNote = true
                    }
                },
            ) {
                Text(stringResource(R.string.settings_cache_clear))
            }
            if (clearedNote) {
                Text(
                    text = stringResource(R.string.settings_cache_cleared),
                    style = MaterialTheme.typography.bodySmall,
                    color = FsdOnSurfaceMuted,
                )
            }
        }

        // ------------------------------------------------ about
        SettingsCard(title = stringResource(R.string.settings_about)) {
            Image(
                painter = painterResource(R.drawable.fsd_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
            Text(
                text = stringResource(R.string.about_version, version),
                style = MaterialTheme.typography.bodyMedium,
                color = FsdOnSurface,
            )
            Text(
                text = stringResource(R.string.about_credits),
                style = MaterialTheme.typography.bodySmall,
                color = FsdOnSurfaceMuted,
            )
            Text(
                text = stringResource(R.string.about_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = FsdOnSurfaceMuted,
            )
        }

        // ------------------------------------------------ links
        SettingsCard(title = stringResource(R.string.settings_links)) {
            fun open(url: String) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching { context.startActivity(intent) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { open("https://youtube.com/@FSDETC") }) {
                    Text(stringResource(R.string.link_youtube))
                }
                OutlinedButton(onClick = { open("https://discord.gg/u8PdQ59qTv") }) {
                    Text(stringResource(R.string.link_discord))
                }
                OutlinedButton(onClick = { open("https://fsdmc.xyz") }) {
                    Text(stringResource(R.string.link_website))
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable Column.() -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = FsdSurface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = FsdOnSurface)
            content()
        }
    }
}
