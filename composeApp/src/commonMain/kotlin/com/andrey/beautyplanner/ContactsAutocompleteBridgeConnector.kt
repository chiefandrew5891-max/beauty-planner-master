package com.andrey.beautyplanner

import kotlinx.coroutines.CompletableDeferred

object ContactsAutocompleteBridgeConnector {
    var isPermissionGranted:
            ((CompletableDeferred<Boolean>) -> Unit)? = null

    var requestPermission:
            ((CompletableDeferred<Boolean>) -> Unit)? = null

    var findSuggestions:
            ((String, Int, CompletableDeferred<List<Map<String, String>>>) -> Unit)? = null
}