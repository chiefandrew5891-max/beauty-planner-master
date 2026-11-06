import UIKit
import FirebaseCore
import FirebaseAnalytics
import AVFoundation // 1. Импортируем фреймворк для работы со звуком

final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        if FirebaseApp.app() == nil {
            FirebaseApp.configure()
            AppAnalytics.shared.handler = { name, params in
                Analytics.logEvent(name, parameters: params as? [String: Any])
                return KotlinUnit()
            }
        }

        StoreKitBridge.startObservingTransactions()

        // 2. Настраиваем и активируем аудиосессию при старте
        configureAudioSession()

        return true
    }

    // 3. Вспомогательная функция настройки аудио
    private func configureAudioSession() {
        do {
            let audioSession = AVAudioSession.sharedInstance()
            try audioSession.setCategory(.playback, mode: .default, options: [])
            try audioSession.setActive(true, options: .notifyOthersOnDeactivation)
        } catch {
            print("Ошибка настройки AVAudioSession: \(error.localizedDescription)")
        }
    }
}