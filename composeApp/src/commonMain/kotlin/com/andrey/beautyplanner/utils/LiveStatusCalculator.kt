package com.andrey.beautyplanner.utils

import com.andrey.beautyplanner.Appointment
import kotlinx.datetime.LocalDate

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