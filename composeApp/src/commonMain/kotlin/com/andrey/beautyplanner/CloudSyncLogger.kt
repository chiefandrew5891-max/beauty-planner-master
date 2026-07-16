package com.andrey.beautyplanner

import androidx.compose.runtime.mutableStateListOf
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object CloudSyncLogger {
    private const val MAX_ENTRIES = 120

    val entries = mutableStateListOf<String>()

    fun log(message: String) {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val hh = now.hour.toString().padStart(2, '0')
        val mm = now.minute.toString().padStart(2, '0')
        val ss = now.second.toString().padStart(2, '0')
        val line = "$hh:$mm:$ss  $message"

        println("[CloudSync] $line")

        if (entries.size >= MAX_ENTRIES) {
            entries.removeAt(0)
        }
        entries.add(line)
    }

    fun clear() {
        entries.clear()
    }
}