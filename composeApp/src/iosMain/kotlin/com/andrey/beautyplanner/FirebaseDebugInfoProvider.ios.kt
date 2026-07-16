package com.andrey.beautyplanner

actual object FirebaseDebugInfoProvider {
    actual fun get(): FirebaseDebugInfo {
        return FirebaseDebugInfo(
            platform = "IOS",
            firebaseProjectId = "",
            firebaseAppId = "",
            firestoreDatabaseId = "",
            firestoreHost = ""
        )
    }
}