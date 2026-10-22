package com.fsdetc.fsdstudio.ui

import android.app.Activity
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.imageLoader
import com.fsdetc.fsdstudio.FsdStudioApp
import com.fsdetc.fsdstudio.R
import com.fsdetc.fsdstudio.data.Api
import com.fsdetc.fsdstudio.data.ImageSaver
import com.fsdetc.fsdstudio.data.Plan
import com.fsdetc.fsdstudio.data.UserSettings
import com.fsdetc.fsdstudio.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun SettingsScreen(onOpenPro: () -> Unit, onReplayTutorial: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as FsdStudioApp
    val container = app.container
    val settings = container.userSettings
    val clipboard = LocalClipboardManager.current

    var cacheSizeBytes by remember { mutableStateOf<Long?>(null) }
    var clearedNote by remember { mutableStateOf(false) }

    fun cacheBytes(): Long =
        ImageSaver.folderSizeBytes(File(context.cacheDir, "http_cache")) +
            ImageSaver.folderSizeBytes(File(context.cacheDir, "coil_cache"))

    LaunchedEffect(Unit) { cacheSizeBytes = cacheBytes() }

    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.2"
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

        // ------------------------------------------------ account
        val character by container.characterStore.character.collectAsState(initial = null)
        var userId by remember { mutableStateOf<String?>(null) }
        var logoutDialog by remember { mutableStateOf(false) }
        var resetDialog by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { userId = settings.ensureUserId() }
        SettingsCard(title = stringResource(R.string.settings_account)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val faceId = character?.renderId
                if (faceId != null) {
                    AsyncImage(
                        model = Api.renderUrl(faceId, "face", 128),
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.fsd_logo),
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    )
                }
                Spacer(Modifier.size(12.dp))
                Text(
                    text = character?.name ?: stringResource(R.string.account_guest),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.account_user_id) + ": " + (userId ?: "…"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        userId?.let { id ->
                            clipboard.setText(AnnotatedString(id))
                            container.postSnackbar(context.getString(R.string.account_user_id_copied))
                        }
                    },
                ) {
                    Text(stringResource(R.string.account_copy))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { logoutDialog = true }) {
                    Text(stringResource(R.string.account_logout))
                }
                OutlinedButton(onClick = { resetDialog = true }) {
                    Text(
                        text = stringResource(R.string.account_reset),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        if (logoutDialog) {
            AlertDialog(
                onDismissRequest = { logoutDialog = false },
                title = { Text(stringResource(R.string.account_logout_title)) },
                text = { Text(stringResource(R.string.account_logout_body)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            logoutDialog = false
                            scope.launch {
                                container.logout()
                                container.postSnackbar(context.getString(R.string.account_logged_out))
                            }
                        },
                    ) { Text(stringResource(R.string.account_logout)) }
                },
                dismissButton = {
                    TextButton(onClick = { logoutDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
            )
        }
        if (resetDialog) {
            AlertDialog(
                onDismissRequest = { resetDialog = false },
                title = { Text(stringResource(R.string.account_reset_title)) },
                text = { Text(stringResource(R.string.account_reset_body)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            resetDialog = false
                            scope.launch {
                                container.resetAllData()
                                context.imageLoader.memoryCache?.clear()
                                withContext(Dispatchers.IO) {
                                    runCatching { context.imageLoader.diskCache?.clear() }
                                }
                                cacheSizeBytes = 0L
                                userId = settings.ensureUserId()
                                container.postSnackbar(context.getString(R.string.account_reset_done))
                            }
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.account_reset),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { resetDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
            )
        }

        // ------------------------------------------------ language (real switching)
        val appLanguage by settings.appLanguage.collectAsState(initial = UserSettings.DEFAULT_LANGUAGE)
        SettingsCard(title = stringResource(R.string.settings_language)) {
            UserSettings.LANGUAGE_CODES.forEach { code ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = appLanguage == code,
                        onClick = {
                            if (appLanguage != code) {
                                scope.launch {
                                    settings.setAppLanguage(code)
                                    (context as? Activity)?.recreate()
                                }
                            }
                        },
                    )
                    Text(
                        text = stringResource(languageLabelRes(code)),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
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
        val plan by container.entitlements.plan.collectAsState(initial = Plan.FREE)
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

        // ------------------------------------------------ AI (built in, no setup)
        SettingsCard(title = stringResource(R.string.settings_ai)) {
            Text(
                text = stringResource(R.string.settings_ai_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ------------------------------------------------ tutorial
        SettingsCard(title = stringResource(R.string.settings_tutorial)) {
            TextButton(onClick = onReplayTutorial) {
                Text(stringResource(R.string.tut_replay))
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
                            runCatching { container.skinRepository.httpClient.cache?.evictAll() }
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

/** Native display name for each supported language code. */
private fun languageLabelRes(code: String): Int = when (code) {
    "hi" -> R.string.language_hindi
    "bn" -> R.string.language_bangla
    "ur" -> R.string.language_urdu
    "es" -> R.string.language_spanish
    "id" -> R.string.language_indonesian
    else -> R.string.language_english
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
