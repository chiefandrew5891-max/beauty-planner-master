package com.andrey.beautyplanner

actual object CloudSyncRepositoryProvider {
    actual val repository: CloudSyncRepository = FirestoreCloudSyncRepository()
}