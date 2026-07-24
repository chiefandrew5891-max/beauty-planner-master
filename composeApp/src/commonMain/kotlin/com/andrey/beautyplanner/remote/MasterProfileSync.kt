package com.andrey.beautyplanner.remote

import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.CloudSyncJson
import kotlinx.serialization.encodeToString
import com.andrey.beautyplanner.ServiceTemplate

object MasterProfileSync {
    suspend fun syncIfAuthenticated(): Result<Map<String, String>> {
        val userId = AppSettings.backendUserId.trim()
        if (userId.isBlank()) return Result.success(emptyMap())

        return runCatching {
            val serviceTemplatesJson =
                CloudSyncJson.json.encodeToString(AppSettings.serviceTemplates)

            BackendBridge.syncMasterProfile(
                userId = userId,
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
        val userId = AppSettings.backendUserId.trim()
        if (userId.isBlank()) return Result.success(Unit)

        if (!force && masterProfilePulledThisSession) {
            return Result.success(Unit)
        }

        return runCatching {
            val payload = BackendBridge.getMasterProfile(userId)
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

            AppSettings.serviceTemplates = payload.serviceTemplates.map {
                ServiceTemplate(
                    id = it.id,
                    title = it.title,
                    defaultPrice = it.defaultPrice,
                    isActive = it.isActive
                )
            }

            AppSettings.persist()
            masterProfilePulledThisSession = true
        }
    }

    var masterProfilePulledThisSession: Boolean = false
        private set

    fun resetSessionPullState() {
        masterProfilePulledThisSession = false
    }

    suspend fun pullIfAuthenticated(): Result<Unit> {
        val userId = AppSettings.backendUserId.trim()
        if (userId.isBlank()) return Result.success(Unit)

        return runCatching {
            val payload = BackendBridge.getMasterProfile(userId)
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

            AppSettings.serviceTemplates = payload.serviceTemplates.map {
                ServiceTemplate(
                    id = it.id,
                    title = it.title,
                    defaultPrice = it.defaultPrice,
                    isActive = it.isActive
                )
            }

            AppSettings.persist()
        }
    }
}