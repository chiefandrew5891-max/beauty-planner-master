package com.andrey.beautyplanner

import platform.Foundation.NSBundle

actual object AppInfoProvider {
    actual fun get(): AppInfo {
        val info = NSBundle.mainBundle.infoDictionary
        val versionName = info?.get("CFBundleShortVersionString") as? String ?: "—"
        val buildNumber = info?.get("CFBundleVersion") as? String ?: "—"

        return AppInfo(
            versionName = versionName,
            buildNumber = buildNumber,
            platformLabel = "iOS"
        )
    }
}