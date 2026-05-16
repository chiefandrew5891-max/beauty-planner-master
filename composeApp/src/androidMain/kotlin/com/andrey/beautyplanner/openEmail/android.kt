package com.andrey.beautyplanner

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri

actual fun openEmail(email: String) {
    val activity = AndroidAppContext.activity ?: return

    val emailUri = Uri.parse("mailto:${Uri.encode(email)}")

    val intent = Intent(Intent.ACTION_SENDTO, emailUri).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        putExtra(Intent.EXTRA_SUBJECT, "Beauty Planner")
    }

    try {
        activity.startActivity(Intent.createChooser(intent, "Send email"))
    } catch (_: ActivityNotFoundException) {
        try {
            activity.startActivity(intent)
        } catch (_: Exception) {
            // no-op
        }
    }
}