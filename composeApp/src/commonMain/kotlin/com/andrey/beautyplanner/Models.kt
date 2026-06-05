package com.andrey.beautyplanner

import kotlinx.serialization.Serializable

@Serializable
data class Appointment(
    val id: String,
    val dateString: String,
    val time: String,
    val clientName: String,
    val phone: String,
    val serviceName: String,
    val price: String,
    val durationMinutes: Int = 0,
    val durationHours: Int = 1,
    val notes: String = "",
    val currency: String = "EUR"
)
@Serializable
data class ServiceTemplate(
    val id: String,
    val title: String,
    val defaultPrice: String = "",
    val isActive: Boolean = true
)
@Serializable
data class WeeklyBlockedInterval(
    val id: String,
    val dayOfWeek: Int, // 1..7 (Mon..Sun)
    val startTime: String,
    val endTime: String,
    val isActive: Boolean = true
)
@Serializable
data class ScheduleDateOverride(
    val id: String,
    val date: String, // YYYY-MM-DD
    val unblockAll: Boolean = true
)
enum class Screen {
    MONTH,
    DAY_DETAILS,
    SETTINGS,
    STATS,
    FEEDBACK,
    PRIVACY_POLICY,
    PREMIUM_ACCESS,
    SERVICE_TEMPLATES,
    WORK_SCHEDULE,
    APPEARANCE_SETTINGS,
    DEVELOPER_ACCESS,
    BACKUP_SETTINGS,
    NOTIFICATION_SETTINGS
}