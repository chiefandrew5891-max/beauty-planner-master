package com.andrey.beautyplanner.billing

import kotlinx.coroutines.CompletableDeferred
import kotlinx.datetime.Clock

actual class BillingManager actual constructor() {

    actual suspend fun startConnection(): Boolean {
        return StoreKitBridgeConnector.loadProducts != null &&
                StoreKitBridgeConnector.purchaseProduct != null &&
                StoreKitBridgeConnector.restorePurchases != null &&
                StoreKitBridgeConnector.currentSubscriptionInfo != null
    }

    actual suspend fun loadProducts(productIds: List<String>): List<BillingProduct> {
        val deferred = CompletableDeferred<List<Map<String, String>>>()
        val loader = StoreKitBridgeConnector.loadProducts ?: return emptyList()

        loader.invoke(productIds, deferred)
        val result = deferred.await()

        return result.mapNotNull { item ->
            val productId = item["productId"].orEmpty()
            if (productId.isBlank()) return@mapNotNull null

            BillingProduct(
                productId = productId,
                title = item["title"].orEmpty(),
                description = item["description"].orEmpty(),
                formattedPrice = item["formattedPrice"].orEmpty(),
                offerToken = "ios_storekit_offer"
            )
        }
    }

    actual suspend fun purchasePremium(
        productId: String,
        obfuscatedAccountId: String
    ): PurchaseResult {
        val purchaser = StoreKitBridgeConnector.purchaseProduct
            ?: return PurchaseResult.Error("StoreKit bridge is not connected.")

        return try {
            val deferred = CompletableDeferred<Map<String, String>>()
            purchaser.invoke(productId, obfuscatedAccountId, deferred)
            val result = deferred.await()

            println("BillingManager.ios purchasePremium result = $result")

            PurchaseResult.Success(
                productId = result["productId"] ?: productId,
                purchaseToken = result["purchaseToken"].orEmpty(),
                transactionId = result["transactionId"].orEmpty()
            )
        } catch (t: Throwable) {
            when (t.message.orEmpty()) {
                "USER_CANCELLED" -> PurchaseResult.Cancelled
                "PURCHASE_PENDING" -> PurchaseResult.Error("Purchase is pending approval.")
                else -> PurchaseResult.Error(t.message ?: "iOS purchase failed.")
            }
        }
    }

    actual suspend fun restorePurchases(): RestoreResult {
        val restorer = StoreKitBridgeConnector.restorePurchases
            ?: return RestoreResult.Error("StoreKit bridge is not connected.")

        return try {
            val deferred = CompletableDeferred<Map<String, String>>()
            restorer.invoke(deferred)
            deferred.await()
            RestoreResult.Restored
        } catch (t: Throwable) {
            if (t.message.orEmpty() == "NOTHING_TO_RESTORE") {
                RestoreResult.NothingToRestore
            } else {
                RestoreResult.Error(t.message ?: "Restore failed on iOS.")
            }
        }
    }

    actual suspend fun getSubscriptionInfo(): SubscriptionInfo {
        val getter = StoreKitBridgeConnector.currentSubscriptionInfo
            ?: return SubscriptionInfo(state = SubscriptionState.NONE)

        return try {
            val deferred = CompletableDeferred<Map<String, String>>()
            getter.invoke(deferred)
            val result = deferred.await()

            val state = when (result["state"].orEmpty().uppercase()) {
                "ACTIVE" -> SubscriptionState.ACTIVE
                "EXPIRED" -> SubscriptionState.EXPIRED
                "CANCELED" -> SubscriptionState.CANCELED
                "IN_GRACE_PERIOD" -> SubscriptionState.IN_GRACE_PERIOD
                "ON_HOLD" -> SubscriptionState.ON_HOLD
                else -> SubscriptionState.NONE
            }

            SubscriptionInfo(
                state = state,
                productId = result["productId"].orEmpty(),
                purchaseToken = result["purchaseToken"].orEmpty(),
                isAutoRenewing = result["isAutoRenewing"].equals("true", ignoreCase = true),
                startTimeMillis = null,
                expiryTimeMillis = null,
                lastVerifiedAtMillis = Clock.System.now().toEpochMilliseconds()
            )
        } catch (_: Throwable) {
            SubscriptionInfo(state = SubscriptionState.NONE)
        }
    }

    actual fun dispose() = Unit
}