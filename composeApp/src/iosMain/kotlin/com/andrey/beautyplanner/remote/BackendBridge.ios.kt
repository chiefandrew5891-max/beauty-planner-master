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

    actual suspend fun syncMasterProfile(
        userId: String,
        ownerName: String,
        profileDisplayCustomName: Boolean,
        profilePhone: String,
        profilePhoneVisible: Boolean,
        profileSpecialization: String,
        profileRating: Float,
        profileAvatarUrl: String,
        profileAvatarBase64: String,
        clientInteractionsEnabled: Boolean,
        serviceTemplatesJson: String
    ): Map<String, String> {
        ensureAuthenticated()
        return callBackendFunction(
            name = "syncMasterProfile",
            payload = mapOf(
                "userId" to userId,
                "ownerName" to ownerName,
                "profileDisplayCustomName" to profileDisplayCustomName.toString(),
                "profilePhone" to profilePhone,
                "profilePhoneVisible" to profilePhoneVisible.toString(),
                "profileSpecialization" to profileSpecialization,
                "profileRating" to profileRating.toString(),
                "profileAvatarUrl" to profileAvatarUrl,
                "profileAvatarBase64" to profileAvatarBase64,
                "clientInteractionsEnabled" to clientInteractionsEnabled.toString(),
                "serviceTemplatesJson" to serviceTemplatesJson
            )
        )
    }

    actual suspend fun getMasterProfile(
        userId: String
    ): MasterProfilePayload {
        ensureAuthenticated()

        val result = callBackendFunction(
            name = "getMasterProfile",
            payload = mapOf("userId" to userId)
        )

        val rawTemplates = result["serviceTemplates"] as? List<*> ?: emptyList<Any?>()
        val templates = rawTemplates.mapNotNull { item ->
            val entry = item as? Map<*, *> ?: return@mapNotNull null
            val id = entry["id"]?.toString().orEmpty()
            val title = entry["title"]?.toString().orEmpty()
            val defaultPrice = entry["defaultPrice"]?.toString().orEmpty()
            val isActive = when (val raw = entry["isActive"]) {
                is Boolean -> raw
                is String -> raw.equals("true", ignoreCase = true)
                is Number -> raw.toInt() != 0
                else -> false
            }

            if (id.isBlank() || title.isBlank()) return@mapNotNull null

            MasterServiceTemplatePayload(
                id = id,
                title = title,
                defaultPrice = defaultPrice,
                isActive = isActive
            )
        }

        return MasterProfilePayload(
            found = result["found"]?.toString() == "true",
            userId = result["userId"]?.toString().orEmpty(),
            ownerName = result["ownerName"]?.toString().orEmpty(),
            profileDisplayCustomName = result["profileDisplayCustomName"]?.toString() == "true",
            profilePhone = result["profilePhone"]?.toString().orEmpty(),
            profilePhoneVisible = result["profilePhoneVisible"]?.toString() == "true",
            profileSpecialization = result["profileSpecialization"]?.toString().orEmpty(),
            profileRating = result["profileRating"]?.toString()?.toFloatOrNull() ?: 0f,
            profileAvatarUrl = result["profileAvatarUrl"]?.toString().orEmpty(),
            profileAvatarBase64 = result["profileAvatarBase64"]?.toString().orEmpty(),
            clientInteractionsEnabled = result["clientInteractionsEnabled"]?.toString() == "true",
            serviceTemplates = templates,
            updatedAt = result["updatedAt"]?.toString()?.toLongOrNull() ?: 0L
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