package com.andrey.beautyplanner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.andrey.beautyplanner.notifications.NotificationSound
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.experimental.xor

private val settingsJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
}

@Serializable
private data class SettingsSnapshot(
    val isDarkMode: Boolean = false,
    val selectedLanguage: String = "Русский",
    val fontSizeMode: String = "Средний",

    val ownerName: String = "Euvgi",

    val notificationsEnabled: Boolean = true,
    val notificationSound: String = NotificationSound.DEFAULT.name,

    val reminderDaysBefore: Int = 0,
    val reminderHoursBefore: Int = 1,

    val servicePhone: String = "",

    val trialStartedAtMillis: Long = 0L,
    val premiumUnlocked: Boolean = false,

    // --- security ---
    val pinEnabled: Boolean = false,
    val adminPinHash: String = "" // stored hash, not pin
)
object AppSettings {

    private val storage: SettingsStorage by lazy { createSettingsStorage() }

    var isDarkMode by mutableStateOf(false)
    var selectedLanguage by mutableStateOf("Русский")

    /**
     * Stored values:
     * - "Мелкий"
     * - "Средний"
     * - "Крупный"
     *
     * Важно: не переименовываем эти строки, чтобы не ломать существующие сохранения.
     * Мы меняем только коэффициенты (getFontScale()) — это безопасно.
     */
    var fontSizeMode by mutableStateOf("Средний")

    var ownerName by mutableStateOf("Euvgi")

    // --- notifications settings ---
    var notificationsEnabled by mutableStateOf(true)
    var notificationSound by mutableStateOf(NotificationSound.DEFAULT)

    var reminderDaysBefore by mutableStateOf(0)   // 0..3
    var reminderHoursBefore by mutableStateOf(1)  // 0..12

    var servicePhone by mutableStateOf("")
    var trialStartedAtMillis by mutableStateOf(0L)
    var premiumUnlocked by mutableStateOf(false)

    // --- security ---
    var pinEnabled by mutableStateOf(false)
    private var adminPinHash by mutableStateOf("")

    fun reminderMinutesComputed(): List<Int> {
        val total = reminderDaysBefore * 24 * 60 + reminderHoursBefore * 60
        return if (total > 0) listOf(total) else emptyList()
    }

    val languageCodes = mapOf(
        "Italiano" to "it",
        "Русский" to "ru",
        "English" to "en",
        "Українська" to "uk"
    )

    /**
     * New mapping:
     * - "Мелкий"  : 0.80
     * - "Средний" : 1.10
     * - "Крупный" : 1.22
     */
    fun getFontScale(): Float = when (fontSizeMode) {
        "Мелкий" -> 0.80f
        "Крупный" -> 1.22f
        else -> 1.10f // "Средний"
    }

    fun isPinSet(): Boolean = adminPinHash.isNotBlank()

    fun isPinValidFormat(pin: String): Boolean =
        pin.length in 4..8 && pin.all { it.isDigit() }

    private fun hashPin(pin: String): String {
        // FNV-1a 32-bit
        var hash = 0x811C9DC5.toInt()
        val prime = 0x01000193
        val bytes = pin.encodeToByteArray()
        for (b in bytes) {
            hash = hash xor (b.toInt() and 0xff)
            hash *= prime
        }

        val mixed = hash xor 0x5A5A5A5A
        return mixed.toUInt().toString(16).padStart(8, '0')
    }

    fun setPin(pin: String): Boolean {
        if (!isPinValidFormat(pin)) return false
        adminPinHash = hashPin(pin)
        pinEnabled = true
        persist()
        return true
    }

    fun clearPin() {
        adminPinHash = ""
        pinEnabled = false
        persist()
    }

    fun verifyPin(pin: String): Boolean {
        if (!isPinValidFormat(pin)) return false
        if (adminPinHash.isBlank()) return false
        return adminPinHash == hashPin(pin)
    }

    fun load() {
        val raw = runCatching { storage.read() }.getOrNull() ?: return
        if (raw.isBlank()) return

        val snapshot = runCatching { settingsJson.decodeFromString<SettingsSnapshot>(raw) }
            .getOrNull() ?: return

        isDarkMode = snapshot.isDarkMode
        selectedLanguage = snapshot.selectedLanguage
        fontSizeMode = snapshot.fontSizeMode

        notificationsEnabled = snapshot.notificationsEnabled
        notificationSound = runCatching { NotificationSound.valueOf(snapshot.notificationSound) }
            .getOrNull() ?: NotificationSound.DEFAULT

        reminderDaysBefore = snapshot.reminderDaysBefore
        reminderHoursBefore = snapshot.reminderHoursBefore

        servicePhone = snapshot.servicePhone
        ownerName = snapshot.ownerName

        trialStartedAtMillis = snapshot.trialStartedAtMillis
        premiumUnlocked = snapshot.premiumUnlocked

        pinEnabled = snapshot.pinEnabled
        adminPinHash = snapshot.adminPinHash

        val code = languageCodes[selectedLanguage] ?: "en"
        Locales.currentLanguage = code
    }

    fun persist() {
        val snapshot = SettingsSnapshot(
            isDarkMode = isDarkMode,
            selectedLanguage = selectedLanguage,
            fontSizeMode = fontSizeMode,
            ownerName = ownerName,

            notificationsEnabled = notificationsEnabled,
            notificationSound = notificationSound.name,

            reminderDaysBefore = reminderDaysBefore,
            reminderHoursBefore = reminderHoursBefore,

            servicePhone = servicePhone,

            trialStartedAtMillis = trialStartedAtMillis,
            premiumUnlocked = premiumUnlocked,

            pinEnabled = pinEnabled,
            adminPinHash = adminPinHash
        )

        runCatching {
            storage.write(settingsJson.encodeToString(snapshot))
        }
    }
}