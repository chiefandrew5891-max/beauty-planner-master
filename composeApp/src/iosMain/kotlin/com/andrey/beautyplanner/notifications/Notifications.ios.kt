package com.andrey.beautyplanner.notifications

import com.andrey.beautyplanner.Appointment
import kotlinx.datetime.TimeZone
import platform.UserNotifications.*

actual object Notifications {

    actual suspend fun requestPermissionIfNeeded(): Boolean {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.requestAuthorizationWithOptions(
            options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        ) { _, _ -> }
        return true
    }

    actual fun cancelAll() {
        UNUserNotificationCenter.currentNotificationCenter().removeAllPendingNotificationRequests()
    }

    actual fun rescheduleAll(
        appointments: List<Appointment>,
        reminderMinutes: List<Int>,
        soundType: String,
        soundId: String,
        nowEpochMillis: Long
    ) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.removeAllPendingNotificationRequests()

        val tz = TimeZone.currentSystemDefault()

        appointments.forEach { appt ->
            val startMs = appt.startEpochMillis(tz) ?: return@forEach

            reminderMinutes.forEach { mins ->
                val triggerAt = startMs - mins * 60_000L
                if (triggerAt <= nowEpochMillis) return@forEach

                val content = UNMutableNotificationContent().apply {
                    // ИСПОЛЬЗУЕМ СЕТТЕРЫ, так как напрямую менять val нельзя
                    setTitle("Beauty Planner")
                    setBody("${appt.clientName}: ${appt.serviceName} • ${appt.dateString} ${appt.time}")

                    val iosSound = when (soundType) {
                        "SILENT" -> null
                        "BUNDLED" -> {
                            if (soundId.isNotBlank()) {
                                UNNotificationSound.soundNamed(soundId)
                            } else {
                                UNNotificationSound.defaultSound()
                            }
                        }
                        else -> UNNotificationSound.defaultSound()
                    }
                    setSound(iosSound)
                    setSound(iosSound)
                }

                val seconds = ((triggerAt - nowEpochMillis) / 1000.0).coerceAtLeast(1.0)
                val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(seconds, repeats = false)

                val id = "${appt.id}_$mins"
                val request = UNNotificationRequest.requestWithIdentifier(id, content, trigger)
                center.addNotificationRequest(request) { _ -> }
            }
        }
    }
    actual fun rescheduleFromStorage() {
        // iOS version can be implemented later if needed.
    }
}
// fix 7