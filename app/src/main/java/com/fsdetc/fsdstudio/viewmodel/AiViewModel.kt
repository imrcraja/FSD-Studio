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
import com.fsdetc.fsdstudio.data.AiEngineException
import com.fsdetc.fsdstudio.data.AiError
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
 * AI tab state. Queue discipline: at most one generation in flight; the daily
 * counter is checked BEFORE any request is made and only incremented on success.
 *
 * Engine selection: when the user has stored their own Gemini key, Gemini runs
 * first (best character likeness); ANY Gemini failure falls back to the free
 * built-in engine and surfaces a small note. Without a key, the free engine is
 * used directly.
 */
class AiViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FsdStudioApp
    private val container = app.container

    val downloadGate: DownloadGate = container.downloadGate

    val character: StateFlow<CharacterInfo?> = container.characterStore.character
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val plan: StateFlow<Plan> = container.entitlements.plan
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Plan.FREE)

    val remaining = MutableStateFlow(0)

    var prompt by mutableStateOf("")
    var generating by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    /** Small note under a successful result, e.g. when the free engine took over. */
    var engineNote by mutableStateOf<String?>(null)
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

    fun generate() {
        if (generating) return
        val userPrompt = prompt.trim()
        if (userPrompt.isEmpty()) {
            error = app.getString(R.string.ai_need_prompt)
            return
        }
        error = null
        engineNote = null
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
                val fullPrompt =
                    "$userPrompt, Minecraft blocky voxel character, full body, game render style"
                val renderId = if (ch?.source == CharacterSource.USERNAME) ch.renderId else null
                val referenceUrl = renderId?.let { Api.renderUrl(it, "body", 512) }

                val rawBytes = generateWithEngineSelection(fullPrompt, referenceUrl, renderId)

                // Engines return JPEG/PNG bytes; decode + re-encode to PNG so
                // every history file is a real PNG.
                val bmp = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                    ?: throw IOException("Bad image data")
                val pngBytes = SkinBitmaps.bitmapToPngBytes(bmp)
                val item = container.historyRepository.recordBytes(
                    pngBytes,
                    kind = "AI",
                    title = "AI render",
                    subtitle = userPrompt,
                    sourceName = ch?.name ?: "Prompt only",
                    uniqueKey = "ai:${System.currentTimeMillis()}",
                )
                container.entitlements.recordAiUse()
                refreshRemaining()
                resultPath = item.filePath
            } catch (e: Exception) {
                error = errorMessageFor(e)
            } finally {
                generating = false
            }
        }
    }

    /**
     * Gemini (user's key) first when a key exists; on ANY Gemini failure the
     * free engine takes over and [engineNote] explains the switch. When both
     * fail, the Gemini error wins if it is a quota/key problem, otherwise the
     * free engine's failure is thrown.
     */
    private suspend fun generateWithEngineSelection(
        fullPrompt: String,
        referenceUrl: String?,
        renderId: String?,
    ): ByteArray {
        val hasKey = container.userSettings.geminiKey() != null
        if (!hasKey) {
            return container.pollinationsEngine
                .generate(fullPrompt, referenceUrl, null)
                .getOrThrow()
        }
        val referenceBytes = renderId?.let { id ->
            runCatching {
                container.skinRepository.downloadBytes(Api.renderUrl(id, "body", 512))
            }.getOrNull()
        }
        val geminiResult = container.geminiEngine.generate(fullPrompt, null, referenceBytes)
        val geminiBytes = geminiResult.getOrNull()
        if (geminiBytes != null) return geminiBytes

        val geminiError = geminiResult.exceptionOrNull()
        val freeResult = container.pollinationsEngine.generate(fullPrompt, referenceUrl, null)
        val freeBytes = freeResult.getOrNull()
        if (freeBytes != null) {
            engineNote = app.getString(R.string.ai_fallback_note)
            return freeBytes
        }
        // Both engines failed: prefer a quota/key explanation when there is one.
        if (geminiError is AiEngineException &&
            (geminiError.error == AiError.Quota || geminiError.error == AiError.BadKey)
        ) {
            throw geminiError
        }
        throw freeResult.exceptionOrNull() ?: geminiError ?: IOException("AI request failed")
    }

    private fun errorMessageFor(e: Throwable): String {
        if (e is AiEngineException) {
            return when (val err = e.error) {
                AiError.Quota -> app.getString(R.string.ai_error_quota)
                AiError.BadKey -> app.getString(R.string.ai_error_badkey)
                AiError.Timeout -> app.getString(R.string.error_timeout)
                is AiError.Other -> app.getString(err.cause.toErrorKind().messageRes)
            }
        }
        return app.getString(e.toErrorKind().messageRes)
    }
}
