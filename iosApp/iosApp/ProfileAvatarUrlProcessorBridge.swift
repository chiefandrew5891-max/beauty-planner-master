import UIKit
import ComposeApp

final class ProfileAvatarUrlProcessorBridge {
    private static let avatarCompressionQuality: CGFloat = 0.85

    static func register() {
        ProfileAvatarUrlProcessor.shared.processImpl = { url, onResult in
            DispatchQueue.global(qos: .userInitiated).async {
                let result = Self.process(urlString: url)
                DispatchQueue.main.async {
                    onResult(result)
                }
            }
        }
    }

    private static func process(urlString: String) -> String? {
        guard let url = URL(string: urlString),
              let scheme = url.scheme?.lowercased(),
              scheme == "http" || scheme == "https",
              let data = try? Data(contentsOf: url),
              let image = UIImage(data: data) else {
            return nil
        }

        let normalized = normalize(image)
        guard let cgImage = normalized.cgImage else { return nil }

        let width = CGFloat(cgImage.width)
        let height = CGFloat(cgImage.height)
        let side = min(width, height)
        let cropRect = CGRect(
            x: max(0, (width - side) / 2.0),
            y: max(0, (height - side) / 2.0),
            width: side,
            height: side
        )

        guard let croppedCg = cgImage.cropping(to: cropRect) else { return nil }

        let targetSize = CGSize(width: 512, height: 512)
        let renderer = UIGraphicsImageRenderer(size: targetSize)
        let resized = renderer.image { _ in
            UIImage(cgImage: croppedCg).draw(in: CGRect(origin: .zero, size: targetSize))
        }

        guard let jpegData = resized.jpegData(compressionQuality: avatarCompressionQuality) else { return nil }
        return jpegData.base64EncodedString()
    }

    private static func normalize(_ image: UIImage) -> UIImage {
        if image.imageOrientation == .up { return image }
        let renderer = UIGraphicsImageRenderer(size: image.size)
        return renderer.image { _ in
            image.draw(in: CGRect(origin: .zero, size: image.size))
        }
    }
}
