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

    fun saveToDatabase(data: List<Appointment>) {
        try {
            val jsonString = jsonConfig.encodeToString(data)
            storage.write(jsonString)
        } catch (e: Exception) {
            println("Save error: ${e.message}")
        }
    }

    fun loadFromDatabase(): List<Appointment> {
        return try {
            val jsonString = storage.read() ?: return emptyList()
            if (jsonString.isBlank()) emptyList()
            else jsonConfig.decodeFromString<List<Appointment>>(jsonString)
        } catch (e: Exception) {
            println("Load error: ${e.message}")
            emptyList()
        }
    }

    fun exportBackup(data: List<Appointment>): String =
        try { jsonConfig.encodeToString(data) } catch (_: Exception) { "" }

    fun importBackup(json: String): List<Appointment> {
        if (json.isBlank()) return emptyList()
        return try { jsonConfig.decodeFromString<List<Appointment>>(json) } catch (_: Exception) { emptyList() }
    }
}

interface AppointmentStorage {
    fun write(text: String)
    fun read(): String?
}

expect fun createAppointmentStorage(): AppointmentStorage