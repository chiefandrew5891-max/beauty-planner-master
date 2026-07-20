package com.andrey.beautyplanner.utils

import com.andrey.beautyplanner.Appointment
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

enum class LiveStatusKey(val localeKey: String) {
    WAITING("appt_status_waiting"),           // Ожидается / Waiting / Очікується / In attesa
    IN_PROCESS("appt_status_in_progress"),    // Выполняется / In progress / Виконується / In corso
    DONE("appt_status_done"),                 // Выполнено / Done / Виконано / Completato
    CANCELED("appt_status_canceled")          // Отменено / Canceled / Скасовано / Annullato
}

fun getLiveStatus(
    appt: Appointment,
    nowDate: LocalDate,
    nowMinutes: Int,
    isCanceled: Boolean = false
): LiveStatusKey {
    // Если отменено — всегда отменено!
    if (isCanceled) return LiveStatusKey.CANCELED

    val apptDate = try { LocalDate.parse(appt.dateString) } catch (_: Exception) { nowDate }
    val timeParts = appt.time.split(":")
    val startMinutes = if (timeParts.size == 2)
        (timeParts[0].toIntOrNull() ?: 0) * 60 + (timeParts[1].toIntOrNull() ?: 0)
    else 0

    val duration = if (appt.durationMinutes > 0) appt.durationMinutes else appt.durationHours * 60
    val endMinutes = startMinutes + duration

    return when {
        apptDate > nowDate -> LiveStatusKey.WAITING
        apptDate < nowDate -> LiveStatusKey.DONE
        nowMinutes < startMinutes -> LiveStatusKey.WAITING
        nowMinutes in startMinutes until endMinutes -> LiveStatusKey.IN_PROCESS
        nowMinutes >= endMinutes -> LiveStatusKey.DONE
        else -> LiveStatusKey.WAITING
    }
}

// Пример утилиты
fun parseHmToMinutes(hm: String): Int {
    val p = hm.split(":")
    return (p.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (p.getOrNull(1)?.toIntOrNull() ?: 0)
}

private const val EDIT_GRACE_PERIOD_MS = 24L * 60L * 60L * 1000L

/**
 * Returns true if the given appointment is within the 24-hour editing grace period.
 *
 * After an appointment's date/time has passed, clients have 24 hours to make
 * corrections before the record becomes fully read-only.
 *
 * @param apptDate  The appointment's date (already parsed to [LocalDate]).
 * @param apptTime  The appointment's start time string in HH:MM format.
 */
fun isWithinEditGracePeriod(apptDate: LocalDate, apptTime: String): Boolean {
    val apptTimeMin = parseHmToMinutes(apptTime)
    val apptLocalDt = LocalDateTime(
        apptDate.year, apptDate.month, apptDate.dayOfMonth,
        apptTimeMin / 60, apptTimeMin % 60, 0
    )
    val apptInstant = apptLocalDt.toInstant(TimeZone.currentSystemDefault())
    val diffMs = Clock.System.now().toEpochMilliseconds() - apptInstant.toEpochMilliseconds()
    return diffMs in 0L..EDIT_GRACE_PERIOD_MS
}