package com.andrey.beautyplanner.billing

actual class BillingManager actual constructor() {

    actual suspend fun startConnection(): Boolean = false

    actual suspend fun loadProducts(productIds: List<String>): List<BillingProduct> = emptyList()

    actual suspend fun purchasePremium(productId: String): PurchaseResult {
        return PurchaseResult.Error("Billing is not available on iOS in this version.")
    }

    actual suspend fun restorePurchases(): RestoreResult {
        return RestoreResult.Error("Restore is not available on iOS in this version.")
    }
    actual suspend fun getSubscriptionInfo(): SubscriptionInfo {
        return SubscriptionInfo(state = SubscriptionState.NONE)
    }

    actual fun dispose() = Unit
}