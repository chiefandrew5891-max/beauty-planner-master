package com.andrey.beautyplanner.auth

import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.andrey.beautyplanner.AndroidAppContext
import com.andrey.beautyplanner.Locales
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


actual object AuthGateway {
    actual suspend fun getCurrentUser(): AuthUser? {
        val user = Firebase.auth.currentUser ?: return null
        val provider = when {
            user.isAnonymous -> SignInProvider.ANONYMOUS
            user.providerData.any { it.providerId == "google.com" } -> SignInProvider.GOOGLE
            user.providerData.any { it.providerId == "password" } -> SignInProvider.EMAIL
            user.providerData.any { it.providerId == "apple.com" } -> SignInProvider.APPLE
            else -> SignInProvider.ANONYMOUS
        }

        return AuthUser(
            uid = user.uid,
            provider = provider,
            email = user.email.orEmpty(),
            displayName = user.displayName.orEmpty()
        )
    }

    actual suspend fun signInAnonymously(): SignInResult {
        val current = getCurrentUser()
        if (current != null) {
            return SignInResult.Success(current)
        }

        return try {
            val authResult = suspendCancellableCoroutine<com.google.firebase.auth.AuthResult> { cont ->
                Firebase.auth.signInAnonymously()
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }

            val user = authResult.user
                ?: return SignInResult.Error("Anonymous sign-in returned null user")

            SignInResult.Success(
                AuthUser(
                    uid = user.uid,
                    provider = SignInProvider.ANONYMOUS,
                    email = user.email.orEmpty(),
                    displayName = user.displayName.orEmpty()
                )
            )
        } catch (e: Exception) {
            SignInResult.Error(e.message ?: "Anonymous sign-in failed")
        }
    }

    actual suspend fun signInWithGoogle(): SignInResult {
        val activity = AndroidAppContext.activity
            ?: return SignInResult.Error(Locales.t("auth_google_failed"))

        val serverClientId = getServerClientId(activity)
            ?: return SignInResult.Error(Locales.t("auth_google_failed"))

        runCatching { prepareForNewSignIn() }

        val credentialManagerResult = tryCredentialManagerGoogleSignIn(
            activity = activity,
            serverClientId = serverClientId
        )

        if (credentialManagerResult is SignInResult.Success) {
            return credentialManagerResult
        }

        // Если пользователь сам отменил выбор аккаунта — не ретраим,
        // иначе снова откроется системный диалог.
        if (credentialManagerResult is SignInResult.Cancelled) {
            return credentialManagerResult
        }

        Log.w(
            "AuthGateway",
            "Credential Manager Google sign-in failed, retry after clearing state"
        )

        // Первая попытка часто падает из-за протухшего credential state
        // после signOut/удаления аккаунта. Чистим и пробуем ещё раз.
        runCatching { clearCredentialState() }

        val retryResult = tryCredentialManagerGoogleSignIn(
            activity = activity,
            serverClientId = serverClientId
        )
        if (retryResult is SignInResult.Success) {
            return retryResult
        }

        Log.w(
            "AuthGateway",
            "Credential Manager retry failed, fallback to GoogleSignInClient"
        )

        return tryLegacyGoogleSignIn(
            activity = activity,
            serverClientId = serverClientId,
            primaryFailure = retryResult
        )
    }

    actual suspend fun signInWithEmail(email: String, password: String): SignInResult {
        return try {
            val authResult = suspendCancellableCoroutine<com.google.firebase.auth.AuthResult> { cont ->
                Firebase.auth.signInWithEmailAndPassword(email.trim(), password)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }

            val user = authResult.user
                ?: return SignInResult.Error(Locales.t("auth_email_sign_in_failed"))

            suspendCancellableCoroutine<Unit> { cont ->
                user.reload()
                    .addOnSuccessListener { cont.resume(Unit) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }

            if (!user.isEmailVerified) {
                Firebase.auth.signOut()
                return SignInResult.Error(Locales.t("auth_email_not_verified"))
            }

            SignInResult.Success(
                AuthUser(
                    uid = user.uid,
                    provider = SignInProvider.EMAIL,
                    email = user.email.orEmpty(),
                    displayName = user.displayName.orEmpty()
                )
            )
        } catch (e: Exception) {
            Log.e("AuthGateway", "Email sign-in failed", e)
            SignInResult.Error(
                e.message ?: Locales.t("auth_email_sign_in_failed")
            )
        }
    }

    actual suspend fun signInWithApple(): SignInResult {
        return SignInResult.Error("Apple Sign-In is not available on Android in this version.")
    }

    actual suspend fun registerWithEmail(email: String, password: String): SignInResult {
        return try {
            val cleanEmail = email.trim()

            suspendCancellableCoroutine<com.google.firebase.auth.AuthResult> { cont ->
                Firebase.auth.createUserWithEmailAndPassword(cleanEmail, password)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }

            val currentUser = Firebase.auth.currentUser
                ?: return SignInResult.Error(Locales.t("auth_email_register_failed"))

            suspendCancellableCoroutine<Unit> { cont ->
                currentUser.sendEmailVerification()
                    .addOnSuccessListener { cont.resume(Unit) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }

            Firebase.auth.signOut()

            val verifyResult = suspendCancellableCoroutine<com.google.firebase.auth.AuthResult> { cont ->
                Firebase.auth.signInWithEmailAndPassword(cleanEmail, password)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }

            val user = verifyResult.user
                ?: return SignInResult.Error(Locales.t("auth_email_register_failed"))

            Firebase.auth.signOut()

            SignInResult.Success(
                AuthUser(
                    uid = user.uid,
                    provider = SignInProvider.EMAIL,
                    email = user.email.orEmpty(),
                    displayName = user.displayName.orEmpty()
                )
            )
        } catch (e: Exception) {
            Log.e("AuthGateway", "Email registration failed", e)
            SignInResult.Error(
                e.message ?: Locales.t("auth_email_register_failed")
            )
        }
    }

    actual suspend fun linkAnonymousWithGoogle(): SignInResult {
        val activity = AndroidAppContext.activity
            ?: return SignInResult.Error(Locales.t("auth_google_failed"))

        val serverClientId = getServerClientId(activity)
            ?: return SignInResult.Error(Locales.t("auth_google_failed"))

        val currentUser = Firebase.auth.currentUser
            ?: return SignInResult.Error("guest_link_requires_anonymous_user")

        if (!currentUser.isAnonymous) {
            return SignInResult.Error("guest_link_requires_anonymous_user")
        }

        val credentialManagerResult = tryCredentialManagerGoogleLink(
            activity = activity,
            serverClientId = serverClientId
        )

        if (credentialManagerResult is SignInResult.Success) {
            return credentialManagerResult
        }

        if (credentialManagerResult is SignInResult.Cancelled) {
            return credentialManagerResult
        }

        Log.w(
            "AuthGateway",
            "Credential Manager Google link failed, retry after clearing state"
        )

        runCatching { clearCredentialState() }

        val retryResult = tryCredentialManagerGoogleLink(
            activity = activity,
            serverClientId = serverClientId
        )
        if (retryResult is SignInResult.Success) {
            return retryResult
        }

        Log.w(
            "AuthGateway",
            "Credential Manager link retry failed, fallback to GoogleSignInClient"
        )

        return tryLegacyGoogleLink(
            activity = activity,
            serverClientId = serverClientId,
            primaryFailure = retryResult
        )
    }

    actual suspend fun linkAnonymousWithApple(): SignInResult {
        return SignInResult.Error("Apple Sign-In is not available on Android in this version.")
    }

    actual suspend fun linkAnonymousWithEmail(email: String, password: String): SignInResult {
        val currentUser = Firebase.auth.currentUser
            ?: return SignInResult.Error("guest_link_requires_anonymous_user")

        if (!currentUser.isAnonymous) {
            return SignInResult.Error("guest_link_requires_anonymous_user")
        }

        return try {
            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(
                email.trim(),
                password
            )

            val authResult = suspendCancellableCoroutine<com.google.firebase.auth.AuthResult> { cont ->
                currentUser.linkWithCredential(credential)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }

            val user = authResult.user
                ?: return SignInResult.Error(Locales.t("auth_email_register_failed"))

            suspendCancellableCoroutine<Unit> { cont ->
                user.sendEmailVerification()
                    .addOnSuccessListener { cont.resume(Unit) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }

            SignInResult.Success(
                AuthUser(
                    uid = user.uid,
                    provider = SignInProvider.EMAIL,
                    email = user.email.orEmpty(),
                    displayName = user.displayName.orEmpty()
                )
            )
        } catch (e: FirebaseAuthUserCollisionException) {
            Log.w("AuthGateway", "Anonymous email link collision", e)
            SignInResult.Error("guest_upgrade_account_already_exists")
        } catch (e: Exception) {
            Log.e("AuthGateway", "Anonymous email link failed", e)

            val lower = e.message.orEmpty().lowercase()
            if (lower.contains("credential-already-in-use") ||
                lower.contains("email-already-in-use") ||
                lower.contains("account-exists-with-different-credential") ||
                lower.contains("guest_upgrade_account_already_exists")
            ) {
                return SignInResult.Error("guest_upgrade_account_already_exists")
            }

            SignInResult.Error(
                e.message ?: Locales.t("auth_email_register_failed")
            )
        }
    }
    actual suspend fun signOut() {
        Firebase.auth.signOut()

        AndroidAppContext.activity?.let { activity ->
            runCatching {
                GoogleSignIn.getClient(
                    activity,
                    buildGoogleSignInOptions(
                        getServerClientId(activity).orEmpty()
                    )
                ).signOut()
            }.onFailure {
                Log.w("AuthGateway", "GoogleSignInClient signOut failed", it)
            }
        }
    }

    actual suspend fun sendPasswordReset(email: String): SignInResult {
        return try {
            suspendCancellableCoroutine<Unit> { cont ->
                Firebase.auth.sendPasswordResetEmail(email.trim())
                    .addOnSuccessListener { cont.resume(Unit) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }

            SignInResult.Success(
                AuthUser(
                    uid = "",
                    provider = SignInProvider.EMAIL,
                    email = email.trim(),
                    displayName = ""
                )
            )
        } catch (e: Exception) {
            Log.e("AuthGateway", "Password reset failed", e)
            SignInResult.Error(
                e.message ?: Locales.t("auth_password_reset_failed")
            )
        }
    }

    actual suspend fun clearCredentialState() {
        val activity = AndroidAppContext.activity ?: return
        val credentialManager = CredentialManager.create(activity)
        runCatching {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        }.onFailure {
            Log.e("AuthGateway", "Failed to clear credential state", it)
        }
    }

    actual suspend fun prepareForNewSignIn() {
        runCatching { Firebase.auth.signOut() }

        AndroidAppContext.activity?.let { activity ->
            runCatching {
                GoogleSignIn.getClient(
                    activity,
                    buildGoogleSignInOptions(
                        getServerClientId(activity).orEmpty()
                    )
                ).signOut()
            }.onFailure {
                Log.w("AuthGateway", "GoogleSignInClient signOut during prepareForNewSignIn failed", it)
            }
        }

        runCatching { clearCredentialState() }
    }
    private fun getServerClientId(activity: android.app.Activity): String? {
        val webClientIdRes = activity.resources.getIdentifier(
            "default_web_client_id",
            "string",
            activity.packageName
        )
        if (webClientIdRes == 0) return null
        return activity.getString(webClientIdRes).takeIf { it.isNotBlank() }
    }

    private suspend fun tryCredentialManagerGoogleSignIn(
        activity: android.app.Activity,
        serverClientId: String
    ): SignInResult {
        return try {
            val credentialManager = CredentialManager.create(activity)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                context = activity,
                request = request
            )

            val googleIdTokenCredential = try {
                GoogleIdTokenCredential.createFrom(result.credential.data)
            } catch (_: GoogleIdTokenParsingException) {
                return SignInResult.Error(Locales.t("auth_google_failed"))
            }

            signInFirebaseWithGoogleIdToken(googleIdTokenCredential.idToken)
        } catch (e: Exception) {
            Log.e("AuthGateway", "Credential Manager Google sign-in failed", e)

            val message = e.message.orEmpty()

            when {
                message.contains("No credentials available", ignoreCase = true) ->
                    SignInResult.Error(Locales.t("auth_google_no_credentials"))

                message.contains("cancel", ignoreCase = true) ->
                    SignInResult.Cancelled

                else ->
                    SignInResult.Error(Locales.t("auth_google_failed"))
            }
        }
    }

    private suspend fun tryLegacyGoogleSignIn(
        activity: android.app.Activity,
        serverClientId: String,
        primaryFailure: SignInResult
    ): SignInResult {
        return try {
            val signInClient = buildGoogleSignInClient(activity, serverClientId)
            val intent = signInClient.signInIntent

            val deferred = CompletableDeferred<GoogleSignInFallbackResult>()
            val launcher = GoogleSignInFallbackBridge.launchSignInIntent
                ?: return SignInResult.Error(Locales.t("auth_google_failed"))

            launcher.invoke(intent, deferred)

            when (val result = deferred.await()) {
                is GoogleSignInFallbackResult.Success -> {
                    signInFirebaseWithGoogleIdToken(result.idToken)
                }

                is GoogleSignInFallbackResult.Cancelled -> {
                    SignInResult.Cancelled
                }

                is GoogleSignInFallbackResult.Error -> {
                    when (primaryFailure) {
                        is SignInResult.Error -> primaryFailure
                        else -> SignInResult.Error(Locales.t("auth_google_failed"))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AuthGateway", "Legacy Google sign-in failed", e)
            when (primaryFailure) {
                is SignInResult.Error -> primaryFailure
                else -> SignInResult.Error(Locales.t("auth_google_failed"))
            }
        }
    }

    private fun buildGoogleSignInOptions(serverClientId: String): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(serverClientId)
            .build()
    }

    private fun buildGoogleSignInClient(
        activity: android.app.Activity,
        serverClientId: String
    ): GoogleSignInClient {
        return GoogleSignIn.getClient(
            activity,
            buildGoogleSignInOptions(serverClientId)
        )
    }

    private suspend fun tryCredentialManagerGoogleLink(
        activity: android.app.Activity,
        serverClientId: String
    ): SignInResult {
        return try {
            val credentialManager = CredentialManager.create(activity)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                context = activity,
                request = request
            )

            val googleIdTokenCredential = try {
                GoogleIdTokenCredential.createFrom(result.credential.data)
            } catch (_: GoogleIdTokenParsingException) {
                return SignInResult.Error(Locales.t("auth_google_failed"))
            }

            linkAnonymousFirebaseWithGoogleIdToken(googleIdTokenCredential.idToken)
        } catch (e: Exception) {
            Log.e("AuthGateway", "Credential Manager Google link failed", e)

            val message = e.message.orEmpty()
            val lower = message.lowercase()

            when {
                lower.contains("guest_upgrade_account_already_exists") ->
                    SignInResult.Error("guest_upgrade_account_already_exists")

                message.contains("No credentials available", ignoreCase = true) ->
                    SignInResult.Error(Locales.t("auth_google_no_credentials"))

                message.contains("cancel", ignoreCase = true) ->
                    SignInResult.Cancelled

                else ->
                    SignInResult.Error(Locales.t("auth_google_failed"))
            }
        }
    }

    private suspend fun tryLegacyGoogleLink(
        activity: android.app.Activity,
        serverClientId: String,
        primaryFailure: SignInResult
    ): SignInResult {
        return try {
            val signInClient = buildGoogleSignInClient(activity, serverClientId)
            val intent = signInClient.signInIntent

            val deferred = CompletableDeferred<GoogleSignInFallbackResult>()
            val launcher = GoogleSignInFallbackBridge.launchSignInIntent
                ?: return SignInResult.Error(Locales.t("auth_google_failed"))

            launcher.invoke(intent, deferred)

            when (val result = deferred.await()) {
                is GoogleSignInFallbackResult.Success -> {
                    linkAnonymousFirebaseWithGoogleIdToken(result.idToken)
                }

                is GoogleSignInFallbackResult.Cancelled -> {
                    SignInResult.Cancelled
                }

                is GoogleSignInFallbackResult.Error -> {
                    when (primaryFailure) {
                        is SignInResult.Error -> primaryFailure
                        else -> SignInResult.Error(Locales.t("auth_google_failed"))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AuthGateway", "Legacy Google link failed", e)

            val lower = e.message.orEmpty().lowercase()
            if (lower.contains("guest_upgrade_account_already_exists")) {
                return SignInResult.Error("guest_upgrade_account_already_exists")
            }

            when (primaryFailure) {
                is SignInResult.Error -> primaryFailure
                else -> SignInResult.Error(Locales.t("auth_google_failed"))
            }
        }
    }

    private suspend fun linkAnonymousFirebaseWithGoogleIdToken(idToken: String): SignInResult {
        return try {
            val currentUser = Firebase.auth.currentUser
                ?: return SignInResult.Error("guest_link_requires_anonymous_user")

            if (!currentUser.isAnonymous) {
                return SignInResult.Error("guest_link_requires_anonymous_user")
            }

            val firebaseCredential: AuthCredential =
                GoogleAuthProvider.getCredential(idToken, null)

            val authResult = suspendCancellableCoroutine<com.google.firebase.auth.AuthResult> { cont ->
                currentUser.linkWithCredential(firebaseCredential)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }

            val user = authResult.user
                ?: return SignInResult.Error(Locales.t("auth_google_failed"))

            SignInResult.Success(
                AuthUser(
                    uid = user.uid,
                    provider = SignInProvider.GOOGLE,
                    email = user.email.orEmpty(),
                    displayName = user.displayName.orEmpty()
                )
            )
        } catch (e: FirebaseAuthUserCollisionException) {
            Log.w("AuthGateway", "Anonymous Google link collision", e)
            SignInResult.Error("guest_upgrade_account_already_exists")
        } catch (e: Exception) {
            Log.e("AuthGateway", "Firebase Google credential link failed", e)

            val lower = e.message.orEmpty().lowercase()
            if (lower.contains("credential-already-in-use") ||
                lower.contains("email-already-in-use") ||
                lower.contains("account-exists-with-different-credential") ||
                lower.contains("guest_upgrade_account_already_exists")
            ) {
                return SignInResult.Error("guest_upgrade_account_already_exists")
            }

            SignInResult.Error(Locales.t("auth_google_failed"))
        }
    }
    private suspend fun signInFirebaseWithGoogleIdToken(idToken: String): SignInResult {
        return try {
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)

            val authResult = suspendCancellableCoroutine<com.google.firebase.auth.AuthResult> { cont ->
                Firebase.auth.signInWithCredential(firebaseCredential)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }

            val user = authResult.user
                ?: return SignInResult.Error(Locales.t("auth_google_failed"))

            SignInResult.Success(
                AuthUser(
                    uid = user.uid,
                    provider = SignInProvider.GOOGLE,
                    email = user.email.orEmpty(),
                    displayName = user.displayName.orEmpty()
                )
            )
        } catch (e: Exception) {
            Log.e("AuthGateway", "Firebase Google credential sign-in failed", e)
            SignInResult.Error(Locales.t("auth_google_failed"))
        }
    }
}
//