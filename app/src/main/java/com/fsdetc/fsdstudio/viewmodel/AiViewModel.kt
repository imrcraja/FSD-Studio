package com.fsdetc.fsdstudio.viewmodel

import android.app.Application
import android.graphics.BitmapFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fsdetc.fsdstudio.FsdStudioApp
import com.fsdetc.fsdstudio.R
import com.fsdetc.fsdstudio.data.Api
import com.fsdetc.fsdstudio.data.CharacterInfo
import com.fsdetc.fsdstudio.data.CharacterSource
import com.fsdetc.fsdstudio.data.DownloadGate
import com.fsdetc.fsdstudio.data.Plan
import com.fsdetc.fsdstudio.data.SkinBitmaps
import com.fsdetc.fsdstudio.data.toErrorKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * AI tab state — EDIT mode. The tab edits the current character's render:
 * the user's text is an edit instruction, sent to the one built-in engine
 * (Pollinations) together with the render's public URL as the reference
 * image; on failure the engine itself retries without the reference.
 * Uploaded skins have no public render URL, so their edits run prompt-only.
 *
 * Queue discipline: at most one edit in flight; the daily counter is checked
 * BEFORE any request is made and only incremented on success.
 */
class AiViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FsdStudioApp
    private val container = app.container

    val downloadGate: DownloadGate = container.downloadGate

    /** Post a transient message to the nav host's bottom SnackbarHost. */
    fun postSnackbar(message: String) = container.postSnackbar(message)

    val character: StateFlow<CharacterInfo?> = container.characterStore.character
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val plan: StateFlow<Plan> = container.entitlements.plan
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Plan.FREE)

    val remaining = MutableStateFlow(0)

    var prompt by mutableStateOf("")
    var generating by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var resultPath by mutableStateOf<String?>(null)

    init {
        refreshCount()
    }

    fun refreshCount() {
        viewModelScope.launch { refreshRemaining() }
    }

    private suspend fun refreshRemaining() {
        val used = container.entitlements.aiUsedToday()
        remaining.value =
            (container.entitlements.aiPerDay(plan.value) - used).coerceAtLeast(0)
    }

    /**
     * The render this tab edits: the current character's default body render
     * (512 px). Null for uploaded skins — they have no public render URL.
     */
    fun sourceRenderUrl(ch: CharacterInfo?): String? {
        if (ch == null || ch.source != CharacterSource.USERNAME) return null
        val renderId = ch.renderId ?: return null
        return Api.renderUrl(renderId, "body", 512)
    }

    fun generate() {
        if (generating) return
        val userPrompt = prompt.trim()
        if (userPrompt.isEmpty()) {
            error = app.getString(R.string.ai_need_prompt)
            return
        }
        error = null
        viewModelScope.launch {
            if (remaining.value <= 0) {
                refreshRemaining()
                if (remaining.value <= 0) {
                    error = app.getString(R.string.ai_limit_reached)
                    return@launch
                }
            }
            generating = true
            try {
                val ch = character.value
                val referenceUrl = sourceRenderUrl(ch)
                val fullPrompt = if (referenceUrl != null) {
                    "Edit this Minecraft character render: $userPrompt. " +
                        "Keep the same character, the same skin and outfit, and the " +
                        "same Minecraft blocky style — only apply the requested change."
                } else {
                    "$userPrompt, Minecraft blocky voxel character, full body, game render style"
                }

                val rawBytes = container.pollinationsEngine
                    .generate(fullPrompt, referenceUrl)
                    .getOrThrow()

                // The engine returns JPEG/PNG bytes; decode + re-encode to PNG
                // so every history file is a real PNG.
                val bmp = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                    ?: throw IOException("Bad image data")
                val pngBytes = SkinBitmaps.bitmapToPngBytes(bmp)
                val item = container.historyRepository.recordBytes(
                    pngBytes,
                    kind = "AI",
                    title = "AI edit",
                    subtitle = userPrompt,
                    sourceName = ch?.name ?: "Prompt only",
                    uniqueKey = "ai:${System.currentTimeMillis()}",
                )
                container.entitlements.recordAiUse()
                refreshRemaining()
                resultPath = item.filePath
            } catch (e: Exception) {
                error = app.getString(e.toErrorKind().messageRes)
            } finally {
                generating = false
            }
        }
    }
}
