package com.andrey.beautyplanner.notifications

import kotlinx.serialization.Serializable

enum class NotificationSoundType {
    DEFAULT,
    SILENT,
    BUNDLED,
    IMPORTED
}

@Serializable
data class NotificationSoundConfig(
    val type: String = NotificationSoundType.DEFAULT.name,
    val soundId: String = "",
    val displayName: String = ""
)

data class ReminderPreset(
    val minutesBefore: Int,
    val key: String
)

val DefaultReminderPresets = listOf(
    ReminderPreset(1440, "remind_1day"),
    ReminderPreset(60, "remind_1hour"),
    ReminderPreset(30, "remind_30min"),
    ReminderPreset(15, "remind_15min"),
    ReminderPreset(5, "remind_5min")
)