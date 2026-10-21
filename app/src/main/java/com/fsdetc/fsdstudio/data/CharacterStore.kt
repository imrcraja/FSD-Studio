package com.fsdetc.fsdstudio.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Persists the current character in DataStore so it survives restarts. */
class CharacterStore(private val context: Context) {

    private object Keys {
        val SOURCE = stringPreferencesKey("char_source")
        val NAME = stringPreferencesKey("char_name")
        val UUID = stringPreferencesKey("char_uuid")
        val MODEL = stringPreferencesKey("char_model")
        val SKIN_HASH = stringPreferencesKey("char_skin_hash")
        val SKIN_URL = stringPreferencesKey("char_skin_url")
        val LOCAL_PATH = stringPreferencesKey("char_local_path")
    }

    val character: Flow<CharacterInfo?> = context.dataStore.data.map { p ->
        val sourceName = p[Keys.SOURCE] ?: return@map null
        val name = p[Keys.NAME] ?: return@map null
        CharacterInfo(
            name = name,
            uuid = p[Keys.UUID],
            model = p[Keys.MODEL],
            skinHash = p[Keys.SKIN_HASH],
            skinUrl = p[Keys.SKIN_URL],
            localSkinPath = p[Keys.LOCAL_PATH],
            source = runCatching { CharacterSource.valueOf(sourceName) }
                .getOrDefault(CharacterSource.USERNAME),
        )
    }

    suspend fun save(c: CharacterInfo) {
        context.dataStore.edit { p ->
            p[Keys.SOURCE] = c.source.name
            p[Keys.NAME] = c.name
            setOrRemove(p, Keys.UUID, c.uuid)
            setOrRemove(p, Keys.MODEL, c.model)
            setOrRemove(p, Keys.SKIN_HASH, c.skinHash)
            setOrRemove(p, Keys.SKIN_URL, c.skinUrl)
            setOrRemove(p, Keys.LOCAL_PATH, c.localSkinPath)
        }
    }

    suspend fun clear() {
        context.dataStore.edit { p ->
            p.remove(Keys.SOURCE)
            p.remove(Keys.NAME)
            p.remove(Keys.UUID)
            p.remove(Keys.MODEL)
            p.remove(Keys.SKIN_HASH)
            p.remove(Keys.SKIN_URL)
            p.remove(Keys.LOCAL_PATH)
        }
    }

    private fun setOrRemove(
        p: androidx.datastore.preferences.core.MutablePreferences,
        key: androidx.datastore.preferences.core.Preferences.Key<String>,
        value: String?,
    ) {
        if (value == null) p.remove(key) else p[key] = value
    }
}
