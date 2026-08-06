package com.andrey.beautyplanner.access

import com.andrey.beautyplanner.AccessState
import com.andrey.beautyplanner.AccessTier
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.remote.AccessStatusResponse
import kotlin.math.ceil

object AccessRepository {
    fun applyRemoteStatus(
        remote: AccessStatusResponse,
        currentAuthUserId: String?
    ) {
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

        if (remote.hasPremium || remote.tier == "PREMIUM" || remote.subscriptionState == "ACTIVE") {
            AppSettings.premiumLastOwnerAuthUserId = currentAuthUserId.orEmpty()
            AppSettings.premiumLastOwnerBackendUserId = remote.userId
        }

        AppSettings.localPremiumFallbackBlocked = false
        AppSettings.localPremiumFallbackPendingSync = false
        AppSettings.persist()
    }

    fun canApplyLocalPremiumFallback(
        currentAuthUserId: String?,
        currentBackendUserId: String?
    ): Boolean {
        val authUserId = currentAuthUserId.orEmpty()
        val backendUserId = currentBackendUserId.orEmpty()

        if (AppSettings.localPremiumFallbackBlocked) return false
        if (authUserId.isBlank()) return false

        val lastOwnerAuth = AppSettings.premiumLastOwnerAuthUserId
        val lastOwnerBackend = AppSettings.premiumLastOwnerBackendUserId

        val sameAuthUser =
            lastOwnerAuth.isNotBlank() && lastOwnerAuth == authUserId

        val sameBackendUser =
            backendUserId.isNotBlank() &&
                    lastOwnerBackend.isNotBlank() &&
                    lastOwnerBackend == backendUserId

        return sameAuthUser || sameBackendUser
    }

    fun applyLocalPremiumFallback(
        currentAuthUserId: String?,
        currentBackendUserId: String?,
        productId: String,
        subscriptionState: String,
        expiryMillis: Long,
        autoRenewing: Boolean
    ): Boolean {
        if (!canApplyLocalPremiumFallback(currentAuthUserId, currentBackendUserId)) {
            return false
        }

        AppSettings.premiumSubscriptionState = subscriptionState
        AppSettings.premiumSubscribedProductId = productId
        AppSettings.premiumSubscriptionExpiryMillis = expiryMillis
        AppSettings.premiumSubscriptionAutoRenewing = autoRenewing

        AppSettings.cachedAccessTier = "PREMIUM"
        AppSettings.cachedHasPremium = true
        AppSettings.cachedSubscriptionState = subscriptionState

        AppSettings.localPremiumFallbackPendingSync = true
        AppSettings.persist()
        return true
    }

    fun clearLocalPremiumState(blockAutoFallback: Boolean = false) {
        AppSettings.premiumSubscriptionState = "NONE"
        AppSettings.premiumSubscribedProductId = ""
        AppSettings.premiumSubscriptionToken = ""
        AppSettings.premiumSubscriptionStartMillis = 0L
        AppSettings.premiumSubscriptionExpiryMillis = 0L
        AppSettings.premiumSubscriptionAutoRenewing = false
        AppSettings.premiumOrderId = ""
        AppSettings.premiumLastVerifiedAtMillis = 0L

        AppSettings.cachedAccessTier = "FREE_LIMITED"
        AppSettings.cachedHasPremium = false
        AppSettings.cachedSubscriptionState = "NONE"

        AppSettings.localPremiumFallbackPendingSync = false

        if (blockAutoFallback) {
            AppSettings.localPremiumFallbackBlocked = true
            AppSettings.premiumLastOwnerAuthUserId = ""
            AppSettings.premiumLastOwnerBackendUserId = ""
        }

        AppSettings.persist()
    }

    fun markPremiumOwnership(
        currentAuthUserId: String?,
        currentBackendUserId: String?
    ) {
        AppSettings.premiumLastOwnerAuthUserId = currentAuthUserId.orEmpty()
        AppSettings.premiumLastOwnerBackendUserId = currentBackendUserId.orEmpty()
        AppSettings.localPremiumFallbackBlocked = false
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