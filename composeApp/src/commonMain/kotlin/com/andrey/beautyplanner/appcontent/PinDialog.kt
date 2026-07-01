package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Locales
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.TextStyle
import com.andrey.beautyplanner.appcontent.appFontFamily

@Composable
fun PinDialog(
    title: String,
    text: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirmPin: (String) -> Unit,
    allowDismiss: Boolean = true
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.25f))
            .blur(radius = 24.dp),
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
                                    .padding(top = 0.dp)
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

                    Text(
                        text = text,
                        color = MaterialTheme.colors.onSurface
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { v -> pin = v.filter { it.isDigit() }.take(8) },
                        label = { Text(Locales.t("pin_label")) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        isError = tried && !validFormat,
                        textStyle = TextStyle(
                            fontFamily = appFontFamily(),
                            color = MaterialTheme.colors.onSurface
                        )
                    )

                    if (tried && !validFormat) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = Locales.t("pin_invalid_format"),
                            color = MaterialTheme.colors.error
                        )
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
                        ) {
                            Text(Locales.t("cancel"))
                        }

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
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 18.dp,
                            vertical = 10.dp
                        ),
                        elevation = ButtonDefaults.elevation(0.dp, 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.primary,
                            contentColor = MaterialTheme.colors.onPrimary
                        )
                    ) {
                        Text(confirmText)
                    }
                }
            },
            shape = AppDialogShape
        )
    }
}

@Composable
fun SetPinDialog(
    onDismiss: () -> Unit,
    onPinSet: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.25f))
            .blur(radius = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        var pin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var tried by remember { mutableStateOf(false) }

        val pinValid = AppSettings.isPinValidFormat(pin)
        val confirmValid = AppSettings.isPinValidFormat(confirmPin)
        val pinsMatch = pin == confirmPin
        val formValid = pinValid && confirmValid && pinsMatch

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = Locales.t("pin_set"),
                    color = MaterialTheme.colors.onSurface
                )
            },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        text = Locales.t("pin_create_hint"),
                        color = MaterialTheme.colors.onSurface
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(8) },
                        label = { Text(Locales.t("pin_create_label")) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        isError = tried && !pinValid,
                        textStyle = TextStyle(
                            fontFamily = appFontFamily(),
                            color = MaterialTheme.colors.onSurface
                        )
                    )

                    Spacer(Modifier.height(12.dp))
                    val validFormat = pin.length in 4..8
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { confirmPin = it.filter { ch -> ch.isDigit() }.take(8) },
                        label = { Text(Locales.t("pin_confirm_label")) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        isError = tried && (!confirmValid || !pinsMatch),
                        textStyle = TextStyle(
                            fontFamily = appFontFamily(),
                            color = MaterialTheme.colors.onSurface
                        ),
                        colors = androidx.compose.material.TextFieldDefaults.outlinedTextFieldColors(
                            textColor = MaterialTheme.colors.onSurface,
                            focusedBorderColor = MaterialTheme.colors.primary,
                            unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.28f),
                            focusedLabelColor = MaterialTheme.colors.primary,
                            unfocusedLabelColor = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
                            cursorColor = MaterialTheme.colors.primary,
                            backgroundColor = MaterialTheme.colors.surface,
                            placeholderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.50f),
                            errorBorderColor = MaterialTheme.colors.error,
                            errorCursorColor = MaterialTheme.colors.error
                        )
                    )

                    if (tried && !pinValid) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = Locales.t("pin_invalid_format"),
                            color = MaterialTheme.colors.error
                        )
                    } else if (tried && !pinsMatch) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = Locales.t("pin_mismatch"),
                            color = MaterialTheme.colors.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        tried = true
                        if (!formValid) return@Button
                        if (AppSettings.setPin(pin)) {
                            onPinSet()
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(Locales.t("save"))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(Locales.t("cancel"))
                }
            },
            shape = AppDialogShape
        )
    }
}