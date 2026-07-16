package com.andrey.beautyplanner

import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

actual object CloudSyncRepositoryProvider {
    actual val repository: CloudSyncRepository = object : CloudSyncRepository {

        override suspend fun pullAll(userId: String): CloudSyncPullResult {
            val deferred = CompletableDeferred<Map<String, String>>()
            val loader = CloudSyncBridgeConnector.pullAll
                ?: error("iOS cloud sync bridge is not connected.")

            loader.invoke(userId, deferred)
            val result = deferred.await()

            val appointmentsPayload = result["appointments"].orEmpty()
            val settingsPayload = result["settings"].orEmpty()

            val appointments = if (appointmentsPayload.isBlank()) {
                emptyList()
            } else {
                runCatching {
                    CloudSyncJson.json.decodeFromString<List<Appointment>>(appointmentsPayload)
                }.getOrElse {
                    CloudSyncLogger.log("iOS pullAll: failed to decode appointments: ${it.message}")
                    emptyList()
                }
            }

            val settings = if (settingsPayload.isBlank()) {
                null
            } else {
                runCatching {
                    CloudSyncJson.json.decodeFromString<CloudSettingsSnapshot>(settingsPayload)
                }.getOrNull()
            }

            return CloudSyncPullResult(
                appointments = appointments,
                settings = settings
            )
        }

        override suspend fun pushAppointments(
            userId: String,
            appointments: List<Appointment>
        ) {
            val deferred = CompletableDeferred<Map<String, String>>()
            val pusher = CloudSyncBridgeConnector.pushAppointments
                ?: error("iOS cloud sync bridge is not connected.")

            val docs = appointments.map { appointment ->
                mapOf(
                    "id" to appointment.id,
                    "dateString" to appointment.dateString,
                    "time" to appointment.time,
                    "updatedAtMillis" to appointment.updatedAtMillis.toString(),
                    "isDeleted" to appointment.isDeleted.toString(),
                    "paymentStatus" to appointment.paymentStatus,
                    "payload" to CloudSyncJson.json.encodeToString(appointment)
                )
            }

            pusher.invoke(userId, docs, deferred)
            deferred.await()
        }

        override suspend fun pushSettings(
            userId: String,
            settings: CloudSettingsSnapshot
        ) {
            val deferred = CompletableDeferred<Map<String, String>>()
            val pusher = CloudSyncBridgeConnector.pushSettings
                ?: error("iOS cloud sync bridge is not connected.")

            val payload = mapOf(
                "updatedAtMillis" to settings.updatedAtMillis.toString(),
                "payload" to CloudSyncJson.json.encodeToString(settings)
            )

            pusher.invoke(userId, payload, deferred)
            deferred.await()
        }
    }
}