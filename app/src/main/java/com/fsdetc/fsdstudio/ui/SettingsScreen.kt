package com.fsdetc.fsdstudio.ui

import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.imageLoader
import com.fsdetc.fsdstudio.FsdStudioApp
import com.fsdetc.fsdstudio.R
import com.fsdetc.fsdstudio.data.ImageSaver
import com.fsdetc.fsdstudio.data.Plan
import com.fsdetc.fsdstudio.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun SettingsScreen(onOpenPro: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as FsdStudioApp
    val settings = app.container.userSettings

    var cacheSizeBytes by remember { mutableStateOf<Long?>(null) }
    var clearedNote by remember { mutableStateOf(false) }

    fun cacheBytes(): Long =
        ImageSaver.folderSizeBytes(File(context.cacheDir, "http_cache")) +
            ImageSaver.folderSizeBytes(File(context.cacheDir, "coil_cache"))

    LaunchedEffect(Unit) { cacheSizeBytes = cacheBytes() }

    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.1"
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
            color = MaterialTheme.colorScheme.onSurface,
        )

        // ------------------------------------------------ language
        SettingsCard(title = stringResource(R.string.settings_language)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = true, onClick = { /* English is the active language. */ })
                Text(stringResource(R.string.language_english), color = MaterialTheme.colorScheme.onSurface)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = false, onClick = {}, enabled = false)
                Text(
                    text = stringResource(R.string.language_hindi) + " · " +
                        stringResource(R.string.coming_soon),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = false, onClick = {}, enabled = false)
                Text(
                    text = stringResource(R.string.language_bangla) + " · " +
                        stringResource(R.string.coming_soon),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ------------------------------------------------ theme
        val theme by settings.theme.collectAsState(initial = AppTheme.FSD_PURPLE)
        SettingsCard(title = stringResource(R.string.settings_theme)) {
            ThemeRow(
                label = stringResource(R.string.theme_purple),
                selected = theme == AppTheme.FSD_PURPLE,
            ) { scope.launch { settings.setTheme(AppTheme.FSD_PURPLE) } }
            ThemeRow(
                label = stringResource(R.string.theme_midnight),
                selected = theme == AppTheme.MIDNIGHT,
            ) { scope.launch { settings.setTheme(AppTheme.MIDNIGHT) } }
            ThemeRow(
                label = stringResource(R.string.theme_light),
                selected = theme == AppTheme.LIGHT,
            ) { scope.launch { settings.setTheme(AppTheme.LIGHT) } }
        }

        // ------------------------------------------------ render defaults
        val defaultBgName by settings.defaultBgName.collectAsState(initial = "Transparent")
        val defaultSize by settings.defaultSize.collectAsState(initial = 512)
        SettingsCard(title = stringResource(R.string.settings_defaults)) {
            Text(
                text = stringResource(R.string.settings_default_bg),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            BgSelector(
                selected = BgOption.fromName(defaultBgName),
                onSelect = { scope.launch { settings.setDefaultBg(it.name) } },
            )
            Text(
                text = stringResource(R.string.settings_default_size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row {
                listOf(512, 1024).forEach { sizeOption ->
                    FilterChip(
                        selected = defaultSize == sizeOption,
                        onClick = { scope.launch { settings.setDefaultSize(sizeOption) } },
                        label = { Text(sizeOption.toString()) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
        }

        // ------------------------------------------------ plan
        val plan by app.container.entitlements.plan.collectAsState(initial = Plan.FREE)
        SettingsCard(title = stringResource(R.string.sub_title)) {
            Text(
                text = stringResource(
                    R.string.sub_current_plan,
                    stringResource(
                        when (plan) {
                            Plan.FREE -> R.string.plan_name_free
                            Plan.PRO -> R.string.plan_name_pro
                            Plan.MAX -> R.string.plan_name_max
                        },
                    ),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onOpenPro) {
                Text(stringResource(R.string.sub_view_plans))
            }
        }

        // ------------------------------------------------ AI engine / Gemini key
        val geminiKey by settings.geminiKey.collectAsState(initial = null)
        var keyInput by remember { mutableStateOf("") }
        var keySavedNote by remember { mutableStateOf(false) }
        SettingsCard(title = stringResource(R.string.settings_ai)) {
            Text(
                text = stringResource(R.string.settings_gemini_explain),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    if (geminiKey != null) {
                        R.string.settings_ai_engine_gemini
                    } else {
                        R.string.settings_ai_engine_free
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it; keySavedNote = false },
                label = { Text(stringResource(R.string.settings_gemini_label)) },
                placeholder = { Text(stringResource(R.string.settings_gemini_hint)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        val toSave = keyInput.trim()
                        if (toSave.isNotEmpty()) {
                            scope.launch {
                                settings.setGeminiKey(toSave)
                                keyInput = ""
                                keySavedNote = true
                            }
                        }
                    },
                ) {
                    Text(stringResource(R.string.settings_gemini_save))
                }
                if (geminiKey != null) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                settings.setGeminiKey(null)
                                keySavedNote = false
                            }
                        },
                    ) {
                        Text(stringResource(R.string.settings_gemini_clear))
                    }
                }
            }
            if (keySavedNote) {
                Text(
                    text = stringResource(R.string.settings_gemini_saved),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.about_credits),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.about_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun ThemeRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            content()
        }
    }
}
