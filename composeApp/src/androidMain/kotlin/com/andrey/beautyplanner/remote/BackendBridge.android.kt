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