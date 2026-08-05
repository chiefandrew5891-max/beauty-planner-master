import Foundation
import StoreKit

@objc final class StoreKitBridge: NSObject {

    private static let premiumProductId = "beautyplanner_premium_yearly"

    private struct SubscriptionSnapshot {
        let state: String
        let productId: String
        let purchaseToken: String
        let transactionId: String
        let originalTransactionId: String
        let isAutoRenewing: Bool
        let startTimeMillis: Int64?
        let expiryTimeMillis: Int64?
        let lastVerifiedAtMillis: Int64
    }

    private static var updatesTask: Task<Void, Never>?
    private static let lock = NSLock()
    private static var cachedSnapshot: SubscriptionSnapshot?

    @objc static func startObservingTransactions() {
        lock.lock()
        defer { lock.unlock() }

        guard updatesTask == nil else {
            print("StoreKitBridge: transaction observer already started")
            return
        }

        print("StoreKitBridge: starting Transaction.updates observer")

        updatesTask = Task.detached(priority: .background) {
            for await update in Transaction.updates {
                do {
                    let transaction = try checkVerified(update)
                    print("StoreKitBridge.Transaction.updates: verified transaction for \(transaction.productID) id=\(transaction.id)")

                    if transaction.productID == premiumProductId {
                        let snapshot = await buildSnapshot(for: transaction)
                        storeSnapshot(snapshot)
                        print("StoreKitBridge.Transaction.updates: cached premium snapshot state=\(snapshot.state)")
                    } else {
                        print("StoreKitBridge.Transaction.updates: ignoring unrelated product \(transaction.productID)")
                    }

                    await transaction.finish()
                } catch {
                    print("StoreKitBridge.Transaction.updates error: \(error.localizedDescription)")
                }
            }
        }
    }

    @objc static func stopObservingTransactions() {
        lock.lock()
        defer { lock.unlock() }

        updatesTask?.cancel()
        updatesTask = nil
        print("StoreKitBridge: transaction observer stopped")
    }

