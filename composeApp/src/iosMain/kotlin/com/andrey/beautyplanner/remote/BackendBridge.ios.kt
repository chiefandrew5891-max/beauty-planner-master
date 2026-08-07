package com.andrey.beautyplanner.remote

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

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

    actual suspend fun syncMyMasterProfile(
        ownerName: String,
        profileDisplayCustomName: Boolean,
        profilePhone: String,
        profilePhoneVisible: Boolean,
        profileSpecialization: String,
        profileRating: Float,
        profileAvatarUrl: String,
        profileAvatarBase64: String,
        clientInteractionsEnabled: Boolean,
        serviceTemplatesJson: String,
        weeklyBlockedIntervalsJson: String,
        scheduleDateOverridesJson: String
    ): Map<String, String> {
        ensureAuthenticated()

        return callBackendFunction(
            name = "syncMyMasterProfile",
            payload = mapOf(
                "ownerName" to ownerName,
                "profileDisplayCustomName" to profileDisplayCustomName.toString(),
                "profilePhone" to profilePhone,
                "profilePhoneVisible" to profilePhoneVisible.toString(),
                "profileSpecialization" to profileSpecialization,
                "profileRating" to profileRating.toString(),
                "profileAvatarUrl" to profileAvatarUrl,
                "profileAvatarBase64" to profileAvatarBase64,
                "clientInteractionsEnabled" to clientInteractionsEnabled.toString(),
                "serviceTemplatesJson" to serviceTemplatesJson,
                "weeklyBlockedIntervalsJson" to weeklyBlockedIntervalsJson,
                "scheduleDateOverridesJson" to scheduleDateOverridesJson
            )
        )
    }

    actual suspend fun getMyMasterProfile(): MasterProfilePayload {
        ensureAuthenticated()

        val result = callBackendFunction(
            name = "getMyMasterProfile",
            payload = emptyMap()
        )

        val templates = parseServiceTemplates(result["serviceTemplates"])

        return MasterProfilePayload(
            found = result["found"]?.toString() == "true",
            firebaseUid = result["firebaseUid"]?.toString().orEmpty(),
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
            weeklyBlockedIntervalsJson = result["weeklyBlockedIntervalsJson"]?.toString().orEmpty(),
            scheduleDateOverridesJson = result["scheduleDateOverridesJson"]?.toString().orEmpty(),
            updatedAt = result["updatedAt"]?.toString()?.toLongOrNull() ?: 0L,
            createdAt = result["createdAt"]?.toString()?.toLongOrNull() ?: 0L
        )
    }

    actual suspend fun syncMyPublicSchedule(
        autoPublishBusySlots: Boolean,
        busySlotsJson: String
    ): Map<String, String> {
        ensureAuthenticated()

        return callBackendFunction(
            name = "syncMyPublicSchedule",
            payload = mapOf(
                "autoPublishBusySlots" to autoPublishBusySlots.toString(),
                "busySlotsJson" to busySlotsJson
            )
        )
    }

    actual suspend fun validateCurrentSession(): Map<String, String> {
        ensureAuthenticated()

        return callBackendFunction(
            name = "validateCurrentSession",
            payload = emptyMap()
        )
    }

    actual suspend fun deleteMyAccount(): Map<String, String> {
        ensureAuthenticated()

        return callBackendFunction(
            name = "deleteMyAccount",
            payload = emptyMap()
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

        return withTimeout(8000) {
            deferred.await()
        }
    }

    private fun parseServiceTemplates(raw: String?): List<MasterServiceTemplatePayload> {
        if (raw.isNullOrBlank()) return emptyList()

        val trimmed = raw.trim()
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return emptyList()
        }

        return runCatching {
            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
            }

            val root = json.parseToJsonElement(trimmed)
            val array = root as? JsonArray ?: return emptyList()

            array.mapNotNull { item ->
                val obj = item as? JsonObject ?: return@mapNotNull null

                val id = obj["id"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val title = obj["title"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val defaultPrice = obj["defaultPrice"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val isActive =
                    obj["isActive"]?.jsonPrimitive?.booleanOrNull
                        ?: obj["isActive"]?.jsonPrimitive?.contentOrNull?.equals("true", ignoreCase = true)
                        ?: false

                if (id.isBlank() || title.isBlank()) return@mapNotNull null

                MasterServiceTemplatePayload(
                    id = id,
                    title = title,
                    defaultPrice = defaultPrice,
                    isActive = isActive
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun String?.toBooleanStrictOrFalse(): Boolean {
        return this?.equals("true", ignoreCase = true) == true
    }
}