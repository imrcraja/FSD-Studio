package com.fsdetc.fsdstudio.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.fsdetc.fsdstudio.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * User-facing preferences that are not entitlements: app theme, render defaults
 * and the optional bring-your-own Gemini API key (stored locally only, never
 * leaves the device except to Google's API as its auth header).
 */
class UserSettings(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme")
    private val defaultBgKey = stringPreferencesKey("default_bg")
    private val defaultSizeKey = intPreferencesKey("default_size")
    private val geminiKeyKey = stringPreferencesKey("gemini_key")

    // ------------------------------------------------------------ theme

    val theme: Flow<AppTheme> = context.dataStore.data.map { p ->
        runCatching { AppTheme.valueOf(p[themeKey] ?: "") }.getOrDefault(AppTheme.FSD_PURPLE)
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { p -> p[themeKey] = theme.name }
    }

    // ------------------------------------------------------------ render defaults

    /** Name of the BgOption the detail screens should start with. */
    val defaultBgName: Flow<String> = context.dataStore.data.map { p ->
        p[defaultBgKey] ?: DEFAULT_BG
    }

    suspend fun setDefaultBg(name: String) {
        context.dataStore.edit { p -> p[defaultBgKey] = name }
    }

    /** Preferred render size; clamped by the plan wherever it is used. */
    val defaultSize: Flow<Int> = context.dataStore.data.map { p ->
        p[defaultSizeKey] ?: DEFAULT_SIZE
    }

    suspend fun setDefaultSize(size: Int) {
        context.dataStore.edit { p -> p[defaultSizeKey] = size }
    }

    // ------------------------------------------------------------ Gemini key

    /** The user's own Gemini API key, or null when none is stored. */
    val geminiKey: Flow<String?> = context.dataStore.data.map { p ->
        p[geminiKeyKey]?.takeIf { it.isNotBlank() }
    }

    suspend fun geminiKey(): String? = geminiKey.first()

    /** Save a key; null or blank clears it. */
    suspend fun setGeminiKey(key: String?) {
        context.dataStore.edit { p ->
            val trimmed = key?.trim()
            if (trimmed.isNullOrEmpty()) p.remove(geminiKeyKey) else p[geminiKeyKey] = trimmed
        }
    }

    companion object {
        const val DEFAULT_BG = "Transparent"
        const val DEFAULT_SIZE = 512
    }
}
