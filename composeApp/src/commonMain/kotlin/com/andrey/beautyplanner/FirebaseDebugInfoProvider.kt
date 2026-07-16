package com.andrey.beautyplanner

data class FirebaseDebugInfo(
    val platform: String,
    val firebaseProjectId: String,
    val firebaseAppId: String,
    val firestoreDatabaseId: String,
    val firestoreHost: String
)

expect object FirebaseDebugInfoProvider {
    fun get(): FirebaseDebugInfo
}