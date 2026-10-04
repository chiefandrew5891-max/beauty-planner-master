package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AccessManager
import com.andrey.beautyplanner.AccessState
import com.andrey.beautyplanner.AccessTier
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Locales
import com.andrey.beautyplanner.StoreOpener
import com.andrey.beautyplanner.billing.BillingStatus
import com.andrey.beautyplanner.billing.BillingUiState
import com.andrey.beautyplanner.billing.PREMIUM_SUBS_PRODUCT_ID_MONTHLY
import com.andrey.beautyplanner.billing.PREMIUM_SUBS_PRODUCT_ID_YEARLY
import com.andrey.beautyplanner.billing.PREMIUM_SUBS_PRODUCT_IDS
import com.andrey.beautyplanner.getPlatform
import kotlinx.datetime.Clock

private const val TERMS_OF_USE_URL = "https://sites.google.com/view/beautyplanner/terms-of-use"

@Composable
fun PremiumAccessScreen(
    accessState: AccessState,
    message: String,
    billingUiState: BillingUiState,
    accountLabel: String,
    isGuestUser: Boolean,
    onContinueFree: () -> Unit,
    onUnlockPremium: (String) -> Unit,
    onRestorePurchases: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit
) {
    val isIos = getPlatform().backendPlatform == "ios"
    val fontScale = AppSettings.getFontScale()
    val linkColor = MaterialTheme.colors.primary

    val premiumProducts = billingUiState.products
        .filter { it.productId in PREMIUM_SUBS_PRODUCT_IDS }
        .sortedBy { product ->
            when (product.productId) {
                PREMIUM_SUBS_PRODUCT_ID_MONTHLY -> 0
                PREMIUM_SUBS_PRODUCT_ID_YEARLY -> 1
                else -> 99
            }
        }

    val monthlyProduct = premiumProducts.firstOrNull {
        it.productId == PREMIUM_SUBS_PRODUCT_ID_MONTHLY
    }

    val yearlyProduct = premiumProducts.firstOrNull {
        it.productId == PREMIUM_SUBS_PRODUCT_ID_YEARLY
    }

    val isPremiumActive = AccessManager.isPremiumAccessActive(
        Clock.System.now().toEpochMilliseconds()
    )

    val unifiedStatusLabel = AccessManager.getUnifiedAccessStatusLabel(accessState)

    val subtitle = when {
        accessState.tier == AccessTier.PREMIUM || accessState.hasPremium ->
            Locales.t("premium_active_subtitle")

        accessState.isTrialActive ->
            Locales.t("premium_trial_active_subtitle")

        else ->
            Locales.t("premium_trial_expired_subtitle")
    }

    val resolvedMessage = message.takeIf {
        it.isNotBlank() && it != Locales.t("premium_required_default")
    }.orEmpty()

    val buyEnabled =
        !isGuestUser &&
                !isPremiumActive &&
                billingUiState.status != BillingStatus.PURCHASING &&
                billingUiState.status != BillingStatus.RESTORING

    val expiryMillis = AppSettings.premiumSubscriptionExpiryMillis
    val daysLeft = calculateSubscriptionDaysLeft(
        expiryMillis = expiryMillis,
        nowMillis = Clock.System.now().toEpochMilliseconds()
    )
    val expiryText = formatSubscriptionExpiry(expiryMillis)

    val autoRenewEnabled = AppSettings.premiumSubscriptionAutoRenewing
    val showCancelledButActiveNotice =
        isPremiumActive &&
                expiryMillis > Clock.System.now().toEpochMilliseconds() &&
                !autoRenewEnabled

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .navigationBarsPadding()
    ) {
        CenteredNarrowContentContainer {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = subtitle,
                    fontSize = (18 * fontScale).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPremiumActive) {
                        MaterialTheme.colors.primary
                    } else {
                        MaterialTheme.colors.onBackground
                    },
                    lineHeight = (24 * fontScale).sp
                )

                Spacer(modifier = Modifier.padding(top = 14.dp))

                if (resolvedMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.padding(top = 18.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colors.primary.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = resolvedMessage,
                            fontSize = (15 * fontScale).sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colors.onBackground,
                            lineHeight = (22 * fontScale).sp
                        )
                    }
                }

                if (!isPremiumActive && !billingUiState.errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.padding(top = 18.dp))

                    val rawMessage = billingUiState.errorMessage.orEmpty()

                    val isSoftInfoMessage =
                        rawMessage == Locales.t("premium_product_not_found") ||
                                rawMessage == Locales.t("premium_store_unavailable")

                    val displayMessage = if (isSoftInfoMessage) {
                        rawMessage
                    } else {
                        "${Locales.t("premium_important_prefix")} $rawMessage"
                    }

                    val messageTextColor = if (isSoftInfoMessage) {
                        MaterialTheme.colors.primary.copy(alpha = 0.92f)
                    } else {
                        MaterialTheme.colors.onBackground
                    }

                    val messageFontWeight = if (isSoftInfoMessage) {
                        FontWeight.Medium
                    } else {
                        FontWeight.SemiBold
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colors.primary.copy(alpha = 0.10f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = displayMessage,
                            fontSize = (14 * fontScale).sp,
                            fontWeight = messageFontWeight,
                            color = messageTextColor,
                            lineHeight = (20 * fontScale).sp
                        )
                    }
                }

                Spacer(modifier = Modifier.padding(top = 24.dp))

                Text(
                    text = Locales.t("premium_features_title"),
                    fontSize = (18 * fontScale).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colors.onBackground
                )

                Spacer(modifier = Modifier.padding(top = 12.dp))

                PremiumBullet(Locales.t("premium_feature_unlimited"), fontScale)
                PremiumBullet(Locales.t("premium_feature_stats"), fontScale)
                PremiumBullet(Locales.t("premium_feature_cloud_sync"), fontScale)
                PremiumBullet(Locales.t("premium_feature_services"), fontScale)
                PremiumBullet(Locales.t("premium_feature_schedule"), fontScale)
                PremiumBullet(Locales.t("premium_feature_payments"), fontScale)
                PremiumBullet(Locales.t("premium_feature_future"), fontScale)

                Spacer(modifier = Modifier.padding(top = 24.dp))

                Text(
                    text = Locales.t("premium_subscription_status_title"),
                    fontSize = (16 * fontScale).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colors.onBackground
                )

                Spacer(modifier = Modifier.padding(top = 8.dp))

                Text(
                    text = buildAnnotatedString {
                        append("${Locales.t("premium_status_label")}: ")
                        withStyle(
                            SpanStyle(fontWeight = FontWeight.SemiBold)
                        ) {
                            append(unifiedStatusLabel)
                        }
                    },
                    fontSize = (14 * fontScale).sp,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.88f)
                )

                if (expiryMillis > 0L) {
                    Spacer(modifier = Modifier.padding(top = 6.dp))
                    Text(
                        text = "${Locales.t("premium_subscription_expires")}: $expiryText",
                        fontSize = (14 * fontScale).sp,
                        color = MaterialTheme.colors.onBackground.copy(alpha = 0.88f)
                    )

                    Spacer(modifier = Modifier.padding(top = 6.dp))

                    Text(
                        text = buildAnnotatedString {
                            append("${Locales.t("premium_subscription_days_left")}: ")

                            val daysText = Locales.daysCount(daysLeft)
                            val firstSpaceIndex = daysText.indexOf(' ')

                            if (firstSpaceIndex > 0) {
                                withStyle(
                                    SpanStyle(fontWeight = FontWeight.SemiBold)
                                ) {
                                    append(daysText.substring(0, firstSpaceIndex))
                                }
                                append(daysText.substring(firstSpaceIndex))
                            } else {
                                withStyle(
                                    SpanStyle(fontWeight = FontWeight.SemiBold)
                                ) {
                                    append(daysText)
                                }
                            }
                        },
                        fontSize = (14 * fontScale).sp,
                        color = MaterialTheme.colors.onBackground.copy(alpha = 0.88f)
                    )
                }

                Spacer(modifier = Modifier.padding(top = 6.dp))
                Text(
                    text = "${Locales.t("premium_subscription_auto_renew")}: ${
                        if (autoRenewEnabled) {
                            Locales.t("premium_subscription_auto_renew_on")
                        } else {
                            Locales.t("premium_subscription_auto_renew_off")
                        }
                    }",
                    fontSize = (14 * fontScale).sp,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.88f)
                )

                if (showCancelledButActiveNotice) {
                    Spacer(modifier = Modifier.padding(top = 10.dp))
                    Text(
                        text = "${Locales.t("premium_subscription_state_canceled")} • ${Locales.t("premium_subscription_expires")}: $expiryText",
                        fontSize = (13 * fontScale).sp,
                        color = MaterialTheme.colors.primary.copy(alpha = 0.90f),
                        lineHeight = (18 * fontScale).sp
                    )
                }

                Spacer(modifier = Modifier.padding(top = 24.dp))

                Text(
                    text = Locales.t("billing_account_binding_title"),
                    fontSize = (16 * fontScale).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colors.onBackground
                )

                Spacer(modifier = Modifier.padding(top = 8.dp))

                Text(
                    text = when {
                        isGuestUser ->
                            Locales.t("premium_guest_binding_impossible_message")

                        isPremiumActive ->
                            Locales.t("billing_account_binding_active_message")

                        else ->
                            Locales.t("billing_account_binding_message")
                    },
                    fontSize = (14 * fontScale).sp,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.85f),
                    lineHeight = (20 * fontScale).sp
                )

                Spacer(modifier = Modifier.padding(top = 4.dp))

                Text(
                    text = accountLabel,
                    fontSize = (15 * fontScale).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colors.primary
                )

                if (!isIos) {
                    Spacer(modifier = Modifier.padding(top = 6.dp))
                    Text(
                        text = Locales.t("billing_account_binding_google_play_note"),
                        fontSize = (13 * fontScale).sp,
                        color = MaterialTheme.colors.onBackground.copy(alpha = 0.72f),
                        lineHeight = (18 * fontScale).sp
                    )
                }

                Spacer(modifier = Modifier.padding(top = 8.dp))

                Text(
                    text = if (isIos) {
                        Locales.t("billing_store_note_ios")
                    } else {
                        Locales.t("billing_account_binding_google_play_note")
                    },
                    fontSize = (12 * fontScale).sp,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.65f),
                    lineHeight = (18 * fontScale).sp
                )

                Spacer(modifier = Modifier.padding(top = 6.dp))

                Text(
                    text = if (isIos) {
                        Locales.t("billing_privacy_notice_ios")
                    } else {
                        Locales.t("billing_privacy_notice")
                    },
                    fontSize = (12 * fontScale).sp,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.65f),
                    lineHeight = (18 * fontScale).sp
                )

                Spacer(modifier = Modifier.padding(top = 4.dp))

                Text(
                    text = Locales.t("billing_token_notice"),
                    fontSize = (12 * fontScale).sp,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.65f),
                    lineHeight = (18 * fontScale).sp
                )

                Spacer(modifier = Modifier.padding(top = 4.dp))

                Text(
                    text = Locales.t("billing_account_link_notice"),
                    fontSize = (12 * fontScale).sp,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.65f),
                    lineHeight = (18 * fontScale).sp
                )

                Spacer(modifier = Modifier.padding(top = 8.dp))

                Text(
                    text = if (isIos) {
                        Locales.t("billing_refund_info_message_ios")
                    } else {
                        Locales.t("billing_refund_info_message")
                    },
                    fontSize = (12 * fontScale).sp,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.65f),
                    lineHeight = (18 * fontScale).sp
                )

                Spacer(modifier = Modifier.padding(top = 10.dp))

                if (isGuestUser) {
                    Spacer(modifier = Modifier.padding(top = 14.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colors.primary.copy(alpha = 0.10f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = Locales.t("premium_guest_account_required_message"),
                            fontSize = (14 * fontScale).sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colors.primary.copy(alpha = 0.92f),
                            lineHeight = (20 * fontScale).sp
                        )
                    }
                }

                Spacer(modifier = Modifier.padding(top = 24.dp))

                if (isPremiumActive) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier
                                .widthIn(max = 420.dp)
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Locales.t("premium_already_owned"),
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(
                                            color = Color(0xFF2EAD62),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    monthlyProduct?.let { product ->
                        SubscriptionPlanButton(
                            title = Locales.t("premium_plan_monthly"),
                            price = product.formattedPrice,
                            onClick = { onUnlockPremium(product.productId) },
                            enabled = buyEnabled && product.offerToken.isNotBlank(),
                            fontScale = fontScale
                        )

                        Spacer(modifier = Modifier.padding(top = 10.dp))
                    }

                    yearlyProduct?.let { product ->
                        SubscriptionPlanButton(
                            title = Locales.t("premium_plan_yearly"),
                            price = product.formattedPrice,
                            onClick = { onUnlockPremium(product.productId) },
                            enabled = buyEnabled && product.offerToken.isNotBlank(),
                            fontScale = fontScale
                        )
                    }

                    if (premiumProducts.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier
                                    .widthIn(max = 420.dp)
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (
                                        billingUiState.status == BillingStatus.LOADING_PRODUCTS ||
                                        billingUiState.status == BillingStatus.CONNECTING
                                    ) {
                                        Locales.t("premium_loading_price")
                                    } else {
                                        Locales.t("premium_buy_btn")
                                    },
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.padding(top = 10.dp))

                SecondaryActionButton(
                    text = Locales.t("premium_restore_btn"),
                    onClick = onRestorePurchases,
                    enabled = !isGuestUser && billingUiState.status != BillingStatus.PURCHASING
                )

                if (!isPremiumActive && !isGuestUser) {
                    Spacer(modifier = Modifier.padding(top = 10.dp))

                    SecondaryActionButton(
                        text = Locales.t("premium_continue_free_btn"),
                        onClick = onContinueFree
                    )
                }

                Spacer(modifier = Modifier.padding(top = 20.dp))

                TextButton(
                    onClick = onOpenPrivacyPolicy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = Locales.t("privacy_policy"),
                        color = linkColor,
                        textDecoration = TextDecoration.Underline,
                        fontSize = (12 * fontScale).sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                TextButton(
                    onClick = {
                        StoreOpener.open(TERMS_OF_USE_URL)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = Locales.t("terms_of_use_beauty_planner"),
                        color = linkColor,
                        textDecoration = TextDecoration.Underline,
                        fontSize = (12 * fontScale).sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun SubscriptionPlanButton(
    title: String,
    price: String,
    onClick: () -> Unit,
    enabled: Boolean,
    fontScale: Float
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    fontSize = (15 * fontScale).sp,
                    modifier = Modifier.fillMaxWidth()
                )

                if (price.isNotBlank()) {
                    Text(
                        text = price,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        fontSize = (13 * fontScale).sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumBullet(
    text: String,
    fontScale: Float
) {
    Text(
        text = "• $text",
        fontSize = (15 * fontScale).sp,
        lineHeight = (22 * fontScale).sp,
        color = MaterialTheme.colors.onBackground,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}