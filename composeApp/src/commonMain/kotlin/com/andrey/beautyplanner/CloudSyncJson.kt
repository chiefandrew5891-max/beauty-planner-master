package com.andrey.beautyplanner

import kotlinx.serialization.json.Json

object CloudSyncJson {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }
}