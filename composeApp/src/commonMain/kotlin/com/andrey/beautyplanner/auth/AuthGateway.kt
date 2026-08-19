package com.andrey.beautyplanner.auth

expect object AuthGateway {
    suspend fun getCurrentUser(): AuthUser?
    suspend fun signInAnonymously(): SignInResult
    suspend fun signInWithGoogle(): SignInResult
    suspend fun signInWithApple(): SignInResult
    suspend fun signInWithEmail(email: String, password: String): SignInResult
    suspend fun registerWithEmail(email: String, password: String): SignInResult

    suspend fun linkAnonymousWithGoogle(): SignInResult
    suspend fun linkAnonymousWithApple(): SignInResult
    suspend fun linkAnonymousWithEmail(email: String, password: String): SignInResult

    suspend fun sendPasswordReset(email: String): SignInResult
    suspend fun signOut()
    suspend fun clearCredentialState()
    suspend fun prepareForNewSignIn()
}