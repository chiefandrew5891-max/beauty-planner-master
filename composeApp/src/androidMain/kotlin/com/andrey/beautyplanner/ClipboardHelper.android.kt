package com.andrey.beautyplanner

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

actual object ClipboardHelper {
    actual fun copyText(label: String, text: String): Boolean {
        val context = AndroidAppContext.context ?: return false
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return false

        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        return true
    }
}