package com.andrey.beautyplanner.notifications

import com.andrey.beautyplanner.Appointment
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

expect object Notifications {
    suspend fun requestPermissionIfNeeded(): Boolean

    /**
     * Планирует (или пересоздаёт) напоминания по всем записям.
     * Чтобы не усложнять диффы — MVP: просто пересоздаём всё после каждого save/delete/import.
     */
    fun rescheduleAll(
        appointments: List<Appointment>,
        reminderMinutes: List<Int>,
        sound: NotificationSound,
        nowEpochMillis: Long
    )

    fun cancelAll()
}

/**
 * Утилита для commonMain: конвертируем Appointment(dateString + "HH:MM") -> epochMillis начала.
 */
fun Appointment.startEpochMillis(timeZone: TimeZone): Long? {
    val date = runCatching { LocalDate.parse(dateString) }.getOrNull() ?: return null
    val parts = time.trim().split(":")
    if (parts.size != 2) return null
    val hh = parts[0].toIntOrNull() ?: return null
    val mm = parts[1].toIntOrNull() ?: return null

    val ldt = LocalDateTime(date.year, date.month, date.dayOfMonth, hh, mm)
    return ldt.toInstant(timeZone).toEpochMilliseconds()
}

fun Long.toInstantKmp(): Instant = Instant.fromEpochMilliseconds(this)