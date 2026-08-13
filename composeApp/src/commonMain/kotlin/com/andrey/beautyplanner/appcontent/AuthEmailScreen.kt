package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Locales

@Composable
fun AuthEmailScreen(
    isRegisterMode: Boolean,
    errorMessage: String?,
    infoMessage: String?,
    onModeChange: (Boolean) -> Unit,
    onSubmit: (email: String, password: String, confirmPassword: String) -> Unit,
    onForgotPassword: (email: String) -> Unit
) {
    val fontScale = AppSettings.getFontScale()
    val onBg = MaterialTheme.colors.onBackground
    val onSurface = MaterialTheme.colors.onSurface
    val primary = MaterialTheme.colors.primary
    val surface = MaterialTheme.colors.surface

    val emailState = remember { mutableStateOf(AppSettings.lastAuthEmail) }
    val passwordState = remember { mutableStateOf("") }
    val confirmPasswordState = remember { mutableStateOf("") }
    val showPasswordState = remember { mutableStateOf(false) }
    var emailMenuExpanded by remember { mutableStateOf(false) }

    val recentEmails = remember(emailState.value, AppSettings.recentAuthEmails) {
        val query = emailState.value.trim()
        AppSettings.recentAuthEmailSuggestions().filter {
            query.isBlank() || it.contains(query, ignoreCase = true)
        }
    }

    val shouldShowEmailMenu = remember(emailMenuExpanded, recentEmails, emailState.value) {
        val trimmed = emailState.value.trim()
        emailMenuExpanded &&
                recentEmails.isNotEmpty() &&
                !(recentEmails.size == 1 && recentEmails.first().equals(trimmed, ignoreCase = true))
    }

    val passwordVisualTransformation =
        if (showPasswordState.value) VisualTransformation.None
        else PasswordVisualTransformation()

    CenteredNarrowContentContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(20.dp))

            Text(
                text = Locales.t("auth_email_title"),
                fontSize = (24 * fontScale).sp,
                fontWeight = FontWeight.Bold,
                color = onBg
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = surface,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = onSurface.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AuthModeTabButton(
                    text = Locales.t("auth_email_mode_sign_in"),
                    selected = !isRegisterMode,
                    onClick = {
                        emailMenuExpanded = false
                        onModeChange(false)
                    },
                    modifier = Modifier.weight(1f)
                )

                AuthModeTabButton(
                    text = Locales.t("auth_email_mode_register"),
                    selected = isRegisterMode,
                    onClick = {
                        emailMenuExpanded = false
                        onModeChange(true)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = emailState.value,
                    onValueChange = {
                        emailState.value = it
                        emailMenuExpanded = it.isNotBlank()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(Locales.t("auth_email_field")) },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = appFontFamily(),
                        color = onSurface
                    ),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = onSurface,
                        focusedBorderColor = primary,
                        unfocusedBorderColor = onSurface.copy(alpha = 0.28f),
                        focusedLabelColor = primary,
                        unfocusedLabelColor = onSurface.copy(alpha = 0.68f),
                        cursorColor = primary,
                        backgroundColor = surface
                    )
                )

                DropdownMenu(
                    expanded = shouldShowEmailMenu,
                    onDismissRequest = { emailMenuExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                ) {
                    recentEmails.forEach { savedEmail ->
                        DropdownMenuItem(
                            onClick = {
                                emailState.value = savedEmail
                                emailMenuExpanded = false
                            }
                        ) {
                            Text(
                                text = savedEmail,
                                color = onSurface,
                                fontSize = (14 * fontScale).sp
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = passwordState.value,
                onValueChange = {
                    passwordState.value = it
                    emailMenuExpanded = false
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(Locales.t("auth_password_field")) },
                visualTransformation = passwordVisualTransformation,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = appFontFamily(),
                    color = onSurface
                ),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = onSurface,
                    focusedBorderColor = primary,
                    unfocusedBorderColor = onSurface.copy(alpha = 0.28f),
                    focusedLabelColor = primary,
                    unfocusedLabelColor = onSurface.copy(alpha = 0.68f),
                    cursorColor = primary,
                    backgroundColor = surface
                )
            )

            if (isRegisterMode) {
                OutlinedTextField(
                    value = confirmPasswordState.value,
                    onValueChange = {
                        confirmPasswordState.value = it
                        emailMenuExpanded = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(Locales.t("auth_password_confirm_field")) },
                    visualTransformation = passwordVisualTransformation,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = appFontFamily(),
                        color = onSurface
                    ),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = onSurface,
                        focusedBorderColor = primary,
                        unfocusedBorderColor = onSurface.copy(alpha = 0.28f),
                        focusedLabelColor = primary,
                        unfocusedLabelColor = onSurface.copy(alpha = 0.68f),
                        cursorColor = primary,
                        backgroundColor = surface
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = showPasswordState.value,
                    onCheckedChange = { showPasswordState.value = it }
                )
                Text(
                    text = Locales.t("auth_show_password"),
                    fontSize = (14 * fontScale).sp,
                    color = onSurface
                )
            }

            if (!isRegisterMode) {
                TextButton(
                    onClick = {
                        emailMenuExpanded = false
                        onForgotPassword(emailState.value)
                    }
                ) {
                    Text(Locales.t("auth_password_reset"))
                }
            }

            PrimaryActionButton(
                text = if (isRegisterMode) {
                    Locales.t("auth_email_submit_register")
                } else {
                    Locales.t("auth_email_submit_sign_in")
                },
                onClick = {
                    emailMenuExpanded = false
                    onSubmit(
                        emailState.value,
                        passwordState.value,
                        confirmPasswordState.value
                    )
                }
            )

            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colors.error,
                    fontSize = (13 * fontScale).sp,
                )
            }

            if (!infoMessage.isNullOrBlank()) {
                Text(
                    text = infoMessage,
                    color = MaterialTheme.colors.onSurface,
                    fontSize = (13 * fontScale).sp,
                )
            }
        }
    }
}

@Composable
private fun AuthModeTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colors.primary
    val onPrimary = MaterialTheme.colors.onPrimary
    val onSurface = MaterialTheme.colors.onSurface

    androidx.compose.material.Button(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = androidx.compose.material.ButtonDefaults.elevation(
            defaultElevation = if (selected) 2.dp else 0.dp
        ),
        colors = androidx.compose.material.ButtonDefaults.buttonColors(
            backgroundColor = if (selected) primary else Color.Transparent,
            contentColor = if (selected) onPrimary else onSurface
        )
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Medium
        )
    }
}