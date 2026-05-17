package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Locales

@Composable
fun PinDialog(
    title: String,
    text: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirmPin: (String) -> Unit,
    allowDismiss: Boolean = true // если false — крестик и "Отмена" скрываются
) {
    // --- Блюр по всему фону под PIN-диалогом ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.25f)) // чуть затемняем фон
            .blur(radius = 24.dp), // блюр на весь экран (Compose Multiplatform)
        contentAlignment = Alignment.Center
    ) {
        var pin by remember { mutableStateOf("") }
        var tried by remember { mutableStateOf(false) }

        val validFormat = AppSettings.isPinValidFormat(pin)
        val safeDismiss = { if (allowDismiss) onDismiss() }

        AlertDialog(
            onDismissRequest = safeDismiss,
            title = null,
            text = {
                Column(Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth()) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.h6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = if (allowDismiss) 44.dp else 0.dp),
                            color = MaterialTheme.colors.onSurface
                        )

                        if (allowDismiss) {
                            IconButton(
                                onClick = safeDismiss,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 14.dp, y = (-5).dp)
                                    .size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = Locales.t("close"),
                                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.65f)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(text, color = MaterialTheme.colors.onSurface)

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { v -> pin = v.filter { it.isDigit() }.take(8) },
                        label = { Text(Locales.t("pin_label")) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        isError = tried && !validFormat
                    )

                    if (tried && !validFormat) {
                        Spacer(Modifier.height(6.dp))
                        Text(Locales.t("pin_invalid_format"), color = MaterialTheme.colors.error)
                    }
                }
            },

            buttons = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (allowDismiss) {
                        TextButton(
                            onClick = safeDismiss,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.75f)
                            )
                        ) { Text(Locales.t("cancel")) }

                        Spacer(Modifier.width(15.dp))
                    }

                    Button(
                        onClick = {
                            tried = true
                            if (!validFormat) return@Button
                            onConfirmPin(pin)
                        },
                        modifier = Modifier.height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                        elevation = ButtonDefaults.elevation(0.dp, 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.primary,
                            contentColor = MaterialTheme.colors.onPrimary
                        )
                    ) { Text(confirmText) }
                }
            },

            shape = AppDialogShape
        )
    }
}