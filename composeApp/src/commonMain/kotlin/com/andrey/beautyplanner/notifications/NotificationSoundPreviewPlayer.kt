package com.andrey.beautyplanner.notifications

expect object NotificationSoundPreviewPlayer {
    fun play(soundType: String, soundId: String)
    fun stop()
}