package com.andrey.beautyplanner

import com.andrey.beautyplanner.access.AccessRepository

object AccessManager {
    const val FREE_ACTIVE_APPOINTMENTS_LIMIT = 20

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

    fun hasFeature(feature: PremiumFeature, nowMillis: Long): Boolean {
        val state = getAccessState(nowMillis)
        if (state.tier == AccessTier.PREMIUM || state.tier == AccessTier.TRIAL) {
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
        return when (state.tier) {
            AccessTier.PREMIUM,
            AccessTier.TRIAL -> true
            AccessTier.FREE_LIMITED -> currentAppointmentsCount < FREE_ACTIVE_APPOINTMENTS_LIMIT
        }
    }

    fun getRemainingFreeSlots(
        currentAppointmentsCount: Int,
        nowMillis: Long
    ): Int {
        val state = getAccessState(nowMillis)
        return when (state.tier) {
            AccessTier.PREMIUM,
            AccessTier.TRIAL -> Int.MAX_VALUE
            AccessTier.FREE_LIMITED -> (FREE_ACTIVE_APPOINTMENTS_LIMIT - currentAppointmentsCount)
                .coerceAtLeast(0)
        }
    }
}