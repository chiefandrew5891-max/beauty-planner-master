package com.andrey.beautyplanner

enum class AccessTier {
    TRIAL,
    FREE_LIMITED,
    PREMIUM
}

enum class PremiumFeature {
    STATS,
    ARCHIVE,
    BACKUP_EXPORT,
    BACKUP_IMPORT,
    UNLIMITED_APPOINTMENTS,
    PREMIUM_NOTIFICATIONS,
    CUSTOM_SERVICES,
    WORK_SCHEDULE
}

data class AccessState(
    val tier: AccessTier,
    val trialStartedAtMillis: Long,
    val trialEndsAtMillis: Long,
    val isTrialActive: Boolean,
    val hasPremium: Boolean,
    val trialDaysLeft: Int
)