package com.andrey.beautyplanner.remote

import com.google.firebase.auth.ktx.auth
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

actual object BackendBridge {
    actual suspend fun ensureAuthenticated(): String {
        val auth = Firebase.auth
        val current = auth.currentUser
        if (current != null) return current.uid

        return suspendCancellableCoroutine { cont ->
            auth.signInAnonymously()
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid
                    if (uid.isNullOrBlank()) {
                        cont.resumeWithException(
                            IllegalStateException("Anonymous auth returned empty uid")
                        )
                    } else {
                        cont.resume(uid)
                    }
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }
    }
    actual suspend fun checkAppUpdate(
        platform: String,
        versionName: String,
        buildNumber: String
    ): Map<String, String> {
        val functions = Firebase.functions

        return suspendCancellableCoroutine { cont ->
            functions
                .getHttpsCallable("checkAppUpdate")
                .call(
                    mapOf(
                        "platform" to platform,
                        "versionName" to versionName,
                        "buildNumber" to buildNumber
                    )
                )
                .addOnSuccessListener { result ->
                    try {
                        val map = result.data as? Map<*, *>
                            ?: throw IllegalStateException("checkAppUpdate returned non-map result")

                        val parsed = map.entries.associate { (key, value) ->
                            key.toString() to (value?.toString() ?: "")
                        }

                        cont.resume(parsed)
                    } catch (e: Exception) {
                        cont.resumeWithException(e)
                    }
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }
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
        return callFunction(
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
    }

    actual suspend fun verifySubscription(
        userId: String,
        productId: String,
        purchaseToken: String,
        platform: String,
        transactionId: String
    ): AccessStatusResponse {
        ensureAuthenticated()
        return callFunction(
            "verifySubscription",
            mapOf(
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
        return callFunction("getAccessStatus", mapOf("userId" to userId))
    }

    actual suspend fun syncIdentity(
        firebaseUid: String,
        email: String,
        displayName: String,
        authProvider: String
    ): AccessStatusResponse {
        ensureAuthenticated()
        return callFunction(
            "syncIdentity",
            mapOf(
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
        return callRawFunction(
            "syncMasterProfile",
            mapOf(
                "userId" to userId,
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

    actual suspend fun getMasterProfile(
        userId: String
    ): MasterProfilePayload {
        ensureAuthenticated()

        val functions = Firebase.functions

        return suspendCancellableCoroutine { cont ->
            functions
                .getHttpsCallable("getMasterProfile")
                .call(mapOf("userId" to userId))
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
                            userId = map["userId"]?.toString().orEmpty(),
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
                            updatedAt = map["updatedAt"]?.toString()?.toLongOrNull() ?: 0L
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

    private suspend fun callRawFunction(
        name: String,
        data: Map<String, Any?>
    ): Map<String, String> {
        val functions = Firebase.functions

        return suspendCancellableCoroutine { cont ->
            functions
                .getHttpsCallable(name)
                .call(data)
                .addOnSuccessListener { result ->
                    try {
                        val map = result.data as? Map<*, *> ?: emptyMap<Any?, Any?>()
                        val parsed = map.entries.associate { (key, value) ->
                            key.toString() to (value?.toString() ?: "")
                        }
                        cont.resume(parsed)
                    } catch (e: Exception) {
                        cont.resumeWithException(e)
                    }
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }
    }

    private suspend fun callFunction(
        name: String,
        data: Any
    ): AccessStatusResponse {
        val functions = Firebase.functions

        return suspendCancellableCoroutine { cont ->
            functions
                .getHttpsCallable(name)
                .call(data)
                .addOnSuccessListener { result ->
                    try {
                        val map = result.data as? Map<*, *>
                            ?: throw IllegalStateException("Function $name returned non-map result")

                        val parsed = AccessStatusResponse(
                            userId = map["userId"] as? String ?: "",
                            tier = map["tier"] as? String ?: "FREE_LIMITED",
                            trialStartedAtMillis = (map["trialStartedAtMillis"] as? Number)?.toLong() ?: 0L,
                            trialEndsAtMillis = (map["trialEndsAtMillis"] as? Number)?.toLong() ?: 0L,
                            isTrialActive = map["isTrialActive"] as? Boolean ?: false,
                            hasPremium = map["hasPremium"] as? Boolean ?: false,
                            trialDaysLeft = (map["trialDaysLeft"] as? Number)?.toInt() ?: 0,
                            subscriptionState = map["subscriptionState"] as? String ?: "NONE",
                            premiumProductId = map["premiumProductId"] as? String ?: "",
                            subscriptionExpiryMillis = (map["subscriptionExpiryMillis"] as? Number)?.toLong() ?: 0L,
                            subscriptionAutoRenewing = map["subscriptionAutoRenewing"] as? Boolean ?: false,
                            subscriptionOrderId = map["subscriptionOrderId"] as? String ?: ""
                        )
                        cont.resume(parsed)
                    } catch (e: Exception) {
                        cont.resumeWithException(e)
                    }
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }
    }
}