package com.andrey.beautyplanner

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking

actual object ContactsAutocomplete {
    actual fun isPermissionGranted(): Boolean {
        val bridge = ContactsAutocompleteBridgeConnector.isPermissionGranted ?: return false

        return runBlocking {
            val deferred = CompletableDeferred<Boolean>()
            bridge.invoke(deferred)
            deferred.await()
        }
    }

    actual fun wasPermissionRequestedOnce(): Boolean {
        return false
    }

    actual fun requestPermission() {
        val bridge = ContactsAutocompleteBridgeConnector.requestPermission ?: return

        runBlocking {
            val deferred = CompletableDeferred<Boolean>()
            bridge.invoke(deferred)
            deferred.await()
        }
    }

    actual fun findSuggestions(query: String, limit: Int): List<ContactSuggestion> {
        if (query.trim().length < 2) return emptyList()

        val bridge = ContactsAutocompleteBridgeConnector.findSuggestions ?: return emptyList()

        return runBlocking {
            val deferred = CompletableDeferred<List<Map<String, String>>>()
            bridge.invoke(query, limit, deferred)

            deferred.await().mapNotNull { item ->
                val displayName = item["displayName"].orEmpty().trim()
                val phone = item["phone"].orEmpty().trim()

                if (displayName.isBlank()) return@mapNotNull null

                ContactSuggestion(
                    displayName = displayName,
                    phone = phone
                )
            }
        }
    }
}