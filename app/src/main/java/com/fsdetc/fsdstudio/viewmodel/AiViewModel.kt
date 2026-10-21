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
import com.fsdetc.fsdstudio.data.SkinBitmaps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.random.Random

/**
 * AI tab state. Queue discipline: at most one generation in flight; the daily
 * counter is checked BEFORE any request is made and only incremented on success.
 */
class AiViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FsdStudioApp
    private val container = app.container

    val character: StateFlow<CharacterInfo?> = container.characterStore.character
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isPro: StateFlow<Boolean> = container.entitlements.isPro
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

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
        remaining.value = (container.entitlements.aiLimit(isPro.value) - used).coerceAtLeast(0)
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
                val fullPrompt =
                    "$userPrompt, Minecraft blocky voxel character, full body, game render style"
                val referenceUrl = if (ch?.source == CharacterSource.USERNAME) {
                    ch.renderId?.let { Api.renderUrl(it, "body", 512) }
                } else {
                    null
                }
                val seed = Random.nextInt(1, Int.MAX_VALUE)
                val rawBytes = container.skinRepository.generateAiImage(fullPrompt, seed, referenceUrl)
                // The AI service returns JPEG; decode + re-encode to PNG so every
                // history file is a real PNG.
                val bmp = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                    ?: throw IOException("Bad image data")
                val pngBytes = SkinBitmaps.bitmapToPngBytes(bmp)
                val item = container.historyRepository.recordBytes(
                    pngBytes,
                    kind = "AI",
                    title = "AI render",
                    subtitle = userPrompt,
                    sourceName = ch?.name ?: "Prompt only",
                    uniqueKey = "ai:${System.currentTimeMillis()}:$seed",
                )
                container.entitlements.recordAiUse()
                refreshRemaining()
                resultPath = item.filePath
            } catch (e: Exception) {
                error = app.getString(R.string.ai_error)
            } finally {
                generating = false
            }
        }
    }
}
