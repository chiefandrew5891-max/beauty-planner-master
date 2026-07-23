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

    fun getCachedAccessState(nowMillis: Long): AccessState {
        val tier = when (AppSettings.cachedAccessTier) {
            "PREMIUM" -> AccessTier.PREMIUM
            "TRIAL" -> AccessTier.TRIAL
            else -> AccessTier.FREE_LIMITED
        }

        val daysLeft = if (AppSettings.cachedTrialEndsAtMillis > nowMillis) {
            ceil(
                (AppSettings.cachedTrialEndsAtMillis - nowMillis).toDouble() /
                        (24 * 60 * 60 * 1000.0)
            ).toInt().coerceAtLeast(0)
        } else {
            0
        }

        val trialEndsAtMillis = AppSettings.cachedTrialEndsAtMillis
        return AccessState(
            tier = tier,
            trialStartedAtMillis = AppSettings.trialStartedAtMillis,
            trialEndsAtMillis = trialEndsAtMillis,
            isTrialActive = tier == AccessTier.TRIAL && trialEndsAtMillis > nowMillis,
            hasPremium = AppSettings.cachedHasPremium,
            trialDaysLeft = daysLeft
        )
    }
}