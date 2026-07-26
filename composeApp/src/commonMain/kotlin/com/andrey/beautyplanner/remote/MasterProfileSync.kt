package com.andrey.beautyplanner.remote

import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.CloudSyncJson
import com.andrey.beautyplanner.ServiceTemplate
import kotlinx.serialization.encodeToString

object MasterProfileSync {
    var masterProfilePulledThisSession: Boolean = false
        private set

    var lastPullDebug: String = "no pull yet"
        private set

    fun resetSessionPullState() {
        masterProfilePulledThisSession = false
        lastPullDebug = "session pull state reset"
    }

    suspend fun syncIfAuthenticated(): Result<Map<String, String>> {
        return runCatching {
            val serviceTemplatesJson =
                CloudSyncJson.json.encodeToString(AppSettings.serviceTemplates)

            BackendBridge.syncMyMasterProfile(
                ownerName = AppSettings.ownerName,
                profileDisplayCustomName = AppSettings.profileDisplayCustomName,
                profilePhone = AppSettings.profilePhone,
                profilePhoneVisible = AppSettings.profilePhoneVisible,
                profileSpecialization = AppSettings.profileSpecialization,
                profileRating = AppSettings.profileRating,
                profileAvatarUrl = AppSettings.profileAvatarUrl,
                profileAvatarBase64 = AppSettings.profileAvatarBase64,
                clientInteractionsEnabled = AppSettings.clientInteractionsEnabled,
                serviceTemplatesJson = serviceTemplatesJson
            )
        }
    }

    suspend fun pullIfAuthenticated(force: Boolean = false): Result<Unit> {
        if (!force && masterProfilePulledThisSession) {
            lastPullDebug = "pull skipped: already pulled this session"
            return Result.success(Unit)
        }

        return runCatching {
            val payload = BackendBridge.getMyMasterProfile()

            lastPullDebug =
                "payload(found=${payload.found}, firebaseUid='${payload.firebaseUid}', owner='${payload.ownerName}', phone='${payload.profilePhone}', spec='${payload.profileSpecialization}', avatarLen=${payload.profileAvatarBase64.length}, templates=${payload.serviceTemplates.size}, updatedAt=${payload.updatedAt}, createdAt=${payload.createdAt})"

            if (!payload.found) return@runCatching

            AppSettings.ownerName = payload.ownerName
            AppSettings.profileDisplayCustomName = payload.profileDisplayCustomName
            AppSettings.profilePhone = payload.profilePhone
            AppSettings.profilePhoneVisible = payload.profilePhoneVisible
            AppSettings.profileSpecialization = payload.profileSpecialization
            AppSettings.profileRating = payload.profileRating
            AppSettings.profileAvatarUrl = payload.profileAvatarUrl
            AppSettings.profileAvatarBase64 = payload.profileAvatarBase64
            AppSettings.clientInteractionsEnabled = payload.clientInteractionsEnabled

            AppSettings.serviceTemplates = payload.serviceTemplates.map { template ->
                ServiceTemplate(
                    id = template.id,
                    title = template.title,
                    defaultPrice = template.defaultPrice,
                    isActive = template.isActive
                )
            }

            AppSettings.persist()
            masterProfilePulledThisSession = true

            lastPullDebug +=
                " | applied(owner='${AppSettings.ownerName}', phone='${AppSettings.profilePhone}', spec='${AppSettings.profileSpecialization}', avatarLen=${AppSettings.profileAvatarBase64.length})"
        }.onFailure { error ->
            lastPullDebug = "pull failed: ${error.message}"
        }
    }
}