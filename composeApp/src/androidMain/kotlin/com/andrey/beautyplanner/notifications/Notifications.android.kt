package com.andrey.beautyplanner.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.andrey.beautyplanner.Appointment
import kotlinx.datetime.TimeZone

internal object NotificationsAndroidContext {
    var context: Context? = null
    fun init(appContext: Context) {
        context = appContext.applicationContext
    }
}

actual object Notifications {

    private fun ctx(): Context =
        NotificationsAndroidContext.context
            ?: error("NotificationsAndroidContext.context is not set. Call NotificationsPlatform.init(context) in MainActivity.onCreate().")

    actual suspend fun requestPermissionIfNeeded(): Boolean = true

    actual fun cancelAll() { }

    actual fun rescheduleAll(
        appointments: List<Appointment>,
        reminderMinutes: List<Int>,
        sound: NotificationSound,
        nowEpochMillis: Long
    ) {
        val context = ctx()
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val tz = TimeZone.currentSystemDefault()

        val minutesList = reminderMinutes
            .filter { it > 0 }
            .distinct()
            .sortedDescending()

        apptLoop@ for (appt in appointments) {
            val startMs = appt.startEpochMillis(tz) ?: continue@apptLoop

            minsLoop@ for (mins in minutesList) {
                val triggerAt = startMs - mins * 60_000L
                if (triggerAt <= nowEpochMillis) continue@minsLoop

                val title = "Beauty Planner"
                val body = "${appt.clientName}: ${appt.serviceName} • ${appt.dateString} ${appt.time}"

                val requestCode = stableRequestCode(appt.id, mins)

                val intent = Intent(context, ReminderReceiver::class.java).apply {
                    putExtra("EXTRA_TITLE", title)
                    putExtra("EXTRA_BODY", body)
                    putExtra("EXTRA_NOTIFICATION_ID", requestCode)
                }

                val pi = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                alarm.cancel(pi)

                // ИСПРАВЛЕНИЕ ЛОГИКИ: Безопасная установка времени
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarm.canScheduleExactAlarms()) {
                        alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                    } else {
                        // Если прав нет или версия старая — используем обычный метод, чтобы не было вылета
                        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                    }
                } catch (e: SecurityException) {
                    // Последний рубеж защиты от вылета
                    alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                }
            }
        }
    }

    private fun stableRequestCode(apptId: String, mins: Int): Int {
        return (apptId.hashCode() * 31 + mins).let { if (it < 0) -it else it }
    }
}