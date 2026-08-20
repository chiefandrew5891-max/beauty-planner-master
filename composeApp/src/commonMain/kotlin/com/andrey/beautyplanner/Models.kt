package com.andrey.beautyplanner

import kotlinx.serialization.Serializable

enum class AppointmentPaymentStatus {
    PAID,
    PAYMENT_LATER,
    PAID_AFTER_DELAY
}

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
    val paymentDeferred: Boolean = false,
    val paymentStatus: String = "",
    val updatedAtMillis: Long = 0L,
    val isDeleted: Boolean = false,
    val currency: String = "EUR",

    val bookingSource: String = "manual"
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
data class DateRangeBlockedInterval(
    val id: String,
    val dateFrom: String, // YYYY-MM-DD
    val dateTo: String,   // YYYY-MM-DD
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

enum class ClientProfileStatus {
    NONE,
    VIP,
    CONFIRMATION_REQUIRED,
    RISK_OF_CANCELLATION,
    DO_NOT_BOOK
}

@Serializable
data class ClientProfile(
    val id: String,
    val displayName: String,
    val phone: String = "",
    val notes: String = "",
    val colorTag: String = "",
    val status: String = ClientProfileStatus.NONE.name,
    val updatedAtMillis: Long = 0L
)
enum class Screen {
    AUTH_WELCOME,
    AUTH_EMAIL,
    MONTH,
    DAY_DETAILS,
    SETTINGS,
    STATS,
    CLIENT_DATABASE,
    UNPAID_APPOINTMENTS,
    ARCHIVE,
    CLIENT_INTERACTIONS,
    FEEDBACK,
    USER_GUIDE,
    PRIVACY_POLICY,
    PREMIUM_ACCESS,
    SERVICE_TEMPLATES,
    WORK_SCHEDULE,
    APPEARANCE_SETTINGS,
    PERSONAL_INFO_SETTINGS,
    DEVELOPER_ACCESS,
    BACKUP_SETTINGS,
    NOTIFICATION_SETTINGS,
    GUEST_ACCOUNT_REGISTRATION
}