    @objc static func loadProducts(
        _ productIds: [String],
        completion: @escaping (NSArray?, NSString?) -> Void
    ) {
        Task {
            do {
                print("StoreKitBridge.loadProducts: requested ids = \(productIds)")

                let products = try await Product.products(for: productIds)

                print("StoreKitBridge.loadProducts: loaded products count = \(products.count)")
                print("StoreKitBridge.loadProducts: loaded ids = \(products.map { $0.id })")

                let mapped: [[String: String]] = products.map { product in
                    [
                        "productId": product.id,
                        "title": product.displayName,
                        "description": product.description,
                        "formattedPrice": product.displayPrice,
                        "type": product.type.debugName
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
                print("StoreKitBridge.purchaseProduct: productId=\(productId) appAccountToken=\(appAccountToken)")

                let products = try await Product.products(for: [productId])

                guard let product = products.first else {
                    print("StoreKitBridge.purchaseProduct: product not found in App Store for id=\(productId)")
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
                    let snapshot = await buildSnapshot(for: transaction)
                    storeSnapshot(snapshot)

                    print("StoreKitBridge.purchaseProduct: success for \(transaction.productID), tx=\(transaction.id), state=\(snapshot.state)")

                    await transaction.finish()

                    print("StoreKitBridge.purchaseProduct payload:", [
                        "productId": snapshot.productId,
                        "purchaseToken": snapshot.purchaseToken,
                        "transactionId": snapshot.transactionId,
                        "originalTransactionId": snapshot.originalTransactionId
                    ])

                    let dict: NSDictionary = [
                        "productId": snapshot.productId,
                        "purchaseToken": snapshot.purchaseToken,
                        "transactionId": snapshot.transactionId,
                        "originalTransactionId": snapshot.originalTransactionId,
                        "subscriptionActive": snapshot.state == "ACTIVE" ? "true" : "false",
                        "state": snapshot.state,
                        "isAutoRenewing": snapshot.isAutoRenewing ? "true" : "false",
                        "startTimeMillis": snapshot.startTimeMillis.map(String.init) ?? "",
                        "expiryTimeMillis": snapshot.expiryTimeMillis.map(String.init) ?? "",
                        "lastVerifiedAtMillis": String(snapshot.lastVerifiedAtMillis)
                    ]

                    completion(dict, nil)

                case .userCancelled:
                    print("StoreKitBridge.purchaseProduct: user cancelled")
                    completion(nil, "USER_CANCELLED")

                case .pending:
                    print("StoreKitBridge.purchaseProduct: purchase pending")
                    completion(nil, "PURCHASE_PENDING")

                @unknown default:
                    print("StoreKitBridge.purchaseProduct: unknown purchase result")
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
                print("StoreKitBridge.restorePurchases: starting AppStore.sync()")
                try await AppStore.sync()

                let snapshot = await currentPremiumSnapshot()

                guard let snapshot else {
                    print("StoreKitBridge.restorePurchases: nothing to restore")
                    completion(nil, "NOTHING_TO_RESTORE")
                    return
                }

                storeSnapshot(snapshot)

                let dict: NSDictionary = [
                    "productId": snapshot.productId,
                    "purchaseToken": snapshot.purchaseToken,
                    "transactionId": snapshot.transactionId,
                    "originalTransactionId": snapshot.originalTransactionId,
                    "subscriptionActive": snapshot.state == "ACTIVE" ? "true" : "false",
                    "state": snapshot.state,
                    "isAutoRenewing": snapshot.isAutoRenewing ? "true" : "false",
                    "startTimeMillis": snapshot.startTimeMillis.map(String.init) ?? "",
                    "expiryTimeMillis": snapshot.expiryTimeMillis.map(String.init) ?? "",
                    "lastVerifiedAtMillis": String(snapshot.lastVerifiedAtMillis)
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
            let snapshot = await currentPremiumSnapshot() ?? cachedSnapshot

            guard let snapshot else {
                completion([
                    "state": "NONE",
                    "productId": "",
                    "purchaseToken": "",
                    "transactionId": "",
                    "originalTransactionId": "",
                    "isAutoRenewing": "false",
                    "startTimeMillis": "",
                    "expiryTimeMillis": "",
                    "lastVerifiedAtMillis": String(nowMillis())
                ], nil)
                return
            }

            storeSnapshot(snapshot)

            let dict: NSDictionary = [
                "state": snapshot.state,
                "productId": snapshot.productId,
                "purchaseToken": snapshot.purchaseToken,
                "transactionId": snapshot.transactionId,
                "originalTransactionId": snapshot.originalTransactionId,
                "isAutoRenewing": snapshot.isAutoRenewing ? "true" : "false",
                "startTimeMillis": snapshot.startTimeMillis.map(String.init) ?? "",
                "expiryTimeMillis": snapshot.expiryTimeMillis.map(String.init) ?? "",
                "lastVerifiedAtMillis": String(snapshot.lastVerifiedAtMillis)
            ]

            completion(dict, nil)
        }
    }

    private static func currentPremiumSnapshot() async -> SubscriptionSnapshot? {
        do {
            for await entitlement in Transaction.currentEntitlements {
                let transaction = try checkVerified(entitlement)

                guard transaction.productID == premiumProductId else {
                    continue
                }

                let snapshot = await buildSnapshot(for: transaction)
                print("StoreKitBridge.currentPremiumSnapshot: found premium entitlement state=\(snapshot.state)")
                return snapshot
            }

            print("StoreKitBridge.currentPremiumSnapshot: no premium entitlement found")
            return nil
        } catch {
            print("StoreKitBridge.currentPremiumSnapshot error: \(error.localizedDescription)")
            return nil
        }
    }

    private static func buildSnapshot(for transaction: Transaction) async -> SubscriptionSnapshot {
        let now = nowMillis()
        print("StoreKitBridge.buildSnapshot raw transaction:", [
            "productID": transaction.productID,
            "id": String(describing: transaction.id),
            "originalID": String(describing: transaction.originalID),
            "purchaseDate": String(describing: transaction.purchaseDate),
            "expirationDate": String(describing: transaction.expirationDate),
            "revocationDate": String(describing: transaction.revocationDate)
        ])
        let expirationMillis = transaction.expirationDate.map { Int64($0.timeIntervalSince1970 * 1000.0) }
        let purchaseMillis = Int64(transaction.purchaseDate.timeIntervalSince1970 * 1000.0)
        let revoked = transaction.revocationDate != nil
        let expired = if let expirationMillis {
            expirationMillis <= now
        } else {
            false
        }

        let renewalInfo = await currentRenewalInfo(for: transaction.productID)

        let state: String
        if revoked {
            state = "CANCELED"
        } else if expired {
            state = "EXPIRED"
        } else {
            state = "ACTIVE"
        }

        let extractedIds = extractTransactionIds(from: transaction)

        let transactionIdValue = extractedIds.transactionId.trimmingCharacters(in: .whitespacesAndNewlines)
        let originalTransactionIdValue = extractedIds.originalTransactionId.trimmingCharacters(in: .whitespacesAndNewlines)

        let normalizedTransactionId =
            transactionIdValue.isEmpty || transactionIdValue == "0"
                ? originalTransactionIdValue
                : transactionIdValue

        let normalizedOriginalTransactionId =
            originalTransactionIdValue.isEmpty || originalTransactionIdValue == "0"
                ? normalizedTransactionId
                : originalTransactionIdValue

        print("StoreKitBridge.buildSnapshot normalized ids:", [
            "transactionIdValue": transactionIdValue,
            "originalTransactionIdValue": originalTransactionIdValue,
            "normalizedTransactionId": normalizedTransactionId,
            "normalizedOriginalTransactionId": normalizedOriginalTransactionId
        ])

        return SubscriptionSnapshot(
            state: state,
            productId: transaction.productID,
            purchaseToken: normalizedTransactionId == "0" ? "" : normalizedTransactionId,
            transactionId: normalizedTransactionId == "0" ? "" : normalizedTransactionId,
            originalTransactionId: normalizedOriginalTransactionId == "0" ? "" : normalizedOriginalTransactionId,
            isAutoRenewing: renewalInfo?.willAutoRenew ?? false,
            startTimeMillis: purchaseMillis,
            expiryTimeMillis: expirationMillis,
            lastVerifiedAtMillis: now
        )
    }

    private static func currentRenewalInfo(for productId: String) async -> Product.SubscriptionInfo.RenewalInfo? {
        do {
            let products = try await Product.products(for: [productId])
            guard let product = products.first,
                  let subscription = product.subscription else {
                return nil
            }

            let statuses = try await subscription.status

            for status in statuses {
                let renewalInfo = try? checkVerified(status.renewalInfo)
                let transaction = try? checkVerified(status.transaction)

                if transaction?.productID == productId, let renewalInfo {
                    return renewalInfo
                }
            }

            return nil
        } catch {
            print("StoreKitBridge.currentRenewalInfo error: \(error.localizedDescription)")
            return nil
        }
    }

    private static func extractTransactionIds(from transaction: Transaction) -> (transactionId: String, originalTransactionId: String) {
        let fallbackTransactionId = String(describing: transaction.id).trimmingCharacters(in: .whitespacesAndNewlines)
        let fallbackOriginalId = String(describing: transaction.originalID).trimmingCharacters(in: .whitespacesAndNewlines)

        guard
            let jsonObject = try? JSONSerialization.jsonObject(with: transaction.jsonRepresentation),
            let json = jsonObject as? [String: Any]
        else {
            print("StoreKitBridge.extractTransactionIds: failed to parse jsonRepresentation")
            return (fallbackTransactionId, fallbackOriginalId)
        }

        let rawTransactionId =
            (json["transactionId"] as? String) ??
                (json["transactionId"] as? NSNumber)?.stringValue ??
                (json["id"] as? String) ??
                (json["id"] as? NSNumber)?.stringValue ??
                fallbackTransactionId

        let rawOriginalTransactionId =
            (json["originalTransactionId"] as? String) ??
                (json["originalTransactionId"] as? NSNumber)?.stringValue ??
                fallbackOriginalId

        print("StoreKitBridge.extractTransactionIds json:", json)
        print("StoreKitBridge.extractTransactionIds resolved:", [
            "rawTransactionId": rawTransactionId,
            "rawOriginalTransactionId": rawOriginalTransactionId,
            "fallbackTransactionId": fallbackTransactionId,
            "fallbackOriginalId": fallbackOriginalId
        ])

        return (rawTransactionId, rawOriginalTransactionId)
    }

    private static func storeSnapshot(_ snapshot: SubscriptionSnapshot) {
        lock.lock()
        cachedSnapshot = snapshot
        lock.unlock()
    }

    private static func nowMillis() -> Int64 {
        Int64(Date().timeIntervalSince1970 * 1000.0)
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

private extension Product.ProductType {
    var debugName: String {
        switch self {
        case .consumable: return "consumable"
        case .nonConsumable: return "nonConsumable"
        case .nonRenewable: return "nonRenewable"
        case .autoRenewable: return "autoRenewable"
        default: return "unknown"
        }
    }
}
