package com.andrey.beautyplanner.notifications

enum class NotificationSound {
    DEFAULT,
    SILENT
}

data class ReminderPreset(
    val minutesBefore: Int,
    val key: String // для локализации в UI
)

val DefaultReminderPresets = listOf(
    ReminderPreset(1440, "remind_1day"),
    ReminderPreset(60, "remind_1hour"),
    ReminderPreset(30, "remind_30min"),
    ReminderPreset(15, "remind_15min"),
    ReminderPreset(5, "remind_5min")
)