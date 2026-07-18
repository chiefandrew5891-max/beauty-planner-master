package com.andrey.beautyplanner

import com.andrey.beautyplanner.remote.BackendBridge

private fun parseBooleanLike(value: String?): Boolean {
    val normalized = value.orEmpty().trim().lowercase()
    return normalized == "true" || normalized == "1" || normalized == "yes"
}

object AppUpdateChecker {
    suspend fun check(): AppUpdateStatus {
        val appInfo = AppInfoProvider.get()
        val platform = getPlatform().backendPlatform

        return try {
            val result = BackendBridge.checkAppUpdate(
                platform = platform,
                versionName = appInfo.versionName,
                buildNumber = appInfo.buildNumber
            )

            AppUpdateStatus(
                checked = true,
                updateAvailable = parseBooleanLike(result["updateAvailable"]),
                latestVersion = result["latestVersion"].orEmpty(),
                latestBuild = result["latestBuild"].orEmpty(),
                storeUrl = result["storeUrl"].orEmpty(),
                errorMessage = result["errorMessage"].orEmpty()
            )
        } catch (t: Throwable) {
            AppUpdateStatus(
                checked = true,
                updateAvailable = false,
                errorMessage = t.message ?: Locales.t("about_app_update_failed")
            )
        }
    }
}