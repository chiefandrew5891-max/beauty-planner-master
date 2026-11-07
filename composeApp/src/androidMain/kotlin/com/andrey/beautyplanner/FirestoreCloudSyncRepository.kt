package com.andrey.beautyplanner

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.withTimeout
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

class FirestoreCloudSyncRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : CloudSyncRepository {

    private fun userRoot(userId: String) =
        firestore.collection("users").document(userId)

    override suspend fun pullAll(userId: String, sinceMillis: Long): CloudSyncPullResult {
        CloudSyncLogger.log("pullAll: start userId=$userId since=$sinceMillis")

        val baseQuery = userRoot(userId).collection("appointments")
        val appointmentsQuery = if (sinceMillis > 0L) {
            baseQuery.whereGreaterThanOrEqualTo(
                "syncedAt",
                Timestamp(sinceMillis / 1000, ((sinceMillis % 1000) * 1_000_000).toInt())
            )
        } else {
            baseQuery
        }

        val appointmentsSnapshot = withTimeout(60_000) {
            appointmentsQuery.get().await()
        }

        val maxSyncedAtMillis = appointmentsSnapshot.documents.maxOfOrNull {
            it.getTimestamp("syncedAt")?.toDate()?.time ?: 0L
        } ?: 0L

        val appointments = appointmentsSnapshot.documents.mapNotNull { snapshot ->
            val payload = snapshot.getString("payload") ?: return@mapNotNull null
            runCatching {
                CloudSyncJson.json.decodeFromString<Appointment>(payload)
            }.onFailure {
                CloudSyncLogger.log("pullAll: failed to decode appointment doc=${snapshot.id}: ${it.message}")
            }.getOrNull()
        }

        val settingsSnapshot = withTimeout(15_000) {
            userRoot(userId)
                .collection("private")
                .document("app")
                .collection("meta")
                .document("settings")
                .get()
                .await()
        }

        val settings = settingsSnapshot.getString("payload")?.let { payload ->
            runCatching {
                CloudSyncJson.json.decodeFromString<CloudSettingsSnapshot>(payload)
            }.onFailure {
                CloudSyncLogger.log("pullAll: failed to decode settings: ${it.message}")
            }.getOrNull()
        }

        CloudSyncLogger.log(
            "pullAll: done userId=$userId appointments=${appointments.size} settingsPresent=${settings != null}"
        )

        return CloudSyncPullResult(
            appointments = appointments,
            settings = settings,
            maxSyncedAtMillis = maxSyncedAtMillis
        )
    }

    override suspend fun pushAppointments(
        userId: String,
        appointments: List<Appointment>
    ) {
        CloudSyncLogger.log(
            "pushAppointments: start userId=$userId total=${appointments.size}"
        )

        val collection = userRoot(userId).collection("appointments")

        appointments.chunked(350).forEachIndexed { index, chunk ->
            val batch = firestore.batch()

            chunk.forEach { appointment ->
                val docRef = collection.document(appointment.id)
                val payload = CloudSyncJson.json.encodeToString(appointment)

                batch.set(
                    docRef,
                    mapOf(
                        "id" to appointment.id,
                        "dateString" to appointment.dateString,
                        "time" to appointment.time,
                        "updatedAtMillis" to appointment.updatedAtMillis,
                        "isDeleted" to appointment.isDeleted,
                        "paymentStatus" to appointment.paymentStatus,
                        "payload" to payload,
                        "syncedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            CloudSyncLogger.log(
                "pushAppointments: committed chunk=${index + 1} size=${chunk.size}"
            )
        }

        CloudSyncLogger.log("pushAppointments: done userId=$userId")
    }

    override suspend fun pushSettings(
        userId: String,
        settings: CloudSettingsSnapshot
    ) {
        CloudSyncLogger.log(
            "pushSettings: start userId=$userId updatedAt=${settings.updatedAtMillis}"
        )

        val payload = CloudSyncJson.json.encodeToString(settings)

        userRoot(userId)
            .collection("private")
            .document("app")
            .collection("meta")
            .document("settings")
            .set(
                mapOf(
                    "updatedAtMillis" to settings.updatedAtMillis,
                    "payload" to payload
                )
            )
            .await()

        CloudSyncLogger.log("pushSettings: done userId=$userId")
    }
}