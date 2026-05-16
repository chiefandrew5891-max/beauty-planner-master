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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        com.andrey.beautyplanner.notifications.NotificationsPlatform.init(applicationContext)

        AndroidAppContext.context = applicationContext
        AndroidAppContext.activity = this

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

        // Bind BackupFilePicker implementations (IMPORTANT)
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