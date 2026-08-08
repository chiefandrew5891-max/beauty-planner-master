package com.andrey.beautyplanner.remote

import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.CloudSyncJson
import com.andrey.beautyplanner.ScheduleDateOverride
import com.andrey.beautyplanner.ServiceTemplate
import com.andrey.beautyplanner.WeeklyBlockedInterval
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

object MasterProfileSync {
    var lastPullDebug: String = "no pull yet"
        private set

    fun resetSessionPullState() {
        lastPullDebug = "session pull state reset"
    }

    suspend fun syncIfAuthenticated(): Result<Map<String, String>> {
        return runCatching {
            val serviceTemplatesJson =
                CloudSyncJson.json.encodeToString(AppSettings.serviceTemplates)
            val weeklyBlockedIntervalsJson =
                CloudSyncJson.json.encodeToString(AppSettings.weeklyBlockedIntervals)
            val scheduleDateOverridesJson =
                CloudSyncJson.json.encodeToString(AppSettings.scheduleDateOverrides)

            BackendBridge.syncMyMasterProfile(
                ownerName = AppSettings.ownerName,
                profileDisplayCustomName = AppSettings.profileDisplayCustomName,
                profilePhone = AppSettings.profilePhone,
                profilePhoneVisible = AppSettings.profilePhoneVisible,
                profileSpecialization = AppSettings.profileSpecialization,
                profileRating = AppSettings.profileRating,
                profileAvatarUrl = AppSettings.profileAvatarUrl,
                profileAvatarBase64 = AppSettings.profileAvatarBase64,
                profileAvatarStoragePath = AppSettings.profileAvatarStoragePath,
                clientInteractionsEnabled = AppSettings.clientInteractionsEnabled,
                serviceTemplatesJson = serviceTemplatesJson,
                weeklyBlockedIntervalsJson = weeklyBlockedIntervalsJson,
                scheduleDateOverridesJson = scheduleDateOverridesJson
            )
        }
    }

    suspend fun pullIfAuthenticated(force: Boolean = false): Result<Unit> {
        return runCatching {
            val payload = BackendBridge.getMyMasterProfile()

            lastPullDebug =
                "payload(found=${payload.found}, firebaseUid='${payload.firebaseUid}', owner='${payload.ownerName}', phone='${payload.profilePhone}', spec='${payload.profileSpecialization}', avatarLen=${payload.profileAvatarBase64.length}, templates=${payload.serviceTemplates.size}, weeklyBlockedIntervalsJsonLen=${payload.weeklyBlockedIntervalsJson.length}, scheduleDateOverridesJsonLen=${payload.scheduleDateOverridesJson.length}, updatedAt=${payload.updatedAt}, createdAt=${payload.createdAt}, force=$force)"

            val hasMeaningfulProfileData =
                payload.ownerName.isNotBlank() ||
                        payload.profilePhone.isNotBlank() ||
                        payload.profileSpecialization.isNotBlank() ||
                        payload.profileAvatarBase64.isNotBlank() ||
                        payload.serviceTemplates.isNotEmpty() ||
                        payload.weeklyBlockedIntervalsJson.isNotBlank() ||
                        payload.scheduleDateOverridesJson.isNotBlank()

            if (!payload.found && !hasMeaningfulProfileData) return@runCatching

            AppSettings.ownerName = payload.ownerName
            AppSettings.profileDisplayCustomName = payload.profileDisplayCustomName
            AppSettings.profilePhone = payload.profilePhone
            AppSettings.profilePhoneVisible = payload.profilePhoneVisible
            AppSettings.profileSpecialization = payload.profileSpecialization
            AppSettings.profileRating = payload.profileRating
            AppSettings.profileAvatarUrl = payload.profileAvatarUrl
            AppSettings.profileAvatarBase64 = payload.profileAvatarBase64
            AppSettings.profileAvatarStoragePath = payload.profileAvatarStoragePath
            AppSettings.clientInteractionsEnabled = payload.clientInteractionsEnabled

            AppSettings.serviceTemplates = payload.serviceTemplates.map { template ->
                ServiceTemplate(
                    id = template.id,
                    title = template.title,
                    defaultPrice = template.defaultPrice,
                    isActive = template.isActive
                )
            }

            AppSettings.weeklyBlockedIntervals =
                runCatching {
                    CloudSyncJson.json.decodeFromString<List<WeeklyBlockedInterval>>(
                        payload.weeklyBlockedIntervalsJson
                    )
                }.getOrDefault(emptyList())

            AppSettings.scheduleDateOverrides =
                runCatching {
                    CloudSyncJson.json.decodeFromString<List<ScheduleDateOverride>>(
                        payload.scheduleDateOverridesJson
                    )
                }.getOrDefault(emptyList())

            AppSettings.persist()

            lastPullDebug +=
                " | applied(owner='${AppSettings.ownerName}', phone='${AppSettings.profilePhone}', spec='${AppSettings.profileSpecialization}', avatarLen=${AppSettings.profileAvatarBase64.length}, templates=${AppSettings.serviceTemplates.size}, weeklyBlockedIntervals=${AppSettings.weeklyBlockedIntervals.size}, scheduleDateOverrides=${AppSettings.scheduleDateOverrides.size})"
        }.onFailure { error ->
            lastPullDebug = "pull failed: ${error.message}"
        }
    }
}