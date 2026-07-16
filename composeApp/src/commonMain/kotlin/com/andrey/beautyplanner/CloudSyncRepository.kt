package com.andrey.beautyplanner

interface CloudSyncRepository {
    suspend fun pullAll(userId: String): CloudSyncPullResult

    suspend fun pushAppointments(
        userId: String,
        appointments: List<Appointment>
    )

    suspend fun pushSettings(
        userId: String,
        settings: CloudSettingsSnapshot
    )

    suspend fun pushAll(
        userId: String,
        appointments: List<Appointment>,
        settings: CloudSettingsSnapshot
    ) {
        pushAppointments(userId, appointments)
        pushSettings(userId, settings)
    }
}