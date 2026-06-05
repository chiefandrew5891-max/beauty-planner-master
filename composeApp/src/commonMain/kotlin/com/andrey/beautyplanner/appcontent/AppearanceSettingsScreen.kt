package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role.Companion.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Locales

@Composable
fun AppearanceSettingsScreen(state: com.andrey.beautyplanner.appcontent.approot.AppRootState) {
    val languages = AppSettings.languageCodes.keys.toList()
    val themeOptions = listOf(Locales.t("theme_light"), Locales.t("theme_dark"))
    val fontOptions = listOf(
        Locales.t("font_small"),
        Locales.t("font_medium"),
        Locales.t("font_large")
    )

    val fontScale = state.fontScale
    val onSurface = MaterialTheme.colors.onSurface
    val onBg = MaterialTheme.colors.onBackground

    var selectedLanguageDraft by remember { mutableStateOf(AppSettings.selectedLanguage) }
    var selectedThemeDraft by remember {
        mutableStateOf(
            if (AppSettings.isDarkMode) {
                Locales.t("theme_dark")
            } else {
                Locales.t("theme_light")
            }
        )
    }
    var selectedFontDraft by remember {
        mutableStateOf(
            when (AppSettings.fontSizeMode) {
                "Мелкий" -> Locales.t("font_small")
                "Крупный" -> Locales.t("font_large")
                else -> Locales.t("font_medium")
            }
        )
    }
    var selectedCurrencyDraft by remember {
        mutableStateOf(
            when (AppSettings.selectedCurrency) {
                "USD" -> "USD ($)"
                "RUB" -> "RUB (₽)"
                "UAH" -> "UAH (₴)"
                else -> "EUR (€)"
            }
        )
    }
    var userNameDraft by remember { mutableStateOf(AppSettings.ownerName) }
    var useShortTextCurrencyDraft by remember { mutableStateOf(AppSettings.useShortTextCurrency) }

    // Если мастер выходит назад или переключает вкладку не сохранившись — сбрасываем живое превью на исходные
    DisposableEffect(Unit) {
        onDispose {
            state.resetLivePreviews()
        }
    }

    val currentThemeValue =
        if (AppSettings.isDarkMode) Locales.t("theme_dark") else Locales.t("theme_light")

    val currentFontValue = when (AppSettings.fontSizeMode) {
        "Мелкий" -> Locales.t("font_small")
        "Крупный" -> Locales.t("font_large")
        else -> Locales.t("font_medium")
    }

    val currentCurrencyValue = when (AppSettings.selectedCurrency) {
        "USD" -> "USD ($)"
        "RUB" -> "RUB (₽)"
        "UAH" -> "UAH (₴)"
        else -> "EUR (€)"
    }

    val hasChanges =
        selectedLanguageDraft != AppSettings.selectedLanguage ||
                selectedThemeDraft != currentThemeValue ||
                selectedFontDraft != currentFontValue ||
                selectedCurrencyDraft != currentCurrencyValue ||
                useShortTextCurrencyDraft != AppSettings.useShortTextCurrency ||
                userNameDraft.trim() != AppSettings.ownerName.trim()

    CenteredNarrowContentContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = Locales.t("appearance_settings"),
                fontSize = (22 * fontScale).sp,
                fontWeight = FontWeight.Bold,
                color = onBg
            )

            Text(
                text = Locales.t("appearance_settings_hint"),
                fontSize = (14 * fontScale).sp,
                color = onBg.copy(alpha = 0.7f)
            )

            androidx.compose.material.Divider()

            SettingsDropdown(
                label = Locales.t("language_label"),
                selected = selectedLanguageDraft,
                items = languages,
                onSelect = { newValue ->
                    selectedLanguageDraft = newValue
                }
            )

            SettingsDropdown(
                label = Locales.t("theme_label"),
                selected = selectedThemeDraft,
                items = themeOptions,
                onSelect = { newValue ->
                    selectedThemeDraft = newValue
                    // Мгновенно перекрашиваем корень всего приложения для превью
                    state.currentLiveDarkMode = (newValue == Locales.t("theme_dark"))
                }
            )

            SettingsDropdown(
                label = Locales.t("font_size_label"),
                selected = selectedFontDraft,
                items = fontOptions,
                onSelect = { newValue ->
                    selectedFontDraft = newValue
                    // Мгновенно масштабируем шрифты всего приложения для превью
                    state.fontScale = when (newValue) {
                        Locales.t("font_small") -> 0.85f
                        Locales.t("font_large") -> 1.2f
                        else -> 1.0f
                    }
                }
            )

            SettingsDropdown(
                label = Locales.t("currency_label"),
                selected = selectedCurrencyDraft,
                items = listOf("EUR (€)", "USD ($)", "RUB (₽)", "UAH (₴)"),
                onSelect = { newValue ->
                    selectedCurrencyDraft = newValue
                }
            )
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = when (Locales.currentLanguage) {
                        "uk" -> "Текстовий формат валюти (USD/UAH)"
                        "it" -> "Formato testo valuta (USD/UAH)"
                        else -> "Текстовый формат валюты (USD/UAH)"
                    },
                    fontSize = (16 * fontScale).sp,
                    color = onSurface
                )
                androidx.compose.material.Switch(
                    checked = useShortTextCurrencyDraft,
                    onCheckedChange = { useShortTextCurrencyDraft = it },
                    colors = androidx.compose.material.SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colors.primary,
                        checkedTrackColor = MaterialTheme.colors.primary.copy(alpha = 0.5f)
                    )
                )
            }

            androidx.compose.material.Divider()

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = Locales.t("user_name_label"),
                    fontSize = (16 * fontScale).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = onSurface.copy(alpha = 0.85f)
                )

                OutlinedTextField(
                    value = userNameDraft,
                    onValueChange = { userNameDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = {
                        Text(
                            Locales.t("user_name_hint"),
                            color = onSurface.copy(alpha = 0.65f)
                        )
                    },
                    textStyle = TextStyle(
                        fontSize = (16 * fontScale).sp,
                        color = onSurface
                    )
                )
            }

            Spacer(Modifier.padding(top = 4.dp))

            PrimaryActionButton(
                text = Locales.t("save"),
                onClick = {
                    // Теперь строки условий посимвольно совпадают со значениями из списка items
                    val targetCurrencyCode = when (selectedCurrencyDraft) {
                        "USD ($)" -> "USD"
                        "RUB (₽)" -> "RUB"
                        "UAH (₴)" -> "UAH" // <-- Теперь строка совпадает со списком идеально!
                        else -> "EUR"
                    }

                    // Сохраняем остальные параметры оформления
                    AppSettings.isDarkMode = (selectedThemeDraft == Locales.t("theme_dark"))
                    AppSettings.fontSizeMode = when (selectedFontDraft) {
                        Locales.t("font_small") -> "Мелкий"
                        Locales.t("font_large") -> "Крупный"
                        else -> "Средний"
                    }
                    AppSettings.ownerName = userNameDraft.trim()

                    // Синхронно пишем в settings.json и код валюты, и состояние чекбокса альтернативного текста
                    AppSettings.saveCurrencySynchronously(targetCurrencyCode, useShortTextCurrencyDraft)

                    // В последнюю очередь меняем локаль
                    AppSettings.selectedLanguage = selectedLanguageDraft
                    val code = AppSettings.languageCodes[selectedLanguageDraft] ?: "en"
                    Locales.currentLanguage = code

                    AppSettings.persist()
                },
                enabled = hasChanges
            )
        }
    }
}