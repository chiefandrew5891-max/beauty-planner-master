package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Locales
import com.andrey.beautyplanner.ProfileImageCropper
import com.andrey.beautyplanner.rememberProfileAvatarBitmap

/**
 * Full-screen dialog that lets the user pan/zoom a selected photo under a fixed circle crop region.
 * On confirm, the cropped 512×512 JPEG base64 is passed to [onConfirm].
 */
@Composable
fun AvatarCropEditorDialog(
    rawBase64: String,
    onConfirm: (croppedBase64: String) -> Unit,
    onDismiss: () -> Unit
) {
    val fontScale = AppSettings.getFontScale()
    val onSurface = MaterialTheme.colors.onSurface
    val primary = MaterialTheme.colors.primary

    val avatarBitmap = rememberProfileAvatarBitmap(rawBase64)

    var offsetX by remember(rawBase64) { mutableStateOf(0f) }
    var offsetY by remember(rawBase64) { mutableStateOf(0f) }
    var scale by remember(rawBase64) { mutableStateOf(1f) }

    // Physical pixel size of the crop container, tracked after layout.
    // Reset with new source image so offset/scale/container geometry all start from a clean state.
    var containerSizePx by remember(rawBase64) { mutableStateOf(0f) }

    val minScale = 1f
    val maxScale = 6f
    val coercedScale = scale.coerceIn(minScale, maxScale)

    val bitmapW = avatarBitmap?.width?.toFloat() ?: 0f
    val bitmapH = avatarBitmap?.height?.toFloat() ?: 0f
    val baseScale =
        if (avatarBitmap != null && containerSizePx > 0f && bitmapW > 0f && bitmapH > 0f) {
            containerSizePx / minOf(bitmapW, bitmapH)
        } else {
            0f
        }

    fun calculateMaxOffsets(scaleValue: Float): Pair<Float, Float> {
        if (baseScale <= 0f || containerSizePx <= 0f) return 0f to 0f
        val displayedW = bitmapW * baseScale * scaleValue
        val displayedH = bitmapH * baseScale * scaleValue
        return maxOf(0f, (displayedW - containerSizePx) / 2f) to
            maxOf(0f, (displayedH - containerSizePx) / 2f)
    }

    val (maxOffsetX, maxOffsetY) = calculateMaxOffsets(coercedScale)
    val clampedOffsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
    val clampedOffsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)

    var isProcessing by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!isProcessing) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(20.dp),
            backgroundColor = MaterialTheme.colors.surface,
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = Locales.t("avatar_crop_title"),
                    fontSize = (18 * fontScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurface
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = Locales.t("avatar_crop_hint"),
                    fontSize = (13 * fontScale).sp,
                    color = onSurface.copy(alpha = 0.68f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                // Crop container: square, fills available width
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
                        .clipToBounds()
                        .onGloballyPositioned { coords ->
                            containerSizePx = coords.size.width.toFloat()
                        }
                        .pointerInput(baseScale, containerSizePx) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val nextScale = (scale * zoom).coerceIn(minScale, maxScale)
                                val (nextMaxX, nextMaxY) = calculateMaxOffsets(nextScale)

                                scale = nextScale
                                offsetX = (offsetX + pan.x).coerceIn(-nextMaxX, nextMaxX)
                                offsetY = (offsetY + pan.y).coerceIn(-nextMaxY, nextMaxY)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarBitmap != null) {
                        Image(
                            bitmap = avatarBitmap,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .graphicsLayer(
                                    scaleX = coercedScale,
                                    scaleY = coercedScale,
                                    translationX = clampedOffsetX,
                                    translationY = clampedOffsetY
                                ),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Loading / decode placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .background(MaterialTheme.colors.onSurface.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "...",
                                color = onSurface.copy(alpha = 0.45f),
                                fontSize = 28.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = { if (!isProcessing) onDismiss() }
                    ) {
                        Text(
                            text = Locales.t("avatar_crop_cancel"),
                            color = onSurface.copy(alpha = 0.72f),
                            fontSize = (15 * fontScale).sp
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    TextButton(
                        modifier = Modifier.weight(1f),
                        enabled = avatarBitmap != null && !isProcessing,
                        onClick = {
                            if (isProcessing) return@TextButton
                            isProcessing = true
                            ProfileImageCropper.cropImage(
                                base64 = rawBase64,
                                offsetXPx = clampedOffsetX,
                                offsetYPx = clampedOffsetY,
                                displaySizePx = containerSizePx,
                                scale = coercedScale,
                                targetSize = 512
                            ) { cropped ->
                                isProcessing = false
                                if (!cropped.isNullOrBlank()) {
                                    onConfirm(cropped)
                                } else {
                                    // Fallback: pass the raw base64 if cropping failed
                                    onConfirm(rawBase64)
                                }
                            }
                        }
                    ) {
                        Text(
                            text = if (isProcessing) "..." else Locales.t("avatar_crop_confirm"),
                            color = if (avatarBitmap != null && !isProcessing) primary else onSurface.copy(alpha = 0.38f),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = (15 * fontScale).sp
                        )
                    }
                }
            }
        }
    }
}
