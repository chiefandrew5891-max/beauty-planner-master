import Foundation
import AuthenticationServices
import CryptoKit
import FirebaseAuth
import UIKit

@objc final class AppleAuthBridge: NSObject {

    private static var currentNonce: String?
    private static var completionHandler: ((NSDictionary?, NSString?) -> Void)?
    private static var revokeCompletionHandler: ((NSDictionary?, NSString?) -> Void)?
    private static var deleteFlowMode: Bool = false

    @objc static func signInWithApple(
        completion: @escaping (NSDictionary?, NSString?) -> Void
    ) {
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let window = windowScene.windows.first(where: { $0.isKeyWindow }),
              let rootViewController = window.rootViewController else {
            completion(nil, "Root view controller not found")
            return
        }

        let nonce = randomNonceString()
        currentNonce = nonce
        completionHandler = completion
        revokeCompletionHandler = nil
        deleteFlowMode = false

        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = [.fullName, .email]
        request.nonce = sha256(nonce)

        let controller = ASAuthorizationController(authorizationRequests: [request])
        let delegate = AppleSignInDelegate(rootViewController: rootViewController)
        controller.delegate = delegate
        controller.presentationContextProvider = delegate
        delegateHolder = delegate
        controller.performRequests()
    }
        @objc static func reauthenticateAndRevokeForDeletion(
            completion: @escaping (NSDictionary?, NSString?) -> Void
        ) {
            guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                  let window = windowScene.windows.first(where: { $0.isKeyWindow }),
                  let rootViewController = window.rootViewController else {
                completion(nil, "Root view controller not found")
                return
            }

            let nonce = randomNonceString()
            currentNonce = nonce
            revokeCompletionHandler = completion
            completionHandler = nil
            deleteFlowMode = true

            let request = ASAuthorizationAppleIDProvider().createRequest()
            request.requestedScopes = []
            request.nonce = sha256(nonce)

            let controller = ASAuthorizationController(authorizationRequests: [request])
            let delegate = AppleSignInDelegate(rootViewController: rootViewController)
            controller.delegate = delegate
            controller.presentationContextProvider = delegate
            delegateHolder = delegate
            controller.performRequests()
        }

    private static var delegateHolder: AppleSignInDelegate?

        fileprivate static func handleAuthorization(
            credential: ASAuthorizationAppleIDCredential
        ) {
            guard let nonce = currentNonce else {
                completionHandler?(nil, "Missing login state")
                revokeCompletionHandler?(nil, "Missing login state")
                clearState()
                return
            }

            guard let appleIDToken = credential.identityToken else {
                completionHandler?(nil, "Unable to fetch identity token")
                revokeCompletionHandler?(nil, "Unable to fetch identity token")
                clearState()
                return
            }

            guard let idTokenString = String(data: appleIDToken, encoding: .utf8) else {
                completionHandler?(nil, "Unable to serialize token string")
                revokeCompletionHandler?(nil, "Unable to serialize token string")
                clearState()
                return
            }

            let firebaseCredential = OAuthProvider.appleCredential(
                withIDToken: idTokenString,
                rawNonce: nonce,
                fullName: credential.fullName
            )

            if deleteFlowMode {
                guard let currentUser = Auth.auth().currentUser else {
                    revokeCompletionHandler?(nil, "No authenticated user")
                    clearState()
                    return
                }

                guard let authCodeData = credential.authorizationCode,
                      let authCodeString = String(data: authCodeData, encoding: .utf8),
                      !authCodeString.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
                    revokeCompletionHandler?(nil, "Unable to fetch authorization code")
                    clearState()
                    return
                }

                currentUser.reauthenticate(with: firebaseCredential) { _, reauthError in
                    if let reauthError = reauthError {
                        revokeCompletionHandler?(nil, reauthError.localizedDescription as NSString)
                        clearState()
                        return
                    }

                    Auth.auth().revokeToken(withAuthorizationCode: authCodeString) { revokeError in
                        if let revokeError = revokeError {
                            revokeCompletionHandler?(nil, revokeError.localizedDescription as NSString)
                            clearState()
                            return
                        }

                        let dict: NSDictionary = [
                            "uid": currentUser.uid,
                            "email": currentUser.email ?? "",
                            "displayName": currentUser.displayName ?? "",
                            "provider": "APPLE",
                            "revoked": "true"
                        ]

                        revokeCompletionHandler?(dict, nil)
                        clearState()
                    }
                }

                return
            }

            Auth.auth().signIn(with: firebaseCredential) { authResult, error in
                if let error = error {
                    completionHandler?(nil, error.localizedDescription as NSString)
                    clearState()
                    return
                }

                guard let firebaseUser = authResult?.user else {
                    completionHandler?(nil, "Firebase user is null")
                    clearState()
                    return
                }

                let dict: NSDictionary = [
                    "uid": firebaseUser.uid,
                    "email": firebaseUser.email ?? "",
                    "displayName": firebaseUser.displayName ?? "",
                    "provider": "APPLE"
                ]

                completionHandler?(dict, nil)
                clearState()
            }
        }

    fileprivate static func handleError(_ error: Error) {
        let nsError = error as NSError

        if nsError.domain == ASAuthorizationError.errorDomain &&
               nsError.code == ASAuthorizationError.canceled.rawValue {
            completionHandler?(nil, "USER_CANCELLED")
            clearState()
            return
        }

        completionHandler?(nil, nsError.localizedDescription as NSString)
        clearState()
    }

    private static func clearState() {
        currentNonce = nil
        completionHandler = nil
        revokeCompletionHandler = nil
        deleteFlowMode = false
        delegateHolder = nil
    }

    private static func sha256(_ input: String) -> String {
        let inputData = Data(input.utf8)
        let hashedData = SHA256.hash(data: inputData)
        return hashedData.map { String(format: "%02x", $0) }.joined()
    }

    private static func randomNonceString(length: Int = 32) -> String {
        precondition(length > 0)
        let charset: [Character] = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")
        var result = ""
        var remainingLength = length

        while remainingLength > 0 {
            let randoms: [UInt8] = (0..<16).map { _ in
                var random: UInt8 = 0
                let errorCode = SecRandomCopyBytes(kSecRandomDefault, 1, &random)
                if errorCode != errSecSuccess {
                    fatalError("Unable to generate nonce. SecRandomCopyBytes failed with OSStatus \(errorCode)")
                }
                return random
            }

            randoms.forEach { random in
                if remainingLength == 0 {
                    return
                }

                if random < charset.count {
                    result.append(charset[Int(random)])
                    remainingLength -= 1
                }
            }
        }

        return result
    }
}

private final class AppleSignInDelegate: NSObject, ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding {
    private weak var rootViewController: UIViewController?

    init(rootViewController: UIViewController) {
        self.rootViewController = rootViewController
    }

    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        return rootViewController?.view.window ?? ASPresentationAnchor()
    }

    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization authorization: ASAuthorization
    ) {
        guard let appleIDCredential = authorization.credential as? ASAuthorizationAppleIDCredential else {
            AppleAuthBridge.handleError(
                NSError(
                    domain: "AppleAuthBridge",
                    code: 1002,
                    userInfo: [NSLocalizedDescriptionKey: "Invalid Apple credential type"]
                )
            )
            return
        }

        AppleAuthBridge.handleAuthorization(credential: appleIDCredential)
    }

    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError error: Error
    ) {
        AppleAuthBridge.handleError(error)
    }
}