package com.andrey.beautyplanner

data class AppUpdateStatus(
    val checked: Boolean = false,
    val updateAvailable: Boolean = false,
    val latestVersion: String = "",
    val latestBuild: String = "",
    val storeUrl: String = "",
    val errorMessage: String = ""
)