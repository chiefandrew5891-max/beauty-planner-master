package com.andrey.beautyplanner

actual object AppInfoProvider {
    actual fun get(): AppInfo {
        val context = AndroidAppContext.context

        if (context == null) {
            return AppInfo(
                versionName = "—",
                buildNumber = "—",
                platformLabel = "Android"
            )
        }

        return runCatching {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)

            val versionName = packageInfo.versionName ?: "—"
            val buildNumber = packageInfo.longVersionCode.toString()

            AppInfo(
                versionName = versionName,
                buildNumber = buildNumber,
                platformLabel = "Android"
            )
        }.getOrElse {
            AppInfo(
                versionName = "—",
                buildNumber = "—",
                platformLabel = "Android"
            )
        }
    }
}