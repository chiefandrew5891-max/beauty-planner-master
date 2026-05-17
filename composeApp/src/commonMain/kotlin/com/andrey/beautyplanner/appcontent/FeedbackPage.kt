package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Locales

@Composable
fun FeedbackPage(
    phone: String,
    onCallClick: (String) -> Unit
) {
    val fontScale = AppSettings.getFontScale()

    val primaryText = UiColors.primaryText()
    val secondaryText = UiColors.secondaryText()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = Locales.t("nav_feedback"),
            fontSize = (22 * fontScale).sp,
            fontWeight = FontWeight.Bold,
            color = primaryText
        )

        Text(
            text = Locales.t("support_feedback_text"),
            color = secondaryText,
            fontSize = (14 * fontScale).sp
        )

        Divider()

        Text(
            text = "${Locales.t("support_phone_label")}: ${if (phone.isBlank()) Locales.t("support_phone_empty") else phone}",
            fontSize = (16 * fontScale).sp,
            color = primaryText
        )

        Button(
            onClick = { onCallClick(phone) },
            enabled = phone.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.Call, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text(Locales.t("support_call"))
        }
    }
}