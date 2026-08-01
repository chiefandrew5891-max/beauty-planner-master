package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val isTablet = maxWidth >= 700.dp

        // Смещение контента:
        // phone -> чуть выше центра
        // tablet -> почти по центру
        val topSpacerHeight = if (isTablet) {
            maxHeight * 0.18f
        } else {
            maxHeight * 0.10f
        }

        CenteredNarrowContentContainer {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(topSpacerHeight))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = Locales.t("auth_title"),
                        fontSize = (24 * fontScale).sp,
                        fontWeight = FontWeight.Bold,
                        color = onBg,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = Locales.t("auth_subtitle"),
                        fontSize = (14 * fontScale).sp,
                        color = onBg.copy(alpha = 0.72f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(6.dp))

                    BrandedAuthButton(
                        text = Locales.t("auth_google"),
                        onClick = onContinueWithGoogle,
                        backgroundColor = Color.White,
                        contentColor = Color(0xFF1F1F1F),
                        borderColor = Color(0xFFDADCE0),
                        leadingContent = {
                            GoogleIcon()
                        }
                    )

                    if (isAppleSignInSupported()) {
                        BrandedAuthButton(
                            text = Locales.t("auth_apple"),
                            onClick = onContinueWithApple,
                            backgroundColor = Color.Black,
                            contentColor = Color.White,
                            borderColor = Color.Black,
                            leadingContent = {
                                AppleGlyphIcon()
                            }
                        )
                    }

                    BrandedAuthButton(
                        text = Locales.t("auth_email_sign_in"),
                        onClick = onContinueWithEmail,
                        backgroundColor = Color.White,
                        contentColor = onSurface,
                        borderColor = onSurface.copy(alpha = 0.14f),
                        leadingContent = {
                            MailIcon()
                        }
                    )

                    BrandedAuthButton(
                        text = Locales.t("auth_anonymous"),
                        onClick = onContinueAnonymously,
                        backgroundColor = Color.White,
                        contentColor = onSurface,
                        borderColor = onSurface.copy(alpha = 0.14f),
                        leadingContent = {
                            Text(
                                text = "👤",
                                color = MaterialTheme.colors.primary,
                                fontSize = 18.sp
                            )
                        }
                    )

                    if (!errorMessage.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = errorMessage,
                            fontSize = (13 * fontScale).sp,
                            color = MaterialTheme.colors.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}