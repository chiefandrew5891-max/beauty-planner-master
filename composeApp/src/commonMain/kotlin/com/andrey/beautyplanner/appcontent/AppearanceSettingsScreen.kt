package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.CurrencyCatalog
import com.andrey.beautyplanner.CurrencyNames
import com.andrey.beautyplanner.Locales
import com.andrey.beautyplanner.appcontent.approot.AppRootState
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun AppearanceSettingsScreen(state: AppRootState) {
    val scope = rememberCoroutineScope()

    val languages = AppSettings.languageCodes.keys.toList()
    val themeItems = listOf(
        "light" to Locales.t("theme_light"),
        "dark" to Locales.t("theme_dark")
    )
    val fontItems = listOf(
        "small" to Locales.t("font_small"),
        "medium" to Locales.t("font_medium"),
        "large" to Locales.t("font_large")
    )

    val currentLangCode = Locales.currentLanguage
    val currencyInfos = CurrencyCatalog.all
    val currencyLabels = currencyInfos.map {
        CurrencyNames.getDisplayLabel(it.code, currentLangCode)
    }

    val fontScale = state.fontScale
    val onSurface = MaterialTheme.colors.onSurface
    val onBg = MaterialTheme.colors.onBackground

    var selectedLanguageDraft by remember { mutableStateOf(AppSettings.selectedLanguage) }
    var selectedThemeDraftKey by remember {
        mutableStateOf(if (AppSettings.isDarkMode) "dark" else "light")
    }
    var selectedFontDraftKey by remember {
        mutableStateOf(
            when (AppSettings.fontSizeMode) {
                "small" -> "small"
                "large" -> "large"
                else -> "medium"
            }
        )
    }
    var selectedCurrencyCodeDraft by remember {
        mutableStateOf(AppSettings.selectedCurrency)
    }

    var userNameDraft by remember { mutableStateOf(AppSettings.ownerName) }
    var useShortTextCurrencyDraft by remember { mutableStateOf(AppSettings.useShortTextCurrency) }

    LaunchedEffect(selectedFontDraftKey) {
        val previewScale = when (selectedFontDraftKey) {
            "small" -> 0.80f
            "large" -> 1.22f
            else -> 1.10f
        }
        state.fontScale = previewScale
        AppSettings.previewFontScaleOverride = previewScale
    }

    DisposableEffect(Unit) {
        onDispose {
            AppSettings.previewFontScaleOverride = null
            state.resetLivePreviews()
        }
    }

    val currentThemeKey = if (AppSettings.isDarkMode) "dark" else "light"
    val currentFontKey = when (AppSettings.fontSizeMode) {
        "small" -> "small"
        "large" -> "large"
        else -> "medium"
    }

    val selectedThemeLabel = themeItems.first { it.first == selectedThemeDraftKey }.second
    val selectedFontLabel = fontItems.first { it.first == selectedFontDraftKey }.second
    val selectedCurrencyLabel =
        CurrencyNames.getDisplayLabel(selectedCurrencyCodeDraft, currentLangCode)

    val hasChanges =
        selectedLanguageDraft != AppSettings.selectedLanguage ||
                selectedThemeDraftKey != currentThemeKey ||
                selectedFontDraftKey != currentFontKey ||
                selectedCurrencyCodeDraft != AppSettings.selectedCurrency ||
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

            SelectionDialogField(
                label = Locales.t("language_label"),
                selected = selectedLanguageDraft,
                items = languages,
                onSelect = { newValue ->
                    selectedLanguageDraft = newValue
                }
            )

            SelectionDialogField(
                label = Locales.t("theme_label"),
                selected = selectedThemeLabel,
                items = themeItems.map { it.second },
                onSelect = { newValue ->
                    val selectedKey = themeItems.firstOrNull { it.second == newValue }?.first ?: "light"
                    selectedThemeDraftKey = selectedKey
                    state.currentLiveDarkMode = (selectedKey == "dark")
                }
            )

            SelectionDialogField(
                label = Locales.t("font_size_label"),
                selected = selectedFontLabel,
                items = fontItems.map { it.second },
                onSelect = { newValue ->
                    val selectedKey = fontItems.firstOrNull { it.second == newValue }?.first ?: "medium"
                    selectedFontDraftKey = selectedKey

                    val previewScale = when (selectedKey) {
                        "small" -> 0.80f
                        "large" -> 1.22f
                        else -> 1.10f
                    }
                    state.fontScale = previewScale
                    AppSettings.previewFontScaleOverride = previewScale
                }
            )

            SelectionDialogField(
                label = Locales.t("currency_label"),
                selected = selectedCurrencyLabel,
                items = currencyLabels,
                onSelect = { newValue ->
                    val index = currencyLabels.indexOf(newValue)
                    if (index >= 0) {
                        selectedCurrencyCodeDraft = currencyInfos[index].code
                    }
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Locales.t("currency_text_format_label"),
                    fontSize = (16 * fontScale).sp,
                    color = onSurface
                )
                AppSwitch(
                    checked = useShortTextCurrencyDraft,
                    onCheckedChange = { useShortTextCurrencyDraft = it }
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
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    placeholder = {
                        Text(
                            text = Locales.t("user_name_placeholder"),
                            color = onSurface.copy(alpha = 0.50f)
                        )
                    },
                    textStyle = TextStyle(
                        fontFamily = appFontFamily(),
                        fontSize = (16 * fontScale).sp,
                        color = onSurface
                    ),
                    colors = androidx.compose.material.TextFieldDefaults.outlinedTextFieldColors(
                        textColor = onSurface,
                        focusedBorderColor = MaterialTheme.colors.primary,
                        unfocusedBorderColor = onSurface.copy(alpha = 0.28f),
                        focusedLabelColor = MaterialTheme.colors.primary,
                        unfocusedLabelColor = onSurface.copy(alpha = 0.68f),
                        cursorColor = MaterialTheme.colors.primary,
                        backgroundColor = MaterialTheme.colors.surface,
                        placeholderColor = onSurface.copy(alpha = 0.50f)
                    )
                )
            }

            Spacer(Modifier.height(8.dp))

            PrimaryActionButton(
                text = Locales.t("save"),
                onClick = {
                    scope.launch {
                        state.showGlobalLoading(Locales.t("loading"))
                        try {
                            AppSettings.isDarkMode = (selectedThemeDraftKey == "dark")
                            AppSettings.fontSizeMode = selectedFontDraftKey
                            AppSettings.ownerName = userNameDraft.trim()
                            AppSettings.saveCurrencySynchronously(
                                selectedCurrencyCodeDraft,
                                useShortTextCurrencyDraft
                            )

                            AppSettings.selectedLanguage = selectedLanguageDraft
                            val code = AppSettings.languageCodes[selectedLanguageDraft] ?: "en"

                            AppSettings.previewFontScaleOverride = null
                            AppSettings.persist()

                            Locales.onLanguageChanged(code)
                            state.refreshBillingLocalization()

                            state.currentLiveDarkMode = AppSettings.isDarkMode
                            state.fontScale = AppSettings.getFontScale()

                            selectedLanguageDraft = AppSettings.selectedLanguage
                        } finally {
                            state.hideGlobalLoading()
                        }
                    }
                },
                enabled = hasChanges
            )
        }
    }
}

