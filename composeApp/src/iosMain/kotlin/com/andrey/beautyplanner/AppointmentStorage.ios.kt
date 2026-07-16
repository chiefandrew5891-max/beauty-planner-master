package com.andrey.beautyplanner

import platform.Foundation.NSUserDefaults

actual fun createAppointmentStorage(): AppointmentStorage = IosUserDefaultsStorage()

private class IosUserDefaultsStorage : AppointmentStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun write(profileKey: String, text: String) {
        defaults.setObject(text, forKey = keyFor(profileKey))
    }

    override fun read(profileKey: String): String? {
        return defaults.stringForKey(keyFor(profileKey))
    }

    private fun keyFor(profileKey: String): String {
        val safeKey = profileKey
            .trim()
            .ifBlank { "guest" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return "appointments_db_json_$safeKey"
    }
}