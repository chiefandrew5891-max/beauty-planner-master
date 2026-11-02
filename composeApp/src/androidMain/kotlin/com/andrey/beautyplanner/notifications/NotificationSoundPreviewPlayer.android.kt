package com.andrey.beautyplanner.notifications

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.andrey.beautyplanner.AndroidAppContext
import java.io.File

actual object NotificationSoundPreviewPlayer {
    private var mediaPlayer: MediaPlayer? = null

    actual fun play(soundType: String, soundId: String) {
        stop()

        if (soundId.isBlank() || (soundType != "BUNDLED" && soundType != "IMPORTED")) return

        val context = AndroidAppContext.context ?: return

        val player = if (soundType == "IMPORTED") {
            val file = File(File(context.filesDir, "sounds"), soundId)
            if (!file.exists()) return
            MediaPlayer.create(context, Uri.fromFile(file)) ?: return
        } else {
            val resName = soundId.substringBeforeLast(".")
            val resId = context.resources.getIdentifier(resName, "raw", context.packageName)
            if (resId == 0) return
            MediaPlayer.create(context, resId) ?: return
        }

        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        player.setOnCompletionListener {
            it.release()
            if (mediaPlayer === it) {
                mediaPlayer = null
            }
        }
        player.setOnErrorListener { mp, _, _ ->
            mp.release()
            if (mediaPlayer === mp) {
                mediaPlayer = null
            }
            true
        }

        mediaPlayer = player
        player.start()
    }

    actual fun stop() {
        val player = mediaPlayer ?: return
        runCatching {
            if (player.isPlaying) player.stop()
        }
        runCatching { player.release() }
        mediaPlayer = null
    }
}