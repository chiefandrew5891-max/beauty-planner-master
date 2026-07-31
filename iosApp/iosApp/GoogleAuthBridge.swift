import Foundation
import UIKit
import FirebaseCore
import FirebaseAuth
import FirebaseFunctions
import GoogleSignIn

@objc final class GoogleAuthBridge: NSObject {

    @objc static func signInWithGoogle(
        completion: @escaping (NSDictionary?, NSString?) -> Void
    ) {
        guard let rootVC = topViewController() else {
            completion(nil, "Root view controller not found")
            return
        }

        guard let app = FirebaseApp.app() else {
            completion(nil, "Firebase default app is not configured")
            return
        }

        guard let clientID = app.options.clientID else {
            completion(nil, "Firebase clientID is missing")
            return
        }

        let config = GIDConfiguration(clientID: clientID)
        GIDSignIn.sharedInstance.configuration = config

        GIDSignIn.sharedInstance.signIn(withPresenting: rootVC) { result, error in
            if let error = error {
                completion(nil, error.localizedDescription as NSString)
                return
            }

            guard let user = result?.user else {
                completion(nil, "Google user is null")
                return
            }

            guard let idToken = user.idToken?.tokenString else {
                completion(nil, "Google ID token is missing")
                return
            }

            let accessToken = user.accessToken.tokenString
            let credential = GoogleAuthProvider.credential(withIDToken: idToken, accessToken: accessToken)

            Auth.auth().signIn(with: credential) { authResult, error in
                if let error = error {
                    completion(nil, error.localizedDescription as NSString)
                    return
                }

                guard let firebaseUser = authResult?.user else {
                    completion(nil, "Firebase user is null")
                    return
                }

                completion(userDict(firebaseUser, fallbackProvider: "GOOGLE"), nil)
            }
        }
    }

    @objc static func signInAnonymously(
        completion: @escaping (NSDictionary?, NSString?) -> Void
    ) {
        if let currentUser = Auth.auth().currentUser {
            let provider = currentUser.isAnonymous ? "ANONYMOUS" : detectProvider(currentUser)
            let dict: NSDictionary = [
                "uid": currentUser.uid,
                "email": currentUser.email ?? "",
                "displayName": currentUser.displayName ?? "",
                "provider": provider,
                "isEmailVerified": currentUser.isEmailVerified ? "true" : "false"
            ]
            completion(dict, nil)
            return
        }

        Auth.auth().signInAnonymously { authResult, error in
            if let error = error {
                completion(nil, error.localizedDescription as NSString)
                return
            }

            guard let user = authResult?.user else {
                completion(nil, "Anonymous auth returned null user")
                return
            }

            completion(userDict(user, fallbackProvider: "ANONYMOUS"), nil)
        }
    }

    @objc static func signInWithEmail(
        _ email: String,
        password: String,
        completion: @escaping (NSDictionary?, NSString?) -> Void
    ) {
        let cleanEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)

        Auth.auth().signIn(withEmail: cleanEmail, password: password) { authResult, error in
            if let error = error {
                completion(nil, error.localizedDescription as NSString)
                return
            }

            guard let user = authResult?.user else {
                completion(nil, "Email sign-in returned null user")
                return
            }

            completion(userDict(user, fallbackProvider: "EMAIL"), nil)
        }
    }

    @objc static func registerWithEmail(
        _ email: String,
        password: String,
        completion: @escaping (NSDictionary?, NSString?) -> Void
    ) {
        let cleanEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)

        Auth.auth().createUser(withEmail: cleanEmail, password: password) { authResult, error in
            if let error = error {
                completion(nil, error.localizedDescription as NSString)
                return
            }

            guard let user = authResult?.user else {
                completion(nil, "Email registration returned null user")
                return
            }

            completion(userDict(user, fallbackProvider: "EMAIL"), nil)
        }
    }

    @objc static func sendEmailVerification(
        completion: @escaping (NSString?) -> Void
    ) {
        guard let user = Auth.auth().currentUser else {
            completion("No authenticated user for email verification")
            return
        }

        user.sendEmailVerification { error in
            if let error = error {
                completion(error.localizedDescription as NSString)
            } else {
                completion(nil)
            }
        }
    }

    @objc static func reloadCurrentUser(
        completion: @escaping (NSDictionary?, NSString?) -> Void
    ) {
        guard let user = Auth.auth().currentUser else {
            completion(nil, "No authenticated user")
            return
        }

        user.reload { error in
            if let error = error {
                completion(nil, error.localizedDescription as NSString)
                return
            }

            completion(userDict(user, fallbackProvider: "EMAIL"), nil)
        }
    }

    @objc static func sendPasswordReset(
        _ email: String,
        completion: @escaping (NSString?) -> Void
    ) {
        let cleanEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)

        Auth.auth().sendPasswordReset(withEmail: cleanEmail) { error in
            if let error = error {
                completion(error.localizedDescription as NSString)
            } else {
                completion(nil)
            }
        }
    }

    @objc static func currentUser() -> NSDictionary? {
        guard let user = Auth.auth().currentUser else { return nil }
        return userDict(user, fallbackProvider: "ANONYMOUS")
    }

    @objc static func signOutUser() -> NSString? {
        do {
            try Auth.auth().signOut()
            GIDSignIn.sharedInstance.signOut()
            return nil
        } catch {
            return error.localizedDescription as NSString
        }
    }

    @objc static func prepareForNewSignIn() -> NSString? {
        do {
            if Auth.auth().currentUser != nil {
                try Auth.auth().signOut()
            }

            GIDSignIn.sharedInstance.signOut()
            GIDSignIn.sharedInstance.disconnect { error in
                // intentionally ignored, best-effort cleanup
            }

            return nil
        } catch {
            return error.localizedDescription as NSString
        }
    }

    @objc static func callBackend(
        _ name: String,
        payload: NSDictionary,
        completion: @escaping (NSDictionary?, NSString?) -> Void
    ) {
        let functions = Functions.functions()

        functions.httpsCallable(name).call(payload as? [String: Any]) { result, error in
            if let error = error {
                completion(nil, error.localizedDescription as NSString)
                return
            }

            guard let data = result?.data as? NSDictionary else {
                completion(nil, "Function returned invalid result")
                return
            }

            completion(data, nil)
        }
    }

    private static func userDict(_ user: User, fallbackProvider: String) -> NSDictionary {
        let provider = user.isAnonymous ? "ANONYMOUS" : detectProvider(user)
        return [
            "uid": user.uid,
            "email": user.email ?? "",
            "displayName": user.displayName ?? "",
            "provider": provider.isEmpty ? fallbackProvider : provider,
            "isEmailVerified": user.isEmailVerified ? "true" : "false"
        ]
    }

    private static func detectProvider(_ user: User) -> String {
        for info in user.providerData {
            if info.providerID == "google.com" { return "GOOGLE" }
            if info.providerID == "password" { return "EMAIL" }
            if info.providerID == "apple.com" { return "APPLE" }
        }
        return "ANONYMOUS"
    }

    private static func topViewController() -> UIViewController? {
        guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let window = scene.windows.first(where: { $0.isKeyWindow }),
              var top = window.rootViewController else {
            return nil
        }

        while let presented = top.presentedViewController {
            top = presented
        }

        return top
    }
}