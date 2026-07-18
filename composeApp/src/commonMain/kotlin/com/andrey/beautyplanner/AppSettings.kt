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
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.minutes

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
    val premiumOrderId: String = "",
    val premiumLastVerifiedAtMillis: Long = 0L,

    val installId: String = "",
    val backendUserId: String = "",
    val localProfileUserId: String = "",
    val lastAuthProvider: String = "",
    val lastAuthEmail: String = "",
    val lastAuthDisplayName: String = "",
    val lastAuthenticatedAppOpenAtMillis: Long = 0L,
    val cachedAccessTier: String = "FREE_LIMITED",
    val cachedTrialEndsAtMillis: Long = 0L,
    val cachedHasPremium: Boolean = false,
    val cachedSubscriptionState: String = "NONE",

    val serviceTemplates: List<ServiceTemplate> = emptyList(),
    val weeklyBlockedIntervals: List<WeeklyBlockedInterval> = emptyList(),
    val scheduleDateOverrides: List<ScheduleDateOverride> = emptyList(),

    val pinEnabled: Boolean = false,
    val adminPinHash: String = "",
    val developerModeUnlocked: Boolean = false,
    val developerPremiumOverrideEnabled: Boolean = false,

    val cloudSettingsUpdatedAtMillis: Long = 0L,

    val lastUpdateCheckAtMillis: Long = 0L,
    val lastKnownUpdateAvailable: Boolean = false,
    val lastKnownLatestVersion: String = "",
    val lastKnownLatestBuild: String = "",
    val lastKnownStoreUrl: String = "",
)

object AppSettings {
    const val SHOW_DEVELOPER_PREMIUM_TOOLS = true
    private val storage: SettingsStorage by lazy { createSettingsStorage() }

    var cloudSettingsUpdatedAtMillis by mutableStateOf(0L)

    var selectedCurrency by mutableStateOf("EUR")
    var useShortTextCurrency by mutableStateOf(false)

