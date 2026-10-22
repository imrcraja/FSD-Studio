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

/** Subscription tiers retained for compatibility; every tier is unlocked in this build. */
enum class Plan { FREE, PRO, MAX }

/** All in-app features are unlocked. Daily counters remain for local usage history only. */
class Entitlements(private val context: Context) {

    private val planKey = stringPreferencesKey("plan")
    private val legacyIsProKey = booleanPreferencesKey("is_pro")
    private val aiDateKey = stringPreferencesKey("ai_count_date")
    private val aiCountKey = intPreferencesKey("ai_count")

    /** Existing screens keep working, while every feature gate sees MAX access. */
    val plan: Flow<Plan> = context.dataStore.data.map { Plan.MAX }

    suspend fun currentPlan(): Plan = Plan.MAX

    suspend fun resetPlan() {
        context.dataStore.edit { p ->
            p.remove(planKey)
            p.remove(legacyIsProKey)
        }
    }

    fun poseUnlocked(pose: PoseDef, plan: Plan): Boolean = true

    /** Highest practical render size supported by the existing render service. */
    fun maxRenderSize(plan: Plan): Int = 1024

    fun downloadsPerDay(plan: Plan): Int = Int.MAX_VALUE

    fun aiPerDay(plan: Plan): Int = Int.MAX_VALUE

    fun canUseAiBoost(plan: Plan): Boolean = true

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

    private fun dlKey(date: String) = intPreferencesKey("dl_$date")
    private fun popupKey(date: String) = booleanPreferencesKey("popup_$date")

    val downloadsToday: Flow<Int> = context.dataStore.data.map { it[dlKey(today())] ?: 0 }

    suspend fun downloadsUsedToday(): Int =
        context.dataStore.data.first()[dlKey(today())] ?: 0

    suspend fun recordDownload(): Int {
        val key = dlKey(today())
        var total = 0
        context.dataStore.edit { p ->
            total = (p[key] ?: 0) + 1
            p[key] = total
        }
        return total
    }

    suspend fun popupShownToday(): Boolean =
        context.dataStore.data.first()[popupKey(today())] ?: false

    suspend fun markPopupShown() {
        val key = popupKey(today())
        context.dataStore.edit { p -> p[key] = true }
    }

    companion object {
        const val FREE_DOWNLOADS_PER_DAY = Int.MAX_VALUE
    }
}