@Composable
fun SelectionDialogField(
    label: String,
    selected: String,
    items: List<String>,
    onSelect: (String) -> Unit,
    labelSpacing: Dp = 10.dp
) {
    var showDialog by remember { mutableStateOf(false) }
    val fontScale = AppSettings.getFontScale()
    val onSurface = MaterialTheme.colors.onSurface
    val interactionSource = remember { MutableInteractionSource() }

    Column {
        Text(
            text = label,
            fontSize = (14 * fontScale).sp,
            fontWeight = FontWeight.SemiBold,
            color = onSurface.copy(alpha = 0.85f)
        )

        Spacer(modifier = Modifier.height(labelSpacing))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current
                ) {
                    showDialog = true
                },
            shape = RoundedCornerShape(12.dp),
            elevation = 0.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                onSurface.copy(alpha = 0.25f)
            ),
            backgroundColor = MaterialTheme.colors.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selected,
                    fontSize = (16 * fontScale).sp,
                    color = onSurface,
                    maxLines = 1
                )
                Text(
                    text = "▼",
                    fontSize = (12 * fontScale).sp,
                    color = onSurface.copy(alpha = 0.65f)
                )
            }
        }
    }

    if (showDialog) {
        SelectionDialog(
            title = label,
            items = items,
            onDismiss = { showDialog = false },
            onSelect = {
                onSelect(it)
                showDialog = false
            }
        )
    }
}

@Composable
fun SelectionDialog(
    title: String,
    items: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val fontScale = AppSettings.getFontScale()
    val onSurface = MaterialTheme.colors.onSurface

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            backgroundColor = MaterialTheme.colors.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = title,
                    fontSize = (18 * fontScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurface
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                ) {
                    items(items.size) { index ->
                        val item = items[index]

                        TextButton(
                            onClick = { onSelect(item) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = item,
                                fontSize = (16 * fontScale).sp,
                                color = onSurface,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(Locales.t("cancel"))
                }
            }
        }
    }
}