package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Locales
import com.andrey.beautyplanner.auth.isAppleSignInSupported

@Composable
fun AuthWelcomeScreen(
    errorMessage: String?,
    onContinueWithGoogle: () -> Unit,
    onContinueWithApple: () -> Unit,
    onContinueWithEmail: () -> Unit,
    onRegisterWithEmail: () -> Unit,
    onContinueAnonymously: () -> Unit
) {
    val fontScale = AppSettings.getFontScale()
    val onBg = MaterialTheme.colors.onBackground
    val onSurface = MaterialTheme.colors.onSurface

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
                text = Locales.t("auth_title"),
                fontSize = (24 * fontScale).sp,
                fontWeight = FontWeight.Bold,
                color = onBg
            )

            Text(
                text = Locales.t("auth_subtitle"),
                fontSize = (14 * fontScale).sp,
                color = onBg.copy(alpha = 0.72f)
            )

            Spacer(Modifier.height(8.dp))

            PrimaryActionButton(
                text = Locales.t("auth_google"),
                onClick = onContinueWithGoogle
            )

            if (isAppleSignInSupported()) {
                SecondaryActionButton(
                    text = Locales.t("auth_apple"),
                    onClick = onContinueWithApple
                )
            }

            SecondaryActionButton(
                text = Locales.t("auth_email_sign_in"),
                onClick = onContinueWithEmail
            )

            SecondaryActionButton(
                text = Locales.t("auth_email_register"),
                onClick = onRegisterWithEmail
            )

            SecondaryActionButton(
                text = Locales.t("auth_anonymous"),
                onClick = onContinueAnonymously
            )

            Text(
                text = Locales.t("auth_skip_hint"),
                fontSize = (12 * fontScale).sp,
                color = onSurface.copy(alpha = 0.65f)
            )

            if (!errorMessage.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    fontSize = (13 * fontScale).sp,
                    color = MaterialTheme.colors.error
                )
            }
        }
    }
}