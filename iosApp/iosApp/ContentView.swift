import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let signInBridge = GoogleSignInBridge()
        let appleBridge = AppleSignInBridge()

        signInBridge.startGoogleSignIn = { deferred in
            GoogleAuthBridge.signInWithGoogle { result, error in
                if let error = error {
                    let errorResult = SignInResult.Error(message: error as String)
                    deferred.complete(value: errorResult)
                    return
                }

                guard let result = result else {
                    let errorResult = SignInResult.Error(message: "Google sign-in returned empty result")
                    deferred.complete(value: errorResult)
                    return
                }

                let uid = result["uid"] as? String ?? ""
                let email = result["email"] as? String ?? ""
                let displayName = result["displayName"] as? String ?? ""
                let providerRaw = result["provider"] as? String ?? "GOOGLE"

                let provider: SignInProvider
                switch providerRaw {
                case "ANONYMOUS":
                    provider = .anonymous
                case "EMAIL":
                    provider = .email
                case "APPLE":
                    provider = .apple
                default:
                    provider = .google
                }

                let user = AuthUser(
                    uid: uid,
                    provider: provider,
                    email: email,
                    displayName: displayName
                )

                let successResult = SignInResult.Success(user: user)
                deferred.complete(value: successResult)
            }
        }

        appleBridge.startAppleSignIn = { deferred in
            AppleAuthBridge.signInWithApple { result, error in
                if let error = error {
                    let lower = (error as String).lowercased()
                    if lower.contains("canceled") || lower.contains("cancelled") {
                        deferred.complete(value: SignInResult.Cancelled())
                        return
                    }

                    let errorResult = SignInResult.Error(message: error as String)
                    deferred.complete(value: errorResult)
                    return
                }

                guard let result = result else {
                    let errorResult = SignInResult.Error(message: "Apple sign-in returned empty result")
                    deferred.complete(value: errorResult)
                    return
                }

                let uid = result["uid"] as? String ?? ""
                let email = result["email"] as? String ?? ""
                let displayName = result["displayName"] as? String ?? ""

                let user = AuthUser(
                    uid: uid,
                    provider: .apple,
                    email: email,
                    displayName: displayName
                )

                let successResult = SignInResult.Success(user: user)
                deferred.complete(value: successResult)
            }
        }

        BackendBridgeConnector().callBackend = { name, payload, deferred in
            if name == "__currentUser" {
                if let currentUser = GoogleAuthBridge.currentUser() {
                    var result: [String: String] = [:]
                    result["uid"] = currentUser["uid"] as? String ?? ""
                    result["email"] = currentUser["email"] as? String ?? ""
                    result["displayName"] = currentUser["displayName"] as? String ?? ""
                    result["provider"] = currentUser["provider"] as? String ?? ""
                    deferred.complete(value: result)
                } else {
                    deferred.complete(value: [:])
                }
                return
            }

            let nsPayload = NSMutableDictionary()
            for (key, value) in payload {
                nsPayload[key] = value
            }

            GoogleAuthBridge.callBackend(name, payload: nsPayload) { result, error in
                if let error = error {
                    deferred.completeExceptionally(
                        exception: KotlinIllegalStateException(message: error as String)
                    )
                    return
                }

                guard let result = result else {
                    deferred.completeExceptionally(
                        exception: KotlinIllegalStateException(message: "Backend function returned null result")
                    )
                    return
                }

                var mapped: [String: String] = [:]
                for (keyAny, valueAny) in result {
                    guard let key = keyAny as? String else { continue }

                    if let value = valueAny as? String {
                        mapped[key] = value
                    } else if let value = valueAny as? NSNumber {
                        mapped[key] = value.stringValue
                    } else if let value = valueAny as? NSString {
                        mapped[key] = value as String
                    } else {
                        mapped[key] = "\(valueAny)"
                    }
                }

                deferred.complete(value: mapped)
            }
        }
                CloudSyncBridgeConnector().pullAll = { userId, deferred in
                    FirestoreBridge.pullAll(userId: userId) { result, error in
                        if let error = error {
                            deferred.completeExceptionally(
                                exception: KotlinIllegalStateException(message: error)
                            )
                            return
                        }

                        deferred.complete(value: result ?? [:])
                    }
                }

                CloudSyncBridgeConnector().pushAppointments = { userId, appointments, deferred in
                    let mappedAppointments: [[String: String]] = appointments.map { item in
                        var mapped: [String: String] = [:]
                        for (key, value) in item {
                            mapped[key] = value
                        }
                        return mapped
                    }

                    FirestoreBridge.pushAppointments(userId: userId, appointments: mappedAppointments) { result, error in
                        if let error = error {
                            deferred.completeExceptionally(
                                exception: KotlinIllegalStateException(message: error)
                            )
                            return
                        }

                        deferred.complete(value: result ?? [:])
                    }
                }

                CloudSyncBridgeConnector().pushSettings = { userId, settings, deferred in
                    var mappedSettings: [String: String] = [:]
                    for (key, value) in settings {
                        mappedSettings[key] = value
                    }

                    FirestoreBridge.pushSettings(userId: userId, settings: mappedSettings) { result, error in
                        if let error = error {
                            deferred.completeExceptionally(
                                exception: KotlinIllegalStateException(message: error)
                            )
                            return
                        }

                        deferred.complete(value: result ?? [:])
                    }
                }

        StoreKitBridgeConnector().loadProducts = { productIds, deferred in
            StoreKitBridge.loadProducts(productIds) { result, error in
                if let error = error {
                    deferred.completeExceptionally(
                        exception: KotlinIllegalStateException(message: error as String)
                    )
                    return
                }

                guard let result = result as? [[String: String]] else {
                    deferred.complete(value: [])
                    return
                }

                deferred.complete(value: result)
            }
        }

        StoreKitBridgeConnector().purchaseProduct = { productId, appAccountToken, deferred in
            StoreKitBridge.purchaseProduct(productId, appAccountToken: appAccountToken) { result, error in
                if let error = error {
                    deferred.completeExceptionally(
                        exception: KotlinIllegalStateException(message: error as String)
                    )
                    return
                }

                guard let result = result as? [String: String] else {
                    deferred.completeExceptionally(
                        exception: KotlinIllegalStateException(message: "Purchase result is empty")
                    )
                    return
                }

                deferred.complete(value: result)
            }
        }

        StoreKitBridgeConnector().restorePurchases = { deferred in
            StoreKitBridge.restorePurchases { result, error in
                if let error = error {
                    deferred.completeExceptionally(
                        exception: KotlinIllegalStateException(message: error as String)
                    )
                    return
                }

                guard let result = result as? [String: String] else {
                    deferred.completeExceptionally(
                        exception: KotlinIllegalStateException(message: "Restore result is empty")
                    )
                    return
                }

                deferred.complete(value: result)
            }
        }

        StoreKitBridgeConnector().currentSubscriptionInfo = { deferred in
            StoreKitBridge.currentSubscriptionInfo { result, error in
                if let error = error {
                    deferred.completeExceptionally(
                        exception: KotlinIllegalStateException(message: error as String)
                    )
                    return
                }

                guard let result = result as? [String: String] else {
                    deferred.complete(value: [:])
                    return
                }

                deferred.complete(value: result)
            }
        }

        return MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}