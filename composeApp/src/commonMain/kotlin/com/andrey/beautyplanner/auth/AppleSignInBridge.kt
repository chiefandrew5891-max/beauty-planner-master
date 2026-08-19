package com.andrey.beautyplanner.auth

import kotlinx.coroutines.CompletableDeferred

object AppleSignInBridge {
    var startAppleSignIn: ((CompletableDeferred<SignInResult>) -> Unit)? = null
    var linkAnonymousWithApple: ((CompletableDeferred<SignInResult>) -> Unit)? = null
}