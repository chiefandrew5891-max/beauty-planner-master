package com.andrey.beautyplanner

import com.andrey.beautyplanner.access.AccessRepository

object AccessManager {
    const val FREE_ACTIVE_APPOINTMENTS_LIMIT = 20

    // Thresholds for the persistent banner warning (based on remaining slots)
    private val FREE_LIMIT_BANNER_THRESHOLDS = setOf(5, 3, 1)

    // Thresholds for one-time popup (based on visible appointment count)
    val FREE_LIMIT_POPUP_THRESHOLDS = setOf(1, 15, 17, 19, 20)

    fun getAccessState(nowMillis: Long): AccessState {
        if (AppSettings.developerPremiumOverrideEnabled) {
            return AccessState(
                tier = AccessTier.PREMIUM,
                trialStartedAtMillis = AppSettings.trialStartedAtMillis,
                trialEndsAtMillis = AppSettings.cachedTrialEndsAtMillis,
                isTrialActive = false,
                hasPremium = true,
                trialDaysLeft = 0
            )
        }

        return AccessRepository.getCachedAccessState(nowMillis)
    }

    fun isTrialActuallyActive(nowMillis: Long): Boolean {
        val state = getAccessState(nowMillis)
        return state.isTrialActive
    }

    fun hasPremiumScreenAccess(nowMillis: Long): Boolean {
        val state = getAccessState(nowMillis)
        return state.tier == AccessTier.PREMIUM || state.isTrialActive
    }

    fun hasFeature(feature: PremiumFeature, nowMillis: Long): Boolean {
        val state = getAccessState(nowMillis)
        if (state.tier == AccessTier.PREMIUM || state.isTrialActive) {
            return true
        }

        return when (feature) {
            PremiumFeature.STATS -> false
            PremiumFeature.ARCHIVE -> false
            PremiumFeature.BACKUP_EXPORT -> false
            PremiumFeature.BACKUP_IMPORT -> false
            PremiumFeature.UNLIMITED_APPOINTMENTS -> false
            PremiumFeature.PREMIUM_NOTIFICATIONS -> false
            PremiumFeature.CUSTOM_SERVICES -> false
            PremiumFeature.WORK_SCHEDULE -> false
        }
    }

    fun canCreateAppointment(
        currentAppointmentsCount: Int,
        nowMillis: Long
    ): Boolean {
        val state = getAccessState(nowMillis)
        if (state.tier == AccessTier.PREMIUM || state.isTrialActive) return true
        return currentAppointmentsCount < FREE_ACTIVE_APPOINTMENTS_LIMIT
    }

    fun getRemainingFreeSlots(
        currentAppointmentsCount: Int,
        nowMillis: Long
    ): Int {
        val state = getAccessState(nowMillis)
        if (state.tier == AccessTier.PREMIUM || state.isTrialActive) return Int.MAX_VALUE
        return (FREE_ACTIVE_APPOINTMENTS_LIMIT - currentAppointmentsCount).coerceAtLeast(0)
    }

    /** Banner warning based on remaining free slots (for the persistent home-screen notice). */
    fun shouldShowFreeLimitWarning(
        remainingFreeSlots: Int
    ): Boolean {
        return remainingFreeSlots in FREE_LIMIT_BANNER_THRESHOLDS
    }

    /** Popup warning based on visible appointment count. */
    fun shouldShowFreeLimitWarningForCount(
        visibleAppointmentsCount: Int
    ): Boolean {
        return visibleAppointmentsCount in FREE_LIMIT_POPUP_THRESHOLDS
    }

    /**
     * Returns the matched popup threshold if [visibleCount] is at a warning threshold,
     * otherwise null. Call after saving a new appointment.
     */
    fun getFreeLimitPopupThreshold(visibleCount: Int): Int? {
        return if (visibleCount in FREE_LIMIT_POPUP_THRESHOLDS) visibleCount else null
    }
}