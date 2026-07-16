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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AccessState
import com.andrey.beautyplanner.AccessTier
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Locales
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.TextStyle
import com.andrey.beautyplanner.appcontent.appFontFamily

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

    var showDisablePinConfirm by remember { mutableStateOf(false) }
    var pendingPinEnabledValue by remember(AppSettings.pinEnabled) {
        mutableStateOf(AppSettings.pinEnabled)
    }
    var securityTapCount by remember { mutableStateOf(0) }
    var showDeveloperPasswordDialog by remember { mutableStateOf(false) }
    var developerPasswordInput by remember { mutableStateOf("") }
    var developerPasswordError by remember { mutableStateOf(false) }

    val dbOpsAllowed = AppSettings.pinEnabled && AppSettings.isPinSet()

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
            },
            title = {
                Text(
                    text = Locales.t("developer_password_title"),
                    color = onSurface,
                    fontSize = (18 * fontScale).sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = Locales.t("developer_password_hint"),
                        color = onSurface.copy(alpha = 0.72f),
                        fontSize = (13 * fontScale).sp
                    )

                    androidx.compose.material.OutlinedTextField(
                        value = developerPasswordInput,
                        onValueChange = {
                            developerPasswordInput = it
                            developerPasswordError = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = {
                            Text(
                                text = Locales.t("developer_password_hint"),
                                color = onSurface.copy(alpha = 0.50f)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = appFontFamily(),
                            fontSize = (15 * fontScale).sp,
                            color = onSurface
                        ),
                        isError = developerPasswordError,
                        colors = androidx.compose.material.TextFieldDefaults.outlinedTextFieldColors(
                            textColor = onSurface,
                            focusedBorderColor = MaterialTheme.colors.primary,
                            unfocusedBorderColor = onSurface.copy(alpha = 0.28f),
                            focusedLabelColor = MaterialTheme.colors.primary,
                            unfocusedLabelColor = onSurface.copy(alpha = 0.68f),
                            cursorColor = MaterialTheme.colors.primary,
                            backgroundColor = MaterialTheme.colors.surface,
                            placeholderColor = onSurface.copy(alpha = 0.50f),
                            errorBorderColor = MaterialTheme.colors.error,
                            errorCursorColor = MaterialTheme.colors.error
                        )
                    )

                    if (developerPasswordError) {
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
                    },
                    shape = RoundedCornerShape(12.dp)
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
                        securityTapCount = 0
                    }
                ) {
                    Text(
                        text = Locales.t("cancel"),
                        color = onSurface.copy(alpha = 0.85f)
                    )
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    CenteredContentContainer {
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

            SettingsSectionBlock(
                title = Locales.t("appearance_settings"),
                description = Locales.t("appearance_settings_hint"),
                actionButton = {
                    PrimaryActionButton(
                        text = Locales.t("appearance_settings"),
                        onClick = onOpenAppearanceSettings
                    )
                }
            )

            Divider(
                modifier = Modifier.padding(vertical = 2.dp),
                color = onSurface.copy(alpha = 0.10f)
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

            SettingsSectionBlock(
                title = Locales.t("premium_section_title"),
                description = premiumHintText,
                extraContent = {
                    Text(
                        text = "${Locales.t("premium_status_label")}: $premiumStatusText",
                        fontSize = (16 * fontScale).sp,
                        fontWeight = FontWeight.Medium,
                        color = onSurface
                    )

                    if (accessState.tier == AccessTier.TRIAL) {
                        Text(
                            text = "${Locales.t("premium_trial_days_left")}: ${accessState.trialDaysLeft}",
                            fontSize = (14 * fontScale).sp,
                            color = onSurface.copy(alpha = 0.80f)
                        )
                    }
                },
                actionButton = {
                    PrimaryActionButton(
                        text = Locales.t("premium_open_screen_btn"),
                        onClick = onOpenPremiumScreen
                    )
                }
            )

            Divider(
                modifier = Modifier.padding(vertical = 2.dp),
                color = onSurface.copy(alpha = 0.10f)
            )

            SettingsSectionBlock(
                title = Locales.t("my_services"),
                description = Locales.t("my_services_hint"),
                actionButton = {
                    PrimaryActionButton(
                        text = Locales.t("my_services"),
                        onClick = onOpenServiceTemplates
                    )
                }
            )

            Divider(
                modifier = Modifier.padding(vertical = 2.dp),
                color = onSurface.copy(alpha = 0.10f)
            )

            SettingsSectionBlock(
                title = Locales.t("work_schedule"),
                description = Locales.t("work_schedule_hint"),
                actionButton = {
                    PrimaryActionButton(
                        text = Locales.t("work_schedule"),
                        onClick = onOpenWorkSchedule
                    )
                }
            )

            Divider(
                modifier = Modifier.padding(vertical = 2.dp),
                color = onSurface.copy(alpha = 0.10f)
            )

            SettingsSectionBlock(
                title = Locales.t("notifications_section"),
                description = Locales.t("notifications_settings_hint"),
                actionButton = {
                    PrimaryActionButton(
                        text = Locales.t("notifications_settings_open"),
                        onClick = onOpenNotificationSettings
                    )
                }
            )

            Divider(
                modifier = Modifier.padding(vertical = 2.dp),
                color = onSurface.copy(alpha = 0.10f)
            )

            SettingsSectionBlock(
                title = Locales.t("backup_settings_title"),
                description = Locales.t("backup_settings_hint"),
                extraContent = if (!dbOpsAllowed) {
                    {
                        Text(
                            text = Locales.t("backup_pin_required_hint"),
                            color = onSurface.copy(alpha = 0.72f),
                            fontSize = (12 * fontScale).sp
                        )
                    }
                } else null,
                actionButton = {
                    PrimaryActionButton(
                        text = Locales.t("backup_settings_open"),
                        onClick = onOpenBackupSettings
                    )
                }
            )

            Divider(
                modifier = Modifier.padding(vertical = 2.dp),
                color = onSurface.copy(alpha = 0.10f)
            )

            Column {
                Text(
                    text = Locales.t("security_section"),
                    fontSize = (16 * fontScale).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = onSurface.copy(alpha = 0.85f),
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            if (showDeveloperPasswordDialog) return@clickable

                            securityTapCount += 1
                            if (securityTapCount >= 15) {
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

                    AppSwitch(
                        checked = pendingPinEnabledValue,
                        onCheckedChange = { newValue ->
                            if (newValue) {
                                if (AppSettings.isPinSet()) {
                                    AppSettings.pinEnabled = true
                                    AppSettings.persist()
                                    pendingPinEnabledValue = true
                                } else {
                                    pendingPinEnabledValue = false
                                    onSetOrChangePin()
                                }
                            } else {
                                pendingPinEnabledValue = false
                                showDisablePinConfirm = true
                            }
                        }
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
                Divider(
                    modifier = Modifier.padding(vertical = 2.dp),
                    color = onSurface.copy(alpha = 0.10f)
                )

                SettingsSectionBlock(
                    title = Locales.t("developer_mode_title"),
                    description = Locales.t("developer_mode_hint"),
                    actionButton = {
                        PrimaryActionButton(
                            text = Locales.t("developer_mode_open"),
                            onClick = onOpenDeveloperAccess
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = Locales.t("privacy_policy"),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 4.dp)
                    .clickable { onOpenPrivacyPolicy() },
                color = onSurface.copy(alpha = 0.72f),
                fontSize = (11 * fontScale).sp,
                textDecoration = TextDecoration.Underline
            )
        }
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
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .widthIn(min = 220.dp, max = 420.dp)
                    .heightIn(max = 320.dp)
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
@Composable
private fun SettingsSectionBlock(
    title: String,
    description: String,
    extraContent: (@Composable () -> Unit)? = null,
    actionButton: @Composable () -> Unit
) {
    val fontScale = AppSettings.getFontScale()
    val onSurface = MaterialTheme.colors.onSurface

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            fontSize = (16 * fontScale).sp,
            fontWeight = FontWeight.SemiBold,
            color = onSurface.copy(alpha = 0.85f)
        )

        Text(
            text = description,
            fontSize = (13 * fontScale).sp,
            color = onSurface.copy(alpha = 0.70f),
            lineHeight = (19 * fontScale).sp
        )

        extraContent?.invoke()

        Spacer(Modifier.height(2.dp))

        actionButton()
    }
}