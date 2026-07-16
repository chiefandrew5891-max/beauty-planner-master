package com.andrey.beautyplanner

import android.content.Context
import java.io.File

object AndroidAppContext {
    var context: Context? = null
    var activity: android.app.Activity? = null
}

actual fun createAppointmentStorage(): AppointmentStorage {
    val ctx = AndroidAppContext.context
        ?: error("AndroidAppContext.context is not set. Set it before calling DataManager.")
    return FileAppointmentStorage(ctx)
}

private class FileAppointmentStorage(
    private val context: Context
) : AppointmentStorage {

    override fun write(profileKey: String, text: String) {
        fileFor(profileKey).writeText(text)
    }

    override fun read(profileKey: String): String? {
        val file = fileFor(profileKey)
        return if (file.exists()) file.readText() else null
    }

    private fun fileFor(profileKey: String): File {
        val safeKey = profileKey
            .trim()
            .ifBlank { "guest" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")

        return File(context.filesDir, "appointments_db_$safeKey.json")
    }
}