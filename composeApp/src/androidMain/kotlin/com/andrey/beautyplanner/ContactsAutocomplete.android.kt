package com.andrey.beautyplanner

import android.provider.ContactsContract

object ContactsAutocompleteAndroid {
    private var appContextProvider: android.content.Context? = null
    private var permissionCheckerProvider: (() -> Boolean)? = null
    private var requestPermissionProvider: (() -> Unit)? = null

    var permissionGranted: Boolean = false
    var permissionRequestedOnce: Boolean = false

    fun init(
        context: android.content.Context,
        permissionChecker: () -> Boolean,
        requestPermission: () -> Unit
    ) {
        appContextProvider = context.applicationContext
        permissionCheckerProvider = permissionChecker
        requestPermissionProvider = requestPermission
        permissionGranted = permissionChecker()
    }

    fun context(): android.content.Context? = appContextProvider
    fun hasPermission(): Boolean = permissionCheckerProvider?.invoke() == true
    fun requestPermissionNow() {
        requestPermissionProvider?.invoke()
    }
}

actual object ContactsAutocomplete {

    actual fun isPermissionGranted(): Boolean {
        return ContactsAutocompleteAndroid.hasPermission()
    }

    actual fun wasPermissionRequestedOnce(): Boolean {
        return ContactsAutocompleteAndroid.permissionRequestedOnce
    }

    actual fun requestPermission() {
        ContactsAutocompleteAndroid.permissionRequestedOnce = true
        ContactsAutocompleteAndroid.requestPermissionNow()
    }

    actual fun findSuggestions(query: String, limit: Int): List<ContactSuggestion> {
        val context = ContactsAutocompleteAndroid.context() ?: return emptyList()
        if (!isPermissionGranted()) return emptyList()

        val trimmed = query.trim()
        if (trimmed.length < 2) return emptyList()

        val results = linkedMapOf<String, ContactSuggestion>()

        val selection = """
            ${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?
            OR ${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?
        """.trimIndent()

        val args = arrayOf("%$trimmed%", "$trimmed%")

        val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"

        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            selection,
            args,
            sortOrder
        ) ?: return emptyList()

        cursor.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext() && results.size < limit) {
                val name = it.getString(nameIndex)?.trim().orEmpty()
                val phone = it.getString(phoneIndex)?.trim().orEmpty()
                if (name.isBlank() || phone.isBlank()) continue

                val key = "$name|$phone"
                if (!results.containsKey(key)) {
                    results[key] = ContactSuggestion(
                        displayName = name,
                        phone = phone
                    )
                }
            }
        }

        return results.values.toList()
    }
}