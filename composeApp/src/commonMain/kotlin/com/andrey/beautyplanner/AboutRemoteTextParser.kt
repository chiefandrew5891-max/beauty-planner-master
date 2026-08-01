package com.andrey.beautyplanner

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
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

        val parsedElement = runCatching {
            json.parseToJsonElement(source)
        }.getOrNull() ?: return source

        val parsedObject = parsedElement as? JsonObject ?: return source

        val normalizedCurrent = currentLanguage.trim().lowercase()
        val normalizedCurrentBase = normalizedCurrent.substringBefore("-").substringBefore("_")
        val normalizedPrimary = fallbackPrimary.trim().lowercase()
        val normalizedSecondary = fallbackSecondary.trim().lowercase()

        fun getValue(key: String): String? {
            return parsedObject[key]
                ?.jsonPrimitive
                ?.content
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        }

        return getValue(normalizedCurrent)
            ?: getValue(normalizedCurrentBase)
            ?: getValue(normalizedPrimary)
            ?: getValue(normalizedSecondary)
            ?: firstNonBlankValue(parsedObject)
            ?: source
    }

    private fun firstNonBlankValue(obj: JsonObject): String? {
        return obj.values
            .asSequence()
            .mapNotNull { elementToText(it) }
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
    }

    private fun elementToText(element: JsonElement): String? {
        return runCatching {
            element.jsonPrimitive.content
        }.getOrNull()
    }
}