package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.ClientProfile
import com.andrey.beautyplanner.ClientProfileStatus
import com.andrey.beautyplanner.Locales

private data class ClientStatusOption(
    val value: String,
    val title: String
)

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ClientProfileDialog(
    initialProfile: ClientProfile,
    visitCount: Int,
    lastVisitDate: String,
    onDismiss: () -> Unit,
    onSave: (ClientProfile) -> Unit
) {
    val fontScale = AppSettings.getFontScale()
    val onSurface = MaterialTheme.colors.onSurface

    var notes by remember(initialProfile.id) { mutableStateOf(initialProfile.notes) }
    var selectedStatus by remember(initialProfile.id) { mutableStateOf(initialProfile.status) }
    var selectedColorTag by remember(initialProfile.id) { mutableStateOf(initialProfile.colorTag) }

    val statusOptions = listOf(
        ClientStatusOption(ClientProfileStatus.NONE.name, Locales.t("client_status_none")),
        ClientStatusOption(ClientProfileStatus.VIP.name, Locales.t("client_status_vip")),
        ClientStatusOption(
            ClientProfileStatus.CONFIRMATION_REQUIRED.name,
            Locales.t("client_status_confirmation_required")
        ),
        ClientStatusOption(
            ClientProfileStatus.RISK_OF_CANCELLATION.name,
            Locales.t("client_status_risk_of_cancellation")
        ),
        ClientStatusOption(
            ClientProfileStatus.DO_NOT_BOOK.name,
            Locales.t("client_status_do_not_book")
        )
    )

    val colorOptions = listOf(
        "" to Locales.t("client_color_none"),
        "red" to Locales.t("client_color_red"),
        "orange" to Locales.t("client_color_orange"),
        "yellow" to Locales.t("client_color_yellow"),
        "green" to Locales.t("client_color_green"),
        "blue" to Locales.t("client_color_blue"),
        "purple" to Locales.t("client_color_purple"),
        "gray" to Locales.t("client_color_gray")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = initialProfile.displayName,
                    fontSize = (19 * fontScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurface
                )

                if (initialProfile.phone.isNotBlank()) {
                    Text(
                        text = initialProfile.phone,
                        fontSize = (13 * fontScale).sp,
                        color = onSurface.copy(alpha = 0.68f)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${Locales.t("client_database_visits")}: $visitCount",
                    fontSize = (14 * fontScale).sp,
                    color = onSurface.copy(alpha = 0.82f)
                )

                if (lastVisitDate.isNotBlank()) {
                    Text(
                        text = "${Locales.t("client_database_last_visit")}: $lastVisitDate",
                        fontSize = (14 * fontScale).sp,
                        color = onSurface.copy(alpha = 0.82f)
                    )
                }

                StatusDropdownField(
                    label = Locales.t("client_profile_status"),
                    selectedValue = statusOptions.firstOrNull { it.value == selectedStatus }?.title
                        ?: Locales.t("client_status_none"),
                    options = statusOptions.map { it.title },
                    onSelected = { title ->
                        selectedStatus =
                            statusOptions.firstOrNull { it.title == title }?.value
                                ?: ClientProfileStatus.NONE.name
                    }
                )

                Text(
                    text = Locales.t("client_profile_color_tag"),
                    fontSize = (14 * fontScale).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = onSurface.copy(alpha = 0.85f)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorOptions.forEach { (value, title) ->
                        val selected = selectedColorTag == value
                        val swatch = colorTagToColor(value)

                        Box(
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    color = if (selected) {
                                        MaterialTheme.colors.primary
                                    } else {
                                        onSurface.copy(alpha = 0.20f)
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(
                                    color = if (selected) {
                                        MaterialTheme.colors.primary.copy(alpha = 0.10f)
                                    } else {
                                        Color.Transparent
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    selectedColorTag = value
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = swatch,
                                            shape = RoundedCornerShape(50)
                                        )
                                        .padding(8.dp)
                                )
                                Text(
                                    text = title,
                                    color = onSurface
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 110.dp),
                    label = { Text(Locales.t("client_profile_notes")) },
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(
                        fontSize = (15 * fontScale).sp,
                        color = onSurface
                    ),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = onSurface,
                        focusedBorderColor = MaterialTheme.colors.primary,
                        unfocusedBorderColor = onSurface.copy(alpha = 0.28f),
                        focusedLabelColor = MaterialTheme.colors.primary,
                        unfocusedLabelColor = onSurface.copy(alpha = 0.60f),
                        cursorColor = MaterialTheme.colors.primary,
                        backgroundColor = MaterialTheme.colors.surface
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initialProfile.copy(
                            notes = notes.trim(),
                            status = selectedStatus,
                            colorTag = selectedColorTag,
                            updatedAtMillis = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                        )
                    )
                }
            ) {
                Text(Locales.t("save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Locales.t("cancel"))
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun StatusDropdownField(
    label: String,
    selectedValue: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    expanded = true
                },
            label = { Text(label) },
            shape = RoundedCornerShape(12.dp)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 320.dp)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onSelected(option)
                    }
                ) {
                    Text(option)
                }
            }
        }
    }
}

private fun colorTagToColor(tag: String): Color {
    return when (tag) {
        "red" -> Color(0xFFE57373)
        "orange" -> Color(0xFFFFB74D)
        "yellow" -> Color(0xFFFFF176)
        "green" -> Color(0xFF81C784)
        "blue" -> Color(0xFF64B5F6)
        "purple" -> Color(0xFFBA68C8)
        "gray" -> Color(0xFFB0BEC5)
        else -> Color(0xFFE0E0E0)
    }
}