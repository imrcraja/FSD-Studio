package com.fsdetc.fsdstudio.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The single entitlement switch for the whole app plus the local AI daily counters.
 *
 * [isPro] is read from DataStore (default false). There is deliberately no purchase
 * flow in V1 — purchases open at launch; every locked UI checks this one flow.
 * Counters are date-keyed (local date) and reset automatically at local midnight.
 */
class Entitlements(private val context: Context) {

    private val isProKey = booleanPreferencesKey("is_pro")
    private val aiDateKey = stringPreferencesKey("ai_count_date")
    private val aiCountKey = intPreferencesKey("ai_count")

    val isPro: Flow<Boolean> = context.dataStore.data.map { it[isProKey] ?: false }

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    suspend fun aiUsedToday(): Int {
        val p = context.dataStore.data.first()
        return if (p[aiDateKey] == today()) p[aiCountKey] ?: 0 else 0
    }

    suspend fun recordAiUse() {
        val t = today()
        context.dataStore.edit { p ->
            val current = if (p[aiDateKey] == t) (p[aiCountKey] ?: 0) else 0
            p[aiDateKey] = t
            p[aiCountKey] = current + 1
        }
    }

    fun aiLimit(isPro: Boolean): Int = if (isPro) Api.PRO_AI_PER_DAY else Api.FREE_AI_PER_DAY
}
