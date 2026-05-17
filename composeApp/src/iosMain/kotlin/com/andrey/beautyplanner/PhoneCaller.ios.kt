package com.andrey.beautyplanner

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual object PhoneCaller {
    actual fun call(phone: String) {
        val cleaned = phone.trim()
        if (cleaned.isBlank()) return

        val url = NSURL.URLWithString("tel://$cleaned") ?: return
        val app = UIApplication.sharedApplication
        if (app.canOpenURL(url)) {
            app.openURL(url)
        }
    }
}