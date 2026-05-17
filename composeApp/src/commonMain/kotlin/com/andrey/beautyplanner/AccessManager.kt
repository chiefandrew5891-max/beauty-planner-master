package com.andrey.beautyplanner

import kotlin.math.ceil

object AccessManager {

    private const val TRIAL_DURATION_DAYS = 14
    private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
    const val FREE_ACTIVE_APPOINTMENTS_LIMIT = 20

    fun ensureTrialInitialized(nowMillis: Long) {
        if (AppSettings.trialStartedAtMillis <= 0L) {
            AppSettings.trialStartedAtMillis = nowMillis
            AppSettings.persist()
        }
    }

    fun getAccessState(nowMillis: Long): AccessState {
        ensureTrialInitialized(nowMillis)

        val trialStart = AppSettings.trialStartedAtMillis
        val trialEnd = trialStart + TRIAL_DURATION_DAYS * MILLIS_PER_DAY
        val hasPremium = AppSettings.premiumUnlocked
        val trialActive = nowMillis < trialEnd

        val daysLeft = if (trialActive) {
            ceil((trialEnd - nowMillis).toDouble() / MILLIS_PER_DAY.toDouble()).toInt().coerceAtLeast(0)
        } else {
            0
        }

        val tier = when {
            hasPremium -> AccessTier.PREMIUM
            trialActive -> AccessTier.TRIAL
            else -> AccessTier.FREE_LIMITED
        }

        return AccessState(
            tier = tier,
            trialStartedAtMillis = trialStart,
            trialEndsAtMillis = trialEnd,
            isTrialActive = trialActive,
            hasPremium = hasPremium,
            trialDaysLeft = daysLeft
        )
    }

    fun hasFeature(feature: PremiumFeature, nowMillis: Long): Boolean {
        val state = getAccessState(nowMillis)

        if (state.tier == AccessTier.PREMIUM || state.tier == AccessTier.TRIAL) {
            return true
        }

        return when (feature) {
            PremiumFeature.STATS -> false
            PremiumFeature.BACKUP_EXPORT -> false
            PremiumFeature.BACKUP_IMPORT -> false
            PremiumFeature.UNLIMITED_APPOINTMENTS -> false
            PremiumFeature.PREMIUM_NOTIFICATIONS -> false
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