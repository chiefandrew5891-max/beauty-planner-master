package com.andrey.beautyplanner.remote

import kotlinx.coroutines.CompletableDeferred

actual object BackendBridge {

    actual suspend fun ensureAuthenticated(): String {
        val result = callBackendFunction(
            name = "__currentUser",
            payload = emptyMap()
        )

        val uid = result["uid"].orEmpty().trim()
        if (uid.isBlank()) {
            error("No authenticated Firebase user on iOS")
        }

        return uid
    }

    actual suspend fun checkAppUpdate(
        platform: String,
        versionName: String,
        buildNumber: String
    ): Map<String, String> {
        return callBackendFunction(
            name = "checkAppUpdate",
            payload = mapOf(
                "platform" to platform,
                "versionName" to versionName,
                "buildNumber" to buildNumber
            )
        )
    }

    actual suspend fun bootstrapUser(
        installId: String,
        firebaseUid: String,
        platform: String,
        authProvider: String,
        email: String,
        displayName: String
    ): AccessStatusResponse {
        ensureAuthenticated()

        return callAccessFunction(
            name = "bootstrapUser",
            payload = mapOf(
                "installId" to installId,
                "firebaseUid" to firebaseUid,
                "platform" to platform,
                "authProvider" to authProvider,
                "email" to email,
                "displayName" to displayName
            )
        )
    }

    actual suspend fun verifySubscription(
        userId: String,
        productId: String,
        purchaseToken: String,
        platform: String,
        transactionId: String
    ): AccessStatusResponse {
        ensureAuthenticated()

        return callAccessFunction(
            name = "verifySubscription",
            payload = mapOf(
                "userId" to userId,
                "productId" to productId,
                "purchaseToken" to purchaseToken,
                "platform" to platform,
                "transactionId" to transactionId
            )
        )
    }

    actual suspend fun getAccessStatus(userId: String): AccessStatusResponse {
        ensureAuthenticated()

        return callAccessFunction(
            name = "getAccessStatus",
            payload = mapOf(
                "userId" to userId
            )
        )
    }

    actual suspend fun syncIdentity(
        firebaseUid: String,
        email: String,
        displayName: String,
        authProvider: String
    ): AccessStatusResponse {
        ensureAuthenticated()

        return callAccessFunction(
            name = "syncIdentity",
            payload = mapOf(
                "firebaseUid" to firebaseUid,
                "email" to email,
                "displayName" to displayName,
                "authProvider" to authProvider
            )
        )
    }

    private suspend fun callAccessFunction(
        name: String,
        payload: Map<String, String>
    ): AccessStatusResponse {
        val result = callBackendFunction(name, payload)

        return AccessStatusResponse(
            userId = result["userId"].orEmpty(),
            tier = result["tier"] ?: "FREE_LIMITED",
            trialStartedAtMillis = result["trialStartedAtMillis"]?.toLongOrNull() ?: 0L,
            trialEndsAtMillis = result["trialEndsAtMillis"]?.toLongOrNull() ?: 0L,
            isTrialActive = result["isTrialActive"].toBooleanStrictOrFalse(),
            hasPremium = result["hasPremium"].toBooleanStrictOrFalse(),
            trialDaysLeft = result["trialDaysLeft"]?.toIntOrNull() ?: 0,
            subscriptionState = result["subscriptionState"] ?: "NONE",
            premiumProductId = result["premiumProductId"].orEmpty(),
            subscriptionExpiryMillis = result["subscriptionExpiryMillis"]?.toLongOrNull() ?: 0L,
            subscriptionAutoRenewing = result["subscriptionAutoRenewing"].toBooleanStrictOrFalse(),
            subscriptionOrderId = result["subscriptionOrderId"].orEmpty()
        )
    }

    private suspend fun callBackendFunction(
        name: String,
        payload: Map<String, String>
    ): Map<String, String> {
        val deferred = CompletableDeferred<Map<String, String>>()
        val caller = BackendBridgeConnector.callBackend
            ?: error("iOS backend bridge is not connected.")

        caller.invoke(name, payload, deferred)
        return deferred.await()
    }

    private fun String?.toBooleanStrictOrFalse(): Boolean {
        return this?.equals("true", ignoreCase = true) == true
    }
}