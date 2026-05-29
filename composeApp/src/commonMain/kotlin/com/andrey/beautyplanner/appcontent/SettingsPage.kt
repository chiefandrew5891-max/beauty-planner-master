package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AccessState
import com.andrey.beautyplanner.AccessTier
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Locales

@Composable
fun SettingsPage(
    accessState: AccessState,
    onSetOrChangePin: () -> Unit,
    onRemovePin: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenPremiumScreen: () -> Unit,
    onOpenServiceTemplates: () -> Unit,
    onOpenWorkSchedule: () -> Unit,
    onOpenAppearanceSettings: () -> Unit,
    onOpenBackupSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenDeveloperAccess: () -> Unit
) {
    val fontScale = AppSettings.getFontScale()
    val onSurface = MaterialTheme.colors.onSurface
    val onBg = MaterialTheme.colors.onBackground
    val sectionTitlePaddingBottomDp = 10.dp

    var supportEditMode by remember { mutableStateOf(false) }
    var supportPhoneDraft by remember { mutableStateOf(AppSettings.servicePhone) }
    var showSupportEditConfirm by remember { mutableStateOf(false) }

    var showDisablePinConfirm by remember { mutableStateOf(false) }
    var pendingPinEnabledValue by remember { mutableStateOf(AppSettings.pinEnabled) }

    var securityTapCount by remember { mutableStateOf(0) }
    var showDeveloperPasswordDialog by remember { mutableStateOf(false) }
    var developerPasswordInput by remember { mutableStateOf("") }
    var developerPasswordError by remember { mutableStateOf(false) }

    val dbOpsAllowed = AppSettings.pinEnabled && AppSettings.isPinSet()

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
                    Button(
                        onClick = {
                            showSupportEditConfirm = false
                            supportEditMode = true
                            supportPhoneDraft = AppSettings.servicePhone
                        }
                    ) {
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
                    Locales.t("pin_disable_warning"),
                    color = onSurface.copy(alpha = 0.85f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        AppSettings.pinEnabled = false
                        AppSettings.persist()
                        pendingPinEnabledValue = false
                        showDisablePinConfirm = false
                    }
                ) {
                    Text(Locales.t("confirm"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDisablePinConfirm = false
                        pendingPinEnabledValue = true
                    }
                ) {
                    Text(Locales.t("cancel"), color = onSurface.copy(alpha = 0.85f))
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showDeveloperPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeveloperPasswordDialog = false
                developerPasswordInput = ""
                developerPasswordError = false
            },
            title = {
                Text(Locales.t("developer_password_title"), color = onSurface)
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = developerPasswordInput,
                        onValueChange = {
                            developerPasswordInput = it
                            developerPasswordError = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(Locales.t("developer_password_hint")) },
                        isError = developerPasswordError
                    )

                    if (developerPasswordError) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = Locales.t("developer_password_invalid"),
                            color = MaterialTheme.colors.error,
                            fontSize = (12 * fontScale).sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (AppSettings.verifyDeveloperPassword(developerPasswordInput)) {
                            AppSettings.unlockDeveloperMode()
                            securityTapCount = 0
                            showDeveloperPasswordDialog = false
                            developerPasswordInput = ""
                            developerPasswordError = false
                        } else {
                            developerPasswordError = true
                        }
                    }
                ) {
                    Text(Locales.t("developer_unlock"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeveloperPasswordDialog = false
                        developerPasswordInput = ""
                        developerPasswordError = false
                    }
                ) {
                    Text(Locales.t("cancel"), color = onSurface.copy(alpha = 0.85f))
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    val fieldColors: TextFieldColors = TextFieldDefaults.outlinedTextFieldColors(
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

        Column {
            Text(
                text = Locales.t("appearance_settings"),
                fontSize = (16 * fontScale).sp,
                fontWeight = FontWeight.SemiBold,
                color = onSurface.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = sectionTitlePaddingBottomDp)
            )
            Text(
                text = Locales.t("appearance_settings_hint"),
                fontSize = (13 * fontScale).sp,
                color = onSurface.copy(alpha = 0.70f),
                lineHeight = (19 * fontScale).sp
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onOpenAppearanceSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(Locales.t("appearance_settings"))
            }
        }

        Divider()

        Column {
            Text(
                text = Locales.t("premium_section_title"),
                fontSize = (16 * fontScale).sp,
                fontWeight = FontWeight.SemiBold,
                color = onSurface.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = sectionTitlePaddingBottomDp)
            )

            val premiumStatusText = when (accessState.tier) {
                AccessTier.TRIAL -> Locales.t("premium_status_trial")
                AccessTier.FREE_LIMITED -> Locales.t("premium_status_free")
                AccessTier.PREMIUM -> Locales.t("premium_status_premium")
            }

            val premiumHintText = when (accessState.tier) {
                AccessTier.TRIAL -> Locales.t("premium_trial_active_hint")
                AccessTier.FREE_LIMITED -> Locales.t("premium_free_limited_hint")
                AccessTier.PREMIUM -> Locales.t("premium_active_hint")
            }

            Text(
                text = "${Locales.t("premium_status_label")}: $premiumStatusText",
                fontSize = (16 * fontScale).sp,
                fontWeight = FontWeight.Medium,
                color = onSurface
            )

            if (accessState.tier == AccessTier.TRIAL) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${Locales.t("premium_trial_days_left")}: ${accessState.trialDaysLeft}",
                    fontSize = (14 * fontScale).sp,
                    color = onSurface.copy(alpha = 0.80f)
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = premiumHintText,
                fontSize = (13 * fontScale).sp,
                color = onSurface.copy(alpha = 0.70f),
                lineHeight = (19 * fontScale).sp
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onOpenPremiumScreen,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(Locales.t("premium_open_screen_btn"))
            }
        }

        Divider()

        Column {
            Text(
                text = Locales.t("my_services"),
                fontSize = (16 * fontScale).sp,
                fontWeight = FontWeight.SemiBold,
                color = onSurface.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = sectionTitlePaddingBottomDp)
            )
            Text(
                text = Locales.t("my_services_hint"),
                fontSize = (13 * fontScale).sp,
                color = onSurface.copy(alpha = 0.70f),
                lineHeight = (19 * fontScale).sp
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onOpenServiceTemplates,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(Locales.t("my_services"))
            }
        }

        Divider()

        Column {
            Text(
                text = Locales.t("work_schedule"),
                fontSize = (16 * fontScale).sp,
                fontWeight = FontWeight.SemiBold,
                color = onSurface.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = sectionTitlePaddingBottomDp)
            )
            Text(
                text = Locales.t("work_schedule_hint"),
                fontSize = (13 * fontScale).sp,
                color = onSurface.copy(alpha = 0.70f),
                lineHeight = (19 * fontScale).sp
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onOpenWorkSchedule,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(Locales.t("work_schedule"))
            }
        }

        Divider()

        Column {
            Text(
                text = Locales.t("notifications_section"),
                fontSize = (16 * fontScale).sp,
                fontWeight = FontWeight.SemiBold,
                color = onSurface.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = sectionTitlePaddingBottomDp)
            )
            Text(
                text = Locales.t("notifications_settings_hint"),
                fontSize = (13 * fontScale).sp,
                color = onSurface.copy(alpha = 0.70f),
                lineHeight = (19 * fontScale).sp
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onOpenNotificationSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(Locales.t("notifications_settings_open"))
            }
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
                label = {
                    Text(
                        Locales.t("support_phone_label"),
                        color = onSurface.copy(alpha = 0.65f)
                    )
                },
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(Locales.t("support_phone_save"))
                }
            }
        }

        Divider()

        Column {
            Text(
                text = Locales.t("backup_settings_title"),
                fontSize = (16 * fontScale).sp,
                fontWeight = FontWeight.SemiBold,
                color = onSurface.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = sectionTitlePaddingBottomDp)
            )

            Text(
                text = Locales.t("backup_settings_hint"),
                fontSize = (13 * fontScale).sp,
                color = onSurface.copy(alpha = 0.70f),
                lineHeight = (19 * fontScale).sp
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onOpenBackupSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(Locales.t("backup_settings_open"))
            }

            if (!dbOpsAllowed) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = Locales.t("backup_pin_required_hint"),
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
                modifier = Modifier
                    .padding(bottom = sectionTitlePaddingBottomDp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        securityTapCount += 1
                        if (securityTapCount >= 10) {
                            securityTapCount = 0
                            developerPasswordInput = ""
                            developerPasswordError = false
                            showDeveloperPasswordDialog = true
                        }
                    }
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
                    modifier = Modifier
                        .weight(0.45f)
                        .height(38.dp),
                    shape = RoundedCornerShape(12.dp),
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
                    modifier = Modifier
                        .weight(0.45f)
                        .height(38.dp),
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

        if (AppSettings.developerModeUnlocked) {
            Divider()

            Column {
                Text(
                    text = Locales.t("developer_mode_title"),
                    fontSize = (16 * fontScale).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = onSurface.copy(alpha = 0.85f),
                    modifier = Modifier.padding(bottom = sectionTitlePaddingBottomDp)
                )

                Text(
                    text = Locales.t("developer_mode_hint"),
                    fontSize = (13 * fontScale).sp,
                    color = onSurface.copy(alpha = 0.70f),
                    lineHeight = (19 * fontScale).sp
                )

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = onOpenDeveloperAccess,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(Locales.t("developer_mode_open"))
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
    }
}

@Composable
fun SettingsDropdown(
    label: String,
    selected: String,
    items: List<String>,
    onSelect: (String) -> Unit,
    labelSpacing: Dp = 10.dp
) {
    var expanded by remember { mutableStateOf(false) }
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

        Box {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current
                    ) {
                        expanded = true
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

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        onClick = {
                            onSelect(item)
                            expanded = false
                        }
                    ) {
                        Text(
                            text = item,
                            fontSize = (16 * fontScale).sp,
                            color = onSurface
                        )
                    }
                }
            }
        }
    }
}