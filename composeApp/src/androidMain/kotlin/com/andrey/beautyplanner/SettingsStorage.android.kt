package com.andrey.beautyplanner

import java.io.File

private const val SETTINGS_FILE_NAME = "settings.json"

actual fun createSettingsStorage(): SettingsStorage {
    val ctx = AndroidAppContext.context
        ?: error("AndroidAppContext.context is not set. Set it before calling AppSettings.load().")
    val file = File(ctx.filesDir, SETTINGS_FILE_NAME)
    return FileSettingsStorage(file)
}

private class FileSettingsStorage(
    private val file: File
) : SettingsStorage {
    override fun write(text: String) {
        file.writeText(text)
    }

    override fun read(): String? {
        return if (file.exists()) file.readText() else null
    }
}