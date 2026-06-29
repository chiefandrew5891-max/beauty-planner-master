package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
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
import com.andrey.beautyplanner.AccessState
import com.andrey.beautyplanner.AccessTier
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Locales
import com.andrey.beautyplanner.billing.BillingStatus
import com.andrey.beautyplanner.billing.BillingUiState
import com.andrey.beautyplanner.billing.PREMIUM_SUBS_PRODUCT_ID
import kotlinx.datetime.Clock

@Composable
fun PremiumAccessScreen(
    accessState: AccessState,
    message: String,
    billingUiState: BillingUiState,
    accountLabel: String,
    onBack: () -> Unit,
    onContinueFree: () -> Unit,
    onUnlockPremium: () -> Unit,
    onRestorePurchases: () -> Unit
) {
    val fontScale = AppSettings.getFontScale()

    val subtitle = when (accessState.tier) {
        AccessTier.TRIAL -> Locales.t("premium_trial_active_subtitle")
        AccessTier.FREE_LIMITED -> Locales.t("premium_free_limited_subtitle")
        AccessTier.PREMIUM -> Locales.t("premium_active_subtitle")
    }

    val premiumProduct = billingUiState.products.firstOrNull {
        it.productId == PREMIUM_SUBS_PRODUCT_ID
    }

    val isPremiumActive =
        billingUiState.ownedPremium || accessState.tier == AccessTier.PREMIUM

    val buyButtonText = when {
        isPremiumActive ->
            Locales.t("premium_already_owned")

        premiumProduct != null && premiumProduct.formattedPrice.isNotBlank() ->
            "${Locales.t("premium_buy_btn")} • ${premiumProduct.formattedPrice}"

        billingUiState.status == BillingStatus.LOADING_PRODUCTS ||
                billingUiState.status == BillingStatus.CONNECTING ->
            Locales.t("premium_loading_price")

        else ->
            Locales.t("premium_buy_btn")
    }

    val buyEnabled =
        !isPremiumActive &&
                billingUiState.status != BillingStatus.PURCHASING &&
                billingUiState.status != BillingStatus.RESTORING &&
                premiumProduct != null &&
                premiumProduct.offerToken.isNotBlank()

    val subscriptionStateText = subscriptionStateLabel(AppSettings.premiumSubscriptionState)
    val expiryMillis = AppSettings.premiumSubscriptionExpiryMillis
    val daysLeft = calculateSubscriptionDaysLeft(
        expiryMillis = expiryMillis,
        nowMillis = Clock.System.now().toEpochMilliseconds()
    )
    val expiryText = formatSubscriptionExpiry(expiryMillis)
    val autoRenewEnabled = AppSettings.premiumSubscriptionAutoRenewing
    val autoRenewText = if (autoRenewEnabled) {
        Locales.t("premium_subscription_auto_renew_on")
    } else {
        Locales.t("premium_subscription_auto_renew_off")
    }

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
                    fontSize = (15 * fontScale).sp,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.80f),
                    lineHeight = (22 * fontScale).sp
                )

                if (message.isNotBlank()) {
                    Spacer(modifier = Modifier.padding(top = 18.dp))
                    Text(
                        text = message,
                        fontSize = (15 * fontScale).sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.onBackground
                    )
                }

                if (!billingUiState.errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.padding(top = 16.dp))
                    Text(
                        text = billingUiState.errorMessage.orEmpty(),
                        fontSize = (14 * fontScale).sp,
                        color = MaterialTheme.colors.error
                    )
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
                PremiumBullet(Locales.t("premium_feature_backup"), fontScale)
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
                    text = "${Locales.t("premium_status_label")}: $subscriptionStateText",
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
                        text = "${Locales.t("premium_subscription_days_left")}: ${Locales.daysCount(daysLeft)}",
                        fontSize = (14 * fontScale).sp,
                        color = MaterialTheme.colors.onBackground.copy(alpha = 0.88f)
                    )
                }

                Spacer(modifier = Modifier.padding(top = 6.dp))
                Text(
                    text = "${Locales.t("premium_subscription_auto_renew")}: $autoRenewText",
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
                    text = Locales.t("billing_account_binding_message"),
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

                Spacer(modifier = Modifier.padding(top = 8.dp))

                Text(
                    text = Locales.t("billing_account_binding_google_play_note"),
                    fontSize = (12 * fontScale).sp,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.65f),
                    lineHeight = (18 * fontScale).sp
                )

                Spacer(modifier = Modifier.padding(top = 6.dp))

                Text(
                    text = Locales.t("billing_privacy_notice"),
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
                    text = Locales.t("billing_refund_info_message"),
                    fontSize = (12 * fontScale).sp,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.65f),
                    lineHeight = (18 * fontScale).sp
                )

                Spacer(modifier = Modifier.padding(top = 28.dp))

                PrimaryActionButton(
                    text = buyButtonText,
                    onClick = onUnlockPremium,
                    enabled = buyEnabled
                )

                Spacer(modifier = Modifier.padding(top = 10.dp))

                SecondaryActionButton(
                    text = Locales.t("premium_restore_btn"),
                    onClick = onRestorePurchases,
                    enabled = billingUiState.status != BillingStatus.PURCHASING
                )

                if (!isPremiumActive) {
                    Spacer(modifier = Modifier.padding(top = 10.dp))

                    SecondaryActionButton(
                        text = Locales.t("premium_continue_free_btn"),
                        onClick = onContinueFree
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