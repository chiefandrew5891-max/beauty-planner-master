package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Locales

@Composable
fun GuestAccountRegistrationScreen(
    onContinueWithGoogle: () -> Unit,
    onContinueWithApple: () -> Unit,
    onRegisterWithEmail: () -> Unit,
    onBack: () -> Unit
) {
    val fontScale = AppSettings.getFontScale()
    val onBg = MaterialTheme.colors.onBackground

    CenteredNarrowContentContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = Locales.t("guest_upgrade_title"),
                fontSize = (22 * fontScale).sp,
                fontWeight = FontWeight.Bold,
                color = onBg
            )

            Text(
                text = Locales.t("guest_upgrade_p1"),
                fontSize = (14 * fontScale).sp,
                lineHeight = (20 * fontScale).sp,
                color = onBg.copy(alpha = 0.85f)
            )

            Text(
                text = Locales.t("guest_upgrade_p2"),
                fontSize = (14 * fontScale).sp,
                lineHeight = (20 * fontScale).sp,
                color = onBg.copy(alpha = 0.85f)
            )

            Text(
                text = Locales.t("guest_upgrade_p3"),
                fontSize = (14 * fontScale).sp,
                lineHeight = (20 * fontScale).sp,
                color = onBg.copy(alpha = 0.85f)
            )

            Button(
                onClick = onContinueWithGoogle,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(Locales.t("auth_continue_google"))
            }

            Button(
                onClick = onContinueWithApple,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(Locales.t("auth_continue_apple"))
            }

            OutlinedButton(
                onClick = onRegisterWithEmail,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(Locales.t("auth_register_email"))
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(Locales.t("cancel"))
            }
        }
    }
}