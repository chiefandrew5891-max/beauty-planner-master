package com.andrey.beautyplanner.remote

import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Appointment
import com.andrey.beautyplanner.AppointmentSyncUtils
import com.andrey.beautyplanner.CloudSyncJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
data class PublicBusySlot(
    val date: String,
    val startTime: String,
    val endTime: String
)

object MasterScheduleSync {
    suspend fun syncIfEligible(
        appointments: List<Appointment>
    ): Result<Map<String, String>> {
        return runCatching {
            if (!AppSettings.clientInteractionsEnabled) {
                return@runCatching emptyMap()
            }

            val busySlots = if (AppSettings.autoPublishBusySlots) {
                buildBusySlots(appointments)
            } else {
                emptyList()
            }

            BackendBridge.syncMyPublicSchedule(
                autoPublishBusySlots = AppSettings.autoPublishBusySlots,
                busySlotsJson = CloudSyncJson.json.encodeToString(busySlots)
            )
        }
    }

    fun buildBusySlots(
        appointments: List<Appointment>
    ): List<PublicBusySlot> {
        return AppointmentSyncUtils.visibleAppointments(appointments)
            .mapNotNull { appointment ->
                val start = appointment.time.trim()
                if (start.isBlank()) return@mapNotNull null

                val duration =
                    if (appointment.durationMinutes > 0) {
                        appointment.durationMinutes
                    } else {
                        appointment.durationHours.coerceAtLeast(1) * 60
                    }

                val end = addMinutesToHm(start, duration) ?: return@mapNotNull null

                PublicBusySlot(
                    date = appointment.dateString,
                    startTime = start,
                    endTime = end
                )
            }
            .sortedWith(compareBy<PublicBusySlot>({ it.date }, { it.startTime }, { it.endTime }))
    }

    private fun addMinutesToHm(
        hm: String,
        minutesToAdd: Int
    ): String? {
        val parts = hm.split(":")
        if (parts.size != 2) return null

        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null

        val startTotal = hour * 60 + minute
        val endTotal = (startTotal + minutesToAdd).coerceAtMost(24 * 60)

        val endHour = endTotal / 60
        val endMinute = endTotal % 60

        return "${endHour.toString().padStart(2, '0')}:${endMinute.toString().padStart(2, '0')}"
    }
}