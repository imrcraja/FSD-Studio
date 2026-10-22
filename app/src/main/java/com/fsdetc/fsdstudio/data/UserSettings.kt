package com.fsdetc.fsdstudio.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.fsdetc.fsdstudio.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * User-facing preferences that are not entitlements: app theme, render
 * defaults, language, user id and the tutorial flag. (The AI engine is fully
 * built in — there is no user API key anywhere in the app.)
 */
class UserSettings(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme")
    private val defaultBgKey = stringPreferencesKey("default_bg")
    private val defaultSizeKey = intPreferencesKey("default_size")
    private val languageKey = stringPreferencesKey("app_language")
    private val userIdKey = stringPreferencesKey("user_id")
    private val seenTutorialKey = booleanPreferencesKey("seen_tutorial")

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

    // ------------------------------------------------------------ language

    /**
     * In-app language ("en" default; also "hi", "bn", "ur", "es", "id").
     * Translations are first-pass (see the values-hi / values-bn / … folders).
     *
     * The value is mirrored into SharedPreferences ("fsd_prefs") because
     * MainActivity.attachBaseContext must read it synchronously, before any
     * coroutine/DataStore read could complete.
     */
    val appLanguage: Flow<String> = context.dataStore.data.map { p ->
        p[languageKey] ?: DEFAULT_LANGUAGE
    }

    suspend fun setAppLanguage(code: String) {
        context.dataStore.edit { p -> p[languageKey] = code }
        writeLanguageMirror(code)
    }

    private fun writeLanguageMirror(code: String) {
        // commit() (not apply()) so the value is on disk before the activity
        // is recreated and attachBaseContext reads it back.
        context.getSharedPreferences(LANGUAGE_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(LANGUAGE_KEY, code)
            .commit()
    }

    // ------------------------------------------------------------ user id

    /** Stable per-install user id ("FSD-XXXXXXXXXX"), shown in Settings → Account. */
    val userId: Flow<String?> = context.dataStore.data.map { p -> p[userIdKey] }

    /** Read the id, generating and persisting it on first need. */
    suspend fun ensureUserId(): String {
        context.dataStore.data.first()[userIdKey]?.let { return it }
        val generated = "FSD-" + UUID.randomUUID().toString()
            .replace("-", "")
            .take(10)
            .uppercase()
        context.dataStore.edit { p -> p[userIdKey] = generated }
        return generated
    }

    // ------------------------------------------------------------ tutorial

    /** Whether the first-run tutorial has been finished or skipped. */
    val seenTutorial: Flow<Boolean> = context.dataStore.data.map { p ->
        p[seenTutorialKey] ?: false
    }

    suspend fun setSeenTutorial(seen: Boolean) {
        context.dataStore.edit { p -> p[seenTutorialKey] = seen }
    }

    // ------------------------------------------------------------ log out

    /**
     * Log-out reset: theme, render defaults and language return to defaults.
     * Deliberately KEPT (handled by the callers' scope): history, the daily
     * counters and the user id.
     */
    suspend fun clearAccountPrefs() {
        context.dataStore.edit { p ->
            p.remove(themeKey)
            p.remove(defaultBgKey)
            p.remove(defaultSizeKey)
            p.remove(languageKey)
        }
        writeLanguageMirror(DEFAULT_LANGUAGE)
    }

    companion object {
        const val DEFAULT_BG = "Transparent"
        const val DEFAULT_SIZE = 512
        const val DEFAULT_LANGUAGE = "en"

        // SharedPreferences mirror for the language (synchronous reads only).
        const val LANGUAGE_PREFS = "fsd_prefs"
        const val LANGUAGE_KEY = "app_language"

        /** Supported in-app language codes, in Settings display order. */
        val LANGUAGE_CODES = listOf("en", "hi", "bn", "ur", "es", "id")
    }
}
