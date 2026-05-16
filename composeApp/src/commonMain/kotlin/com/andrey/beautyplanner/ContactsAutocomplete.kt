package com.andrey.beautyplanner

data class ContactSuggestion(
    val displayName: String,
    val phone: String
)

expect object ContactsAutocomplete {
    fun isPermissionGranted(): Boolean
    fun wasPermissionRequestedOnce(): Boolean
    fun requestPermission()
    fun findSuggestions(query: String, limit: Int = 8): List<ContactSuggestion>
}