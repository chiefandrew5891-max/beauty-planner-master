import Foundation
import StoreKit
import UIKit

@objc final class ReviewPromptBridge: NSObject {
    @objc static func requestReview() {
        if #available(iOS 14.0, *) {
            guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene else {
                SKStoreReviewController.requestReview()
                return
            }
            SKStoreReviewController.requestReview(in: scene)
        } else {
            SKStoreReviewController.requestReview()
        }
    }
}