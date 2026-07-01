package com.andrey.beautyplanner.appcontent.approot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.BackupCodec
import com.andrey.beautyplanner.BackupFilePicker
import com.andrey.beautyplanner.DataManager
import com.andrey.beautyplanner.Locales
import com.andrey.beautyplanner.ParsedBackupFile
import com.andrey.beautyplanner.appcontent.AppDialogShape
import com.andrey.beautyplanner.appcontent.AppDialogTheme
import com.andrey.beautyplanner.appcontent.PinDialog
import com.andrey.beautyplanner.appcontent.SetPinDialog
import com.andrey.beautyplanner.appcontent.RescheduleClientBDialog
import com.andrey.beautyplanner.appcontent.formatBackupCreatedAt
import kotlinx.datetime.LocalDate



@Composable
fun AppRootDialogs(state: AppRootState) {

    if (state.showSaveError != null) {
        AlertDialog(
            onDismissRequest = { state.showSaveError = null },
            title = { Text(Locales.t("import_db")) },
            text = { Text(state.showSaveError ?: "") },
            confirmButton = {
                TextButton(onClick = { state.showSaveError = null }) {
                    Text(Locales.t("close"))
                }
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        )
    }
    if (state.backupSuccessMessage != null) {
        AlertDialog(
            onDismissRequest = { state.backupSuccessMessage = null },
            title = { Text(Locales.t("backup_section")) },
            text = { Text(state.backupSuccessMessage.orEmpty()) },
            confirmButton = {
                TextButton(onClick = { state.backupSuccessMessage = null }) {
                    Text(Locales.t("close"))
                }
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        )
    }

    if (state.showPinDialog) {
        AppDialogTheme {
            PinDialog(
                title = state.pinDialogTitle,
                text = state.pinDialogText + (state.pinErrorText?.let { "\n\n$it" } ?: ""),
                confirmText = state.pinDialogConfirmText,
                onDismiss = {
                    state.showPinDialog = false
                    state.pinDialogOnSuccess = null
                    state.pinErrorText = null
                },
                onConfirmPin = { pin ->
                    if (AppSettings.verifyPin(pin)) {
                        state.showPinDialog = false
                        state.pinErrorText = null
                        val act = state.pinDialogOnSuccess
                        state.pinDialogOnSuccess = null
                        act?.invoke()
                    } else {
                        state.pinErrorText = Locales.t("pin_wrong")
                    }
                }
            )
        }
    }

    if (state.locked && !state.mustCreatePin) {
        AppDialogTheme {
            PinDialog(
                title = Locales.t("unlock_title"),
                text = Locales.t("unlock_text"),
                confirmText = Locales.t("confirm"),
                onDismiss = { },
                onConfirmPin = { pin ->
                    if (AppSettings.verifyPin(pin)) state.locked = false
                }
            )
        }
    }

    if (state.showSetPinDialog) {
        AppDialogTheme {
            SetPinDialog(
                onDismiss = { state.showSetPinDialog = false },
                onPinSet = {
                    AppSettings.pinEnabled = true
                    AppSettings.persist()
                    state.showSetPinDialog = false
                    state.mustCreatePin = false
                    state.locked = false
                }
            )
        }
    }

    if (state.showRemovePinConfirm) {
        AlertDialog(
            onDismissRequest = { state.showRemovePinConfirm = false },
            title = { Text(Locales.t("pin_remove")) },
            text = { Text(Locales.t("clear_db_confirm")) },
            confirmButton = {
                Button(
                    onClick = {
                        AppSettings.clearPin()
                        state.showRemovePinConfirm = false
                        state.locked = false
                        state.mustCreatePin = false
                    }
                ) {
                    Text(Locales.t("yes"))
                }
            },
            dismissButton = {
                TextButton(onClick = { state.showRemovePinConfirm = false }) {
                    Text(Locales.t("cancel"))
                }
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        )
    }

    if (state.showClearDbBackupPrompt) {
        AppDialogTheme {
            AlertDialog(
                onDismissRequest = { state.showClearDbBackupPrompt = false },
                title = { Text(Locales.t("clear_db_title")) },
                text = { Text(Locales.t("clear_db_warning_backup")) },
                confirmButton = {
                    Button(
                        onClick = {
                            state.showClearDbBackupPrompt = false
                            state.exportFileName = "beautyplanner-backup"
                            state.backupEncryptEnabled = true
                            state.backupPassword = ""
                            state.backupPasswordConfirm = ""
                            state.backupPasswordError = null
                            state.showExportNameDialog = true
                            state.showClearDbFinalConfirm = true
                        }
                    ) {
                        Text(Locales.t("clear_db_make_backup"))
                    }
                },
                dismissButton = {
                    Column {
                        TextButton(
                            onClick = {
                                state.showClearDbBackupPrompt = false
                                state.showClearDbFinalConfirm = true
                            }
                        ) {
                            Text(Locales.t("clear_db_skip_backup"), color = Color.Red)
                        }

                        TextButton(onClick = { state.showClearDbBackupPrompt = false }) {
                            Text(Locales.t("cancel"))
                        }
                    }
                },
                shape = AppDialogShape
            )
        }
    }

    if (state.showClearDbFinalConfirm) {
        AppDialogTheme {
            AlertDialog(
                onDismissRequest = { state.showClearDbFinalConfirm = false },
                title = { Text(Locales.t("clear_db_title")) },
                text = { Text(Locales.t("clear_db_confirm")) },
                confirmButton = {
                    Button(
                        onClick = {
                            state.appointments.clear()
                            state.saveAll()
                            state.showClearDbFinalConfirm = false
                        }
                    ) {
                        Text(Locales.t("yes"))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { state.showClearDbFinalConfirm = false }) {
                        Text(Locales.t("cancel"))
                    }
                },
                shape = AppDialogShape
            )
        }
    }

    if (state.showAutoShiftConfirm && state.pendingNewAppt != null) {
        val day = state.pendingNewAppt!!.dateString
        val blockedId = state.shiftBlockedApptId
        val blockedAppt = blockedId?.let { id ->
            state.appointments.firstOrNull { it.id == id && it.dateString == day }
        }

        val chainText = buildString {
            append(Locales.t("auto_shift_conflict_intro"))
            append("\n\n")
            append(Locales.t("auto_shift_conflict_question"))
            append("\n\n")
            if (state.shiftChain.isNotEmpty()) {
                state.shiftChain.forEachIndexed { idx, item ->
                    val a = state.appointments.firstOrNull {
                        it.id == item.apptId && it.dateString == day
                    }
                    val name = a?.clientName ?: "?"
                    append("${idx + 1}) $name → ${state.minutesToHm(item.newStartMin)}\n")
                }
            }
            if (blockedAppt != null) {
                append("\n")
                append(Locales.t("auto_shift_conflict_no_space_for"))
                append(": ${blockedAppt.clientName}")
            }
        }

        AppDialogTheme {
            AlertDialog(
                onDismissRequest = {
                    state.showAutoShiftConfirm = false
                    state.pendingNewAppt = null
                    state.shiftChain = emptyList()
                    state.shiftBlockedApptId = null
                },
                title = { Text(Locales.t("auto_shift_conflict_title")) },
                text = { Text(chainText) },
                confirmButton = {
                    Button(
                        onClick = {
                            val newAppt = state.pendingNewAppt!!

                            state.applyShiftChain(day, state.shiftChain)
                            state.replaceById(newAppt)
                            state.saveAll()

                            state.showAutoShiftConfirm = false
                            state.pendingNewAppt = null

                            if (blockedAppt != null) {
                                state.conflictB = blockedAppt
                                state.showRescheduleBDialog = true
                            }

                            state.shiftChain = emptyList()
                            state.shiftBlockedApptId = null

                            state.showBookingDialog = false
                            state.editingAppointment = null
                        }
                    ) {
                        Text(Locales.t("shift"))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            state.showAutoShiftConfirm = false
                            state.pendingNewAppt = null
                            state.shiftChain = emptyList()
                            state.shiftBlockedApptId = null
                        }
                    ) {
                        Text(Locales.t("cancel"))
                    }
                },
                shape = AppDialogShape
            )
        }
    }

    if (state.showExportNameDialog) {
        var exportPasswordVisible by remember { mutableStateOf(false) }
        var exportPasswordConfirmVisible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                state.showExportNameDialog = false
                state.backupPassword = ""
                state.backupPasswordConfirm = ""
                state.backupPasswordError = null
            },
            title = { Text(Locales.t("export_db")) },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        Locales.t("backup_export_name_hint"),
                        style = MaterialTheme.typography.body2
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = state.exportFileName,
                        onValueChange = { state.exportFileName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(Locales.t("backup_file_name")) }
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(Locales.t("backup_encrypt_toggle"))
                        Switch(
                            checked = state.backupEncryptEnabled,
                            onCheckedChange = { state.backupEncryptEnabled = it }
                        )
                    }

                    if (state.backupEncryptEnabled) {
                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = state.backupPassword,
                            onValueChange = {
                                state.backupPassword = it
                                state.backupPasswordError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text(Locales.t("backup_password")) },
                            visualTransformation = if (exportPasswordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        exportPasswordVisible = !exportPasswordVisible
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (exportPasswordVisible) {
                                            Icons.Default.VisibilityOff
                                        } else {
                                            Icons.Default.Visibility
                                        },
                                        contentDescription = null
                                    )
                                }
                            }
                        )

                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = state.backupPasswordConfirm,
                            onValueChange = {
                                state.backupPasswordConfirm = it
                                state.backupPasswordError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text(Locales.t("backup_password_confirm")) },
                            visualTransformation = if (exportPasswordConfirmVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        exportPasswordConfirmVisible = !exportPasswordConfirmVisible
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (exportPasswordConfirmVisible) {
                                            Icons.Default.VisibilityOff
                                        } else {
                                            Icons.Default.Visibility
                                        },
                                        contentDescription = null
                                    )
                                }
                            }
                        )

                        if (state.backupPasswordError != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = state.backupPasswordError.orEmpty(),
                                color = MaterialTheme.colors.error
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = Locales.t("backup_extension_note"),
                        style = MaterialTheme.typography.caption,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (state.backupEncryptEnabled) {
                            if (state.backupPassword.length < 6) {
                                state.backupPasswordError =
                                    Locales.t("backup_password_too_short")
                                return@Button
                            }
                            if (state.backupPassword != state.backupPasswordConfirm) {
                                state.backupPasswordError =
                                    Locales.t("backup_password_mismatch")
                                return@Button
                            }
                        }

                        state.showGlobalLoading(Locales.t("loading"))
                        try {
                            val payload = DataManager.exportBackupPayload(state.appointments)
                            val safeName = state.exportFileName.trim()
                                .ifBlank { "beautyplanner-backup" }

                            val fileText = if (state.backupEncryptEnabled) {
                                BackupCodec.createEncryptedBackupFile(
                                    payloadJson = payload,
                                    password = state.backupPassword,
                                    appointmentsCount = state.appointments.size
                                )
                            } else {
                                BackupCodec.createPlainBackupFile(
                                    payloadJson = payload,
                                    appointmentsCount = state.appointments.size
                                )
                            }

                            state.showExportNameDialog = false
                            state.backupPassword = ""
                            state.backupPasswordConfirm = ""
                            state.backupPasswordError = null

                            BackupFilePicker.exportJson(
                                suggestedFileName = safeName,
                                json = fileText
                            )

                            state.backupSuccessMessage = Locales.t("backup_export_success")

                            if (state.pendingImportAfterBackup) {
                                state.pendingImportAfterBackup = false
                                state.showImportConfirm = true
                            }
                        } finally {
                            state.hideGlobalLoading()
                        }
                    }
                ) {
                    Text(Locales.t("save"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        state.showExportNameDialog = false
                        state.backupPassword = ""
                        state.backupPasswordConfirm = ""
                        state.backupPasswordError = null
                    }
                ) {
                    Text(Locales.t("cancel"))
                }
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        )
    }
    if (state.showImportBackupPrompt && state.pendingImportText != null) {
        AlertDialog(
            onDismissRequest = {
                state.showImportBackupPrompt = false
                state.pendingImportText = null
                state.pendingImportPreview = null
            },
            title = { Text(Locales.t("backup_import_preview_title")) },
            text = {
                Column {
                    val preview = state.pendingImportPreview
                    if (preview != null) {
                        Text(
                            text = when {
                                preview.isLegacy -> Locales.t("backup_format_legacy")
                                preview.isEncrypted -> Locales.t("backup_format_encrypted")
                                else -> Locales.t("backup_format_plain")
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("${Locales.t("backup_version")}: ${preview.version ?: "—"}")
                        Text("${Locales.t("backup_created_at")}: ${formatBackupCreatedAt(preview.createdAtEpochMillis)}")
                        Text("${Locales.t("backup_appointments_count")}: ${preview.appointmentsCount ?: "—"}")
                        Spacer(Modifier.height(12.dp))
                    }
                    Text(Locales.t("backup_import_make_safety_copy"))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        state.showImportBackupPrompt = false
                        state.exportFileName = "beautyplanner-backup-before-import"
                        state.backupEncryptEnabled = true
                        state.backupPassword = ""
                        state.backupPasswordConfirm = ""
                        state.backupPasswordError = null
                        state.pendingImportAfterBackup = true
                        state.showExportNameDialog = true
                    }
                ) {
                    Text(Locales.t("clear_db_make_backup"))
                }
            },
            dismissButton = {
                Column {
                    TextButton(
                        onClick = {
                            state.showImportBackupPrompt = false
                            state.showImportConfirm = true
                        }
                    ) {
                        Text(Locales.t("import_btn"))
                    }
                    TextButton(
                        onClick = {
                            state.showImportBackupPrompt = false
                            state.pendingImportText = null
                            state.pendingImportPreview = null
                        }
                    ) {
                        Text(Locales.t("cancel"))
                    }
                }
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        )
    }

    if (state.showImportConfirm && state.pendingImportText != null) {
        AlertDialog(
            onDismissRequest = {
                state.showImportConfirm = false
                state.pendingImportText = null
            },
            title = { Text(Locales.t("import_db")) },
            text = { Text(Locales.t("backup_import_confirm_text")) },
            confirmButton = {
                Button(
                    onClick = {
                        state.showGlobalLoading(Locales.t("loading"))
                        try {
                            val text = state.pendingImportText.orEmpty()
                            val parsed = BackupCodec.parseBackupFile(text)
                            when (parsed) {
                                is ParsedBackupFile.LegacyPlainPayload -> {
                                    val imported = DataManager.importBackupPayload(parsed.payloadJson)
                                    if (imported.isEmpty()) {
                                        state.showImportError = Locales.t("backup_import_invalid_payload")
                                    } else {
                                        state.appointments.clear()
                                        state.appointments.addAll(imported)
                                        state.saveAll()
                                        state.showImportError = null
                                        state.backupSuccessMessage = Locales.t("backup_import_success")
                                        state.pendingImportPreview = null
                                    }
                                    state.showImportConfirm = false
                                    state.pendingImportText = null
                                }
                                is ParsedBackupFile.PlainContainer -> {
                                    val payload = parsed.container.payload.orEmpty()
                                    val imported = DataManager.importBackupPayload(payload)
                                    if (imported.isEmpty()) {
                                        state.showImportError = Locales.t("backup_import_invalid_payload")
                                    } else {
                                        state.appointments.clear()
                                        state.appointments.addAll(imported)
                                        state.saveAll()
                                        state.showImportError = null
                                        state.backupSuccessMessage = Locales.t("backup_import_success")
                                        state.pendingImportPreview = null
                                    }
                                    state.showImportConfirm = false
                                    state.pendingImportText = null
                                }
                                is ParsedBackupFile.EncryptedContainer -> {
                                    state.pendingEncryptedImportText = text
                                    state.importPassword = ""
                                    state.importPasswordError = null
                                    state.showImportPasswordDialog = true
                                    state.showImportConfirm = false
                                    state.pendingImportText = null
                                }
                                null -> {
                                    state.showImportError = Locales.t("backup_import_invalid_file")
                                    state.showImportConfirm = false
                                    state.pendingImportText = null
                                    state.pendingImportPreview = null
                                }
                            }
                        } finally {
                            state.hideGlobalLoading()
                        }
                    }
                ) {
                    Text(Locales.t("import_btn"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        state.showImportConfirm = false
                        state.pendingImportText = null
                    }
                ) {
                    Text(Locales.t("cancel"))
                }
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        )
    }

    if (state.showImportError != null) {
        AlertDialog(
            onDismissRequest = { state.showImportError = null },
            title = { Text(Locales.t("import_db")) },
            text = { Text(state.showImportError ?: "") },
            confirmButton = {
                TextButton(onClick = { state.showImportError = null }) {
                    Text(Locales.t("close"))
                }
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        )
    }

    if (state.showImportPasswordDialog && state.pendingEncryptedImportText != null) {
        var importPasswordVisible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                state.showImportPasswordDialog = false
                state.pendingEncryptedImportText = null
                state.importPassword = ""
                state.importPasswordError = null
            },
            title = { Text(Locales.t("backup_import_password_title")) },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.importPassword,
                        onValueChange = {
                            state.importPassword = it
                            state.importPasswordError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(Locales.t("backup_import_password_hint")) },
                        visualTransformation = if (importPasswordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    importPasswordVisible = !importPasswordVisible
                                }
                            ) {
                                Icon(
                                    imageVector = if (importPasswordVisible) {
                                        Icons.Default.VisibilityOff
                                    } else {
                                        Icons.Default.Visibility
                                    },
                                    contentDescription = null
                                )
                            }
                        }
                    )

                    if (state.importPasswordError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = state.importPasswordError.orEmpty(),
                            color = MaterialTheme.colors.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        state.showGlobalLoading(Locales.t("loading"))
                        try {
                            val text = state.pendingEncryptedImportText.orEmpty()
                            val parsed = BackupCodec.parseBackupFile(text)
                            val encrypted =
                                (parsed as? ParsedBackupFile.EncryptedContainer)?.container
                            if (encrypted == null) {
                                state.importPasswordError = Locales.t("backup_import_invalid_payload")
                                return@Button
                            }
                            val payload = BackupCodec.decryptPayload(
                                container = encrypted,
                                password = state.importPassword
                            )
                            if (payload.isNullOrBlank()) {
                                state.importPasswordError =
                                    Locales.t("backup_import_password_invalid")
                                return@Button
                            }
                            val imported = DataManager.importBackupPayload(payload)
                            if (imported.isEmpty()) {
                                state.importPasswordError = Locales.t("backup_import_invalid_payload")
                                return@Button
                            }
                            state.appointments.clear()
                            state.appointments.addAll(imported)
                            state.saveAll()
                            state.showImportPasswordDialog = false
                            state.pendingEncryptedImportText = null
                            state.importPassword = ""
                            state.importPasswordError = null
                            state.showImportError = null
                            state.backupSuccessMessage = Locales.t("backup_import_success")
                            state.pendingImportPreview = null
                        } finally {
                            state.hideGlobalLoading()
                        }
                    }
                ) {
                    Text(Locales.t("import_btn"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        state.showImportPasswordDialog = false
                        state.pendingEncryptedImportText = null
                        state.importPassword = ""
                        state.importPasswordError = null
                    }
                ) {
                    Text(Locales.t("cancel"))
                }
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        )
    }

    if (
        state.showTransferConflictConfirm &&
        state.transferA != null &&
        state.conflictB != null &&
        state.pendingTargetDate != null
    ) {
        AppDialogTheme {
            AlertDialog(
                onDismissRequest = {
                    state.showTransferConflictConfirm = false
                    state.conflictB = null
                },
                title = { Text(Locales.t("transfer_conflict_title")) },
                text = {
                    Text(
                        "${Locales.t("transfer_conflict_text")}\n\n" +
                                "${Locales.t("transfer_conflict_a")}: ${state.transferA!!.clientName}\n" +
                                "${Locales.t("transfer_conflict_b")}: ${state.conflictB!!.clientName}"
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            state.moveAppointment(
                                state.transferA!!,
                                state.pendingTargetDate!!,
                                state.pendingTargetTime
                            )
                            state.showTransferConflictConfirm = false
                            state.showTransferPickDialog = false
                            state.showRescheduleBDialog = true
                        }
                    ) {
                        Text(Locales.t("transfer_agree"))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            state.showTransferConflictConfirm = false
                            state.conflictB = null
                        }
                    ) {
                        Text(Locales.t("cancel"))
                    }
                },
                shape = AppDialogShape
            )
        }
    }

    if (state.showRescheduleBDialog && state.conflictB != null) {
        RescheduleClientBDialog(
            clientName = state.conflictB!!.clientName,
            initialSelectedDate = LocalDate.parse(state.conflictB!!.dateString),
            initialMonthDate = LocalDate.parse(state.conflictB!!.dateString),
            onDismiss = {
                state.showRescheduleBDialog = false
                state.saveAll()
                state.transferA = null
                state.conflictB = null
                state.pendingTargetDate = null
                state.pendingTargetTime = ""
            },
            onConfirm = { newDate, newTime ->
                state.moveAppointment(state.conflictB!!, newDate, newTime)
                state.saveAll()

                state.showRescheduleBDialog = false
                state.transferA = null
                state.conflictB = null
                state.pendingTargetDate = null
                state.pendingTargetTime = ""
            }
        )
    }

    if (state.showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { state.showDeleteConfirm = null },
            title = { Text(Locales.t("delete_title")) },
            text = {
                val client = state.showDeleteConfirm?.clientName ?: ""
                val time = state.showDeleteConfirm?.time ?: ""
                Text(
                    "${Locales.t("delete_confirm_prefix")} $client " +
                            "${Locales.t("delete_confirm_at")} $time. " +
                            "${Locales.t("continue_question")}"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.appointments.remove(state.showDeleteConfirm)
                        state.saveAll()
                        state.showDeleteConfirm = null
                    }
                ) {
                    Text(
                        Locales.t("delete_btn"),
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { state.showDeleteConfirm = null }) {
                    Text(Locales.t("cancel").uppercase())
                }
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        )
    }
}