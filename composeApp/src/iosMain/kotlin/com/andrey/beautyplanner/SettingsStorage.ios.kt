@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.andrey.beautyplanner

import platform.Foundation.*

private const val SETTINGS_FILE_NAME = "settings.json"

actual fun createSettingsStorage(): SettingsStorage {
    val fm = NSFileManager.defaultManager

    val urls = fm.URLsForDirectory(
        directory = NSDocumentDirectory,
        inDomains = NSUserDomainMask
    ) as List<NSURL>
    val dirUrl = urls.lastOrNull()
        ?: error("Cannot get Documents directory for settings storage")

    val fileUrl = dirUrl.URLByAppendingPathComponent(SETTINGS_FILE_NAME)
        ?: error("Cannot create settings.json URL")

    return IosFileSettingsStorage(fileUrl.path!!)
}

private class IosFileSettingsStorage(
    private val path: String
) : SettingsStorage {

    override fun write(text: String) {
        val ns = text as NSString
        ns.writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    }

    override fun read(): String? {
        return if (NSFileManager.defaultManager.fileExistsAtPath(path)) {
            NSString.stringWithContentsOfFile(path, encoding = NSUTF8StringEncoding, error = null) as String?
        } else {
            null
        }
    }
}