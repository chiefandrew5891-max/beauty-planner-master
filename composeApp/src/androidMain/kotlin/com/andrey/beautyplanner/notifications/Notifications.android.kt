package com.andrey.beautyplanner.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Appointment
import com.andrey.beautyplanner.DataManager
import kotlinx.datetime.TimeZone
import kotlin.math.absoluteValue

internal object NotificationsAndroidContext {
    var context: Context? = null

    fun init(appContext: Context) {
        context = appContext.applicationContext
    }
}

actual object Notifications {

    private fun ctx(): Context =
        NotificationsAndroidContext.context
            ?: error(
                "NotificationsAndroidContext.context is not set. " +
                        "Call NotificationsPlatform.init(context) in MainActivity.onCreate()."
            )

    actual suspend fun requestPermissionIfNeeded(): Boolean = true

    actual fun cancelAll() {
        val context = ctx()
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val allAppointments = runCatching { DataManager.loadFromDatabase() }.getOrNull().orEmpty()
        val reminderMinutes = AppSettings.reminderMinutesComputed()

        allAppointments.forEach { appt ->
            reminderMinutes.forEach { mins ->
                val requestCode = stableRequestCode(appt.id, mins)
                val intent = Intent(context, ReminderReceiver::class.java)
                val pi = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarm.cancel(pi)
                pi.cancel()
            }
        }
    }

    actual fun rescheduleAll(
        appointments: List<Appointment>,
        reminderMinutes: List<Int>,
        soundType: String,
        soundId: String,
        nowEpochMillis: Long
    ) {
        val context = ctx()
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val tz = TimeZone.currentSystemDefault()

        val minutesList = reminderMinutes
            .filter { it > 0 }
            .distinct()
            .sortedDescending()

        appointments.forEach { appt ->
            val startMs = appt.startEpochMillis(tz) ?: return@forEach

            minutesList.forEach { mins ->
                val triggerAt = startMs - mins * 60_000L
                val requestCode = stableRequestCode(appt.id, mins)

                val intent = Intent(context, ReminderReceiver::class.java).apply {
                    putExtra(ReminderReceiver.EXTRA_TITLE, "Beauty Planner")
                    putExtra(
                        ReminderReceiver.EXTRA_BODY,
                        "${appt.clientName}: ${appt.serviceName} • ${appt.dateString} ${appt.time}"
                    )
                    putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, requestCode)
                    putExtra(ReminderReceiver.EXTRA_SOUND_MODE, soundType)
                    putExtra(ReminderReceiver.EXTRA_SOUND_ID, soundId)
                }

                val pi = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                alarm.cancel(pi)

                if (triggerAt <= nowEpochMillis) {
                    pi.cancel()
                    return@forEach
                }

                try {
                    // We prefer exact alarms when permitted, but gracefully fall back
                    // to inexact scheduling if the permission is unavailable on the device.
                    val canUseExactAlarm =
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                                alarm.canScheduleExactAlarms()

                    if (canUseExactAlarm) {
                        alarm.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAt,
                            pi
                        )
                    } else {
                        alarm.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAt,
                            pi
                        )
                    }
                } catch (_: SecurityException) {
                    alarm.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pi
                    )
                }
            }
        }
    }

    actual fun rescheduleFromStorage() {
        val appointments = runCatching { DataManager.loadFromDatabase() }.getOrNull().orEmpty()
        val reminderMinutes = AppSettings.reminderMinutesComputed()
        val now = System.currentTimeMillis()

        if (AppSettings.notificationsEnabled && reminderMinutes.isNotEmpty()) {
            rescheduleAll(
                appointments = appointments,
                reminderMinutes = reminderMinutes,
                soundType = AppSettings.notificationSoundType,
                soundId = AppSettings.notificationSoundId,
                nowEpochMillis = now
            )
        } else {
            cancelAll()
        }
    }

    private fun stableRequestCode(apptId: String, mins: Int): Int {
        return (apptId.hashCode() * 31 + mins).absoluteValue
    }
}