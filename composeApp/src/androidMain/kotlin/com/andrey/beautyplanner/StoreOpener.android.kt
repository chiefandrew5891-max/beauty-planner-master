package com.andrey.beautyplanner

import android.content.Intent
import android.net.Uri

actual object StoreOpener {
    actual fun open(url: String): Boolean {
        val ctx = AndroidAppContext.context ?: return false
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}