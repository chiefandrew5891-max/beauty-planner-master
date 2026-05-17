package com.andrey.beautyplanner.appcontent.approot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andrey.beautyplanner.*
import com.andrey.beautyplanner.appcontent.*
import kotlinx.datetime.LocalDate
import androidx.compose.ui.Modifier


@Composable
fun AppRootDialogs(state: AppRootState) {

    // --- Save error dialog (оставляем как было, не входит в “3 попапа”) ---
    if (state.showSaveError != null) {
        AlertDialog(
            onDismissRequest = { state.showSaveError = null },
            title = { Text(Locales.t("import_db")) },
            text = { Text(state.showSaveError ?: "") },
            confirmButton = {
                TextButton(onClick = { state.showSaveError = null }) { Text(Locales.t("close")) }
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        )
    }

    // --------- Mandatory PIN creation on first run ---------
    if (state.mustCreatePin) {
        // PinDialog внутри себя AlertDialog, поэтому оборачиваем его “снаружи” стилем
        AppDialogTheme {
            PinDialog(
                title = Locales.t("pin_set"),
                text = Locales.t("pin_invalid_format"),
                confirmText = Locales.t("save"),
                onDismiss = { /* cannot dismiss */ },
                onConfirmPin = { newPin ->
                    if (AppSettings.setPin(newPin)) {
                        state.mustCreatePin = false
                        state.locked = false
                    }
                }
            )
        }
    }

    // --------- PIN action dialog (for protected actions) ---------
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

    // --------- Lock screen (PIN on app open) ---------
    if (state.locked && !state.mustCreatePin) {
        AppDialogTheme {
            PinDialog(
                title = Locales.t("unlock_title"),
                text = Locales.t("unlock_text"),
                confirmText = Locales.t("confirm"),
                onDismiss = { /* cannot dismiss */ },
                onConfirmPin = { pin ->
                    if (AppSettings.verifyPin(pin)) state.locked = false
                }
            )
        }
    }

    // --------- Set/Change PIN from Settings ---------
    if (state.showSetPinDialog) {
        AppDialogTheme {
            PinDialog(
                title = if (AppSettings.isPinSet()) Locales.t("pin_change") else Locales.t("pin_set"),
                text = Locales.t("pin_invalid_format"),
                confirmText = Locales.t("save"),
                onDismiss = { state.showSetPinDialog = false },
                onConfirmPin = { newPin ->
                    if (AppSettings.setPin(newPin)) {
                        state.showSetPinDialog = false
                        state.mustCreatePin = false
                    }
                }
            )
        }
    }

    // --------- Remove PIN confirm ---------
    if (state.showRemovePinConfirm) {
        AlertDialog(
            onDismissRequest = { state.showRemovePinConfirm = false },
            title = { Text(Locales.t("pin_remove")) },
            text = { Text(Locales.t("clear_db_confirm")) },
            confirmButton = {
                Button(onClick = {
                    AppSettings.clearPin()
                    state.showRemovePinConfirm = false
                    state.locked = false
                    state.mustCreatePin = true
                }) { Text(Locales.t("yes")) }
            },
            dismissButton = {
                TextButton(onClick = { state.showRemovePinConfirm = false }) { Text(Locales.t("cancel")) }
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        )
    }

    // --------- Clear DB: backup prompt ---------
    if (state.showClearDbBackupPrompt) {
        AppDialogTheme {
            AlertDialog(
                onDismissRequest = { state.showClearDbBackupPrompt = false },
                title = { Text(Locales.t("clear_db_title")) },
                text = { Text(Locales.t("clear_db_warning_backup")) },
                confirmButton = {
                    Button(onClick = {
                        state.showClearDbBackupPrompt = false
                        state.exportFileName = "beautyplanner-backup"
                        state.showExportNameDialog = true
                        state.showClearDbFinalConfirm = true
                    }) { Text(Locales.t("clear_db_make_backup")) }
                },
                dismissButton = {
                    Column {
                        TextButton(onClick = {
                            state.showClearDbBackupPrompt = false
                            state.showClearDbFinalConfirm = true
                        }) { Text(Locales.t("clear_db_skip_backup"), color = Color.Red) }

                        TextButton(onClick = { state.showClearDbBackupPrompt = false }) {
                            Text(Locales.t("cancel"))
                        }
                    }
                },
                shape = AppDialogShape
            )
        }
    }

    // --------- Clear DB: final confirm ---------
    if (state.showClearDbFinalConfirm) {
        AppDialogTheme {
            AlertDialog(
                onDismissRequest = { state.showClearDbFinalConfirm = false },
                title = { Text(Locales.t("clear_db_title")) },
                text = { Text(Locales.t("clear_db_confirm")) },
                confirmButton = {
                    Button(onClick = {
                        state.appointments.clear()
                        state.saveAll()
                        state.showClearDbFinalConfirm = false
                    }) { Text(Locales.t("yes")) }
                },
                dismissButton = {
                    TextButton(onClick = { state.showClearDbFinalConfirm = false }) { Text(Locales.t("cancel")) }
                },
                shape = AppDialogShape
            )
        }
    }

    // --- Auto shift confirm dialog ---
    if (state.showAutoShiftConfirm && state.pendingNewAppt != null) {
        val day = state.pendingNewAppt!!.dateString
        val blockedId = state.shiftBlockedApptId
        val blockedAppt = blockedId?.let { id ->
            state.appointments.firstOrNull { it.id == id && it.dateString == day }
        }

        val chainText = buildString {
            append("Есть пересечение по времени.\n\n")
            append("Сдвинуть следующие записи автоматически?\n\n")
            if (state.shiftChain.isNotEmpty()) {
                state.shiftChain.forEachIndexed { idx, item ->
                    val a = state.appointments.firstOrNull { it.id == item.apptId && it.dateString == day }
                    val name = a?.clientName ?: "?"
                    append("${idx + 1}) $name → ${state.minutesToHm(item.newStartMin)}\n")
                }
            }
            if (blockedAppt != null) {
                append("\nНе хватает места в конце дня для: ${blockedAppt.clientName}")
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
                title = { Text("Конфликт времени") },
                text = { Text(chainText) },
                confirmButton = {
                    Button(onClick = {
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
                    }) { Text("Сдвинуть") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        state.showAutoShiftConfirm = false
                        state.pendingNewAppt = null
                        state.shiftChain = emptyList()
                        state.shiftBlockedApptId = null
                    }) { Text(Locales.t("cancel")) }
                },
                shape = AppDialogShape
            )
        }
    }

    // --- Export name dialog ---
    if (state.showExportNameDialog) {
        AlertDialog(
            onDismissRequest = { state.showExportNameDialog = false },
            title = { Text(Locales.t("export_db")) },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    Text(Locales.t("backup_export_name_hint"), style = MaterialTheme.typography.body2)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.exportFileName,
                        onValueChange = { state.exportFileName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(Locales.t("backup_file_name")) }
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = Locales.t("backup_extension_note"),
                        style = MaterialTheme.typography.caption,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    state.showExportNameDialog = false
                    val json = DataManager.exportBackup(state.appointments)
                    val safeName = state.exportFileName.trim().ifBlank { "beautyplanner-backup" }
                    BackupFilePicker.exportJson(
                        suggestedFileName = safeName,
                        json = json
                    )
                }) { Text(Locales.t("save")) }
            },
            dismissButton = {
                TextButton(onClick = { state.showExportNameDialog = false }) {
                    Text(Locales.t("cancel"))
                }
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        )
    }

    // --- Import confirm dialog ---
    if (state.showImportConfirm && state.pendingImportText != null) {
        AlertDialog(
            onDismissRequest = {
                state.showImportConfirm = false
                state.pendingImportText = null
            },
            title = { Text(Locales.t("import_db")) },
            text = { Text(Locales.t("backup_import_confirm_text")) },
            confirmButton = {
                Button(onClick = {
                    val text = state.pendingImportText.orEmpty()
                    val imported = DataManager.importBackup(text)
                    if (imported.isEmpty()) {
                        state.showImportError = Locales.t("import_invalid_json")
                    } else {
                        state.appointments.clear()
                        state.appointments.addAll(imported)
                        state.saveAll()
                        state.showImportError = null
                    }

                    state.showImportConfirm = false
                    state.pendingImportText = null
                }) { Text(Locales.t("import_btn")) }
            },
            dismissButton = {
                TextButton(onClick = {
                    state.showImportConfirm = false
                    state.pendingImportText = null
                }) { Text(Locales.t("cancel")) }
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        )
    }

    // --- Import error dialog ---
    if (state.showImportError != null) {
        AlertDialog(
            onDismissRequest = { state.showImportError = null },
            title = { Text(Locales.t("import_db")) },
            text = { Text(state.showImportError ?: "") },
            confirmButton = {
                TextButton(onClick = { state.showImportError = null }) { Text(Locales.t("close")) }
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        )
    }

    // --- Transfer conflict confirm ---
    if (state.showTransferConflictConfirm &&
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
                    Button(onClick = {
                        state.moveAppointment(state.transferA!!, state.pendingTargetDate!!, state.pendingTargetTime)
                        state.showTransferConflictConfirm = false
                        state.showTransferPickDialog = false
                        state.showRescheduleBDialog = true
                    }) { Text(Locales.t("transfer_agree")) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        state.showTransferConflictConfirm = false
                        state.conflictB = null
                    }) { Text(Locales.t("cancel")) }
                },
                shape = AppDialogShape
            )
        }
    }

    // --- Reschedule B dialog ---
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

    // --- Delete confirm ---
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
                TextButton(onClick = {
                    state.appointments.remove(state.showDeleteConfirm)
                    state.saveAll()
                    state.showDeleteConfirm = null
                }) {
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