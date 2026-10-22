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

/** The three subscription tiers. Prices are display-only in Settings/subscription copy. */
enum class Plan { FREE, PRO, MAX }

/**
 * The single entitlement switch for the whole app, plus the local daily counters
 * (AI generations and gallery downloads).
 *
 * The plan is stored in DataStore key `plan` (default [Plan.FREE]). There is
 * deliberately no purchase flow in this build — purchases open at launch and
 * every locked UI reads this one flow.
 *
 * V1 migration: the old `is_pro` boolean is honoured when no `plan` value exists
 * yet (is_pro = true → PRO).
 *
 * Counters are date-keyed (local date) and reset automatically at local midnight.
 */
class Entitlements(private val context: Context) {

    private val planKey = stringPreferencesKey("plan")
    private val legacyIsProKey = booleanPreferencesKey("is_pro")
    private val aiDateKey = stringPreferencesKey("ai_count_date")
    private val aiCountKey = intPreferencesKey("ai_count")

    val plan: Flow<Plan> = context.dataStore.data.map { p ->
        val stored = p[planKey]
        when {
            stored != null -> runCatching { Plan.valueOf(stored) }.getOrDefault(Plan.FREE)
            // V1 → V2 migration: a stored is_pro=true becomes the PRO tier.
            p[legacyIsProKey] == true -> Plan.PRO
            else -> Plan.FREE
        }
    }

    suspend fun currentPlan(): Plan = plan.first()

    /** Forget the stored plan (log out) — the next read falls back to FREE. */
    suspend fun resetPlan() {
        context.dataStore.edit { p ->
            p.remove(planKey)
            p.remove(legacyIsProKey)
        }
    }

    // ------------------------------------------------------------ plan limits

    /**
     * Pose access is data-driven since V3: a [PoseDef] carries its own `free`
     * flag — FREE-plan users get exactly the free poses, Pro/Max get all.
     */
    fun poseUnlocked(pose: PoseDef, plan: Plan): Boolean =
        pose.free || plan != Plan.FREE

    /** Largest render size the plan may request. */
    fun maxRenderSize(plan: Plan): Int = if (plan == Plan.FREE) 512 else 1024

    /** Gallery downloads per day; paid plans are unlimited. */
    fun downloadsPerDay(plan: Plan): Int =
        if (plan == Plan.FREE) FREE_DOWNLOADS_PER_DAY else Int.MAX_VALUE

    /** AI generations per day: FREE 3, PRO 3 (basic), MAX 50. */
    fun aiPerDay(plan: Plan): Int = when (plan) {
        Plan.FREE -> 3
        Plan.PRO -> 3
        Plan.MAX -> 50
    }

    /** Priority/boost AI treatment is a MAX-only perk. */
    fun canUseAiBoost(plan: Plan): Boolean = plan == Plan.MAX

    // ------------------------------------------------------------ AI counter

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

    // ------------------------------------------------------------ download counter

    private fun dlKey(date: String) = intPreferencesKey("dl_$date")
    private fun popupKey(date: String) = booleanPreferencesKey("popup_$date")

    /** Today's successful gallery downloads (0 once the date rolls over). */
    val downloadsToday: Flow<Int> = context.dataStore.data.map { it[dlKey(today())] ?: 0 }

    suspend fun downloadsUsedToday(): Int =
        context.dataStore.data.first()[dlKey(today())] ?: 0

    /** Increment today's counter and return the new total. */
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
        const val FREE_DOWNLOADS_PER_DAY = 5
    }
}
