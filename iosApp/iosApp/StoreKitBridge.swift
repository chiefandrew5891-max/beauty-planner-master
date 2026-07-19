import Foundation
import StoreKit

@objc final class StoreKitBridge: NSObject {

    @objc static func loadProducts(
        _ productIds: [String],
        completion: @escaping (NSArray?, NSString?) -> Void
    ) {
        Task {
            do {
                print("StoreKitBridge.loadProducts: requested ids = \(productIds)")

                let products = try await Product.products(for: productIds)

                print("StoreKitBridge.loadProducts: loaded products count = \(products.count)")

                let mapped: [[String: String]] = products.map { product in
                    [
                        "productId": product.id,
                        "title": product.displayName,
                        "description": product.description,
                        "formattedPrice": product.displayPrice
                    ]
                }

                completion(mapped as NSArray, nil)
            } catch {
                print("StoreKitBridge.loadProducts error: \(error.localizedDescription)")
                completion(nil, error.localizedDescription as NSString)
            }
        }
    }

    @objc static func purchaseProduct(
        _ productId: String,
        appAccountToken: String,
        completion: @escaping (NSDictionary?, NSString?) -> Void
    ) {
        Task {
            do {

                let products = try await Product.products(for: [productId])

                guard let product = products.first else {
                    completion(nil, "Product not found in App Store")
                    return
                }

                let uuid = UUID(uuidString: appAccountToken)

                let result: Product.PurchaseResult
                if let uuid {
                    result = try await product.purchase(options: [.appAccountToken(uuid)])
                } else {
                    result = try await product.purchase()
                }

                switch result {
                case .success(let verification):
                    let transaction = try checkVerified(verification)

                    await transaction.finish()

                    let dict: NSDictionary = [
                        "productId": transaction.productID,
                        "purchaseToken": String(transaction.originalID),
                        "transactionId": String(transaction.id),
                        "originalTransactionId": String(transaction.originalID),
                        "subscriptionActive": "true"
                    ]

                    completion(dict, nil)

                case .userCancelled:
                    completion(nil, "USER_CANCELLED")

                case .pending:
                    completion(nil, "PURCHASE_PENDING")

                @unknown default:
                    completion(nil, "Unknown purchase result")
                }
            } catch {
                print("StoreKitBridge.purchaseProduct error: \(error.localizedDescription)")
                completion(nil, error.localizedDescription as NSString)
            }
        }
    }

    @objc static func restorePurchases(
        completion: @escaping (NSDictionary?, NSString?) -> Void
    ) {
        Task {
            do {

                try await AppStore.sync()

                var restoredTransaction: Transaction?

                for await entitlement in Transaction.currentEntitlements {
                    let transaction = try? checkVerified(entitlement)
                    if let transaction {
                        restoredTransaction = transaction
                        break
                    }
                }

                guard let transaction = restoredTransaction else {
                    completion(nil, "NOTHING_TO_RESTORE")
                    return
                }

                let dict: NSDictionary = [
                    "productId": transaction.productID,
                    "purchaseToken": String(transaction.originalID),
                    "transactionId": String(transaction.id),
                    "originalTransactionId": String(transaction.originalID),
                    "subscriptionActive": "true"
                ]

                completion(dict, nil)
            } catch {
                print("StoreKitBridge.restorePurchases error: \(error.localizedDescription)")
                completion(nil, error.localizedDescription as NSString)
            }
        }
    }

    @objc static func currentSubscriptionInfo(
        completion: @escaping (NSDictionary?, NSString?) -> Void
    ) {
        Task {
            var activeTransaction: Transaction?

            for await entitlement in Transaction.currentEntitlements {
                let transaction = try? checkVerified(entitlement)
                if let transaction {
                    activeTransaction = transaction
                    break
                }
            }

            guard let transaction = activeTransaction else {
                completion([
                    "state": "NONE"
                ], nil)
                return
            }

            let dict: NSDictionary = [
                "state": "ACTIVE",
                "productId": transaction.productID,
                "purchaseToken": String(transaction.originalID),
                "transactionId": String(transaction.id),
                "originalTransactionId": String(transaction.originalID),
                "isAutoRenewing": "true"
            ]

            completion(dict, nil)
        }
    }

    private static func checkVerified<T>(
        _ result: VerificationResult<T>
    ) throws -> T {
        switch result {
        case .unverified:
            throw NSError(
                domain: "StoreKitBridge",
                code: 1001,
                userInfo: [NSLocalizedDescriptionKey: "Transaction verification failed"]
            )
        case .verified(let safe):
            return safe
        }
    }
}