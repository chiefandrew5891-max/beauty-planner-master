import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let signInBridge = GoogleSignInBridge()
        let appleBridge = AppleSignInBridge()

        ContactsPermissionHelper.requestPermission { granted in
            print("Contacts permission granted: \(granted)")
        }

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
        EmailAuthBridgeConnector().signIn = { email, password, deferred in
            GoogleAuthBridge.signInWithEmail(email, password: password) { result, error in
                DispatchQueue.main.async {
                    if let error = error {
                        deferred.completeExceptionally(
                            exception: KotlinIllegalStateException(message: error as String)
                        )
                        return
                    }

                    guard let result = result else {
                        deferred.completeExceptionally(
                            exception: KotlinIllegalStateException(message: "Email sign-in returned null result")
                        )
                        return
                    }

                    var mapped: [String: String] = [:]
                    for (keyAny, valueAny) in result {
                        guard let key = keyAny as? String else { continue }

                        if let value = valueAny as? String {
                            mapped[key] = value
                        } else if let value = valueAny as? NSString {
                            mapped[key] = value as String
                        } else if let value = valueAny as? NSNumber {
                            mapped[key] = value.stringValue
                        } else {
                            mapped[key] = "\(valueAny)"
                        }
                    }

                    deferred.complete(value: mapped)
                }
            }
        }

        EmailAuthBridgeConnector().registerUser = { email, password, deferred in
            GoogleAuthBridge.registerWithEmail(email, password: password) { result, error in
                DispatchQueue.main.async {
                    if let error = error {
                        deferred.completeExceptionally(
                            exception: KotlinIllegalStateException(message: error as String)
                        )
                        return
                    }

                    guard let result = result else {
                        deferred.completeExceptionally(
                            exception: KotlinIllegalStateException(message: "Email registration returned null result")
                        )
                        return
                    }

                    var mapped: [String: String] = [:]
                    for (keyAny, valueAny) in result {
                        guard let key = keyAny as? String else { continue }

                        if let value = valueAny as? String {
                            mapped[key] = value
                        } else if let value = valueAny as? NSString {
                            mapped[key] = value as String
                        } else if let value = valueAny as? NSNumber {
                            mapped[key] = value.stringValue
                        } else {
                            mapped[key] = "\(valueAny)"
                        }
                    }

                    deferred.complete(value: mapped)
                }
            }
        }

        EmailAuthBridgeConnector().sendPasswordReset = { email, deferred in
            GoogleAuthBridge.sendPasswordReset(email) { error in
                DispatchQueue.main.async {
                    if let error = error {
                        deferred.completeExceptionally(
                            exception: KotlinIllegalStateException(message: error as String)
                        )
                    } else {
                        deferred.complete(value: [:])
                    }
                }
            }
        }
        EmailAuthBridgeConnector().sendEmailVerification = { deferred in
            GoogleAuthBridge.sendEmailVerification { error in
                DispatchQueue.main.async {
                    if let error = error {
                        deferred.completeExceptionally(
                            exception: KotlinIllegalStateException(message: error as String)
                        )
                    } else {
                        deferred.complete(value: [:])
                    }
                }
            }
        }

        EmailAuthBridgeConnector().reloadCurrentUser = { deferred in
            GoogleAuthBridge.reloadCurrentUser { result, error in
                DispatchQueue.main.async {
                    if let error = error {
                        deferred.completeExceptionally(
                            exception: KotlinIllegalStateException(message: error as String)
                        )
                        return
                    }

                    guard let result = result else {
                        deferred.completeExceptionally(
                            exception: KotlinIllegalStateException(message: "Reload current user returned null result")
                        )
                        return
                    }

                    var mapped: [String: String] = [:]
                    for (keyAny, valueAny) in result {
                        guard let key = keyAny as? String else { continue }

                        if let value = valueAny as? String {
                            mapped[key] = value
                        } else if let value = valueAny as? NSString {
                            mapped[key] = value as String
                        } else if let value = valueAny as? NSNumber {
                            mapped[key] = value.stringValue
                        } else {
                            mapped[key] = "\(valueAny)"
                        }
                    }

                    deferred.complete(value: mapped)
                }
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

            if name == "__signOut" {
                if let error = GoogleAuthBridge.signOutUser() {
                    deferred.completeExceptionally(
                        exception: KotlinIllegalStateException(message: error as String)
                    )
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
        ContactsAutocompleteBridgeConnector().isPermissionGranted = { deferred in
            let granted = ContactsPermissionHelper.isPermissionGranted()
            deferred.complete(value: granted)
        }

        ContactsAutocompleteBridgeConnector().requestPermission = { deferred in
            ContactsPermissionHelper.requestPermission { granted in
                deferred.complete(value: granted)
            }
        }

        ContactsAutocompleteBridgeConnector().findSuggestions = { query, limit, deferred in
            ContactsPermissionHelper.findSuggestions(query: query, limit: Int(limit)) { results in
                let mapped: [[String: String]] = results.map { item in
                    [
                        "displayName": item["displayName"] ?? "",
                        "phone": item["phone"] ?? ""
                    ]
                }
                deferred.complete(value: mapped)
            }
        }
        BackupCryptoBridgeConnector().encrypt = { plaintext, password, saltBase64, iterations, deferred in
            var error: NSError?
            let result = BackupCryptoBridge.encrypt(
                plaintext: plaintext,
                password: password,
                saltBase64: saltBase64,
                iterations: Int32(iterations),
                error: &error
            )

            if let error = error {
                deferred.completeExceptionally(
                    exception: KotlinIllegalStateException(message: error.localizedDescription)
                )
                return
            }

            guard let result = result as? [String: String] else {
                deferred.completeExceptionally(
                    exception: KotlinIllegalStateException(message: "Backup encryption returned invalid result")
                )
                return
            }

            deferred.complete(value: result)
        }

        BackupCryptoBridgeConnector().decrypt = { ciphertextBase64, password, saltBase64, ivBase64, iterations, deferred in
            var error: NSError?
            let result = BackupCryptoBridge.decrypt(
                ciphertextBase64: ciphertextBase64,
                password: password,
                saltBase64: saltBase64,
                ivBase64: ivBase64,
                iterations: Int32(iterations),
                error: &error
            )

            if let error = error {
                deferred.completeExceptionally(
                    exception: KotlinIllegalStateException(message: error.localizedDescription)
                )
                return
            }

            guard let result = result as String? else {
                deferred.completeExceptionally(
                    exception: KotlinIllegalStateException(message: "Backup decryption returned empty result")
                )
                return
            }

            deferred.complete(value: result)
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