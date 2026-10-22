package com.fsdetc.fsdstudio

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.room.Room
import com.fsdetc.fsdstudio.data.CharacterStore
import com.fsdetc.fsdstudio.data.DownloadGate
import com.fsdetc.fsdstudio.data.Entitlements
import com.fsdetc.fsdstudio.data.HistoryRepository
import com.fsdetc.fsdstudio.data.PollinationsEngine
import com.fsdetc.fsdstudio.data.SkinRepository
import com.fsdetc.fsdstudio.data.UserSettings
import com.fsdetc.fsdstudio.data.dataStore
import com.fsdetc.fsdstudio.data.db.AppDatabase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/** Tiny manual dependency container — no Hilt in V1, everything is created once here. */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    val skinRepository: SkinRepository = SkinRepository(appContext)
    val characterStore: CharacterStore = CharacterStore(appContext)

    /** Bundled premade-asset library for the Skin Editor (catalog + patches). */
    val skinAssets: com.fsdetc.fsdstudio.data.SkinAssetRepository =
        com.fsdetc.fsdstudio.data.SkinAssetRepository(appContext)

    /** Saved finished skins from the Skin Editor. */
    val wardrobe: com.fsdetc.fsdstudio.data.WardrobeStore =
        com.fsdetc.fsdstudio.data.WardrobeStore(appContext)
    val entitlements: Entitlements = Entitlements(appContext)
    val userSettings: UserSettings = UserSettings(appContext)
    val downloadGate: DownloadGate = DownloadGate(entitlements)

    /** The one built-in AI engine (Pollinations, keyless) — no user setup. */
    val pollinationsEngine: PollinationsEngine = PollinationsEngine(skinRepository.aiClient)

    val database: AppDatabase by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, "fsd_studio.db").build()
    }

    val historyRepository: HistoryRepository by lazy {
        HistoryRepository(appContext, database.historyDao())
    }

    // ------------------------------------------------------------ snackbar bus

    /**
     * App-wide transient messages. Screens post short status strings here and
     * the nav host shows them in its bottom-anchored SnackbarHost (above the
     * bottom navigation bar) — never as top banners over content.
     */
    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages

    fun postSnackbar(message: String) {
        _snackbarMessages.tryEmit(message)
    }

    // ------------------------------------------------------------ account actions

    /**
     * Log out: clears the current character, forgets the plan (back to FREE)
     * and returns theme / render defaults / language to defaults.
     * History, the daily counters and the user id are KEPT by design.
     */
    suspend fun logout() {
        characterStore.clear()
        entitlements.resetPlan()
        userSettings.clearAccountPrefs()
    }

    /**
     * Reset app data: wipes ALL DataStore preferences (character, plan,
     * counters, settings, user id — it regenerates on next read), every
     * history file + Room row, and the HTTP cache. Coil's caches are cleared
     * by the caller (Settings) through the image loader.
     */
    suspend fun resetAllData() {
        appContext.dataStore.edit { prefs -> prefs.clear() }
        appContext.getSharedPreferences(UserSettings.LANGUAGE_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(UserSettings.LANGUAGE_KEY, UserSettings.DEFAULT_LANGUAGE)
            .commit()
        historyRepository.clearAll()
        runCatching { skinRepository.httpClient.cache?.evictAll() }
    }
}
