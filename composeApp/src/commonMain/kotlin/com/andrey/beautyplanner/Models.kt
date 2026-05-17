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

    // NEW: preferred duration
    val durationMinutes: Int = 0,

    // OLD: keep for old data / fallback
    val durationHours: Int = 1
)

enum class Screen {
    MONTH,
    DAY_DETAILS,
    SETTINGS,
    STATS,
    FEEDBACK,
    PRIVACY_POLICY,
    PREMIUM_ACCESS
}