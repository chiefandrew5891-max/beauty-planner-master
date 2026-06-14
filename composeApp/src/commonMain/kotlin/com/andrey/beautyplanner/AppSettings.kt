package com.andrey.beautyplanner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.andrey.beautyplanner.notifications.NotificationSoundType
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

private fun defaultServiceTemplates(): List<ServiceTemplate> = listOf(
    ServiceTemplate(
        id = "service_gel_polish",
        title = "service_gel_polish",
        defaultPrice = "",
        isActive = true
    ),
    ServiceTemplate(
        id = "service_gel_strengthening",
        title = "service_gel_strengthening",
        defaultPrice = "",
        isActive = true
    ),
    ServiceTemplate(
        id = "service_nail_extensions",
        title = "service_nail_extensions",
        defaultPrice = "",
        isActive = true
    ),
    ServiceTemplate(
        id = "service_lash_extensions",
        title = "service_lash_extensions",
        defaultPrice = "",
        isActive = true
    ),
    ServiceTemplate(
        id = "service_correction",
        title = "service_correction",
        defaultPrice = "",
        isActive = true
    ),
    ServiceTemplate(
        id = "service_repair",
        title = "service_repair",
        defaultPrice = "",
        isActive = true
    )
)

@Serializable
private data class SettingsSnapshot(
    val isDarkMode: Boolean = false,
    val selectedLanguage: String = "Русский",
    val fontSizeMode: String = "medium",

    val ownerName: String = "",

    val notificationsEnabled: Boolean = true,
    val notificationSoundType: String = NotificationSoundType.DEFAULT.name,
    val notificationSoundId: String = "",
    val notificationSoundDisplayName: String = "",

    val selectedCurrency: String = "EUR",
    val useShortTextCurrency: Boolean = false,

    val reminderDaysBefore: Int = 0,
    val reminderHoursBefore: Int = 1,
    val reminderMinutesBefore: Int = 0,

    val servicePhone: String = "",

    val trialStartedAtMillis: Long = 0L,
    val premiumUnlocked: Boolean = false,
    val premiumProductId: String = "",
    val premiumPurchaseToken: String = "",

    val premiumSubscriptionState: String = "NONE",
    val premiumSubscribedProductId: String = "",
    val premiumSubscriptionToken: String = "",
    val premiumSubscriptionStartMillis: Long = 0L,
    val premiumSubscriptionExpiryMillis: Long = 0L,
    val premiumSubscriptionAutoRenewing: Boolean = false,
    val premiumLastVerifiedAtMillis: Long = 0L,

    val serviceTemplates: List<ServiceTemplate> = emptyList(),
    val weeklyBlockedIntervals: List<WeeklyBlockedInterval> = emptyList(),
    val scheduleDateOverrides: List<ScheduleDateOverride> = emptyList(),

    // --- security ---
    val pinEnabled: Boolean = false,
    val adminPinHash: String = "",
    val developerModeUnlocked: Boolean = false
)

object AppSettings {

    private const val DEVELOPER_ACCESS_PASSWORD = "221290"

    const val SHOW_DEVELOPER_PREMIUM_TOOLS = true
    private val storage: SettingsStorage by lazy { createSettingsStorage() }

    var selectedCurrency by mutableStateOf("EUR")
    var useShortTextCurrency by mutableStateOf(false)

    var isDarkMode by mutableStateOf(false)
    var selectedLanguage by mutableStateOf(
        run {
            // Получаем код языка системы (например, "it", "ru", "uk") через системную локаль Android/iOS
            val systemLanguageCode = androidx.compose.ui.text.intl.Locale.current.language.lowercase()
            when (systemLanguageCode) {
                "it" -> "Italiano"
                "uk" -> "Українська"
                "ru" -> "Русский"
                else -> "English" // Если язык немецкий, испанский и т.д. — включаем Английский по умолчанию
            }
        }
    )

