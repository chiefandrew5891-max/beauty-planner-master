package com.andrey.beautyplanner

actual object ContactsAutocomplete {

    actual fun isPermissionGranted(): Boolean {
        // =========================================================
        // TEMP HIDE FOR APP REVIEW: IOS CONTACTS ACCESS
        // Device contacts access is temporarily disabled on iOS.
        // The app continues using only app-local client suggestions.
        // BEGIN TEMP HIDE
        // =========================================================
        return true
        // =========================================================
        // END TEMP HIDE FOR APP REVIEW: IOS CONTACTS ACCESS
        // =========================================================
    }

    actual fun wasPermissionRequestedOnce(): Boolean {
        return true
    }

    actual fun requestPermission() {
        // =========================================================
        // TEMP HIDE FOR APP REVIEW: IOS CONTACTS ACCESS
        // No permission request is performed on iOS in review-safe mode.
        // BEGIN TEMP HIDE
        // =========================================================
        return
        // =========================================================
        // END TEMP HIDE FOR APP REVIEW: IOS CONTACTS ACCESS
        // =========================================================
    }

    actual fun findSuggestions(query: String, limit: Int): List<ContactSuggestion> {
        // =========================================================
        // TEMP HIDE FOR APP REVIEW: IOS CONTACTS ACCESS
        // No device contacts lookup on iOS.
        // Booking suggestions continue to come from app-local data only.
        // BEGIN TEMP HIDE
        // =========================================================
        return emptyList()
        // =========================================================
        // END TEMP HIDE FOR APP REVIEW: IOS CONTACTS ACCESS
        // =========================================================
    }
}