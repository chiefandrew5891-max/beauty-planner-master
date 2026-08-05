package com.andrey.beautyplanner.access

import com.andrey.beautyplanner.AccessState
import com.andrey.beautyplanner.AccessTier
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.remote.AccessStatusResponse
import kotlin.math.ceil

object AccessRepository {
    fun applyRemoteStatus(remote: AccessStatusResponse) {
        AppSettings.backendUserId = remote.userId
        AppSettings.trialStartedAtMillis = remote.trialStartedAtMillis
        AppSettings.premiumSubscriptionState = remote.subscriptionState
        AppSettings.premiumSubscribedProductId = remote.premiumProductId
        AppSettings.premiumSubscriptionExpiryMillis = remote.subscriptionExpiryMillis
        AppSettings.premiumSubscriptionAutoRenewing = remote.subscriptionAutoRenewing
        AppSettings.premiumOrderId = remote.subscriptionOrderId
        AppSettings.cachedAccessTier = remote.tier
        AppSettings.cachedTrialEndsAtMillis = remote.trialEndsAtMillis
        AppSettings.cachedHasPremium = remote.hasPremium
        AppSettings.cachedSubscriptionState = remote.subscriptionState
        AppSettings.persist()
    }

    fun applyLocalPremiumFallback(
        productId: String,
        subscriptionState: String,
        expiryMillis: Long,
        autoRenewing: Boolean
    ) {
        AppSettings.premiumSubscriptionState = subscriptionState
        AppSettings.premiumSubscribedProductId = productId
        AppSettings.premiumSubscriptionExpiryMillis = expiryMillis
        AppSettings.premiumSubscriptionAutoRenewing = autoRenewing

        AppSettings.cachedAccessTier = "PREMIUM"
        AppSettings.cachedHasPremium = true
        AppSettings.cachedSubscriptionState = subscriptionState

        AppSettings.persist()
    }

    fun getCachedAccessState(nowMillis: Long): AccessState {
        val hasActiveSubscriptionState = AppSettings.premiumSubscriptionState == "ACTIVE"

        val hasEffectivePremium =
            AppSettings.cachedHasPremium ||
                    AppSettings.cachedAccessTier == "PREMIUM" ||
                    hasActiveSubscriptionState

        val trialEndsAtMillis = AppSettings.cachedTrialEndsAtMillis

        val daysLeft = if (trialEndsAtMillis > nowMillis) {
            ceil(
                (trialEndsAtMillis - nowMillis).toDouble() /
                        (24 * 60 * 60 * 1000.0)
            ).toInt().coerceAtLeast(0)
        } else {
            0
        }

        val tier = when {
            hasEffectivePremium -> AccessTier.PREMIUM
            AppSettings.cachedAccessTier == "TRIAL" -> AccessTier.TRIAL
            else -> AccessTier.FREE_LIMITED
        }

        val isTrialActive =
            !hasEffectivePremium &&
                    tier == AccessTier.TRIAL &&
                    trialEndsAtMillis > nowMillis

        return AccessState(
            tier = tier,
            trialStartedAtMillis = AppSettings.trialStartedAtMillis,
            trialEndsAtMillis = trialEndsAtMillis,
            isTrialActive = isTrialActive,
            hasPremium = hasEffectivePremium,
            trialDaysLeft = if (hasEffectivePremium) 0 else daysLeft
        )
    }
}