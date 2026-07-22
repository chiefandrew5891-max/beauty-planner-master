package com.andrey.beautyplanner

import kotlinx.serialization.Serializable

@Serializable
data class CloudSettingsSnapshot(
    val ownerName: String = "",
    val profilePhone: String = "",
    val profilePhoneVisible: Boolean = true,
    val profileRating: Float = DEFAULT_PROFILE_RATING,
    val profileAvatarUrl: String = "",
    val profileAvatarBase64: String = "",
    val profileDisplayCustomName: Boolean = false,
    val profileSpecialization: String = "",
    val clientInteractionsEnabled: Boolean = false,
    val selectedCurrency: String = "EUR",
    val useShortTextCurrency: Boolean = false,

    val notificationsEnabled: Boolean = true,
    val notificationSoundType: String = "DEFAULT",
    val notificationSoundId: String = "",
    val notificationSoundDisplayName: String = "",

    val reminderDaysBefore: Int = 0,
    val reminderHoursBefore: Int = 1,
    val reminderMinutesBefore: Int = 0,

    val serviceTemplates: List<ServiceTemplate> = emptyList(),
    val weeklyBlockedIntervals: List<WeeklyBlockedInterval> = emptyList(),
    val scheduleDateOverrides: List<ScheduleDateOverride> = emptyList(),

    val updatedAtMillis: Long = 0L
)

@Serializable
data class CloudSyncPullResult(
    val appointments: List<Appointment> = emptyList(),
    val settings: CloudSettingsSnapshot? = null
)