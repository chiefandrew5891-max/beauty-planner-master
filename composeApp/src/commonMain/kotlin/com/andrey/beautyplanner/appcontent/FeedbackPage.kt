package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.andrey.beautyplanner.AppInfoProvider
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.AppUpdateStatus
import com.andrey.beautyplanner.Locales
import com.andrey.beautyplanner.StoreOpener

@Composable
fun FeedbackPage(
    phone: String,
    updateStatus: AppUpdateStatus,
    isCheckingUpdates: Boolean,
    onCallClick: (String) -> Unit,
    onCheckUpdatesClick: () -> Unit
) {
    val fontScale = AppSettings.getFontScale()
    val appInfo = AppInfoProvider.get()

    val updateStatusText = when {
        updateStatus.errorMessage.isNotBlank() -> updateStatus.errorMessage
        !updateStatus.checked -> Locales.t("about_app_update_unknown")
        updateStatus.updateAvailable -> {
            buildString {
                append(Locales.t("about_app_update_available"))
                if (updateStatus.latestVersion.isNotBlank()) {
                    append(": ")
                    append(updateStatus.latestVersion)
                }
                if (updateStatus.latestBuild.isNotBlank()) {
                    append(" (")
                    append(updateStatus.latestBuild)
                    append(")")
                }
            }
        }
        else -> Locales.t("about_app_up_to_date")
    }

    CenteredNarrowContentContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = Locales.t("about_app_title"),
                fontSize = (22 * fontScale).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onBackground
            )

            Text(
                text = Locales.t("about_app_hint"),
                fontSize = (14 * fontScale).sp,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.padding(top = 2.dp))

            Text(
                text = "${Locales.t("about_app_version")}: ${appInfo.versionName}",
                fontSize = (16 * fontScale).sp,
                color = MaterialTheme.colors.onSurface
            )

            Text(
                text = "${Locales.t("about_app_build")}: ${appInfo.buildNumber}",
                fontSize = (16 * fontScale).sp,
                color = MaterialTheme.colors.onSurface
            )

            Text(
                text = "${Locales.t("about_app_platform")}: ${appInfo.platformLabel}",
                fontSize = (16 * fontScale).sp,
                color = MaterialTheme.colors.onSurface
            )

            Spacer(modifier = Modifier.padding(top = 4.dp))

            Text(
                text = "${Locales.t("support_phone_label")}: ${
                    if (phone.isBlank()) Locales.t("support_phone_empty") else phone
                }",
                fontSize = (16 * fontScale).sp,
                color = MaterialTheme.colors.onSurface
            )

            PrimaryActionButton(
                text = Locales.t("support_call"),
                onClick = { onCallClick(phone) },
                enabled = phone.isNotBlank()
            )

            Spacer(modifier = Modifier.padding(top = 4.dp))

            Text(
                text = "${Locales.t("about_app_update_status")}: $updateStatusText",
                fontSize = (15 * fontScale).sp,
                color = MaterialTheme.colors.onSurface
            )

            PrimaryActionButton(
                text = Locales.t("about_app_check_updates"),
                onClick = onCheckUpdatesClick,
                enabled = !isCheckingUpdates
            )
            if (isCheckingUpdates) {
                Text(
                    text = Locales.t("about_app_checking_updates"),
                    fontSize = (13 * fontScale).sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f)
                )
            }

            if (updateStatus.updateAvailable && updateStatus.storeUrl.isNotBlank()) {
                SecondaryActionButton(
                    text = Locales.t("about_app_update_now"),
                    onClick = {
                        StoreOpener.open(updateStatus.storeUrl)
                    }
                )
            }
        }
    }
}