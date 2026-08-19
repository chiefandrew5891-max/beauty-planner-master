package com.andrey.beautyplanner.auth

import kotlinx.coroutines.CompletableDeferred

object EmailAuthBridgeConnector {
    var signIn:
            ((String, String, CompletableDeferred<Map<String, String>>) -> Unit)? = null

    var registerUser:
            ((String, String, CompletableDeferred<Map<String, String>>) -> Unit)? = null

    var linkAnonymousWithEmail:
            ((String, String, CompletableDeferred<Map<String, String>>) -> Unit)? = null

    var sendPasswordReset:
            ((String, CompletableDeferred<Map<String, String>>) -> Unit)? = null

    var sendEmailVerification:
            ((CompletableDeferred<Map<String, String>>) -> Unit)? = null

    var reloadCurrentUser:
            ((CompletableDeferred<Map<String, String>>) -> Unit)? = null
}