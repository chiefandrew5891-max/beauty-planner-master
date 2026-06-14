package com.andrey.beautyplanner.notifications

import android.media.AudioAttributes
import android.media.MediaPlayer
import com.andrey.beautyplanner.AndroidAppContext

actual object NotificationSoundPreviewPlayer {
    private var mediaPlayer: MediaPlayer? = null

    actual fun play(soundType: String, soundId: String) {
        stop()

        if (soundType != "BUNDLED" || soundId.isBlank()) return

        val context = AndroidAppContext.context ?: return
        val resName = soundId.substringBeforeLast(".")
        val resId = context.resources.getIdentifier(resName, "raw", context.packageName)
        if (resId == 0) return

        val player = MediaPlayer.create(context, resId) ?: return
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