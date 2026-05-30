package com.andrey.beautyplanner.billing

expect class BillingManager() {
    suspend fun startConnection(): Boolean
    suspend fun loadProducts(productIds: List<String>): List<BillingProduct>
    suspend fun purchasePremium(productId: String): PurchaseResult
    suspend fun restorePurchases(): RestoreResult
    suspend fun getSubscriptionInfo(): SubscriptionInfo
    fun dispose()
}