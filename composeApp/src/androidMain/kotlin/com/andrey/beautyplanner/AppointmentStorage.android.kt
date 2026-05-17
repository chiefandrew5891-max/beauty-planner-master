package com.andrey.beautyplanner

import android.content.Context
import java.io.File

private const val DB_FILE_NAME = "appointments_db.json"

object AndroidAppContext {
    var context: Context? = null
    var activity: android.app.Activity? = null
}

actual fun createAppointmentStorage(): AppointmentStorage {
    val ctx = AndroidAppContext.context
        ?: error("AndroidAppContext.context is not set. Set it before calling DataManager.")
    val file = File(ctx.filesDir, DB_FILE_NAME)
    return FileAppointmentStorage(file)
}

private class FileAppointmentStorage(
    private val file: File
) : AppointmentStorage {
    override fun write(text: String) {
        file.writeText(text)
    }

    override fun read(): String? {
        return if (file.exists()) file.readText() else null
    }
}