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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class BillingManager actual constructor() {

    private var cachedProductDetails: Map<String, ProductDetails> = emptyMap()
    private var cachedBillingProducts: Map<String, BillingProduct> = emptyMap()

    private var pendingPurchaseContinuation: ((PurchaseResult) -> Unit)? = null

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
                    if (ok) {
                        callback(
                            PurchaseResult.Success(
                                productId = PREMIUM_SUBS_PRODUCT_ID,
                                purchaseToken = purchase.purchaseToken,
                                transactionId = ""
                            )
                        )
                    } else {
                        callback(
                            PurchaseResult.Error(
                                message ?: "Purchase acknowledgement failed."
                            )
                        )
                    }
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
                        buildBillingErrorMessage(
                            operation = "purchase update",
                            billingResult = billingResult
                        )
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
            billingClient.queryProductDetailsAsync(params) { billingResult, result ->
                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    cachedProductDetails = emptyMap()
                    cachedBillingProducts = emptyMap()
                    cont.resume(emptyList())
                    return@queryProductDetailsAsync
                }

                val detailsList: List<ProductDetails> = result.productDetailsList.orEmpty()

                cachedProductDetails = detailsList.associateBy { details ->
                    details.productId
                }

                val mappedProducts = detailsList.mapNotNull { details ->
                    val subscriptionOffers = details.subscriptionOfferDetails.orEmpty()
                    val selectedOffer = subscriptionOffers.firstOrNull() ?: return@mapNotNull null
                    val pricingPhase = selectedOffer.pricingPhases.pricingPhaseList.firstOrNull()
                        ?: return@mapNotNull null

                    BillingProduct(
                        productId = details.productId,
                        title = details.title,
                        description = details.description,
                        formattedPrice = pricingPhase.formattedPrice,
                        offerToken = selectedOffer.offerToken,
                        basePlanId = selectedOffer.basePlanId,
                        offerId = selectedOffer.offerId,
                        priceAmountMicros = pricingPhase.priceAmountMicros,
                        priceCurrencyCode = pricingPhase.priceCurrencyCode
                    )
                }

                cachedBillingProducts = mappedProducts.associateBy { it.productId }
                cont.resume(mappedProducts)
            }
        }
    }

    actual suspend fun purchasePremium(
        productId: String,
        obfuscatedAccountId: String
    ): PurchaseResult {
        if (!billingClient.isReady) {
            return PurchaseResult.Error("Billing service is not connected.")
        }

        val activity = AndroidAppContext.activity
            ?: return PurchaseResult.Error("Activity is not available.")

        val productDetails = cachedProductDetails[productId]
            ?: return PurchaseResult.Error("Product details are not loaded.")

        val billingProduct = cachedBillingProducts[productId]
            ?: return PurchaseResult.Error("Subscription offer is not loaded.")

        if (billingProduct.offerToken.isBlank()) {
            return PurchaseResult.Error("Subscription offer token is missing.")
        }

        return suspendCancellableCoroutine { cont ->
            pendingPurchaseContinuation = { result ->
                if (cont.isActive) cont.resume(result)
            }

            val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(billingProduct.offerToken)
                .build()

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productParams))
                .setObfuscatedAccountId(obfuscatedAccountId)
                .build()

            val result = billingClient.launchBillingFlow(activity, flowParams)

            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                pendingPurchaseContinuation = null
                if (cont.isActive) {
                    cont.resume(
                        PurchaseResult.Error(
                            buildBillingErrorMessage(
                                operation = "launchBillingFlow",
                                billingResult = result
                            )
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
                            buildBillingErrorMessage(
                                operation = "restore purchases",
                                billingResult = billingResult
                            )
                        )
                    )
                    return@queryPurchasesAsync
                }

                val owned = purchases.orEmpty().firstOrNull {
                    it.products.contains(PREMIUM_SUBS_PRODUCT_ID)
                }

                if (owned == null) {
                    cont.resume(RestoreResult.NothingToRestore)
                    return@queryPurchasesAsync
                }

                handleSuccessfulPurchase(
                    purchase = owned,
                    productId = PREMIUM_SUBS_PRODUCT_ID
                ) { ok, message ->
                    if (ok) {
                        cont.resume(RestoreResult.Restored)
                    } else {
                        cont.resume(
                            RestoreResult.Error(
                                message ?: "Restore acknowledgement failed."
                            )
                        )
                    }
                }
            }
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

                val purchase = purchases.orEmpty().firstOrNull {
                    it.products.contains(PREMIUM_SUBS_PRODUCT_ID)
                }

                if (purchase == null) {
                    cont.resume(SubscriptionInfo(state = SubscriptionState.NONE))
                    return@queryPurchasesAsync
                }

                val state = when (purchase.purchaseState) {
                    Purchase.PurchaseState.PURCHASED -> SubscriptionState.ACTIVE
                    Purchase.PurchaseState.PENDING -> SubscriptionState.ON_HOLD
                    else -> SubscriptionState.NONE
                }

                cont.resume(
                    SubscriptionInfo(
                        state = state,
                        productId = PREMIUM_SUBS_PRODUCT_ID,
                        purchaseToken = purchase.purchaseToken,
                        isAutoRenewing = purchase.isAutoRenewing,
                        startTimeMillis = purchase.purchaseTime,
                        expiryTimeMillis = null,
                        lastVerifiedAtMillis = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    actual fun dispose() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
        pendingPurchaseContinuation = null
        cachedProductDetails = emptyMap()
        cachedBillingProducts = emptyMap()
    }

    private fun handleSuccessfulPurchase(
        purchase: Purchase,
        productId: String,
        onDone: (Boolean, String?) -> Unit
    ) {
        if (!purchase.products.contains(productId)) {
            onDone(false, "Purchased product does not match requested subscription.")
            return
        }

        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            onDone(false, "Purchase is not completed.")
            return
        }

        if (purchase.isAcknowledged) {
            onDone(true, null)
            return
        }

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                onDone(true, null)
            } else {
                onDone(
                    false,
                    buildBillingErrorMessage(
                        operation = "acknowledgePurchase",
                        billingResult = billingResult
                    )
                )
            }
        }
    }

    private fun buildBillingErrorMessage(
        operation: String,
        billingResult: BillingResult
    ): String {
        val code = billingResult.responseCode
        val debugMessage = billingResult.debugMessage.ifBlank { "No debug message from Google Play." }

        val humanReadableHint = when (code) {
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE ->
                "Google Play Billing is unavailable on this device/account or this app build is not eligible for billing tests."

            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE ->
                "The subscription item is unavailable for this account, country, track, or installed app version."

            BillingClient.BillingResponseCode.DEVELOPER_ERROR ->
                "Google Play reported a developer/configuration error. Usually this means Play Console setup, signing, track installation source, or offer configuration mismatch."

            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED ->
                "This device or Play Store version does not support the requested billing feature."

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED ->
                "This Google Play account already owns the subscription."

            BillingClient.BillingResponseCode.NETWORK_ERROR ->
                "Network error while talking to Google Play."

            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE ->
                "Google Play service is temporarily unavailable."

            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED ->
                "Google Play Billing service got disconnected."

            else ->
                "Google Play Billing returned an error."
        }

        return "$humanReadableHint [operation=$operation, code=$code, debugMessage=$debugMessage]"
    }
}