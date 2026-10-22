package com.fsdetc.fsdstudio.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/** Result of a gated gallery save. */
sealed interface DownloadOutcome {
    data object Saved : DownloadOutcome
    data object LimitReached : DownloadOutcome
    data object Failed : DownloadOutcome
}

/**
 * The shared gate every gallery-save path goes through (render detail, history
 * items, library mobs, AI results): FREE is limited to
 * [Entitlements.FREE_DOWNLOADS_PER_DAY] downloads per day, paid plans are
 * unlimited. The counter is incremented only after a save actually succeeds.
 *
 * Also emits [thirdDownloadEvents] exactly once per day: when a FREE user's
 * successful-download count reaches 3 — the nav host turns that into the single
 * gentle upgrade pop-up.
 */
class DownloadGate(private val entitlements: Entitlements) {

    private val _thirdDownloadEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val thirdDownloadEvents: SharedFlow<Unit> = _thirdDownloadEvents

    /** Downloads left today, or null when the current plan is unlimited. */
    suspend fun remainingToday(): Int? {
        val limit = entitlements.downloadsPerDay(entitlements.currentPlan())
        if (limit == Int.MAX_VALUE) return null
        return (limit - entitlements.downloadsUsedToday()).coerceAtLeast(0)
    }

    /**
     * Run [save] only if the daily limit allows it. On success the counter is
     * incremented (and the once-a-day pop-up event fires on the 3rd download).
     */
    suspend fun saveWithGate(save: suspend () -> Boolean): DownloadOutcome {
        val plan = entitlements.currentPlan()
        val limit = entitlements.downloadsPerDay(plan)
        if (limit != Int.MAX_VALUE && entitlements.downloadsUsedToday() >= limit) {
            return DownloadOutcome.LimitReached
        }
        val ok = runCatching { save() }.getOrDefault(false)
        if (!ok) return DownloadOutcome.Failed
        val total = entitlements.recordDownload()
        if (plan == Plan.FREE &&
            total == POPUP_AT_COUNT &&
            !entitlements.popupShownToday()
        ) {
            entitlements.markPopupShown()
            _thirdDownloadEvents.tryEmit(Unit)
        }
        return DownloadOutcome.Saved
    }

    companion object {
        /** The successful-download count that triggers the once-a-day upgrade pop-up. */
        private const val POPUP_AT_COUNT = 3
    }
}
