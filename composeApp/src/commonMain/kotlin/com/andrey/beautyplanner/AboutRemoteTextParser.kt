package com.andrey.beautyplanner

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object AboutRemoteTextParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun resolveLocalizedText(
        raw: String,
        currentLanguage: String,
        fallbackPrimary: String = "en",
        fallbackSecondary: String = "ru"
    ): String {
        val source = raw.trim()
        if (source.isBlank()) return ""

        val parsed = runCatching {
            json.parseToJsonElement(source).jsonObject
        }.getOrNull() ?: return source

        val normalizedCurrent = currentLanguage.trim().lowercase()
        val normalizedPrimary = fallbackPrimary.trim().lowercase()
        val normalizedSecondary = fallbackSecondary.trim().lowercase()

        fun getValue(lang: String): String? {
            return parsed[lang]
                ?.jsonPrimitive
                ?.content
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        }

        return getValue(normalizedCurrent)
            ?: getValue(normalizedPrimary)
            ?: getValue(normalizedSecondary)
            ?: parsed.values
                .firstOrNull()
                ?.jsonPrimitive
                ?.content
                ?.trim()
                .orEmpty()
    }
}