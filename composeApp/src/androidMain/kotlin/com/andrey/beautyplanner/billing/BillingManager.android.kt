package com.andrey.beautyplanner.billing

import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.andrey.beautyplanner.AndroidAppContext
import com.andrey.beautyplanner.AppSettings
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class BillingManager actual constructor() {

    private var cachedProducts: Map<String, ProductDetails> = emptyMap()

    private var pendingPurchaseContinuation:
            ((PurchaseResult) -> Unit)? = null

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        val callback = pendingPurchaseContinuation ?: return@PurchasesUpdatedListener

        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val purchase = purchases.orEmpty().firstOrNull()
                if (purchase == null) {
                    pendingPurchaseContinuation = null
                    callback(PurchaseResult.Error("Purchase data is missing."))
                    return@PurchasesUpdatedListener
                }

                handleSuccessfulPurchase(
                    purchase = purchase,
                    productId = PREMIUM_SUBS_PRODUCT_ID
                ) { ok, message ->
                    pendingPurchaseContinuation = null
                    if (ok) callback(PurchaseResult.Success)
                    else callback(PurchaseResult.Error(message ?: "Purchase acknowledgement failed."))
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                pendingPurchaseContinuation = null
                callback(PurchaseResult.Cancelled)
            }

            else -> {
                pendingPurchaseContinuation = null
                callback(
                    PurchaseResult.Error(
                        billingResult.debugMessage.ifBlank { "Google Play Billing error." }
                    )
                )
            }
        }
    }

    private val billingClient: BillingClient by lazy {
        val context = AndroidAppContext.context
            ?: error("AndroidAppContext.context is not set before BillingManager init.")

        BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()
    }

    actual suspend fun startConnection(): Boolean =
        suspendCancellableCoroutine { cont ->
            if (billingClient.isReady) {
                cont.resume(true)
                return@suspendCancellableCoroutine
            }

            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    cont.resume(
                        billingResult.responseCode == BillingClient.BillingResponseCode.OK
                    )
                }

                override fun onBillingServiceDisconnected() {
                    if (cont.isActive) {
                        cont.resume(false)
                    }
                }
            })
        }

    actual suspend fun loadProducts(productIds: List<String>): List<BillingProduct> {
        if (!billingClient.isReady) return emptyList()

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                productIds.map { productId ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(ProductType.SUBS)
                        .build()
                }
            )
            .build()

        return suspendCancellableCoroutine { cont ->
            billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    cont.resume(emptyList())
                    return@queryProductDetailsAsync
                }

                cachedProducts = productDetailsList.associateBy { it.productId }

                val mapped = productDetailsList.map { details ->
                    val offer = details.oneTimePurchaseOfferDetails
                    BillingProduct(
                        productId = details.productId,
                        title = details.title,
                        description = details.description,
                        formattedPrice = offer?.formattedPrice ?: "",
                        priceAmountMicros = offer?.priceAmountMicros,
                        priceCurrencyCode = offer?.priceCurrencyCode
                    )
                }

                cont.resume(mapped)
            }
        }
    }

    actual suspend fun purchasePremium(productId: String): PurchaseResult {
        if (!billingClient.isReady) {
            return PurchaseResult.Error("Billing service is not connected.")
        }

        val activity = AndroidAppContext.activity
            ?: return PurchaseResult.Error("Activity is not available.")

        val productDetails = cachedProducts[productId]
            ?: return PurchaseResult.Error("Product details are not loaded.")

        return suspendCancellableCoroutine { cont ->
            pendingPurchaseContinuation = { result ->
                if (cont.isActive) cont.resume(result)
            }

            val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productParams))
                .build()

            val result = billingClient.launchBillingFlow(activity, flowParams)
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                pendingPurchaseContinuation = null
                if (cont.isActive) {
                    cont.resume(
                        PurchaseResult.Error(
                            result.debugMessage.ifBlank { "Unable to launch purchase flow." }
                        )
                    )
                }
            }
        }
    }

    actual suspend fun restorePurchases(): RestoreResult {
        if (!billingClient.isReady) {
            return RestoreResult.Error("Billing service is not connected.")
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(ProductType.SUBS)
            .build()

        return suspendCancellableCoroutine { cont ->
            billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    cont.resume(
                        RestoreResult.Error(
                            billingResult.debugMessage.ifBlank { "Failed to restore purchases." }
                        )
                    )
                    return@queryPurchasesAsync
                }

                val premiumPurchase = purchases.firstOrNull { purchase ->
                    purchase.products.contains(PREMIUM_SUBS_PRODUCT_ID) &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }

                if (premiumPurchase == null) {
                    cont.resume(RestoreResult.NothingToRestore)
                    return@queryPurchasesAsync
                }

                handleSuccessfulPurchase(
                    purchase = premiumPurchase,
                    productId = PREMIUM_SUBS_PRODUCT_ID
                ) { ok, message ->
                    if (ok) cont.resume(RestoreResult.Restored)
                    else cont.resume(
                        RestoreResult.Error(message ?: "Failed to finalize restored purchase.")
                    )
                }
            }
        }
    }

    private fun handleSuccessfulPurchase(
        purchase: Purchase,
        productId: String,
        done: (Boolean, String?) -> Unit
    ) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            done(false, "Purchase is not completed.")
            return
        }

        fun finalizeEntitlement() {
            val now = System.currentTimeMillis()
            val isAutoRenewing = purchase.isAutoRenewing

            AppSettings.premiumSubscribedProductId = productId
            AppSettings.premiumSubscriptionToken = purchase.purchaseToken
            AppSettings.premiumSubscriptionStartMillis = now
            AppSettings.premiumSubscriptionAutoRenewing = isAutoRenewing
            AppSettings.premiumSubscriptionState = "ACTIVE"
            AppSettings.premiumLastVerifiedAtMillis = now

            // ВАЖНО:
            // на клиенте точную expiry дату для subscription надёжно не вычисляем.
            // временно можно поставить эвристику 365 дней,
            // но лучше потом получать с backend / Play Developer API.
            AppSettings.premiumSubscriptionExpiryMillis = now + 365L * 24L * 60L * 60L * 1000L

            AppSettings.persist()
            done(true, null)
        }

        if (purchase.isAcknowledged) {
            finalizeEntitlement()
            return
        }

        val ackParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(ackParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                finalizeEntitlement()
            } else {
                done(
                    false,
                    billingResult.debugMessage.ifBlank { "Acknowledge failed." }
                )
            }
        }
    }

    actual fun dispose() {
        pendingPurchaseContinuation = null
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }
    actual suspend fun getSubscriptionInfo(): SubscriptionInfo {
        if (!billingClient.isReady) {
            return SubscriptionInfo(state = SubscriptionState.NONE)
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(ProductType.SUBS)
            .build()

        return suspendCancellableCoroutine { cont ->
            billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    cont.resume(SubscriptionInfo(state = SubscriptionState.NONE))
                    return@queryPurchasesAsync
                }

                val subPurchase = purchases.firstOrNull { purchase ->
                    purchase.products.contains(PREMIUM_SUBS_PRODUCT_ID) &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }

                if (subPurchase == null) {
                    cont.resume(SubscriptionInfo(state = SubscriptionState.NONE))
                    return@queryPurchasesAsync
                }

                val now = System.currentTimeMillis()
                val cachedExpiry = AppSettings.premiumSubscriptionExpiryMillis
                val expiry = if (cachedExpiry > now) cachedExpiry else null

                cont.resume(
                    SubscriptionInfo(
                        state = if (expiry == null || expiry > now) {
                            SubscriptionState.ACTIVE
                        } else {
                            SubscriptionState.EXPIRED
                        },
                        productId = PREMIUM_SUBS_PRODUCT_ID,
                        purchaseToken = subPurchase.purchaseToken,
                        isAutoRenewing = subPurchase.isAutoRenewing,
                        startTimeMillis = subPurchase.purchaseTime,
                        expiryTimeMillis = expiry,
                        lastVerifiedAtMillis = now
                    )
                )
            }
        }
    }
}