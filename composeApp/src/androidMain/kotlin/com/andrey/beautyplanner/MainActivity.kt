package com.andrey.beautyplanner

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import android.content.Intent
import com.andrey.beautyplanner.auth.GoogleSignInFallbackBridge
import com.andrey.beautyplanner.auth.GoogleSignInFallbackResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

private const val AVATAR_DOWNLOAD_CONNECT_TIMEOUT_MS = 10_000
private const val AVATAR_DOWNLOAD_READ_TIMEOUT_MS = 15_000
private const val AVATAR_DOWNLOAD_USER_AGENT = "BeautyPlanner"

class MainActivity : ComponentActivity() {

    private val requestNotificationsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // MVP: nothing
        }

    private val requestContactsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            ContactsAutocompleteAndroid.permissionGranted = granted
            ContactsAutocompleteAndroid.permissionRequestedOnce = true
        }

    // --- Backup: Android system pickers ---
    private var pendingExportJson: String? = null
    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val json = pendingExportJson
        pendingExportJson = null
        if (uri == null || json == null) return@registerForActivityResult

        runCatching {
            contentResolver.openOutputStream(uri)?.use { os ->
                os.write(json.toByteArray(Charsets.UTF_8))
            }
        }
    }

    private var pendingProfileImagePicked: ((String?) -> Unit)? = null

    private val profileImageLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        val callback = pendingProfileImagePicked
        pendingProfileImagePicked = null

        if (uri == null) {
            callback?.invoke(null)
            return@registerForActivityResult
        }

        runCatching {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        runCatching {
            val mime = contentResolver.getType(uri).orEmpty().lowercase()
            val allowed = mime == "image/jpeg" || mime == "image/jpg" || mime == "image/png"
            if (!allowed) {
                callback?.invoke(null)
                return@registerForActivityResult
            }

            val base64 = contentResolver.openInputStream(uri)?.use { input ->
                // Read EXIF orientation before decoding
                val exifOrientation = contentResolver.openInputStream(uri)?.use { exifStream ->
                    ExifInterface(exifStream).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                } ?: ExifInterface.ORIENTATION_NORMAL

                val original = BitmapFactory.decodeStream(input) ?: return@use null
                val oriented = applyExifRotation(original, exifOrientation)
                // Resize to max 1024 on the longer side, preserving aspect ratio
                val resized = resizeBitmapMax(oriented, 1024)
                bitmapToBase64Jpeg(resized, 85)
            }

            callback?.invoke(base64)
        }.onFailure {
            callback?.invoke(null)
        }
    }

    private var pendingImportOnPicked: ((String) -> Unit)? = null
    private var pendingImportOnError: ((String) -> Unit)? = null

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        val onPicked = pendingImportOnPicked
        val onError = pendingImportOnError
        pendingImportOnPicked = null
        pendingImportOnError = null

        if (uri == null) return@registerForActivityResult

        runCatching {
            val text = contentResolver.openInputStream(uri)
                ?.use { it.readBytes().toString(Charsets.UTF_8) }
                .orEmpty()

            if (text.isBlank()) onError?.invoke(Locales.t("backup_import_error_empty"))
            else onPicked?.invoke(text)
        }.onFailure {
            onError?.invoke(Locales.t("backup_import_error_read"))
        }
    }

    private var pendingGoogleFallbackResult: CompletableDeferred<GoogleSignInFallbackResult>? = null

    private val googleSignInFallbackLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val deferred = pendingGoogleFallbackResult
        pendingGoogleFallbackResult = null

        if (deferred == null) return@registerForActivityResult

        if (result.resultCode != RESULT_OK) {
            deferred.complete(GoogleSignInFallbackResult.Cancelled)
            return@registerForActivityResult
        }

        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)

            val idToken = account?.idToken
            if (idToken.isNullOrBlank()) {
                deferred.complete(
                    GoogleSignInFallbackResult.Error("Google ID token is missing")
                )
            } else {
                deferred.complete(
                    GoogleSignInFallbackResult.Success(idToken)
                )
            }
        } catch (e: Exception) {
            deferred.complete(
                GoogleSignInFallbackResult.Error(e.message ?: "Google sign-in failed")
            )
        }
    }

    /**
     * Resize bitmap so that the longer side is at most [maxDim] pixels,
     * preserving aspect ratio.
     */
    private fun resizeBitmapMax(source: Bitmap, maxDim: Int): Bitmap {
        val w = source.width
        val h = source.height
        if (w <= maxDim && h <= maxDim) return source
        val scale = maxDim.toFloat() / maxOf(w, h)
        val newW = (w * scale).toInt().coerceAtLeast(1)
        val newH = (h * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, newW, newH, true)
    }

    /**
     * Apply EXIF orientation to produce an upright bitmap.
     */
    private fun applyExifRotation(source: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
            else -> return source
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    /**
     * Crop the bitmap based on the drag offset from the Compose crop editor.
     *
     * The crop editor uses ContentScale.Crop inside a square container of [displaySizePx].
     * With ContentScale.Crop, the bitmap is scaled so that min(bitmapW, bitmapH) = displaySizePx.
     * The [offsetXPx] / [offsetYPx] is the drag translation applied to the image in display pixels.
     * [scale] is the user zoom multiplier (where 1f is the minimum scale that still covers the crop circle).
     * We reverse this transform to find the crop rect in bitmap coordinates.
     */
    private fun cropBitmapFromOffset(
        source: Bitmap,
        offsetXPx: Float,
        offsetYPx: Float,
        displaySizePx: Float,
        scale: Float,
        targetSize: Int
    ): Bitmap {
        if (displaySizePx <= 0f) {
            return Bitmap.createScaledBitmap(source, targetSize, targetSize, true)
        }

        val bitmapW = source.width.toFloat()
        val bitmapH = source.height.toFloat()
        val minDim = minOf(bitmapW, bitmapH)
        val baseScale = displaySizePx / minDim
        val effectiveScale = baseScale * scale.coerceAtLeast(1f)

        // Center of the crop circle in bitmap coordinates
        val cropCenterX = bitmapW / 2f - offsetXPx / effectiveScale
        val cropCenterY = bitmapH / 2f - offsetYPx / effectiveScale

        val cropSize = (displaySizePx / effectiveScale).coerceIn(1f, minDim)
        val cropHalf = cropSize / 2f

        val maxLeft = (bitmapW - cropSize).coerceAtLeast(0f)
        val maxTop = (bitmapH - cropSize).coerceAtLeast(0f)
        val left = (cropCenterX - cropHalf).coerceIn(0f, maxLeft)
        val top = (cropCenterY - cropHalf).coerceIn(0f, maxTop)

        val leftPx = left.roundToInt().coerceIn(0, source.width - 1)
        val topPx = top.roundToInt().coerceIn(0, source.height - 1)
        val maxSquare = minOf(source.width - leftPx, source.height - topPx)
        val cropSquare = cropSize.roundToInt().coerceIn(1, maxSquare)

        val cropped = Bitmap.createBitmap(source, leftPx, topPx, cropSquare, cropSquare)
        return Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)
    }

    private fun bitmapToBase64Jpeg(source: Bitmap, quality: Int = 82): String {
        val output = ByteArrayOutputStream()
        source.compress(Bitmap.CompressFormat.JPEG, quality, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    private fun processAvatarUrl(url: String, onResult: (String?) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = runCatching { downloadAndProcessAvatar(url) }.getOrNull()
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    private fun downloadAndProcessAvatar(url: String): String? {
        val parsedUrl = URL(url)
        val scheme = parsedUrl.protocol.lowercase()
        if (scheme != "http" && scheme != "https") return null

        val connection = (parsedUrl.openConnection() as? HttpURLConnection) ?: return null
        return try {
            connection.instanceFollowRedirects = true
            connection.connectTimeout = AVATAR_DOWNLOAD_CONNECT_TIMEOUT_MS
            connection.readTimeout = AVATAR_DOWNLOAD_READ_TIMEOUT_MS
            connection.setRequestProperty("Accept", "image/*")
            connection.setRequestProperty("User-Agent", AVATAR_DOWNLOAD_USER_AGENT)
            connection.connect()

            if (connection.responseCode !in 200..299) return null

            val bytes = connection.inputStream.use { it.readBytes() }
            if (bytes.isEmpty()) return null

            val exifOrientation = runCatching {
                ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

            val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val oriented = applyExifRotation(original, exifOrientation)
            val cropSize = minOf(oriented.width, oriented.height)
            val left = ((oriented.width - cropSize) / 2).coerceAtLeast(0)
            val top = ((oriented.height - cropSize) / 2).coerceAtLeast(0)
            val square = Bitmap.createBitmap(oriented, left, top, cropSize, cropSize)
            val resized = Bitmap.createScaledBitmap(square, 512, 512, true)
            val base64 = bitmapToBase64Jpeg(resized, 85)

            if (resized !== square) resized.recycle()
            if (square !== oriented) square.recycle()
            if (oriented !== original) oriented.recycle()
            original.recycle()

            base64
        } finally {
            connection.disconnect()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        com.andrey.beautyplanner.notifications.NotificationsPlatform.init(applicationContext)
        AndroidAppContext.context = applicationContext
        AndroidAppContext.activity = this
        GoogleSignInFallbackBridge.launchSignInIntent = { intent: Intent, deferred ->
            pendingGoogleFallbackResult = deferred
            googleSignInFallbackLauncher.launch(intent)
        }

        ContactsAutocompleteAndroid.init(
            context = applicationContext,
            permissionChecker = {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED
            },
            requestPermission = {
                requestContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        )

        BackupFilePicker.exportImpl = { suggestedFileName, json ->
            val name = suggestedFileName.trim().ifBlank { "beautyplanner-backup" }
            val finalName = if (name.endsWith(".json", ignoreCase = true)) name else "$name.json"
            pendingExportJson = json
            exportLauncher.launch(finalName)
        }

        BackupFilePicker.importImpl = { onPicked, onError ->
            pendingImportOnPicked = onPicked
            pendingImportOnError = onError
            importLauncher.launch(arrayOf("application/json", "text/plain"))
        }

        ProfileImagePicker.pickImageImpl = { onImagePicked ->
            pendingProfileImagePicked = onImagePicked
            profileImageLauncher.launch(arrayOf("image/jpeg", "image/png"))
        }

        ProfileAvatarUrlProcessor.processImpl = { url, onResult ->
            processAvatarUrl(url, onResult)
        }

        ProfileImageCropper.cropImpl = { base64, offsetXPx, offsetYPx, displaySizePx, scale, targetSize, onResult ->
            runCatching {
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap == null) {
                    onResult(null)
                    return@runCatching
                }
                val cropped = cropBitmapFromOffset(bitmap, offsetXPx, offsetYPx, displaySizePx, scale, targetSize)
                onResult(bitmapToBase64Jpeg(cropped, 85))
            }.onFailure {
                onResult(null)
            }
        }

        AppSettings.load()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        maybeRequestPostNotificationsPermission()

        setContent {
            App()
        }
    }

    private fun maybeRequestPostNotificationsPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val permission = Manifest.permission.POST_NOTIFICATIONS
        val alreadyGranted = ContextCompat.checkSelfPermission(this, permission) ==
                PackageManager.PERMISSION_GRANTED

        if (!alreadyGranted) {
            requestNotificationsPermissionLauncher.launch(permission)
        }
    }
}