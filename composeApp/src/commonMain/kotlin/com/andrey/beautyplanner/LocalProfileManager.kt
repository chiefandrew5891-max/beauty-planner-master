package com.andrey.beautyplanner

object LocalProfileManager {
    private const val GUEST_PROFILE_KEY = "guest"

    fun currentProfileKey(): String {
        val localUserId = AppSettings.localProfileUserId.trim()
        val key = if (localUserId.isBlank()) {
            GUEST_PROFILE_KEY
        } else {
            "user_$localUserId"
        }

        CloudSyncLogger.log(
            "LocalProfileManager.currentProfileKey: localProfileUserId=${localUserId.ifBlank { "—" }} key=$key"
        )

        return key
    }

    fun guestProfileKey(): String = GUEST_PROFILE_KEY

    fun profileKeyForUser(userId: String): String {
        val clean = userId.trim()
        return if (clean.isBlank()) GUEST_PROFILE_KEY else "user_$clean"
    }
}