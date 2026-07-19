package com.andrey.beautyplanner

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val jsonConfig = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    encodeDefaults = true
}

object DataManager {
    private val storage: AppointmentStorage by lazy { createAppointmentStorage() }

    fun saveToDatabase(
        data: List<Appointment>,
        profileKey: String = LocalProfileManager.currentProfileKey()
    ) {
        try {
            val jsonString = jsonConfig.encodeToString(data)
            storage.write(profileKey, jsonString)
        } catch (_: Exception) {
        }
    }

    fun loadFromDatabase(
        profileKey: String = LocalProfileManager.currentProfileKey()
    ): List<Appointment> {
        return try {
            val jsonString = storage.read(profileKey) ?: return emptyList()
            if (jsonString.isBlank()) emptyList()
            else jsonConfig.decodeFromString<List<Appointment>>(jsonString)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun exportBackupPayload(data: List<Appointment>): String =
        try {
            jsonConfig.encodeToString(data)
        } catch (_: Exception) {
            ""
        }

    fun importBackupPayload(json: String): List<Appointment> {
        if (json.isBlank()) return emptyList()
        return try {
            jsonConfig.decodeFromString<List<Appointment>>(json)
        } catch (_: Exception) {
            emptyList()
        }
    }
}

interface AppointmentStorage {
    fun write(profileKey: String, text: String)
    fun read(profileKey: String): String?
}

expect fun createAppointmentStorage(): AppointmentStorage