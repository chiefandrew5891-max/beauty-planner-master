package com.andrey.beautyplanner.auth

import kotlinx.coroutines.CompletableDeferred

object GoogleSignInBridge {
    var startGoogleSignIn: ((CompletableDeferred<SignInResult>) -> Unit)? = null
    var linkAnonymousWithGoogle: ((CompletableDeferred<SignInResult>) -> Unit)? = null
}