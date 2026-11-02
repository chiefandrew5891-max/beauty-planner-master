package com.andrey.beautyplanner.notifications

actual object NotificationSoundPicker {
    var pickImpl: ((onPicked: (String, String) -> Unit, onError: (String) -> Unit) -> Unit)? = null

    actual fun pick(onPicked: (String, String) -> Unit, onError: (String) -> Unit) {
        pickImpl?.invoke(onPicked, onError) ?: onError("read")
    }
}