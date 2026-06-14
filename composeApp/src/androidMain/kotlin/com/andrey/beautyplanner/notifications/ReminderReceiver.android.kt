package com.andrey.beautyplanner.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.andrey.beautyplanner.R

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val body = intent.getStringExtra(EXTRA_BODY) ?: ""
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val soundMode = intent.getStringExtra(EXTRA_SOUND_MODE) ?: "DEFAULT"
        val soundId = intent.getStringExtra(EXTRA_SOUND_ID).orEmpty()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = buildChannelId(soundMode, soundId)
            val channelName = buildChannelName(soundMode, soundId)

            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )

            when (soundMode) {
                "SILENT" -> {
                    channel.setSound(null, null)
                    channel.enableVibration(false)
                }

                "BUNDLED" -> {
                    val soundUri = soundUriFor(context, soundId)
                    if (soundUri != null) {
                        val audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()

                        channel.setSound(soundUri, audioAttributes)
                    }
                }

                else -> {
                    // DEFAULT: ничего не задаём, система использует стандартный звук канала
                }
            }

            nm.createNotificationChannel(channel)

            val notif = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .build()

            nm.notify(notificationId, notif)
            return
        }

        val notif = NotificationCompat.Builder(context, CHANNEL_ID_DEFAULT)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .build()

        nm.notify(notificationId, notif)
    }

    companion object {
        const val CHANNEL_ID_DEFAULT = "beautyplanner_reminders_default"
        const val CHANNEL_ID_SILENT = "beautyplanner_reminders_silent"

        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
        const val EXTRA_NOTIFICATION_ID = "nid"
        const val EXTRA_SOUND_MODE = "sound_mode"
        const val EXTRA_SOUND_ID = "sound_id"
    }
}

private fun buildChannelId(soundMode: String, soundId: String): String {
    return when (soundMode) {
        "SILENT" -> ReminderReceiver.CHANNEL_ID_SILENT
        "BUNDLED" -> {
            val key = soundId.substringBeforeLast(".").ifBlank { "default" }
            "beautyplanner_reminders_$key"
        }
        else -> ReminderReceiver.CHANNEL_ID_DEFAULT
    }
}

private fun buildChannelName(soundMode: String, soundId: String): String {
    return when (soundMode) {
        "SILENT" -> "Reminders Silent"
        "BUNDLED" -> {
            val key = soundId.substringBeforeLast(".").ifBlank { "default" }
            "Reminders $key"
        }
        else -> "Reminders"
    }
}

private fun soundUriFor(context: Context, soundId: String): Uri? {
    val resName = soundId.substringBeforeLast(".")
    val resId = context.resources.getIdentifier(resName, "raw", context.packageName)
    if (resId == 0) return null
    return Uri.parse("android.resource://${context.packageName}/$resId")
}