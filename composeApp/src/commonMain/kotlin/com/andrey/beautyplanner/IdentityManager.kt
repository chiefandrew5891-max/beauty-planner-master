package com.andrey.beautyplanner

import kotlin.random.Random

object IdentityManager {
    private const val LENGTH = 32
    private const val ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789"

    fun getOrCreateInstallId(): String {
        val existing = AppSettings.installId.trim()
        if (existing.isNotBlank()) return existing

        val generated = buildString {
            repeat(LENGTH) {
                append(ALPHABET[Random.nextInt(ALPHABET.length)])
            }
        }

        AppSettings.installId = generated
        AppSettings.persist()
        return generated
    }

    fun resetInstallId() {
        AppSettings.installId = ""
        AppSettings.persist()
    }
}