    var fontSizeMode by mutableStateOf("medium")

    var ownerName by mutableStateOf("")

    var notificationsEnabled by mutableStateOf(true)
    var notificationSoundType by mutableStateOf("DEFAULT")
    var notificationSoundId by mutableStateOf("")
    var notificationSoundDisplayName by mutableStateOf("")

    var reminderDaysBefore by mutableStateOf(0)
    var reminderHoursBefore by mutableStateOf(1)
    var reminderMinutesBefore by mutableStateOf(0)

    var servicePhone by mutableStateOf("")
    var trialStartedAtMillis by mutableStateOf(0L)
    var premiumUnlocked by mutableStateOf(false)
    var premiumProductId by mutableStateOf("")
    var premiumPurchaseToken by mutableStateOf("")

    var serviceTemplates by mutableStateOf(defaultServiceTemplates())
    var weeklyBlockedIntervals by mutableStateOf<List<WeeklyBlockedInterval>>(emptyList())
    var scheduleDateOverrides by mutableStateOf<List<ScheduleDateOverride>>(emptyList())

    var pinEnabled by mutableStateOf(false)
    private var adminPinHash by mutableStateOf("")
    var developerModeUnlocked by mutableStateOf(false)
    var previewFontScaleOverride by mutableStateOf<Float?>(null)

    fun currencySymbol(): String {
        return if (useShortTextCurrency) {
            when (selectedCurrency) {
                "USD" -> "USD"
                "RUB" -> "RUB"
                "UAH" -> "UAH"
                else -> "EUR"
            }
        } else {
            when (selectedCurrency) {
                "USD" -> "$"
                "RUB" -> "₽"
                "UAH" -> "₴"
                else -> "€"
            }
        }
    }
    fun currencySymbolFor(
        currencyCode: String,
        useShortText: Boolean = useShortTextCurrency
    ): String {
        return if (useShortText) {
            when (currencyCode.uppercase()) {
                "USD" -> "USD"
                "RUB" -> "RUB"
                "UAH" -> "UAH"
                else -> "EUR"
            }
        } else {
            when (currencyCode.uppercase()) {
                "USD" -> "$"
                "RUB" -> "₽"
                "UAH" -> "₴"
                else -> "€"
            }
        }
    }

    fun formatMoneyAmount(
        amount: String,
        currencyCode: String,
        useShortText: Boolean = useShortTextCurrency
    ): String {
        val trimmed = amount.trim()
        if (trimmed.isBlank()) return ""
        return "$trimmed ${currencySymbolFor(currencyCode, useShortText)}"
    }
    fun saveCurrencySynchronously(newCurrency: String, useShortText: Boolean) {
        selectedCurrency = newCurrency
        useShortTextCurrency = useShortText
        persist()
    }

    fun getActiveServiceTemplates(): List<ServiceTemplate> =
        serviceTemplates.filter { it.isActive }

    fun upsertServiceTemplate(template: ServiceTemplate) {
        val idx = serviceTemplates.indexOfFirst { it.id == template.id }
        serviceTemplates =
            if (idx >= 0) {
                serviceTemplates.toMutableList().apply { set(idx, template) }
            } else {
                serviceTemplates + template
            }
        persist()
    }

    fun removeServiceTemplate(id: String) {
        serviceTemplates = serviceTemplates.filterNot { it.id == id }
        persist()
    }

    fun getActiveWeeklyBlockedIntervals(): List<WeeklyBlockedInterval> =
        weeklyBlockedIntervals.filter { it.isActive }

    fun upsertWeeklyBlockedInterval(interval: WeeklyBlockedInterval) {
        val idx = weeklyBlockedIntervals.indexOfFirst { it.id == interval.id }

        weeklyBlockedIntervals =
            if (idx >= 0) {
                weeklyBlockedIntervals.toMutableList().apply { set(idx, interval) }
            } else {
                val duplicateExists = weeklyBlockedIntervals.any {
                    it.dayOfWeek == interval.dayOfWeek &&
                            it.startTime == interval.startTime &&
                            it.endTime == interval.endTime
                }

                if (duplicateExists) {
                    weeklyBlockedIntervals
                } else {
                    weeklyBlockedIntervals + interval
                }
            }

        persist()
    }

