package com.andrey.beautyplanner.billing

data class BillingProduct(
    val productId: String,
    val title: String,
    val description: String,
    val formattedPrice: String,
    val priceAmountMicros: Long? = null,
    val priceCurrencyCode: String? = null
)

enum class BillingStatus {
    IDLE,
    CONNECTING,
    READY,
    LOADING_PRODUCTS,
    PURCHASING,
    RESTORING,
    PURCHASED,
    ERROR
}

sealed class PurchaseResult {
    data object Success : PurchaseResult()
    data object Cancelled : PurchaseResult()
    data class Error(val message: String) : PurchaseResult()
}

sealed class RestoreResult {
    data object Restored : RestoreResult()
    data object NothingToRestore : RestoreResult()
    data class Error(val message: String) : RestoreResult()
}

data class BillingUiState(
    val status: BillingStatus = BillingStatus.IDLE,
    val products: List<BillingProduct> = emptyList(),
    val errorMessage: String? = null,
    val ownedPremium: Boolean = false
)
enum class SubscriptionState {
    NONE,
    ACTIVE,
    EXPIRED,
    CANCELED,
    IN_GRACE_PERIOD,
    ON_HOLD
}

data class SubscriptionInfo(
    val state: SubscriptionState = SubscriptionState.NONE,
    val productId: String = "",
    val purchaseToken: String = "",
    val isAutoRenewing: Boolean = false,
    val startTimeMillis: Long? = null,
    val expiryTimeMillis: Long? = null,
    val lastVerifiedAtMillis: Long? = null
)