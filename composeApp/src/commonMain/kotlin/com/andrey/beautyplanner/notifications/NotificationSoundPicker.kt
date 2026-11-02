package com.andrey.beautyplanner.notifications

const val MAX_CUSTOM_SOUND_SECONDS = 30
const val MAX_CUSTOM_SOUND_BYTES = 2L * 1024 * 1024

expect object NotificationSoundPicker {
    fun pick(onPicked: (String, String) -> Unit, onError: (String) -> Unit)
}