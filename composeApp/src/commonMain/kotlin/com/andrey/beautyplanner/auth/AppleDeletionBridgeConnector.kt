package com.andrey.beautyplanner.auth

import kotlinx.coroutines.CompletableDeferred

object AppleDeletionBridgeConnector {
    var reauthenticateAndRevoke:
            ((CompletableDeferred<Map<String, String>>) -> Unit)? = null
}