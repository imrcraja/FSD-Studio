package com.fsdetc.fsdstudio.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fsdetc.fsdstudio.R
import com.fsdetc.fsdstudio.data.JavaVersion
import com.fsdetc.fsdstudio.data.Plan
import com.fsdetc.fsdstudio.data.SkinBitmaps
import com.fsdetc.fsdstudio.data.TotemArt
import com.fsdetc.fsdstudio.data.TotemEdition
import com.fsdetc.fsdstudio.data.TotemStyle
import com.fsdetc.fsdstudio.viewmodel.MainViewModel
import com.fsdetc.fsdstudio.viewmodel.TotemOutcome
import com.fsdetc.fsdstudio.viewmodel.TotemViewModel

/**
 * The Totem Maker: turn a player skin into a custom totem of undying.
 * Pick a skin (current character, an upload, or a fetched username), choose
 * Mini me / Full body, download the artwork PNG (free, gated like other
 * renders) or a ready-to-use texture pack (Pro/Max — Java .zip for four
 * versions, or a Bedrock .mcpack marked Beta).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TotemMakerScreen(
    mainVm: MainViewModel,
    totemVm: TotemViewModel,
    onBack: () -> Unit,
    onOpenPro: () -> Unit,
) {
    val context = LocalContext.current
    val character by mainVm.character.collectAsState()
    val plan by mainVm.plan.collectAsState()
    val downloadsLeft by mainVm.downloadsLeft.collectAsState()

    val skinBmp by totemVm.skin.collectAsState()
    val skinName by totemVm.skinName.collectAsState()
    val loading by totemVm.loading.collectAsState()
    val errorRes by totemVm.errorRes.collectAsState()
    val style by totemVm.style.collectAsState()

    var username by remember { mutableStateOf("") }
    var edition by remember { mutableStateOf(TotemEdition.JAVA) }
    var javaVersion by remember { mutableStateOf(JavaVersion.V1_20_1) }
    var busy by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { totemVm.loadUpload(it) } }

    LaunchedEffect(character) { totemVm.bootstrap(character) }

    val preview = remember(skinBmp, style) {
        skinBmp?.let { TotemArt.render(it, style, TotemViewModel.PREVIEW_SCALE) }
    }
    val thumb = remember(skinBmp) {
        skinBmp?.let { SkinBitmaps.renderFrontScaled(it, 4) }
    }

    fun onPngOutcome(outcome: TotemOutcome) {
        busy = false
        when (outcome) {
            TotemOutcome.SAVED ->
                mainVm.postSnackbar(context.getString(R.string.saved_to_gallery))
            TotemOutcome.FAILED ->
                mainVm.postSnackbar(context.getString(R.string.saved_failed))
            TotemOutcome.LIMIT -> {
                mainVm.postSnackbar(context.getString(R.string.dl_limit_reached))
                onOpenPro()
            }
            TotemOutcome.LOCKED -> onOpenPro()
        }
    }

    fun onPackOutcome(outcome: TotemOutcome) {
        busy = false
        when (outcome) {
            TotemOutcome.SAVED ->
                mainVm.postSnackbar(context.getString(R.string.totem_saved_downloads))
            TotemOutcome.FAILED ->
                mainVm.postSnackbar(context.getString(R.string.saved_failed))
            TotemOutcome.LIMIT, TotemOutcome.LOCKED -> onOpenPro()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.totem_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
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
            // ---------------------------------------------------------- source
            SectionTitle(stringResource(R.string.totem_source_title))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (loading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.totem_loading),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else if (skinBmp != null && thumb != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Checkerboard(Modifier.fillMaxSize()) {}
                                Image(
                                    bitmap = thumb.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = skinName ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    val ch = character
                    if (ch != null && totemVm.isResolvable(ch)) {
                        OutlinedButton(
                            onClick = { totemVm.loadFromCharacter(ch) },
                            enabled = !loading,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.totem_use_current) + " · " + ch.name) }
                    } else if (ch != null) {
                        Text(
                            text = stringResource(R.string.totem_need_skin),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    OutlinedButton(
                        onClick = { picker.launch(arrayOf("image/png")) },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.totem_upload))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text(stringResource(R.string.totem_username_label)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(10.dp))
                        Button(
                            onClick = { totemVm.fetchUsername(username) },
                            enabled = username.isNotBlank() && !loading,
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(
                                    if (loading) R.string.totem_fetching else R.string.totem_fetch,
                                ),
                            )
                        }
                    }
                    errorRes?.let {
                        Text(
                            text = stringResource(it),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            // ---------------------------------------------------------- style
            SectionTitle(stringResource(R.string.totem_style_title))
            Row {
                FilterChip(
                    selected = style == TotemStyle.MINI,
                    onClick = { totemVm.setStyle(TotemStyle.MINI) },
                    label = { Text(stringResource(R.string.totem_style_mini)) },
                    modifier = Modifier.padding(end = 8.dp),
                )
                FilterChip(
                    selected = style == TotemStyle.FULL,
                    onClick = { totemVm.setStyle(TotemStyle.FULL) },
                    label = { Text(stringResource(R.string.totem_style_full)) },
                )
            }

            // ---------------------------------------------------------- preview
            SectionTitle(stringResource(R.string.totem_preview_title))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Checkerboard(Modifier.fillMaxSize()) {}
                if (preview != null) {
                    Image(
                        bitmap = preview.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                    // Preview-only branding; every download stays clean.
                    WatermarkOverlay(Modifier.fillMaxSize())
                } else {
                    Text(
                        text = stringResource(R.string.totem_no_skin),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }

            // ---------------------------------------------------------- PNG (free)
            Text(
                text = downloadsLeft?.let { stringResource(R.string.dl_left_today, it) }
                    ?: stringResource(R.string.dl_unlimited),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = {
                    if (busy) return@Button
                    busy = true
                    totemVm.downloadPng(::onPngOutcome)
                },
                enabled = skinBmp != null && !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                } else {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(R.string.totem_download_png))
            }

            // ---------------------------------------------------------- packs (Pro/Max)
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle(stringResource(R.string.totem_pack_title))
                if (plan == Plan.FREE) {
                    Spacer(Modifier.width(10.dp))
                    ProChip()
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.totem_edition_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row {
                        FilterChip(
                            selected = edition == TotemEdition.JAVA,
                            onClick = { edition = TotemEdition.JAVA },
                            label = { Text(stringResource(R.string.totem_edition_java)) },
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        FilterChip(
                            selected = edition == TotemEdition.BEDROCK,
                            onClick = { edition = TotemEdition.BEDROCK },
                            label = { Text(stringResource(R.string.totem_edition_bedrock)) },
                        )
                    }
                    if (edition == TotemEdition.JAVA) {
                        Text(
                            text = stringResource(R.string.totem_version_title),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Row {
                            JavaVersion.entries.forEach { v ->
                                FilterChip(
                                    selected = javaVersion == v,
                                    onClick = { javaVersion = v },
                                    label = { Text(v.label) },
                                    modifier = Modifier.padding(end = 8.dp),
                                )
                            }
                        }
                    }
                    Button(
                        onClick = {
                            if (plan == Plan.FREE) {
                                onOpenPro()
                                return@Button
                            }
                            if (busy) return@Button
                            busy = true
                            when (edition) {
                                TotemEdition.JAVA ->
                                    totemVm.downloadJavaPack(javaVersion, ::onPackOutcome)
                                TotemEdition.BEDROCK ->
                                    totemVm.downloadBedrockPack(::onPackOutcome)
                            }
                        },
                        enabled = skinBmp != null && !busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (plan == Plan.FREE) {
                            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(stringResource(R.string.totem_download_pack))
                    }
                    Text(
                        text = stringResource(R.string.totem_pack_pro_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
