package com.andrey.beautyplanner.notifications

import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSBundle
import platform.Foundation.NSURL

actual object NotificationSoundPreviewPlayer {
    private var player: AVAudioPlayer? = null

    actual fun play(soundType: String, soundId: String) {
        stop()

        if (soundType != "BUNDLED" || soundId.isBlank()) return

        val fileName = soundId.substringBeforeLast(".")
        val extension = soundId.substringAfterLast(".", "")

        val path = NSBundle.mainBundle.pathForResource(fileName, extension)
        if (path == null) return

        val url = NSURL.fileURLWithPath(path)
        val audioPlayer = AVAudioPlayer(contentsOfURL = url, error = null) ?: return
        audioPlayer.numberOfLoops = 0
        audioPlayer.prepareToPlay()
        audioPlayer.play()
        player = audioPlayer
    }

    actual fun stop() {
        player?.stop()
        player = null
    }
}