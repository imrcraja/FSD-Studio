package com.fsdetc.fsdstudio.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fsdetc.fsdstudio.FsdStudioApp
import com.fsdetc.fsdstudio.R
import com.fsdetc.fsdstudio.data.Api
import com.fsdetc.fsdstudio.data.BedrockTotemPack
import com.fsdetc.fsdstudio.data.CharacterInfo
import com.fsdetc.fsdstudio.data.CharacterSource
import com.fsdetc.fsdstudio.data.DownloadOutcome
import com.fsdetc.fsdstudio.data.FileSaver
import com.fsdetc.fsdstudio.data.ImageSaver
import com.fsdetc.fsdstudio.data.JavaTotemPack
import com.fsdetc.fsdstudio.data.JavaVersion
import com.fsdetc.fsdstudio.data.Plan
import com.fsdetc.fsdstudio.data.PlayerNotFoundException
import com.fsdetc.fsdstudio.data.SkinBitmaps
import com.fsdetc.fsdstudio.data.TotemArt
import com.fsdetc.fsdstudio.data.TotemStyle
import com.fsdetc.fsdstudio.data.toErrorKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Result of a Totem Maker download, mapped to snackbar / navigation by the screen. */
enum class TotemOutcome { SAVED, FAILED, LIMIT, LOCKED }

/**
 * State and operations for the Totem Maker.
 *
 * Sources: the current character (default), an uploaded skin PNG, or a
 * fetched username — the same three ways the rest of the app resolves a
 * player skin. Mob *models* never become the current character in this app
 * (they live only as render-detail requests), so the reachable "no totem
 * skin" state is simply an unresolvable/absent character: the screen then
 * shows the mob note plus the upload / username options.
 *
 * Gating (owner's rules): the PNG export goes through DownloadGate like
 * every other render download; the two pack downloads are the ONLY
 * plan-checked actions here (Pro/Max), checked in the download functions
 * themselves, not just in the UI.
 */
class TotemViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FsdStudioApp
    private val container = app.container

    /** The loaded 64×64 skin (base + overlay, as-is), or null. */
    val skin = MutableStateFlow<Bitmap?>(null)

    /** Display name of the loaded skin — used in pack descriptions + file names. */
    val skinName = MutableStateFlow<String?>(null)

    val loading = MutableStateFlow(false)

    /** @StringRes of the last load failure, or null. */
    val errorRes = MutableStateFlow<Int?>(null)

    val style = MutableStateFlow(TotemStyle.FULL)

    private var initialized = false

    fun setStyle(style: TotemStyle) {
        this.style.value = style
    }

    // ------------------------------------------------------------ sources

    fun isResolvable(character: CharacterInfo): Boolean = when (character.source) {
        CharacterSource.UPLOAD -> character.localSkinPath != null
        CharacterSource.USERNAME -> character.skinUrl != null || character.renderId != null
    }

    /** Auto-start from the current character the first time the screen opens. */
    fun bootstrap(character: CharacterInfo?) {
        if (initialized || loading.value) return
        if (character == null || !isResolvable(character)) return
        loadFromCharacter(character)
    }

    fun loadFromCharacter(character: CharacterInfo) {
        loading.value = true
        errorRes.value = null
        viewModelScope.launch {
            val bmp = runCatching {
                when (character.source) {
                    CharacterSource.UPLOAD -> character.localSkinPath?.let {
                        SkinBitmaps.decodeSkinFile(File(it))
                    }
                    CharacterSource.USERNAME -> {
                        val url = character.skinUrl
                            ?: character.renderId?.let { Api.renderUrl(it, "skin", 64) }
                        url?.let {
                            SkinBitmaps.decodeSkinBytes(container.skinRepository.downloadBytes(it))
                        }
                    }
                }
            }.getOrNull()
            loading.value = false
            if (bmp != null) {
                setSkin(bmp, character.name)
            } else {
                errorRes.value = R.string.error_generic
            }
        }
    }

    fun loadUpload(uri: Uri) {
        loading.value = true
        errorRes.value = null
        viewModelScope.launch {
            val bmp = runCatching {
                val bytes = withContext(Dispatchers.IO) {
                    app.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: error("unreadable")
                SkinBitmaps.decodeSkinBytes(bytes)
            }.getOrNull()
            loading.value = false
            if (bmp != null) {
                setSkin(bmp, "Custom")
            } else {
                errorRes.value = R.string.error_upload_invalid
            }
        }
    }

    /** Resolve a username through the normal profile flow, then load its skin texture. */
    fun fetchUsername(rawName: String) {
        val name = rawName.trim()
        if (name.isEmpty() || loading.value) return
        loading.value = true
        errorRes.value = null
        viewModelScope.launch {
            try {
                val info = container.skinRepository.fetchProfile(name)
                val url = info.skinUrl
                    ?: info.renderId?.let { Api.renderUrl(it, "skin", 64) }
                val bmp = url?.let {
                    SkinBitmaps.decodeSkinBytes(container.skinRepository.downloadBytes(it))
                }
                if (bmp != null) {
                    setSkin(bmp, info.name)
                } else {
                    errorRes.value = R.string.error_generic
                }
            } catch (e: PlayerNotFoundException) {
                errorRes.value = R.string.error_player_not_found
            } catch (e: Exception) {
                errorRes.value = e.toErrorKind().messageRes
            } finally {
                loading.value = false
            }
        }
    }

    private fun setSkin(bmp: Bitmap, name: String) {
        skin.value = bmp
        skinName.value = name
        initialized = true
        errorRes.value = null
    }

    // ------------------------------------------------------------ downloads

    /**
     * PNG export (FREE): the clean preview artwork at export resolution,
     * saved to the gallery through DownloadGate (counts toward the free
     * daily limit; Pro/Max unlimited, as everywhere else).
     */
    fun downloadPng(onOutcome: (TotemOutcome) -> Unit) {
        val current = skin.value ?: return onOutcome(TotemOutcome.FAILED)
        val name = skinName.value ?: "Custom"
        val st = style.value
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.Default) {
                SkinBitmaps.bitmapToPngBytes(TotemArt.render(current, st, EXPORT_SCALE))
            }
            val outcome = container.downloadGate.saveWithGate {
                val dir = File(app.cacheDir, "shared").apply { mkdirs() }
                val tmp = File(dir, "totem_${System.currentTimeMillis()}.png")
                tmp.writeBytes(bytes)
                val ok = ImageSaver.saveToGallery(app, tmp, "FSD_Totem_${fileSafe(name)}.png")
                if (ok) {
                    recordHistory(
                        imageBytes = bytes,
                        title = "Totem — $name",
                        subtitle = styleLabel(st),
                        uniqueKey = "totem:png:" + SkinBitmaps.sha256Hex(bytes),
                    )
                }
                ok
            }
            onOutcome(
                when (outcome) {
                    DownloadOutcome.Saved -> TotemOutcome.SAVED
                    DownloadOutcome.LimitReached -> TotemOutcome.LIMIT
                    DownloadOutcome.Failed -> TotemOutcome.FAILED
                },
            )
        }
    }

    /** Java texture pack (Pro/Max only — the plan is checked here, not just in the UI). */
    fun downloadJavaPack(version: JavaVersion, onOutcome: (TotemOutcome) -> Unit) {
        val current = skin.value ?: return onOutcome(TotemOutcome.FAILED)
        val name = skinName.value ?: "Custom"
        val st = style.value
        viewModelScope.launch {
            if (container.entitlements.currentPlan() == Plan.FREE) {
                onOutcome(TotemOutcome.LOCKED)
                return@launch
            }
            val pack = runCatching {
                withContext(Dispatchers.Default) {
                    JavaTotemPack.build(
                        SkinBitmaps.bitmapToPngBytes(current), name, st, version,
                    )
                }
            }.getOrNull() ?: run {
                onOutcome(TotemOutcome.FAILED)
                return@launch
            }
            val ok = FileSaver.saveToDownloads(
                app, pack,
                "FSD_Totem_${fileSafe(name)}_Java_${version.label}.zip",
                "application/zip",
            )
            if (ok) {
                recordHistory(
                    imageBytes = previewBytes(current, st),
                    title = "Totem pack (Java ${version.label}) — $name",
                    subtitle = "Texture pack",
                    uniqueKey = "totem:pack:" + SkinBitmaps.sha256Hex(pack),
                )
            }
            onOutcome(if (ok) TotemOutcome.SAVED else TotemOutcome.FAILED)
        }
    }

    /** Bedrock .mcpack (Pro/Max only, Beta — same gate as the Java pack). */
    fun downloadBedrockPack(onOutcome: (TotemOutcome) -> Unit) {
        val current = skin.value ?: return onOutcome(TotemOutcome.FAILED)
        val name = skinName.value ?: "Custom"
        val st = style.value
        viewModelScope.launch {
            if (container.entitlements.currentPlan() == Plan.FREE) {
                onOutcome(TotemOutcome.LOCKED)
                return@launch
            }
            val pack = runCatching {
                withContext(Dispatchers.Default) {
                    BedrockTotemPack.build(SkinBitmaps.bitmapToPngBytes(current), name, st)
                }
            }.getOrNull() ?: run {
                onOutcome(TotemOutcome.FAILED)
                return@launch
            }
            val ok = FileSaver.saveToDownloads(
                app, pack,
                "FSD_Totem_${fileSafe(name)}_Bedrock.mcpack",
                "application/zip",
            )
            if (ok) {
                recordHistory(
                    imageBytes = previewBytes(current, st),
                    title = "Totem pack (Bedrock) — $name",
                    subtitle = "Texture pack",
                    uniqueKey = "totem:pack:" + SkinBitmaps.sha256Hex(pack),
                )
            }
            onOutcome(if (ok) TotemOutcome.SAVED else TotemOutcome.FAILED)
        }
    }

    // ------------------------------------------------------------ helpers

    private fun previewBytes(skinBmp: Bitmap, st: TotemStyle): ByteArray =
        SkinBitmaps.bitmapToPngBytes(TotemArt.render(skinBmp, st, PREVIEW_SCALE))

    /**
     * History rows are image-backed, so a pack download records its artwork
     * (the preview render) as the entry's image — the entry shows what the
     * pack looks like; the pack file itself lives in Downloads.
     */
    private suspend fun recordHistory(
        imageBytes: ByteArray,
        title: String,
        subtitle: String,
        uniqueKey: String,
    ) {
        runCatching {
            container.historyRepository.recordBytes(
                imageBytes,
                kind = "TOTEM",
                title = title,
                subtitle = subtitle,
                sourceName = skinName.value ?: "Custom",
                uniqueKey = uniqueKey,
            )
        }
    }

    private fun styleLabel(st: TotemStyle): String =
        if (st == TotemStyle.MINI) "Mini me" else "Full body"

    private fun fileSafe(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._-]"), "_")

    companion object {
        /** Preview artwork at 24 px per unit (384 px wide). */
        const val PREVIEW_SCALE = 24

        /** Export artwork at 48 px per unit (768 px wide — high-res). */
        const val EXPORT_SCALE = 48
    }
}
