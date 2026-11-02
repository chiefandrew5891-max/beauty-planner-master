@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.andrey.beautyplanner.notifications

import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSBundle
import platform.Foundation.NSLibraryDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

actual object NotificationSoundPreviewPlayer {
    private var player: AVAudioPlayer? = null

    actual fun play(soundType: String, soundId: String) {
        stop()
        if (soundId.isBlank() || (soundType != "BUNDLED" && soundType != "IMPORTED")) return

        val url = if (soundType == "IMPORTED") {
            val lib = NSSearchPathForDirectoriesInDomains(NSLibraryDirectory, NSUserDomainMask, true).first() as String
            NSURL.fileURLWithPath("$lib/Sounds/$soundId")
        } else {
            val fileName = soundId.substringBeforeLast(".")
            val extension = soundId.substringAfterLast(".", "")
            val path = NSBundle.mainBundle.pathForResource(fileName, extension) ?: return
            NSURL.fileURLWithPath(path)
        }
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