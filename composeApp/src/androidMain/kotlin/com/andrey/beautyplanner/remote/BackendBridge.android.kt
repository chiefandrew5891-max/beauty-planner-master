package com.andrey.beautyplanner.remote

import com.google.firebase.Firebase
import com.google.firebase.functions.functions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

actual object BackendBridge {
    actual suspend fun ensureAuthenticated(): String {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        return user?.uid ?: throw IllegalStateException("User is not authenticated.")
    }

    actual suspend fun checkAppUpdate(
        platform: String,
        versionName: String,
        buildNumber: String
    ): Map<String, String> {
        return callRawFunction(
            "checkAppUpdate",
            mapOf(
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
        val result = callRawFunction(
            "bootstrapUser",
            mapOf(
                "installId" to installId,
                "firebaseUid" to firebaseUid,
                "platform" to platform,
                "authProvider" to authProvider,
                "email" to email,
                "displayName" to displayName
            )
        )
        return result.toAccessStatusResponse()
    }

    actual suspend fun verifySubscription(
        userId: String,
        productId: String,
        purchaseToken: String,
        platform: String,
        transactionId: String
    ): AccessStatusResponse {
        val result = callRawFunction(
            "verifySubscription",
            mapOf(
                "userId" to userId,
                "productId" to productId,
                "purchaseToken" to purchaseToken,
                "platform" to platform,
                "transactionId" to transactionId
            )
        )
        return result.toAccessStatusResponse()
    }

    actual suspend fun getAccessStatus(userId: String): AccessStatusResponse {
        val result = callRawFunction(
            "getAccessStatus",
            mapOf("userId" to userId)
        )
        return result.toAccessStatusResponse()
    }

    actual suspend fun syncIdentity(
        firebaseUid: String,
        email: String,
        displayName: String,
        authProvider: String
    ): AccessStatusResponse {
        val result = callRawFunction(
            "syncIdentity",
            mapOf(
                "firebaseUid" to firebaseUid,
                "email" to email,
                "displayName" to displayName,
                "authProvider" to authProvider
            )
        )
        return result.toAccessStatusResponse()
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
        serviceTemplatesJson: String
    ): Map<String, String> {
        ensureAuthenticated()
        return callRawFunction(
            "syncMyMasterProfile",
            mapOf(
                "ownerName" to ownerName,
                "profileDisplayCustomName" to profileDisplayCustomName,
                "profilePhone" to profilePhone,
                "profilePhoneVisible" to profilePhoneVisible,
                "profileSpecialization" to profileSpecialization,
                "profileRating" to profileRating,
                "profileAvatarUrl" to profileAvatarUrl,
                "profileAvatarBase64" to profileAvatarBase64,
                "clientInteractionsEnabled" to clientInteractionsEnabled,
                "serviceTemplatesJson" to serviceTemplatesJson
            )
        )
    }

    actual suspend fun getMyMasterProfile(): MasterProfilePayload {
        ensureAuthenticated()

        val functions = Firebase.functions

        return suspendCancellableCoroutine { cont ->
            functions
                .getHttpsCallable("getMyMasterProfile")
                .call(emptyMap<String, Any>())
                .addOnSuccessListener { result ->
                    try {
                        val map = result.data as? Map<*, *> ?: emptyMap<Any?, Any?>()

                        val rawTemplates = map["serviceTemplates"] as? List<*> ?: emptyList<Any?>()
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

                        val payload = MasterProfilePayload(
                            found = map["found"]?.toString() == "true",
                            firebaseUid = map["firebaseUid"]?.toString().orEmpty(),
                            ownerName = map["ownerName"]?.toString().orEmpty(),
                            profileDisplayCustomName = map["profileDisplayCustomName"]?.toString() == "true",
                            profilePhone = map["profilePhone"]?.toString().orEmpty(),
                            profilePhoneVisible = map["profilePhoneVisible"]?.toString() == "true",
                            profileSpecialization = map["profileSpecialization"]?.toString().orEmpty(),
                            profileRating = map["profileRating"]?.toString()?.toFloatOrNull() ?: 0f,
                            profileAvatarUrl = map["profileAvatarUrl"]?.toString().orEmpty(),
                            profileAvatarBase64 = map["profileAvatarBase64"]?.toString().orEmpty(),
                            clientInteractionsEnabled = map["clientInteractionsEnabled"]?.toString() == "true",
                            serviceTemplates = templates,
                            updatedAt = map["updatedAt"]?.toString()?.toLongOrNull() ?: 0L,
                            createdAt = map["createdAt"]?.toString()?.toLongOrNull() ?: 0L
                        )

                        cont.resume(payload)
                    } catch (e: Exception) {
                        cont.resumeWithException(e)
                    }
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }
    }

    actual suspend fun syncMyPublicSchedule(
        autoPublishBusySlots: Boolean,
        busySlotsJson: String
    ): Map<String, String> {
        ensureAuthenticated()
        return callRawFunction(
            "syncMyPublicSchedule",
            mapOf(
                "autoPublishBusySlots" to autoPublishBusySlots,
                "busySlotsJson" to busySlotsJson
            )
        )
    }

    actual suspend fun deleteMyAccount(): Map<String, String> {
        ensureAuthenticated()
        return callRawFunction(
            "deleteMyAccount",
            emptyMap<String, Any>()
        )
    }
    private suspend fun callRawFunction(
        name: String,
        payload: Map<String, Any?>
    ): Map<String, String> {
        val functions = Firebase.functions

        return suspendCancellableCoroutine { cont ->
            functions
                .getHttpsCallable(name)
                .call(payload)
                .addOnSuccessListener { result ->
                    try {
                        val map = result.data as? Map<*, *> ?: emptyMap<Any?, Any?>()
                        val normalized = buildMap<String, String> {
                            map.forEach { (key, value) ->
                                val normalizedKey = key?.toString().orEmpty()
                                if (normalizedKey.isNotBlank()) {
                                    put(normalizedKey, value?.toString().orEmpty())
                                }
                            }
                        }
                        cont.resume(normalized)
                    } catch (e: Exception) {
                        cont.resumeWithException(e)
                    }
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }
    }

    private fun Map<String, String>.toAccessStatusResponse(): AccessStatusResponse {
        return AccessStatusResponse(
            userId = this["userId"].orEmpty(),
            tier = this["tier"].orEmpty(),
            trialStartedAtMillis = this["trialStartedAtMillis"]?.toLongOrNull() ?: 0L,
            trialEndsAtMillis = this["trialEndsAtMillis"]?.toLongOrNull() ?: 0L,
            isTrialActive = this["isTrialActive"] == "true",
            hasPremium = this["hasPremium"] == "true",
            trialDaysLeft = this["trialDaysLeft"]?.toIntOrNull() ?: 0,
            subscriptionState = this["subscriptionState"].orEmpty(),
            premiumProductId = this["premiumProductId"].orEmpty(),
            subscriptionExpiryMillis = this["subscriptionExpiryMillis"]?.toLongOrNull() ?: 0L,
            subscriptionAutoRenewing = this["subscriptionAutoRenewing"] == "true",
            subscriptionOrderId = this["subscriptionOrderId"].orEmpty()
        )
    }
}