package com.andrey.beautyplanner

import kotlinx.coroutines.CompletableDeferred

object CloudSyncBridgeConnector {
    var pullAll:
            ((String, CompletableDeferred<Map<String, String>>) -> Unit)? = null

    var pushAppointments:
            ((String, List<Map<String, String>>, CompletableDeferred<Map<String, String>>) -> Unit)? = null

    var pushSettings:
            ((String, Map<String, String>, CompletableDeferred<Map<String, String>>) -> Unit)? = null
}