    var isDarkMode by mutableStateOf(false)
    var selectedLanguage by mutableStateOf(
        run {
            val systemLanguageCode = androidx.compose.ui.text.intl.Locale.current.language.lowercase()
            when (systemLanguageCode) {
                "ar" -> "العربية"
                "bg" -> "Български"
                "cs" -> "Čeština"
                "da" -> "Dansk"
                "de" -> "Deutsch"
                "el" -> "Ελληνικά"
                "es" -> "Español"
                "et" -> "Eesti"
                "fi" -> "Suomi"
                "fr" -> "Français"
                "hi" -> "हिन्दी"
                "hu" -> "Magyar"
                "id" -> "Bahasa Indonesia"
                "it" -> "Italiano"
                "ja" -> "日本語"
                "ko" -> "한국어"
                "lt" -> "Lietuvių"
                "lv" -> "Latviešu"
                "nl" -> "Nederlands"
                "no" -> "Norsk"
                "pl" -> "Polski"
                "pt" -> "Português (Brasil)"
                "ro" -> "Română"
                "ru" -> "Русский"
                "sk" -> "Slovenčina"
                "sl" -> "Slovenščina"
                "sr" -> "Srpski"
                "sv" -> "Svenska"
                "tr" -> "Türkçe"
                "uk" -> "Українська"
                "zh" -> "中文"
                else -> "English"
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
    var developerPremiumOverrideEnabled by mutableStateOf(false)

    val languageCodes = linkedMapOf(
        "العربية" to "ar",
        "Български" to "bg",
        "Čeština" to "cs",
        "Dansk" to "da",
        "Deutsch" to "de",
        "Ελληνικά" to "el",
        "English" to "en",
        "Español" to "es",
        "Eesti" to "et",
        "Suomi" to "fi",
        "Français" to "fr",
        "हिन्दी" to "hi",
        "Magyar" to "hu",
        "Bahasa Indonesia" to "id",
        "Italiano" to "it",
        "日本語" to "ja",
        "한국어" to "ko",
        "Lietuvių" to "lt",
        "Latviešu" to "lv",
        "Nederlands" to "nl",
        "Norsk" to "no",
        "Polski" to "pl",
        "Português (Brasil)" to "pt-BR",
        "Română" to "ro",
        "Русский" to "ru",
        "Slovenčina" to "sk",
        "Slovenščina" to "sl",
        "Srpski" to "sr",
        "Svenska" to "sv",
        "Türkçe" to "tr",
        "Українська" to "uk",
        "中文" to "zh"
    )

    var premiumSubscriptionState by mutableStateOf("NONE")
    var premiumSubscribedProductId by mutableStateOf("")
    var premiumSubscriptionToken by mutableStateOf("")
    var premiumSubscriptionStartMillis by mutableStateOf(0L)
    var premiumSubscriptionExpiryMillis by mutableStateOf(0L)
    var premiumSubscriptionAutoRenewing by mutableStateOf(false)
    var premiumOrderId by mutableStateOf("")
    var premiumLastVerifiedAtMillis by mutableStateOf(0L)

    var installId by mutableStateOf("")
    var backendUserId by mutableStateOf("")
    var localProfileUserId by mutableStateOf("")
    var lastAuthProvider by mutableStateOf("")
    var lastAuthEmail by mutableStateOf("")
    var lastAuthDisplayName by mutableStateOf("")
    var lastAuthenticatedAppOpenAtMillis by mutableStateOf(0L)
    var cachedAccessTier by mutableStateOf("FREE_LIMITED")
    var cachedTrialEndsAtMillis by mutableStateOf(0L)
    var cachedHasPremium by mutableStateOf(false)
    var cachedSubscriptionState by mutableStateOf("NONE")
    var lastUpdateCheckAtMillis by mutableStateOf(0L)
    var lastKnownUpdateAvailable by mutableStateOf(false)
    var lastKnownLatestVersion by mutableStateOf("")
    var lastKnownLatestBuild by mutableStateOf("")
    var lastKnownStoreUrl by mutableStateOf("")

    fun currencySymbol(): String {
        return currencySymbolFor(selectedCurrency, useShortTextCurrency)
    }

    fun currencySymbolFor(
        currencyCode: String,
        useShortText: Boolean = useShortTextCurrency
    ): String {
        val code = currencyCode.uppercase()
        return if (useShortText) {
            code
        } else {
            CurrencyCatalog.getSymbol(code)
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

    fun exportCloudSettingsSnapshot(nowMillis: Long): CloudSettingsSnapshot {
        cloudSettingsUpdatedAtMillis = nowMillis

        return CloudSettingsSnapshot(
            ownerName = ownerName,
            selectedCurrency = selectedCurrency,
            useShortTextCurrency = useShortTextCurrency,

            notificationsEnabled = notificationsEnabled,
            notificationSoundType = notificationSoundType,
            notificationSoundId = notificationSoundId,
            notificationSoundDisplayName = notificationSoundDisplayName,

            reminderDaysBefore = reminderDaysBefore,
            reminderHoursBefore = reminderHoursBefore,
            reminderMinutesBefore = reminderMinutesBefore,

            serviceTemplates = serviceTemplates,
            weeklyBlockedIntervals = weeklyBlockedIntervals,
            scheduleDateOverrides = scheduleDateOverrides,

            updatedAtMillis = nowMillis
        )
    }

    fun applyCloudSettingsSnapshot(snapshot: CloudSettingsSnapshot) {
        ownerName = snapshot.ownerName
        selectedCurrency = snapshot.selectedCurrency
        useShortTextCurrency = snapshot.useShortTextCurrency

        notificationsEnabled = snapshot.notificationsEnabled
        notificationSoundType = snapshot.notificationSoundType
        notificationSoundId = snapshot.notificationSoundId
        notificationSoundDisplayName = snapshot.notificationSoundDisplayName

        reminderDaysBefore = snapshot.reminderDaysBefore
        reminderHoursBefore = snapshot.reminderHoursBefore
        reminderMinutesBefore = snapshot.reminderMinutesBefore

        serviceTemplates = if (snapshot.serviceTemplates.isNotEmpty()) {
            snapshot.serviceTemplates
        } else {
            defaultServiceTemplates()
        }

        weeklyBlockedIntervals = snapshot.weeklyBlockedIntervals
        scheduleDateOverrides = snapshot.scheduleDateOverrides
        cloudSettingsUpdatedAtMillis = snapshot.updatedAtMillis
        persist()
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
        val systemZone = TimeZone.currentSystemDefault()
        val currentMoment = Clock.System.now()
        val oneMinuteAgoMoment = currentMoment.minus(1.minutes)

        val nowDateTime = currentMoment.toLocalDateTime(systemZone)
        val agoDateTime = oneMinuteAgoMoment.toLocalDateTime(systemZone)

        val currentPassword = generateDeveloperPasswordForDateTime(nowDateTime)
        val previousPassword = generateDeveloperPasswordForDateTime(agoDateTime)

        val cleanedInput = password.trim()
        return cleanedInput == currentPassword || cleanedInput == previousPassword
    }

    private fun generateDeveloperPasswordForDateTime(
        dateTime: kotlinx.datetime.LocalDateTime
    ): String {
        val day = dateTime.dayOfMonth
        val month = dateTime.monthNumber
        val year = dateTime.year
        val hour = dateTime.hour
        val minute = dateTime.minute

        val dayMonthSum = day + month
        val hourMinuteSum = hour + minute

        return "$dayMonthSum*$year*$hourMinuteSum"
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
        premiumOrderId = snapshot.premiumOrderId
        premiumLastVerifiedAtMillis = snapshot.premiumLastVerifiedAtMillis

        installId = snapshot.installId
        backendUserId = snapshot.backendUserId
        localProfileUserId = snapshot.localProfileUserId
        lastAuthProvider = snapshot.lastAuthProvider
        lastAuthEmail = snapshot.lastAuthEmail
        lastAuthDisplayName = snapshot.lastAuthDisplayName
        lastAuthenticatedAppOpenAtMillis = snapshot.lastAuthenticatedAppOpenAtMillis
        cachedAccessTier = snapshot.cachedAccessTier
        cachedTrialEndsAtMillis = snapshot.cachedTrialEndsAtMillis
        cachedHasPremium = snapshot.cachedHasPremium
        cachedSubscriptionState = snapshot.cachedSubscriptionState

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
        developerPremiumOverrideEnabled = snapshot.developerPremiumOverrideEnabled
        cloudSettingsUpdatedAtMillis = snapshot.cloudSettingsUpdatedAtMillis

        lastUpdateCheckAtMillis = snapshot.lastUpdateCheckAtMillis
        lastKnownUpdateAvailable = snapshot.lastKnownUpdateAvailable
        lastKnownLatestVersion = snapshot.lastKnownLatestVersion
        lastKnownLatestBuild = snapshot.lastKnownLatestBuild
        lastKnownStoreUrl = snapshot.lastKnownStoreUrl

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

            selectedCurrency = selectedCurrency,
            useShortTextCurrency = useShortTextCurrency,

            reminderDaysBefore = reminderDaysBefore,
            reminderHoursBefore = reminderHoursBefore,
            reminderMinutesBefore = reminderMinutesBefore,

            servicePhone = servicePhone,

            trialStartedAtMillis = trialStartedAtMillis,
            premiumUnlocked = premiumUnlocked,
            premiumProductId = premiumProductId,
            premiumPurchaseToken = premiumPurchaseToken,

            premiumSubscriptionState = premiumSubscriptionState,
            premiumSubscribedProductId = premiumSubscribedProductId,
            premiumSubscriptionToken = premiumSubscriptionToken,
            premiumSubscriptionStartMillis = premiumSubscriptionStartMillis,
            premiumSubscriptionExpiryMillis = premiumSubscriptionExpiryMillis,
            premiumSubscriptionAutoRenewing = premiumSubscriptionAutoRenewing,
            premiumOrderId = premiumOrderId,
            premiumLastVerifiedAtMillis = premiumLastVerifiedAtMillis,

            installId = installId,
            backendUserId = backendUserId,
            localProfileUserId = localProfileUserId,
            lastAuthProvider = lastAuthProvider,
            lastAuthEmail = lastAuthEmail,
            lastAuthDisplayName = lastAuthDisplayName,
            lastAuthenticatedAppOpenAtMillis = lastAuthenticatedAppOpenAtMillis,
            cachedAccessTier = cachedAccessTier,
            cachedTrialEndsAtMillis = cachedTrialEndsAtMillis,
            cachedHasPremium = cachedHasPremium,
            cachedSubscriptionState = cachedSubscriptionState,

            serviceTemplates = serviceTemplates,
            weeklyBlockedIntervals = weeklyBlockedIntervals,
            scheduleDateOverrides = scheduleDateOverrides,

            pinEnabled = pinEnabled,
            adminPinHash = adminPinHash,
            developerModeUnlocked = developerModeUnlocked,
            developerPremiumOverrideEnabled = developerPremiumOverrideEnabled,
            cloudSettingsUpdatedAtMillis = cloudSettingsUpdatedAtMillis,
            lastUpdateCheckAtMillis = lastUpdateCheckAtMillis,
            lastKnownUpdateAvailable = lastKnownUpdateAvailable,
            lastKnownLatestVersion = lastKnownLatestVersion,
            lastKnownLatestBuild = lastKnownLatestBuild,
            lastKnownStoreUrl = lastKnownStoreUrl,
        )

        runCatching {
            storage.write(settingsJson.encodeToString(snapshot))
        }
    }
}