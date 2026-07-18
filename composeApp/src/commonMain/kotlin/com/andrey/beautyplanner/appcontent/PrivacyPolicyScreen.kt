package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Locales
import com.andrey.beautyplanner.openEmail

private const val PRIVACY_EMAIL = "beautyplanner2026@gmail.com"

@Composable
fun PrivacyPolicyScreen(
    languageCode: String,
    onBack: () -> Unit
) {
    val fontScale = AppSettings.getFontScale()

    val docTitleFontSize = (20 * fontScale).sp
    val docTitleLineHeight = (26 * fontScale).sp
    val sectionTitleFontSize = (16 * fontScale).sp
    val sectionTitleLineHeight = (22 * fontScale).sp
    val bodyFontSize = (14 * fontScale).sp
    val bodyLineHeight = (21 * fontScale).sp
    val metaFontSize = (12 * fontScale).sp
    val metaLineHeight = (16 * fontScale).sp

    val sections = privacyPolicySections()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 20.dp)
        ) {
            Text(
                text = Locales.t("privacy_policy_title"),
                color = MaterialTheme.colors.onBackground,
                fontSize = docTitleFontSize,
                lineHeight = docTitleLineHeight,
                style = TextStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            )

            Spacer(modifier = Modifier.size(8.dp))

            Text(
                text = Locales.t("privacy_policy_last_updated"),
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
                fontSize = metaFontSize,
                lineHeight = metaLineHeight
            )

            Spacer(modifier = Modifier.size(16.dp))

            PolicyParagraph(
                text = Locales.t("privacy_policy_intro"),
                fontSize = bodyFontSize,
                lineHeight = bodyLineHeight
            )

            Spacer(modifier = Modifier.size(22.dp))

            sections.forEach { section ->
                Text(
                    text = Locales.t(section.titleKey),
                    color = MaterialTheme.colors.onBackground,
                    fontSize = sectionTitleFontSize,
                    lineHeight = sectionTitleLineHeight,
                    style = TextStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                )

                Spacer(modifier = Modifier.size(10.dp))

                section.paragraphKeys.forEach { key ->
                    PolicyParagraph(
                        text = Locales.t(key),
                        fontSize = bodyFontSize,
                        lineHeight = bodyLineHeight
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                }

                section.bulletKeys.forEach { key ->
                    PolicyBullet(
                        text = Locales.t(key),
                        fontSize = bodyFontSize,
                        lineHeight = bodyLineHeight
                    )
                }

                if (section.showEmail) {
                    Spacer(modifier = Modifier.size(4.dp))
                    PolicyEmailLine(
                        prefix = Locales.t("privacy_policy_contact_email_prefix"),
                        email = PRIVACY_EMAIL,
                        fontSize = bodyFontSize,
                        lineHeight = bodyLineHeight
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }

                Spacer(modifier = Modifier.size(14.dp))
            }
        }
    }
}

private data class PrivacyPolicySectionKeys(
    val titleKey: String,
    val paragraphKeys: List<String> = emptyList(),
    val bulletKeys: List<String> = emptyList(),
    val showEmail: Boolean = false
)

private fun privacyPolicySections(): List<PrivacyPolicySectionKeys> {
    return listOf(
        PrivacyPolicySectionKeys(
            titleKey = "privacy_policy_section_1_title",
            paragraphKeys = listOf(
                "privacy_policy_section_1_p1",
                "privacy_policy_section_1_p2",
                "privacy_policy_section_1_p3"
            )
        ),
        PrivacyPolicySectionKeys(
            titleKey = "privacy_policy_section_2_title",
            paragraphKeys = listOf(
                "privacy_policy_section_2_p1",
                "privacy_policy_section_2_p2",
                "privacy_policy_section_2_p3"
            )
        ),
        PrivacyPolicySectionKeys(
            titleKey = "privacy_policy_section_3_title",
            paragraphKeys = listOf(
                "privacy_policy_section_3_p1",
                "privacy_policy_section_3_p2",
                "privacy_policy_section_3_p3"
            )
        ),
        PrivacyPolicySectionKeys(
            titleKey = "privacy_policy_section_4_title",
            paragraphKeys = listOf(
                "privacy_policy_section_4_p1",
                "privacy_policy_section_4_p2",
                "privacy_policy_section_4_p3",
                "privacy_policy_section_4_p4"
            )
        ),
        PrivacyPolicySectionKeys(
            titleKey = "privacy_policy_section_5_title",
            paragraphKeys = listOf(
                "privacy_policy_section_5_p1",
                "privacy_policy_section_5_p2"
            )
        ),
        PrivacyPolicySectionKeys(
            titleKey = "privacy_policy_section_6_title",
            paragraphKeys = listOf(
                "privacy_policy_section_6_p1"
            ),
            bulletKeys = listOf(
                "privacy_policy_section_6_b1",
                "privacy_policy_section_6_b2",
                "privacy_policy_section_6_b3",
                "privacy_policy_section_6_b4"
            )
        ),
        PrivacyPolicySectionKeys(
            titleKey = "privacy_policy_section_7_title",
            paragraphKeys = listOf(
                "privacy_policy_section_7_p1",
                "privacy_policy_section_7_p2"
            )
        ),
        PrivacyPolicySectionKeys(
            titleKey = "privacy_policy_section_8_title",
            paragraphKeys = listOf(
                "privacy_policy_section_8_p1",
                "privacy_policy_section_8_p2"
            )
        ),
        PrivacyPolicySectionKeys(
            titleKey = "privacy_policy_section_9_title",
            paragraphKeys = listOf(
                "privacy_policy_section_9_p1",
                "privacy_policy_section_9_p2",
                "privacy_policy_section_9_p3"
            )
        ),
        PrivacyPolicySectionKeys(
            titleKey = "privacy_policy_section_10_title",
            paragraphKeys = listOf(
                "privacy_policy_section_10_p1",
                "privacy_policy_section_10_p2",
                "privacy_policy_section_10_p3"
            )
        ),
        PrivacyPolicySectionKeys(
            titleKey = "privacy_policy_section_11_title",
            paragraphKeys = listOf(
                "privacy_policy_section_11_p1",
                "privacy_policy_section_11_p2"
            )
        ),
        PrivacyPolicySectionKeys(
            titleKey = "privacy_policy_section_12_title",
            paragraphKeys = listOf(
                "privacy_policy_section_12_p1"
            )
        ),
        PrivacyPolicySectionKeys(
            titleKey = "privacy_policy_section_13_title",
            paragraphKeys = listOf(
                "privacy_policy_section_13_p1"
            ),
            showEmail = true
        )
    )
}

@Composable
private fun PolicyParagraph(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit
) {
    Text(
        text = text,
        color = MaterialTheme.colors.onBackground,
        fontSize = fontSize,
        lineHeight = lineHeight
    )
}

@Composable
private fun PolicyBullet(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            color = MaterialTheme.colors.onBackground,
            fontSize = fontSize,
            lineHeight = lineHeight,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            color = MaterialTheme.colors.onBackground,
            fontSize = fontSize,
            lineHeight = lineHeight
        )
    }
}

@Composable
private fun PolicyEmailLine(
    prefix: String,
    email: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = prefix,
            color = MaterialTheme.colors.onBackground,
            fontSize = fontSize,
            lineHeight = lineHeight
        )

        Text(
            text = email,
            color = MaterialTheme.colors.primary,
            fontSize = fontSize,
            lineHeight = lineHeight,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable {
                openEmail(email)
            }
        )
    }
}