    fun removeWeeklyBlockedInterval(id: String) {
        weeklyBlockedIntervals = weeklyBlockedIntervals.filterNot { it.id == id }
        persist()
    }

    fun addScheduleDateOverride(override: ScheduleDateOverride) {
        scheduleDateOverrides = scheduleDateOverrides + override
        persist()
    }

    fun removeScheduleDateOverride(id: String) {
        scheduleDateOverrides = scheduleDateOverrides.filterNot { it.id == id }
        persist()
    }

    fun reminderMinutesComputed(): List<Int> {
        val total =
            reminderDaysBefore * 24 * 60 +
                    reminderHoursBefore * 60 +
                    reminderMinutesBefore

        return if (total > 0) listOf(total) else emptyList()
    }
    fun isSilentNotificationSound(): Boolean {
        return notificationSoundType == "SILENT"
    }

    fun isDefaultNotificationSound(): Boolean {
        return notificationSoundType == "DEFAULT"
    }

    fun selectedNotificationSoundLabel(): String {
        return when (notificationSoundType) {
            "SILENT" -> Locales.t("notif_sound_silent")
            "BUNDLED", "IMPORTED" -> notificationSoundDisplayName.ifBlank { Locales.t("notif_sound_custom") }
            else -> Locales.t("notif_sound_default")
        }
    }
    fun notificationChannelKey(): String {
        return when (notificationSoundType) {
            "SILENT" -> "silent"
            "BUNDLED" -> notificationSoundId.ifBlank { "default" }
            "IMPORTED" -> notificationSoundId.ifBlank { "default" }
            else -> "default"
        }
    }

    val languageCodes = mapOf(
        "Italiano" to "it",
        "Русский" to "ru",
        "English" to "en",
        "Українська" to "uk"
    )

    var premiumSubscriptionState by mutableStateOf("NONE")
    var premiumSubscribedProductId by mutableStateOf("")
    var premiumSubscriptionToken by mutableStateOf("")
    var premiumSubscriptionStartMillis by mutableStateOf(0L)
    var premiumSubscriptionExpiryMillis by mutableStateOf(0L)
    var premiumSubscriptionAutoRenewing by mutableStateOf(false)
    var premiumLastVerifiedAtMillis by mutableStateOf(0L)

    private fun normalizeFontSizeMode(value: String): String {
        return when (value.trim()) {
            "Мелкий", "small" -> "small"
            "Крупный", "large" -> "large"
            "Средний", "medium" -> "medium"
            else -> "medium"
        }
    }

    fun getFontScale(): Float {
        previewFontScaleOverride?.let { return it }
        return when (normalizeFontSizeMode(fontSizeMode)) {
            "small" -> 0.80f
            "large" -> 1.22f
            else -> 1.10f
        }
    }

    fun isPinSet(): Boolean = adminPinHash.isNotBlank()

    fun isPinValidFormat(pin: String): Boolean =
        pin.length in 4..8 && pin.all { it.isDigit() }

