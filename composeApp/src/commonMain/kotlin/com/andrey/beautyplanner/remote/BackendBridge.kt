package com.andrey.beautyplanner.remote

data class MasterServiceTemplatePayload(
    val id: String,
    val title: String,
    val defaultPrice: String,
    val isActive: Boolean
)

data class AvatarLibraryItemPayload(
    val id: String,
    val storagePath: String,
    val downloadUrl: String,
    val createdAt: Long
)

data class MasterProfilePayload(
    val found: Boolean,
    val firebaseUid: String,
    val ownerName: String,
    val profileDisplayCustomName: Boolean,
    val profilePhone: String,
    val profilePhoneVisible: Boolean,
    val profileSpecialization: String,
    val profileRating: Float,
    val profileAvatarUrl: String,
    val profileAvatarBase64: String,
    val profileAvatarStoragePath: String,
    val avatarLibrary: List<AvatarLibraryItemPayload>,
    val clientInteractionsEnabled: Boolean,
    val serviceTemplates: List<MasterServiceTemplatePayload>,
    val weeklyBlockedIntervalsJson: String,
    val scheduleDateOverridesJson: String,
    val dateRangeBlockedIntervalsJson: String,
    val updatedAt: Long,
    val createdAt: Long
)

expect object BackendBridge {
    suspend fun ensureAuthenticated(): String

    suspend fun checkAppUpdate(
        platform: String,
        versionName: String,
        buildNumber: String
    ): Map<String, String>

    suspend fun bootstrapUser(
        installId: String,
        firebaseUid: String,
        platform: String,
        authProvider: String,
        email: String,
        displayName: String
    ): AccessStatusResponse

    suspend fun verifySubscription(
        userId: String,
        productId: String,
        purchaseToken: String,
        platform: String = "PLAY",
        transactionId: String = ""
    ): AccessStatusResponse

    suspend fun getAccessStatus(userId: String): AccessStatusResponse

    suspend fun syncIdentity(
        firebaseUid: String,
        email: String,
        displayName: String,
        authProvider: String
    ): AccessStatusResponse

    suspend fun syncMyMasterProfile(
        ownerName: String,
        profileDisplayCustomName: Boolean,
        profilePhone: String,
        profilePhoneVisible: Boolean,
        profileSpecialization: String,
        profileRating: Float,
        profileAvatarUrl: String,
        profileAvatarBase64: String,
        profileAvatarStoragePath: String,
        clientInteractionsEnabled: Boolean,
        serviceTemplatesJson: String,
        weeklyBlockedIntervalsJson: String,
        scheduleDateOverridesJson: String,
        dateRangeBlockedIntervalsJson: String
    ): Map<String, String>

    suspend fun getMyMasterProfile(): MasterProfilePayload

    suspend fun syncMyPublicSchedule(
        autoPublishBusySlots: Boolean,
        busySlotsJson: String
    ): Map<String, String>

    suspend fun clearMyDatabase(): Map<String, String>

    suspend fun uploadMyProfileAvatar(
        avatarBase64: String
    ): Map<String, String>

    suspend fun listMyProfileAvatars(): List<AvatarLibraryItemPayload>

    suspend fun deleteMyProfileAvatar(
        avatarId: String
    ): Map<String, String>

    suspend fun validateCurrentSession(): Map<String, String>

    suspend fun deleteMyAccount(): Map<String, String>
}