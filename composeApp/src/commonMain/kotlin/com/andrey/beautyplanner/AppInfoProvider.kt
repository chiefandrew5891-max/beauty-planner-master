package com.andrey.beautyplanner

data class AppInfo(
    val versionName: String,
    val buildNumber: String,
    val platformLabel: String
)

expect object AppInfoProvider {
    fun get(): AppInfo
}