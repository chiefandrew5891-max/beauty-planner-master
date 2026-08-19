import Foundation
import AuthenticationServices
import CryptoKit
import FirebaseAuth
import UIKit

@objc final class AppleAuthBridge: NSObject {

    private enum FlowMode {
        case signIn
        case linkAnonymous
        case deleteReauth
    }

    private static var currentNonce: String?
    private static var completionHandler: ((NSDictionary?, NSString?) -> Void)?
    private static var revokeCompletionHandler: ((NSDictionary?, NSString?) -> Void)?
    private static var flowMode: FlowMode = .signIn
    private static var delegateHolder: AppleSignInDelegate?

    @objc static func signInWithApple(
        completion: @escaping (NSDictionary?, NSString?) -> Void
    ) {
        startAppleFlow(
            scopes: [.fullName, .email],
            mode: .signIn,
            completion: completion
        )
    }

    @objc static func linkAnonymousWithApple(
        completion: @escaping (NSDictionary?, NSString?) -> Void
    ) {
        guard let currentUser = Auth.auth().currentUser else {
            completion(nil, "guest_link_requires_anonymous_user")
            return
        }

        guard currentUser.isAnonymous else {
            completion(nil, "guest_link_requires_anonymous_user")
            return
        }

        startAppleFlow(
            scopes: [.fullName, .email],
            mode: .linkAnonymous,
            completion: completion
        )
    }

    @objc static func reauthenticateAndRevokeForDeletion(
        completion: @escaping (NSDictionary?, NSString?) -> Void
    ) {
        guard let rootViewController = rootViewController() else {
            completion(nil, "Root view controller not found")
            return
        }

        let nonce = randomNonceString()
        currentNonce = nonce
        revokeCompletionHandler = completion
        completionHandler = nil
        flowMode = .deleteReauth

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

    private static func startAppleFlow(
        scopes: [ASAuthorization.Scope],
        mode: FlowMode,
        completion: @escaping (NSDictionary?, NSString?) -> Void
    ) {
        guard let rootViewController = rootViewController() else {
            completion(nil, "Root view controller not found")
            return
        }

        let nonce = randomNonceString()
        currentNonce = nonce
        completionHandler = completion
        revokeCompletionHandler = nil
        flowMode = mode

        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = scopes
        request.nonce = sha256(nonce)

        let controller = ASAuthorizationController(authorizationRequests: [request])
        let delegate = AppleSignInDelegate(rootViewController: rootViewController)
        controller.delegate = delegate
        controller.presentationContextProvider = delegate
        delegateHolder = delegate
        controller.performRequests()
    }

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

        switch flowMode {
        case .deleteReauth:
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

        case .signIn:
            Auth.auth().signIn(with: firebaseCredential) { authResult, error in
                if let error = error {
                    completionHandler?(nil, normalizedAppleFirebaseError(error))
                    clearState()
                    return
                }

                guard let firebaseUser = authResult?.user else {
                    completionHandler?(nil, "Firebase user is null")
                    clearState()
                    return
                }

                completionHandler?(resolvedUserDict(firebaseUser, credential: credential), nil)
                clearState()
            }

        case .linkAnonymous:
            guard let currentUser = Auth.auth().currentUser else {
                completionHandler?(nil, "guest_link_requires_anonymous_user")
                clearState()
                return
            }

            guard currentUser.isAnonymous else {
                completionHandler?(nil, "guest_link_requires_anonymous_user")
                clearState()
                return
            }

            currentUser.link(with: firebaseCredential) { authResult, error in
                if let error = error as NSError? {
                    if error.code == AuthErrorCode.credentialAlreadyInUse.rawValue ||
                           error.code == AuthErrorCode.emailAlreadyInUse.rawValue ||
                           error.code == AuthErrorCode.accountExistsWithDifferentCredential.rawValue {
                        completionHandler?(nil, "guest_upgrade_account_already_exists")
                        clearState()
                        return
                    }

                    completionHandler?(nil, normalizedAppleFirebaseError(error))
                    clearState()
                    return
                }

                guard let firebaseUser = authResult?.user else {
                    completionHandler?(nil, "Firebase user is null")
                    clearState()
                    return
                }

                completionHandler?(resolvedUserDict(firebaseUser, credential: credential), nil)
                clearState()
            }
        }
    }

    fileprivate static func handleError(_ error: Error) {
        let nsError = error as NSError

        if nsError.domain == ASAuthorizationError.errorDomain {
            if nsError.code == ASAuthorizationError.canceled.rawValue {
                completionHandler?(nil, "USER_CANCELLED")
                clearState()
                return
            }

            if nsError.code == 1000 {
                completionHandler?(nil, "USER_NOT_COMPLETED")
                clearState()
                return
            }
        }

        completionHandler?(nil, nsError.localizedDescription as NSString)
        clearState()
    }

    private static func resolvedUserDict(
        _ firebaseUser: User,
        credential: ASAuthorizationAppleIDCredential
    ) -> NSDictionary {
        let fallbackEmail = credential.email?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let resolvedEmail = (firebaseUser.email ?? "").trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            ? fallbackEmail
            : (firebaseUser.email ?? "")

        let fullNameParts = [
            credential.fullName?.givenName?.trimmingCharacters(in: .whitespacesAndNewlines) ?? "",
            credential.fullName?.familyName?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        ].filter { !$0.isEmpty }

        let fallbackDisplayName = fullNameParts.joined(separator: " ")
        let resolvedDisplayName = (firebaseUser.displayName ?? "").trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            ? fallbackDisplayName
            : (firebaseUser.displayName ?? "")

        return [
            "uid": firebaseUser.uid,
            "email": resolvedEmail,
            "displayName": resolvedDisplayName,
            "provider": "APPLE"
        ]
    }

    private static func normalizedAppleFirebaseError(_ error: Error) -> NSString {
        let nsError = error as NSError

        if nsError.domain == AuthErrorDomain {
            if nsError.code == AuthErrorCode.credentialAlreadyInUse.rawValue ||
                   nsError.code == AuthErrorCode.emailAlreadyInUse.rawValue ||
                   nsError.code == AuthErrorCode.accountExistsWithDifferentCredential.rawValue {
                return "guest_upgrade_account_already_exists"
            }
        }

        return nsError.localizedDescription as NSString
    }

    private static func clearState() {
        currentNonce = nil
        completionHandler = nil
        revokeCompletionHandler = nil
        flowMode = .signIn
        delegateHolder = nil
    }

    private static func rootViewController() -> UIViewController? {
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let window = windowScene.windows.first(where: { $0.isKeyWindow }),
              let rootViewController = window.rootViewController else {
            return nil
        }
        return rootViewController
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