package com.andrey.beautyplanner.remote

import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.CloudSyncJson
import com.andrey.beautyplanner.CloudSyncLogger
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
            val dateRangeBlockedIntervalsJson =
                CloudSyncJson.json.encodeToString(AppSettings.dateRangeBlockedIntervals)

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
                scheduleDateOverridesJson = scheduleDateOverridesJson,
                dateRangeBlockedIntervalsJson = dateRangeBlockedIntervalsJson,
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

            val previousAvatarUrl = AppSettings.profileAvatarUrl
            val previousAvatarBase64 = AppSettings.profileAvatarBase64
            val previousAvatarStoragePath = AppSettings.profileAvatarStoragePath

            AppSettings.ownerName = payload.ownerName
            AppSettings.profileDisplayCustomName = payload.profileDisplayCustomName
            AppSettings.profilePhone = payload.profilePhone
            AppSettings.profilePhoneVisible = payload.profilePhoneVisible
            AppSettings.profileSpecialization = payload.profileSpecialization
            AppSettings.profileRating = payload.profileRating

            AppSettings.profileAvatarUrl = payload.profileAvatarUrl
            AppSettings.profileAvatarStoragePath = payload.profileAvatarStoragePath

            AppSettings.profileAvatarBase64 =
                when {
                    payload.profileAvatarBase64.isNotBlank() -> payload.profileAvatarBase64

                    payload.profileAvatarUrl.isBlank() &&
                            payload.profileAvatarStoragePath.isBlank() -> ""

                    payload.profileAvatarUrl == previousAvatarUrl &&
                            payload.profileAvatarStoragePath == previousAvatarStoragePath &&
                            previousAvatarBase64.isNotBlank() -> previousAvatarBase64

                    else -> previousAvatarBase64
                }

            AppSettings.clientInteractionsEnabled = payload.clientInteractionsEnabled

            val remoteServiceTemplates = payload.serviceTemplates.map { template ->
                ServiceTemplate(
                    id = template.id,
                    title = template.title,
                    defaultPrice = template.defaultPrice,
                    isActive = template.isActive
                )
            }

            val remoteWeeklyBlockedIntervals =
                runCatching {
                    CloudSyncJson.json.decodeFromString<List<WeeklyBlockedInterval>>(
                        payload.weeklyBlockedIntervalsJson
                    )
                }.getOrDefault(emptyList())

            val remoteScheduleDateOverrides =
                runCatching {
                    CloudSyncJson.json.decodeFromString<List<ScheduleDateOverride>>(
                        payload.scheduleDateOverridesJson
                    )
                }.getOrDefault(emptyList())

            fun normalizeTitle(value: String): String {
                return value.trim().lowercase()
            }

            fun mergeServiceTemplates(
                local: List<ServiceTemplate>,
                remote: List<ServiceTemplate>
            ): List<ServiceTemplate> {
                if (remote.isEmpty()) return local

                val merged = local.toMutableList()

                remote.forEach { remoteItem ->
                    val localIndexById = merged.indexOfFirst { it.id == remoteItem.id }
                    val localIndexByTitle = merged.indexOfFirst {
                        normalizeTitle(it.title) == normalizeTitle(remoteItem.title)
                    }

                    when {
                        localIndexById >= 0 -> {
                            val localItem = merged[localIndexById]
                            merged[localIndexById] = localItem.copy(
                                title = if (remoteItem.title.isNotBlank()) remoteItem.title else localItem.title,
                                defaultPrice = if (remoteItem.defaultPrice.isNotBlank()) {
                                    remoteItem.defaultPrice
                                } else {
                                    localItem.defaultPrice
                                },
                                isActive = remoteItem.isActive
                            )
                        }

                        localIndexByTitle >= 0 -> {
                            val localItem = merged[localIndexByTitle]
                            merged[localIndexByTitle] = localItem.copy(
                                id = if (remoteItem.id.isNotBlank()) remoteItem.id else localItem.id,
                                title = if (remoteItem.title.isNotBlank()) remoteItem.title else localItem.title,
                                defaultPrice = if (remoteItem.defaultPrice.isNotBlank()) {
                                    remoteItem.defaultPrice
                                } else {
                                    localItem.defaultPrice
                                },
                                isActive = remoteItem.isActive
                            )
                        }

                        else -> {
                            merged.add(remoteItem)
                        }
                    }
                }

                return merged
            }

            fun mergeWeeklyBlockedIntervals(
                local: List<WeeklyBlockedInterval>,
                remote: List<WeeklyBlockedInterval>
            ): List<WeeklyBlockedInterval> {
                if (remote.isEmpty()) return local

                val merged = local.toMutableList()

                remote.forEach { remoteItem ->
                    val localIndexById = merged.indexOfFirst { it.id == remoteItem.id }
                    val localIndexBySlot = merged.indexOfFirst {
                        it.dayOfWeek == remoteItem.dayOfWeek &&
                                it.startTime == remoteItem.startTime &&
                                it.endTime == remoteItem.endTime
                    }

                    when {
                        localIndexById >= 0 -> {
                            merged[localIndexById] = remoteItem
                        }

                        localIndexBySlot >= 0 -> {
                            val localItem = merged[localIndexBySlot]
                            merged[localIndexBySlot] = localItem.copy(
                                id = if (remoteItem.id.isNotBlank()) remoteItem.id else localItem.id,
                                isActive = remoteItem.isActive
                            )
                        }

                        else -> {
                            merged.add(remoteItem)
                        }
                    }
                }

                return merged
            }

            fun mergeScheduleDateOverrides(
                local: List<ScheduleDateOverride>,
                remote: List<ScheduleDateOverride>
            ): List<ScheduleDateOverride> {
                if (remote.isEmpty()) return local

                val merged = local.toMutableList()

                remote.forEach { remoteItem ->
                    val localIndexById = merged.indexOfFirst { it.id == remoteItem.id }
                    val localIndexByDate = merged.indexOfFirst { it.date == remoteItem.date }

                    when {
                        localIndexById >= 0 -> {
                            merged[localIndexById] = remoteItem
                        }

                        localIndexByDate >= 0 -> {
                            val localItem = merged[localIndexByDate]
                            merged[localIndexByDate] = localItem.copy(
                                id = if (remoteItem.id.isNotBlank()) remoteItem.id else localItem.id,
                                unblockAll = remoteItem.unblockAll
                            )
                        }

                        else -> {
                            merged.add(remoteItem)
                        }
                    }
                }

                return merged
            }

            fun comparableTemplates(list: List<ServiceTemplate>): List<String> {
                return list.map {
                    "${it.id.trim()}|${normalizeTitle(it.title)}|${it.defaultPrice.trim()}|${it.isActive}"
                }.sorted()
            }

            fun comparableWeeklyBlockedIntervals(list: List<WeeklyBlockedInterval>): List<String> {
                return list.map {
                    "${it.id.trim()}|${it.dayOfWeek}|${it.startTime.trim()}|${it.endTime.trim()}|${it.isActive}"
                }.sorted()
            }

            fun comparableScheduleDateOverrides(list: List<ScheduleDateOverride>): List<String> {
                return list.map {
                    "${it.id.trim()}|${it.date.trim()}|${it.unblockAll}"
                }.sorted()
            }

            val localServiceTemplates = AppSettings.serviceTemplates
            val localWeeklyBlockedIntervals = AppSettings.weeklyBlockedIntervals
            val localScheduleDateOverrides = AppSettings.scheduleDateOverrides

            val mergedServiceTemplates = mergeServiceTemplates(
                local = localServiceTemplates,
                remote = remoteServiceTemplates
            )

            val mergedWeeklyBlockedIntervals = mergeWeeklyBlockedIntervals(
                local = localWeeklyBlockedIntervals,
                remote = remoteWeeklyBlockedIntervals
            )

            val mergedScheduleDateOverrides = mergeScheduleDateOverrides(
                local = localScheduleDateOverrides,
                remote = remoteScheduleDateOverrides
            )

            AppSettings.serviceTemplates = mergedServiceTemplates
            AppSettings.weeklyBlockedIntervals = mergedWeeklyBlockedIntervals
            AppSettings.scheduleDateOverrides = mergedScheduleDateOverrides

            AppSettings.persist()

            val remoteTemplatesComparable = comparableTemplates(remoteServiceTemplates)
            val mergedTemplatesComparable = comparableTemplates(mergedServiceTemplates)

            val remoteWeeklyComparable = comparableWeeklyBlockedIntervals(remoteWeeklyBlockedIntervals)
            val mergedWeeklyComparable = comparableWeeklyBlockedIntervals(mergedWeeklyBlockedIntervals)

            val remoteOverridesComparable = comparableScheduleDateOverrides(remoteScheduleDateOverrides)
            val mergedOverridesComparable = comparableScheduleDateOverrides(mergedScheduleDateOverrides)

            val needsResync =
                mergedTemplatesComparable != remoteTemplatesComparable ||
                        mergedWeeklyComparable != remoteWeeklyComparable ||
                        mergedOverridesComparable != remoteOverridesComparable

            if (needsResync) {
                runCatching {
                    syncIfAuthenticated()
                }.onFailure { error ->
                    CloudSyncLogger.log("pullIfAuthenticated: resync after merge failed: ${error.message}")
                }
            }

            lastPullDebug +=
                " | applied(owner='${AppSettings.ownerName}', phone='${AppSettings.profilePhone}', spec='${AppSettings.profileSpecialization}', avatarLen=${AppSettings.profileAvatarBase64.length}, templatesLocal=${localServiceTemplates.size}, templatesRemote=${remoteServiceTemplates.size}, templatesMerged=${mergedServiceTemplates.size}, weeklyLocal=${localWeeklyBlockedIntervals.size}, weeklyRemote=${remoteWeeklyBlockedIntervals.size}, weeklyMerged=${mergedWeeklyBlockedIntervals.size}, overridesLocal=${localScheduleDateOverrides.size}, overridesRemote=${remoteScheduleDateOverrides.size}, overridesMerged=${mergedScheduleDateOverrides.size}, resync=$needsResync)"
        }.onFailure { error ->
            lastPullDebug = "pull failed: ${error.message}"
        }
    }
}