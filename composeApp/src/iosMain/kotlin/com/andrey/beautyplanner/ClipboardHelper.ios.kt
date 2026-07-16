package com.andrey.beautyplanner

actual object ClipboardHelper {
    actual fun copyText(label: String, text: String): Boolean {
        return false
    }
}