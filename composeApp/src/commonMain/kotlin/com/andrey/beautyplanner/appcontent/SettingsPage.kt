package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.DataManager
import com.andrey.beautyplanner.Locales
import com.andrey.beautyplanner.notifications.NotificationSound
import com.andrey.beautyplanner.notifications.Notifications
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SettingsPage(
    onExport: () -> Unit,
    onImport: () -> Unit,
    onSetOrChangePin: () -> Unit,
    onRemovePin: () -> Unit,
    onClearDatabase: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit
) {
    val languages = AppSettings.languageCodes.keys.toList()
    val themeOptions = listOf(Locales.t("theme_light"), Locales.t("theme_dark"))
    val fontOptions = listOf(Locales.t("font_small"), Locales.t("font_medium"), Locales.t("font_large"))

    val fontScale = AppSettings.getFontScale()

    val onSurface = MaterialTheme.colors.onSurface
    val onBg = MaterialTheme.colors.onBackground

    val labelSpacingDp = 10.dp
    val sectionTitlePaddingBottomDp = 10.dp

    var daysSlider by remember { mutableStateOf(AppSettings.reminderDaysBefore.toFloat()) }
    var hoursSlider by remember { mutableStateOf(AppSettings.reminderHoursBefore.toFloat()) }

    val notificationsEnabled = AppSettings.notificationsEnabled
    val notificationSound = AppSettings.notificationSound
    val reminderDays = AppSettings.reminderDaysBefore
    val reminderHours = AppSettings.reminderHoursBefore

    var supportEditMode by remember { mutableStateOf(false) }
    var supportPhoneDraft by remember { mutableStateOf(AppSettings.servicePhone) }
    var showSupportEditConfirm by remember { mutableStateOf(false) }

    var showDisablePinConfirm by remember { mutableStateOf(false) }
    var pendingPinEnabledValue by remember { mutableStateOf(AppSettings.pinEnabled) }

    val dbOpsAllowed = AppSettings.pinEnabled && AppSettings.isPinSet()

    var nameEditMode by remember { mutableStateOf(false) }
    var userNameDraft by remember { mutableStateOf(AppSettings.ownerName) }

    LaunchedEffect(notificationsEnabled, notificationSound, reminderDays, reminderHours) {
        delay(600)
        val all = runCatching { DataManager.loadFromDatabase() }.getOrNull().orEmpty()
        val mins = AppSettings.reminderMinutesComputed()
        runCatching {
            if (AppSettings.notificationsEnabled && mins.isNotEmpty()) {
                Notifications.rescheduleAll(
                    appointments = all,
                    reminderMinutes = mins,
                    sound = AppSettings.notificationSound,
                    nowEpochMillis = Clock.System.now().toEpochMilliseconds()
                )
            } else {
                Notifications.cancelAll()
            }
        }
    }

    if (showSupportEditConfirm) {
        AlertDialog(
            onDismissRequest = { showSupportEditConfirm = false },
            title = {
                Text(
                    text = Locales.t("support_phone_edit_confirm_title"),
                    style = MaterialTheme.typography.subtitle2.copy(
                        fontSize = (14 * fontScale).sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = onSurface.copy(alpha = 0.75f)
                )
            },
            text = {
                Text(
                    Locales.t("support_phone_edit_confirm_text"),
                    color = onSurface.copy(alpha = 0.85f)
                )
            },
            buttons = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showSupportEditConfirm = false }) {
                        Text(Locales.t("cancel"), color = onSurface.copy(alpha = 0.85f))
                    }
                    Spacer(Modifier.width(15.dp))
                    Button(onClick = {
                        showSupportEditConfirm = false
                        supportEditMode = true
                        supportPhoneDraft = AppSettings.servicePhone
                    }) {
                        Text(Locales.t("support_phone_edit_confirm_yes"))
                    }
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showDisablePinConfirm) {
        AlertDialog(
            onDismissRequest = {
                showDisablePinConfirm = false
                pendingPinEnabledValue = AppSettings.pinEnabled
            },
            title = { Text(Locales.t("security_section"), color = onSurface) },
            text = {
                Text(
                    "При отключении PIN-кода будут ограничены операции с базой данных:\n" +
                            "• импорт\n" +
                            "• экспорт\n" +
                            "• очистка базы\n\n" +
                            "Продолжить?",
                    color = onSurface.copy(alpha = 0.85f)
                )
            },
            confirmButton = {
                Button(onClick = {
                    AppSettings.pinEnabled = false
                    AppSettings.persist()
                    pendingPinEnabledValue = false
                    showDisablePinConfirm = false
                }) { Text(Locales.t("confirm")) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDisablePinConfirm = false
                    pendingPinEnabledValue = true
                }) { Text(Locales.t("cancel"), color = onSurface.copy(alpha = 0.85f)) }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    val fieldColors = TextFieldDefaults.outlinedTextFieldColors(
        textColor = onSurface,
        cursorColor = MaterialTheme.colors.primary,
        focusedBorderColor = MaterialTheme.colors.primary,
        unfocusedBorderColor = onSurface.copy(alpha = 0.25f),
        focusedLabelColor = MaterialTheme.colors.primary,
        unfocusedLabelColor = onSurface.copy(alpha = 0.65f),
        trailingIconColor = onSurface.copy(alpha = 0.75f)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = Locales.t("nav_settings"),
            fontSize = (22 * fontScale).sp,
            fontWeight = FontWeight.Bold,
            color = onBg
        )

        SettingsDropdown(
            label = Locales.t("language_label"),
            selected = AppSettings.selectedLanguage,
            items = languages,
            labelSpacing = labelSpacingDp,
            fieldColors = fieldColors,
            onSelect = { newValue ->
                if (AppSettings.selectedLanguage != newValue) {
                    AppSettings.selectedLanguage = newValue
                    val code = AppSettings.languageCodes[newValue] ?: "en"
                    Locales.currentLanguage = code
                    AppSettings.persist()
                }
            }
        )

        SettingsDropdown(
            label = Locales.t("theme_label"),
            selected = if (AppSettings.isDarkMode) Locales.t("theme_dark") else Locales.t("theme_light"),
            items = themeOptions,
            labelSpacing = labelSpacingDp,
            fieldColors = fieldColors,
            onSelect = { newValue ->
                AppSettings.isDarkMode = (newValue == Locales.t("theme_dark"))
                AppSettings.persist()
            }
        )

        SettingsDropdown(
            label = Locales.t("font_size_label"),
            selected = when (AppSettings.fontSizeMode) {
                "Мелкий" -> Locales.t("font_small")
                "Крупный" -> Locales.t("font_large")
                else -> Locales.t("font_medium")
            },
            items = fontOptions,
            labelSpacing = labelSpacingDp,
            fieldColors = fieldColors,
            onSelect = { newValue ->
                AppSettings.fontSizeMode = when (newValue) {
                    Locales.t("font_small") -> "Мелкий"
                    Locales.t("font_large") -> "Крупный"
                    else -> "Средний"
                }
                AppSettings.persist()
            }
        )

        Spacer(modifier = Modifier.height(10.dp))
        Divider()

        Column {
            Text(
                text = Locales.t("notifications_section"),
                fontSize = (16 * fontScale).sp,
                fontWeight = FontWeight.SemiBold,
                color = onSurface.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = sectionTitlePaddingBottomDp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Locales.t("notifications_enabled"),
                    fontSize = (16 * fontScale).sp,
                    color = onSurface
                )
                Switch(
                    checked = AppSettings.notificationsEnabled,
                    onCheckedChange = {
                        AppSettings.notificationsEnabled = it
                        AppSettings.persist()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colors.primary,
                        checkedTrackColor = MaterialTheme.colors.primary.copy(alpha = 0.35f),
                        uncheckedThumbColor = onSurface.copy(alpha = 0.45f),
                        uncheckedTrackColor = onSurface.copy(alpha = 0.20f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val soundItems = listOf(
                Locales.t("notif_sound_default") to NotificationSound.DEFAULT,
                Locales.t("notif_sound_silent") to NotificationSound.SILENT
            )

            SettingsDropdown(
                label = Locales.t("notif_sound_label"),
                selected = soundItems.firstOrNull { it.second == AppSettings.notificationSound }?.first
                    ?: Locales.t("notif_sound_default"),
                items = soundItems.map { it.first },
                labelSpacing = labelSpacingDp,
                fieldColors = fieldColors,
                onSelect = { selected ->
                    val s = soundItems.firstOrNull { it.first == selected }?.second ?: NotificationSound.DEFAULT
                    AppSettings.notificationSound = s
                    AppSettings.persist()
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = Locales.t("reminders_when"),
                fontSize = (14 * fontScale).sp,
                fontWeight = FontWeight.SemiBold,
                color = onSurface.copy(alpha = 0.75f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "${Locales.t("remind_days")}: ${Locales.daysCount(AppSettings.reminderDaysBefore)}",
                fontSize = (15 * fontScale).sp,
                fontWeight = FontWeight.SemiBold,
                color = onSurface
            )

            Slider(
                value = daysSlider,
                onValueChange = { daysSlider = it },
                onValueChangeFinished = {
                    AppSettings.reminderDaysBefore = daysSlider.roundToInt().coerceIn(0, 3)
                    AppSettings.persist()
                },
                valueRange = 0f..3f,
                steps = 0,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colors.primary,
                    activeTrackColor = MaterialTheme.colors.primary.copy(alpha = 0.85f),
                    inactiveTrackColor = onSurface.copy(alpha = 0.20f)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "${Locales.t("remind_hours")}: ${Locales.hoursCount(AppSettings.reminderHoursBefore)}",
                fontSize = (15 * fontScale).sp,
                fontWeight = FontWeight.SemiBold,
                color = onSurface
            )

            Slider(
                value = hoursSlider,
                onValueChange = { hoursSlider = it },
                onValueChangeFinished = {
                    AppSettings.reminderHoursBefore = hoursSlider.roundToInt().coerceIn(0, 12)
                    AppSettings.persist()
                },
                valueRange = 0f..12f,
                steps = 0,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colors.primary,
                    activeTrackColor = MaterialTheme.colors.primary.copy(alpha = 0.85f),
                    inactiveTrackColor = onSurface.copy(alpha = 0.20f)
                )
            )

            val totalMinutes = AppSettings.reminderDaysBefore * 24 * 60 + AppSettings.reminderHoursBefore * 60
            val summary = if (totalMinutes <= 0) Locales.t("remind_off")
            else "${Locales.daysCount(AppSettings.reminderDaysBefore)} • ${Locales.hoursCount(AppSettings.reminderHoursBefore)}"

            Text(
                text = "${Locales.t("remind_summary")}: $summary",
                fontSize = (13 * fontScale).sp,
                color = onSurface.copy(alpha = 0.60f)
            )
        }

        Divider()

        Column {
            Text(
                text = Locales.t("support_section"),
                fontSize = (16 * fontScale).sp,
                fontWeight = FontWeight.SemiBold,
                color = onSurface.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = sectionTitlePaddingBottomDp)
            )

            OutlinedTextField(
                value = if (supportEditMode) supportPhoneDraft else AppSettings.servicePhone,
                onValueChange = { if (supportEditMode) supportPhoneDraft = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = supportEditMode,
                label = { Text(Locales.t("support_phone_label"), color = onSurface.copy(alpha = 0.65f)) },
                colors = fieldColors
            )

            Text(
                text = Locales.t("support_phone_hint"),
                fontSize = (12 * fontScale).sp,
                color = onSurface.copy(alpha = 0.60f)
            )

            Spacer(Modifier.height(10.dp))

            if (!supportEditMode) {
                OutlinedButton(
                    onClick = { showSupportEditConfirm = true },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(Locales.t("support_phone_edit"), color = onSurface)
                }
            } else {
                Button(
                    onClick = {
                        AppSettings.servicePhone = supportPhoneDraft.trim()
                        AppSettings.persist()
                        supportEditMode = false
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(Locales.t("support_phone_save"))
                }
            }
        }

        Divider()

        Column {
            Text(
                text = Locales.t("user_name_label"),
                fontSize = (16 * fontScale).sp,
                fontWeight = FontWeight.SemiBold,
                color = onSurface.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = sectionTitlePaddingBottomDp)
            )

            OutlinedTextField(
                value = if (nameEditMode) userNameDraft else AppSettings.ownerName,
                onValueChange = { if (nameEditMode) userNameDraft = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = nameEditMode,
                label = { Text(Locales.t("user_name_hint"), color = onSurface.copy(alpha = 0.65f)) },
                colors = fieldColors
            )

            Spacer(Modifier.height(10.dp))

            if (!nameEditMode) {
                OutlinedButton(
                    onClick = {
                        nameEditMode = true
                        userNameDraft = AppSettings.ownerName
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(Locales.t("support_phone_edit"), color = onSurface)
                }
            } else {
                Button(
                    onClick = {
                        AppSettings.ownerName = userNameDraft.trim()
                        AppSettings.persist()
                        nameEditMode = false
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(Locales.t("support_phone_save"))
                }
            }
        }

        Divider()

        Column {
            Text(
                text = Locales.t("backup_section"),
                fontSize = (16 * fontScale).sp,
                fontWeight = FontWeight.SemiBold,
                color = onSurface.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = sectionTitlePaddingBottomDp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onExport,
                    enabled = dbOpsAllowed,
                    modifier = Modifier.weight(0.45f).height(38.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.elevation(0.dp, 0.dp)
                ) {
                    Text(text = Locales.t("export_db"), fontSize = (14 * fontScale).sp)
                }

                Spacer(modifier = Modifier.weight(0.1f))

                OutlinedButton(
                    onClick = onImport,
                    enabled = dbOpsAllowed,
                    modifier = Modifier.weight(0.45f).height(38.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = Locales.t("import_db"), fontSize = (14 * fontScale).sp, color = onSurface)
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onClearDatabase,
                enabled = dbOpsAllowed,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
            ) {
                Text(Locales.t("clear_db"), color = Color.Red, fontWeight = FontWeight.SemiBold)
            }

            if (!dbOpsAllowed) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Чтобы использовать импорт/экспорт/очистку базы, включите PIN и установите его.",
                    color = onSurface.copy(alpha = 0.60f),
                    fontSize = (12 * fontScale).sp
                )
            }
        }

        Divider()

        Column {
            Text(
                text = Locales.t("security_section"),
                fontSize = (16 * fontScale).sp,
                fontWeight = FontWeight.SemiBold,
                color = onSurface.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = sectionTitlePaddingBottomDp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Locales.t("pin_enabled"),
                    fontSize = (16 * fontScale).sp,
                    color = onSurface
                )

                Switch(
                    checked = pendingPinEnabledValue,
                    onCheckedChange = { newValue ->
                        if (newValue) {
                            AppSettings.pinEnabled = true
                            AppSettings.persist()
                            pendingPinEnabledValue = true
                        } else {
                            pendingPinEnabledValue = false
                            showDisablePinConfirm = true
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colors.primary,
                        checkedTrackColor = MaterialTheme.colors.primary.copy(alpha = 0.35f),
                        uncheckedThumbColor = onSurface.copy(alpha = 0.45f),
                        uncheckedTrackColor = onSurface.copy(alpha = 0.20f)
                    )
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val pinBtnText =
                    if (AppSettings.isPinSet()) Locales.t("pin_change") else Locales.t("pin_set")

                Button(
                    onClick = onSetOrChangePin,
                    modifier = Modifier.weight(0.45f).height(38.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = true,
                    elevation = ButtonDefaults.elevation(0.dp, 0.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = pinBtnText,
                        fontSize = (13 * fontScale).sp,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.weight(0.1f))

                OutlinedButton(
                    onClick = onRemovePin,
                    modifier = Modifier.weight(0.45f).height(38.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = AppSettings.isPinSet(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = Locales.t("pin_remove"),
                        color = onSurface,
                        fontSize = (13 * fontScale).sp,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = Locales.t("privacy_policy"),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 4.dp)
                .clickable { onOpenPrivacyPolicy() },
            color = onSurface.copy(alpha = 0.60f),
            fontSize = (11 * fontScale).sp,
            textDecoration = TextDecoration.Underline
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SettingsDropdown(
    label: String,
    selected: String,
    items: List<String>,
    onSelect: (String) -> Unit,
    labelSpacing: Dp = 10.dp,
    fieldColors: TextFieldColors = TextFieldDefaults.outlinedTextFieldColors()
) {
    var expanded by remember { mutableStateOf(false) }
    val fontScale = AppSettings.getFontScale()

    val onSurface = MaterialTheme.colors.onSurface

    Column {
        Text(
            text = label,
            fontSize = (14 * fontScale).sp,
            fontWeight = FontWeight.SemiBold,
            color = onSurface.copy(alpha = 0.85f)
        )

        Spacer(modifier = Modifier.height(labelSpacing))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(fontSize = (16 * fontScale).sp, color = onSurface),
                colors = fieldColors
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                items.forEach { item ->
                    DropdownMenuItem(onClick = {
                        onSelect(item)
                        expanded = false
                    }) {
                        Text(text = item, fontSize = (16 * fontScale).sp, color = onSurface)
                    }
                }
            }
        }
    }
}