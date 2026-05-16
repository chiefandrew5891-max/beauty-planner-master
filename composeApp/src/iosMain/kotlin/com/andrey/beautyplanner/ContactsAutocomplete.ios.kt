package com.andrey.beautyplanner

actual object ContactsAutocomplete {
    actual fun isPermissionGranted(): Boolean = false

    actual fun wasPermissionRequestedOnce(): Boolean = false

    actual fun requestPermission() {
        // iOS stub for now
    }

    actual fun findSuggestions(query: String, limit: Int): List<ContactSuggestion> = emptyList()
}