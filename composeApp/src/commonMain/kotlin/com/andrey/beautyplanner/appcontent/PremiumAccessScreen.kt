package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import com.andrey.beautyplanner.billing.PREMIUM_LIFETIME_PRODUCT_ID

@Composable
fun PremiumAccessScreen(
    accessState: AccessState,
    message: String,
    billingUiState: BillingUiState,
    onBack: () -> Unit,
    onContinueFree: () -> Unit,
    onUnlockPremium: () -> Unit,
    onRestorePurchases: () -> Unit
) {
    val fontScale = AppSettings.getFontScale()

    val title = when (accessState.tier) {
        AccessTier.TRIAL -> Locales.t("premium_access_title")
        AccessTier.FREE_LIMITED -> Locales.t("premium_upgrade_title")
        AccessTier.PREMIUM -> Locales.t("premium_active_title")
    }

    val subtitle = when (accessState.tier) {
        AccessTier.TRIAL -> Locales.t("premium_trial_active_subtitle")
        AccessTier.FREE_LIMITED -> Locales.t("premium_free_limited_subtitle")
        AccessTier.PREMIUM -> Locales.t("premium_active_subtitle")
    }

    val premiumProduct = billingUiState.products.firstOrNull {
        it.productId == PREMIUM_LIFETIME_PRODUCT_ID
    }

    val buyButtonText = when {
        billingUiState.ownedPremium || accessState.tier == AccessTier.PREMIUM ->
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
        !billingUiState.ownedPremium &&
                accessState.tier != AccessTier.PREMIUM &&
                billingUiState.status != BillingStatus.PURCHASING &&
                billingUiState.status != BillingStatus.RESTORING &&
                premiumProduct != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 12.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Reply,
                    contentDescription = Locales.t("cd_back"),
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = title,
                color = MaterialTheme.colors.onBackground,
                fontSize = (20 * fontScale).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
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

            Spacer(modifier = Modifier.padding(top = 28.dp))

            Button(
                onClick = onUnlockPremium,
                enabled = buyEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Text(
                    text = buyButtonText,
                    fontSize = (15 * fontScale).sp
                )
            }

            Spacer(modifier = Modifier.padding(top = 10.dp))

            OutlinedButton(
                onClick = onRestorePurchases,
                enabled = billingUiState.status != BillingStatus.PURCHASING,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = Locales.t("premium_restore_btn"),
                    fontSize = (15 * fontScale).sp
                )
            }

            Spacer(modifier = Modifier.padding(top = 10.dp))

            OutlinedButton(
                onClick = onContinueFree,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = Locales.t("premium_continue_free_btn"),
                    fontSize = (15 * fontScale).sp
                )
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