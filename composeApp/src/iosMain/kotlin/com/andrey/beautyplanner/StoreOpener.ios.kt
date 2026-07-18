package com.andrey.beautyplanner

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual object StoreOpener {
    actual fun open(url: String): Boolean {
        val nsUrl = NSURL.URLWithString(url) ?: return false
        val app = UIApplication.sharedApplication
        if (!app.canOpenURL(nsUrl)) return false
        app.openURL(nsUrl)
        return true
    }
}