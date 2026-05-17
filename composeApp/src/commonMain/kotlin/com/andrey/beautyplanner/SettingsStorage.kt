package com.andrey.beautyplanner

interface SettingsStorage {
    fun write(text: String)
    fun read(): String?
}

expect fun createSettingsStorage(): SettingsStorage