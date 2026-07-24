import UIKit
import ComposeApp

/// Provides the platform-side crop implementation for ProfileImageCropper.
/// The Compose crop editor passes the drag offset (in display pixels) and the
/// display-side size of the crop circle, and this implementation reproduces
/// the same ContentScale.Crop geometry to extract the correct region.
final class ProfileImageCropperBridge {

    static func register() {
        ProfileImageCropper.shared.cropImpl = { base64, offsetXPx, offsetYPx, displaySizePx, scale, targetSize, onResult in
            DispatchQueue.global(qos: .userInitiated).async {
                let result = Self.crop(
                    base64: base64,
                    offsetXPx: CGFloat(offsetXPx),
                    offsetYPx: CGFloat(offsetYPx),
                    displaySizePx: CGFloat(displaySizePx),
                    scale: CGFloat(scale),
                    targetSize: Int(targetSize)
                )
                DispatchQueue.main.async {
                    onResult(result)
                }
            }
        }
    }

    private static func crop(
        base64: String,
        offsetXPx: CGFloat,
        offsetYPx: CGFloat,
        displaySizePx: CGFloat,
        scale: CGFloat,
        targetSize: Int
    ) -> String? {
        guard let data = Data(base64Encoded: base64),
              let image = UIImage(data: data) else {
            return nil
        }

        // UIImage.draw respects imageOrientation, so we normalise first.
        let normalised = normalise(image)
        guard let cgImage = normalised.cgImage else { return nil }

        let bitmapW = CGFloat(cgImage.width)
        let bitmapH = CGFloat(cgImage.height)
        if displaySizePx <= 0 {
            guard let jpegData = normalised.jpegData(compressionQuality: 0.85) else { return nil }
            return jpegData.base64EncodedString()
        }

        let minDim = min(bitmapW, bitmapH)
        let baseScale = displaySizePx / minDim
        let effectiveScale = baseScale * max(scale, 1.0)
        let cropSize = min(max(displaySizePx / effectiveScale, 1.0), minDim)

        // Center of the crop circle in bitmap coordinates
        let cropCenterX = bitmapW / 2.0 - offsetXPx / effectiveScale
        let cropCenterY = bitmapH / 2.0 - offsetYPx / effectiveScale

        let cropHalf = cropSize / 2.0
        let left = min(max(0, cropCenterX - cropHalf), max(bitmapW - cropSize, 0))
        let top = min(max(0, cropCenterY - cropHalf), max(bitmapH - cropSize, 0))

        let cropRect = CGRect(x: left, y: top, width: cropSize, height: cropSize)
        guard let croppedCg = cgImage.cropping(to: cropRect) else { return nil }

        let targetSize2D = CGSize(width: targetSize, height: targetSize)
        let renderer = UIGraphicsImageRenderer(size: targetSize2D)
        let resized = renderer.image { _ in
            UIImage(cgImage: croppedCg).draw(in: CGRect(origin: .zero, size: targetSize2D))
        }

        guard let jpegData = resized.jpegData(compressionQuality: 0.85) else { return nil }
        return jpegData.base64EncodedString()
    }

    /// Redraw the image into a context so its orientation is baked in, removing any EXIF rotation.
    private static func normalise(_ image: UIImage) -> UIImage {
        if image.imageOrientation == .up { return image }
        let renderer = UIGraphicsImageRenderer(size: image.size)
        return renderer.image { _ in
            image.draw(in: CGRect(origin: .zero, size: image.size))
        }
    }
}
