package com.andrey.beautyplanner

object ProfileImageCropper {
    /**
     * Platform-provided implementation.
     * Parameters: base64 (raw image), offsetXPx (drag offset in display pixels),
     * offsetYPx, displaySizePx (diameter of the crop circle in display pixels),
     * scale (editor zoom where 1f is minimum cover scale), targetSize (output size in pixels),
     * onResult (callback with cropped base64 or null on error).
     */
    var cropImpl: ((base64: String, offsetXPx: Float, offsetYPx: Float, displaySizePx: Float, scale: Float, targetSize: Int, onResult: (String?) -> Unit) -> Unit)? = null

    fun cropImage(
        base64: String,
        offsetXPx: Float,
        offsetYPx: Float,
        displaySizePx: Float,
        scale: Float = 1f,
        targetSize: Int = 512,
        onResult: (String?) -> Unit
    ) {
        cropImpl?.invoke(base64, offsetXPx, offsetYPx, displaySizePx, scale, targetSize, onResult)
            ?: onResult(null)
    }
}
