package com.andrey.beautyplanner.auth

import com.andrey.beautyplanner.Locales
import com.andrey.beautyplanner.remote.BackendBridgeConnector
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

actual object AuthGateway {

    actual suspend fun getCurrentUser(): AuthUser? {
        val caller = BackendBridgeConnector.callBackend ?: return null
        val deferred = CompletableDeferred<Map<String, String>>()

        return try {
            caller.invoke("__currentUser", emptyMap(), deferred)

            val result = withTimeoutOrNull(5000) {
                deferred.await()
            } ?: return null

            val uid = result["uid"].orEmpty().trim()
            if (uid.isBlank()) return null

            val providerRaw = result["provider"].orEmpty().trim().uppercase()
            val provider = when (providerRaw) {
                "GOOGLE" -> SignInProvider.GOOGLE
                "EMAIL" -> SignInProvider.EMAIL
                "APPLE" -> SignInProvider.APPLE
                "ANONYMOUS" -> SignInProvider.ANONYMOUS
                else -> SignInProvider.ANONYMOUS
            }

            AuthUser(
                uid = uid,
                provider = provider,
                email = result["email"].orEmpty(),
                displayName = result["displayName"].orEmpty()
            )
        } catch (_: Throwable) {
            null
        }
    }

    actual suspend fun signInAnonymously(): SignInResult {
        val existing = getCurrentUser()
        if (existing != null) return SignInResult.Success(existing)

        val bridge = AnonymousAuthBridgeConnector.signIn
            ?: return SignInResult.Error("iOS anonymous auth bridge is not connected.")

        return try {
            val deferred = CompletableDeferred<Map<String, String>>()
            bridge.invoke(deferred)
            val result = deferred.await()

            val user = mapUser(result)
            SignInResult.Success(user)
        } catch (t: Throwable) {
            SignInResult.Error(t.message ?: "Anonymous sign-in failed on iOS.")
        }
    }

    actual suspend fun signInWithGoogle(): SignInResult {
        runCatching { prepareForNewSignIn() }

        val deferred = CompletableDeferred<SignInResult>()
        val launcher = GoogleSignInBridge.startGoogleSignIn
            ?: return SignInResult.Error("Google Sign-In bridge is not connected.")

        return try {
            launcher.invoke(deferred)
            deferred.await()
        } catch (t: Throwable) {
            SignInResult.Error(t.message ?: "Google sign-in failed on iOS.")
        }
    }

    actual suspend fun signInWithApple(): SignInResult {
        val deferred = CompletableDeferred<SignInResult>()
        val launcher = AppleSignInBridge.startAppleSignIn
            ?: return SignInResult.Error("Apple Sign-In bridge is not connected.")

        return try {
            launcher.invoke(deferred)
            deferred.await()
        } catch (t: Throwable) {
            SignInResult.Error(t.message ?: "Apple sign-in failed on iOS.")
        }
    }

    actual suspend fun signInWithEmail(email: String, password: String): SignInResult {
        val bridge = EmailAuthBridgeConnector.signIn
            ?: return SignInResult.Error("iOS email auth bridge is not connected.")

        val reloadBridge = EmailAuthBridgeConnector.reloadCurrentUser
            ?: return SignInResult.Error("iOS reload current user bridge is not connected.")

        return try {
            val deferred = CompletableDeferred<Map<String, String>>()
            bridge.invoke(email.trim(), password, deferred)
            deferred.await()

            val reloadDeferred = CompletableDeferred<Map<String, String>>()
            reloadBridge.invoke(reloadDeferred)
            val reloaded = reloadDeferred.await()

            val verified = reloaded["isEmailVerified"].equals("true", ignoreCase = true)

            if (!verified) {
                signOut()
                return SignInResult.Error("auth_email_not_verified")
            }

            val user = mapUser(reloaded)
            SignInResult.Success(user)
        } catch (t: Throwable) {
            SignInResult.Error(t.message ?: "Email sign-in failed on iOS.")
        }
    }

    actual suspend fun registerWithEmail(email: String, password: String): SignInResult {
        val registerBridge = EmailAuthBridgeConnector.registerUser
            ?: return SignInResult.Error("iOS email auth bridge is not connected.")

        val verificationBridge = EmailAuthBridgeConnector.sendEmailVerification
            ?: return SignInResult.Error("iOS email verification bridge is not connected.")

        return try {
            val deferred = CompletableDeferred<Map<String, String>>()
            registerBridge.invoke(email.trim(), password, deferred)
            val result = deferred.await()

            val verificationDeferred = CompletableDeferred<Map<String, String>>()
            verificationBridge.invoke(verificationDeferred)
            verificationDeferred.await()

            val caller = BackendBridgeConnector.callBackend
            if (caller != null) {
                val signOutDeferred = CompletableDeferred<Map<String, String>>()
                runCatching {
                    caller.invoke("__signOut", emptyMap(), signOutDeferred)
                    signOutDeferred.await()
                }
            }

            SignInResult.Success(mapUser(result))
        } catch (t: Throwable) {
            SignInResult.Error(t.message ?: "Email registration failed on iOS.")
        }
    }

    actual suspend fun linkAnonymousWithGoogle(): SignInResult {
        val current = getCurrentUser()
            ?: return SignInResult.Error("guest_link_requires_anonymous_user")

        if (current.provider != SignInProvider.ANONYMOUS) {
            return SignInResult.Error("guest_link_requires_anonymous_user")
        }

        val deferred = CompletableDeferred<SignInResult>()
        val launcher = GoogleSignInBridge.linkAnonymousWithGoogle
            ?: return SignInResult.Error("iOS anonymous Google link bridge is not connected.")

        return try {
            launcher.invoke(deferred)
            deferred.await()
        } catch (t: Throwable) {
            SignInResult.Error(t.message ?: "Anonymous Google link failed on iOS.")
        }
    }

    actual suspend fun linkAnonymousWithApple(): SignInResult {
        val current = getCurrentUser()
            ?: return SignInResult.Error("guest_link_requires_anonymous_user")

        if (current.provider != SignInProvider.ANONYMOUS) {
            return SignInResult.Error("guest_link_requires_anonymous_user")
        }

        val deferred = CompletableDeferred<SignInResult>()
        val launcher = AppleSignInBridge.linkAnonymousWithApple
            ?: return SignInResult.Error("iOS anonymous Apple link bridge is not connected.")

        return try {
            launcher.invoke(deferred)
            deferred.await()
        } catch (t: Throwable) {
            SignInResult.Error(t.message ?: "Anonymous Apple link failed on iOS.")
        }
    }

    actual suspend fun linkAnonymousWithEmail(email: String, password: String): SignInResult {
        val current = getCurrentUser()
            ?: return SignInResult.Error("guest_link_requires_anonymous_user")

        if (current.provider != SignInProvider.ANONYMOUS) {
            return SignInResult.Error("guest_link_requires_anonymous_user")
        }

        val bridge = EmailAuthBridgeConnector.linkAnonymousWithEmail
            ?: return SignInResult.Error("iOS anonymous email link bridge is not connected.")

        return try {
            val deferred = CompletableDeferred<Map<String, String>>()
            bridge.invoke(email.trim(), password, deferred)
            val result = deferred.await()
            SignInResult.Success(mapUser(result))
        } catch (t: Throwable) {
            SignInResult.Error(t.message ?: "Anonymous email link failed on iOS.")
        }
    }
    actual suspend fun sendPasswordReset(email: String): SignInResult {
        val bridge = EmailAuthBridgeConnector.sendPasswordReset
            ?: return SignInResult.Error("iOS password reset bridge is not connected.")

        return try {
            val deferred = CompletableDeferred<Map<String, String>>()
            bridge.invoke(email.trim(), deferred)
            deferred.await()

            SignInResult.Success(
                AuthUser(
                    uid = "",
                    provider = SignInProvider.EMAIL,
                    email = email.trim(),
                    displayName = ""
                )
            )
        } catch (t: Throwable) {
            SignInResult.Error(t.message ?: "Password reset failed on iOS.")
        }
    }

    actual suspend fun signOut() {
        val caller = BackendBridgeConnector.callBackend
        if (caller != null) {
            val deferred = CompletableDeferred<Map<String, String>>()
            runCatching {
                caller.invoke("__signOut", emptyMap(), deferred)
                deferred.await()
            }
        }
    }

    actual suspend fun clearCredentialState() {
        // no-op on iOS for now; native auth state is queried via __currentUser
    }

    actual suspend fun prepareForNewSignIn() {
        val caller = BackendBridgeConnector.callBackend ?: return
        val deferred = CompletableDeferred<Map<String, String>>()

        runCatching {
            caller.invoke("__prepareForNewSignIn", emptyMap(), deferred)
            deferred.await()
        }
    }

    private fun mapUser(result: Map<String, String>): AuthUser {
        val providerRaw = result["provider"].orEmpty().trim().uppercase()
        val provider = when (providerRaw) {
            "GOOGLE" -> SignInProvider.GOOGLE
            "EMAIL" -> SignInProvider.EMAIL
            "APPLE" -> SignInProvider.APPLE
            "ANONYMOUS" -> SignInProvider.ANONYMOUS
            else -> SignInProvider.EMAIL
        }

        return AuthUser(
            uid = result["uid"].orEmpty(),
            provider = provider,
            email = result["email"].orEmpty(),
            displayName = result["displayName"].orEmpty()
        )
    }
}