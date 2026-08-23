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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.andrey.beautyplanner.Locales

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ClientColorMarkerDialog(
    selectedColorTag: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val onSurface = MaterialTheme.colors.onSurface

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
        title = { Text(Locales.t("client_profile_pick_color_marker")) },
        text = {
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
                                onSelect(value)
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Locales.t("close"))
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

internal fun colorTagToColor(tag: String): Color {
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