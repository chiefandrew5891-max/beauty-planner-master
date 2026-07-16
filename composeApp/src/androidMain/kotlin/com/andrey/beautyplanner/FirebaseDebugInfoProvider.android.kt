package com.andrey.beautyplanner

import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.firestore

actual object FirebaseDebugInfoProvider {
    actual fun get(): FirebaseDebugInfo {
        val app = FirebaseApp.getInstance()
        val options = app.options

        val firestore = Firebase.firestore

        return FirebaseDebugInfo(
            platform = "ANDROID",
            firebaseProjectId = options.projectId ?: "",
            firebaseAppId = options.applicationId ?: "",
            firestoreDatabaseId = firestore.app.options.projectId ?: "",
            firestoreHost = firestore.firestoreSettings.host
        )
    }
}