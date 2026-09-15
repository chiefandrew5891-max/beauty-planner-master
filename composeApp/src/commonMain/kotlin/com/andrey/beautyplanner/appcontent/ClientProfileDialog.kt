package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.ClientProfile
import com.andrey.beautyplanner.ClientProfileStatus
import com.andrey.beautyplanner.Locales
import kotlinx.datetime.Clock

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

    var noteText by remember(initialProfile.id) { mutableStateOf(initialProfile.notes) }
    var selectedColorTag by remember(initialProfile.id) { mutableStateOf(initialProfile.colorTag) }
    var blackListEnabled by remember(initialProfile.id) {
        mutableStateOf(initialProfile.status == ClientProfileStatus.DO_NOT_BOOK.name)
    }

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

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            elevation = 12.dp,
            backgroundColor = MaterialTheme.colors.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        val clientNote = initialProfile.notes.trim()
                        val titleText = if (clientNote.isBlank()) {
                            initialProfile.displayName
                        } else {
                            "${initialProfile.displayName} ($clientNote)"
                        }

                        Text(
                            text = titleText,
                            fontSize = (20 * fontScale).sp,
                            fontWeight = FontWeight.Bold,
                            color = onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = Locales.t("close"),
                            tint = onSurface.copy(alpha = 0.82f)
                        )
                    }
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(Locales.t("client_profile_note_mark")) },
                    shape = RoundedCornerShape(14.dp),
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

                Text(
                    text = Locales.t("client_profile_pick_color_marker"),
                    fontSize = (14 * fontScale).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = onSurface.copy(alpha = 0.90f)
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = Locales.t("client_profile_blacklist"),
                        fontSize = (15 * fontScale).sp,
                        color = onSurface
                    )

                    AppSwitch(
                        checked = blackListEnabled,
                        onCheckedChange = { blackListEnabled = it },
                        enabled = true
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        onSave(
                            initialProfile.copy(
                                notes = noteText.trim(),
                                status = if (blackListEnabled) {
                                    ClientProfileStatus.DO_NOT_BOOK.name
                                } else {
                                    ClientProfileStatus.NONE.name
                                },
                                colorTag = selectedColorTag,
                                updatedAtMillis = Clock.System.now().toEpochMilliseconds()
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.primary
                    )
                ) {
                    Text(
                        text = Locales.t("save").uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
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