    private fun hashPin(pin: String): String {
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

    fun verifyDeveloperPassword(password: String): Boolean {
        return password.trim() == DEVELOPER_ACCESS_PASSWORD
    }

    fun unlockDeveloperMode() {
        developerModeUnlocked = true
        persist()
    }

    fun lockDeveloperMode() {
        developerModeUnlocked = false
        persist()
    }

    fun load() {
        val raw = runCatching { storage.read() }.getOrNull() ?: return
        if (raw.isBlank()) return

        val snapshot = runCatching { settingsJson.decodeFromString<SettingsSnapshot>(raw) }
            .getOrNull() ?: return

        isDarkMode = snapshot.isDarkMode
        selectedLanguage = snapshot.selectedLanguage
        fontSizeMode = normalizeFontSizeMode(snapshot.fontSizeMode)
        selectedCurrency = snapshot.selectedCurrency
        useShortTextCurrency = snapshot.useShortTextCurrency

        notificationsEnabled = snapshot.notificationsEnabled
        notificationSoundType = snapshot.notificationSoundType
        notificationSoundId = snapshot.notificationSoundId
        notificationSoundDisplayName = snapshot.notificationSoundDisplayName

        reminderDaysBefore = snapshot.reminderDaysBefore
        reminderHoursBefore = snapshot.reminderHoursBefore
        reminderMinutesBefore = snapshot.reminderMinutesBefore

        servicePhone = snapshot.servicePhone
        ownerName = snapshot.ownerName

        trialStartedAtMillis = snapshot.trialStartedAtMillis
        premiumUnlocked = snapshot.premiumUnlocked
        premiumProductId = snapshot.premiumProductId
        premiumPurchaseToken = snapshot.premiumPurchaseToken

        premiumSubscriptionState = snapshot.premiumSubscriptionState
        premiumSubscribedProductId = snapshot.premiumSubscribedProductId
        premiumSubscriptionToken = snapshot.premiumSubscriptionToken
        premiumSubscriptionStartMillis = snapshot.premiumSubscriptionStartMillis
        premiumSubscriptionExpiryMillis = snapshot.premiumSubscriptionExpiryMillis
        premiumSubscriptionAutoRenewing = snapshot.premiumSubscriptionAutoRenewing
        premiumLastVerifiedAtMillis = snapshot.premiumLastVerifiedAtMillis

        serviceTemplates = if (snapshot.serviceTemplates.isNotEmpty()) {
            snapshot.serviceTemplates
        } else {
            defaultServiceTemplates()
        }

        weeklyBlockedIntervals = snapshot.weeklyBlockedIntervals
        scheduleDateOverrides = snapshot.scheduleDateOverrides

        pinEnabled = snapshot.pinEnabled
        adminPinHash = snapshot.adminPinHash
        developerModeUnlocked = snapshot.developerModeUnlocked

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
            notificationSoundType = notificationSoundType,
            notificationSoundId = notificationSoundId,
            notificationSoundDisplayName = notificationSoundDisplayName,

            reminderDaysBefore = reminderDaysBefore,
            reminderHoursBefore = reminderHoursBefore,
            reminderMinutesBefore = reminderMinutesBefore,

            servicePhone = servicePhone,

            trialStartedAtMillis = trialStartedAtMillis,
            premiumUnlocked = premiumUnlocked,
            premiumProductId = premiumProductId,
            premiumPurchaseToken = premiumPurchaseToken,

            serviceTemplates = serviceTemplates,
            weeklyBlockedIntervals = weeklyBlockedIntervals,
            scheduleDateOverrides = scheduleDateOverrides,

            premiumSubscriptionState = premiumSubscriptionState,
            premiumSubscribedProductId = premiumSubscribedProductId,
            premiumSubscriptionToken = premiumSubscriptionToken,
            premiumSubscriptionStartMillis = premiumSubscriptionStartMillis,
            premiumSubscriptionExpiryMillis = premiumSubscriptionExpiryMillis,
            premiumSubscriptionAutoRenewing = premiumSubscriptionAutoRenewing,
            premiumLastVerifiedAtMillis = premiumLastVerifiedAtMillis,

            selectedCurrency = selectedCurrency,
            useShortTextCurrency = useShortTextCurrency,

            pinEnabled = pinEnabled,
            adminPinHash = adminPinHash,
            developerModeUnlocked = developerModeUnlocked

        )

        runCatching {
            storage.write(settingsJson.encodeToString(snapshot))
        }
    }
}