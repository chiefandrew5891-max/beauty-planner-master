package com.andrey.beautyplanner

import platform.Foundation.NSUserDefaults

private const val DB_KEY = "appointments_db_json"

actual fun createAppointmentStorage(): AppointmentStorage = IosUserDefaultsStorage()

private class IosUserDefaultsStorage : AppointmentStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun write(text: String) {
        defaults.setObject(text, forKey = DB_KEY)
    }

    override fun read(): String? {
        return defaults.stringForKey(DB_KEY)
    }
}