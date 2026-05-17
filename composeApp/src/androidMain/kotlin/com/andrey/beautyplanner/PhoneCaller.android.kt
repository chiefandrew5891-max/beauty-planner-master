package com.andrey.beautyplanner

import android.content.Intent
import android.net.Uri

actual object PhoneCaller {
    actual fun call(phone: String) {
        val ctx = AndroidAppContext.context ?: return
        val uri = Uri.parse("tel:${phone.trim()}")
        val intent = Intent(Intent.ACTION_DIAL, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(intent)
    }
}