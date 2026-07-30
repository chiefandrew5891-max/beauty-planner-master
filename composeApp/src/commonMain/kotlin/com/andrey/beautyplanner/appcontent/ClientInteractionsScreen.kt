package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Appointment
import com.andrey.beautyplanner.CloudSyncLogger
import com.andrey.beautyplanner.Locales
import com.andrey.beautyplanner.appcontent.approot.rememberAppRootState
import com.andrey.beautyplanner.remote.MasterProfileSync
import com.andrey.beautyplanner.remote.MasterScheduleSync
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun ClientInteractionsScreen() {
    val fontScale = AppSettings.getFontScale()
    val onBg = MaterialTheme.colors.onBackground
    val onSurface = MaterialTheme.colors.onSurface
    val scope = rememberCoroutineScope()

    val appState = rememberAppRootState()

    var showEnableConfirm by remember { mutableStateOf(false) }
    var publishDebugStatus by remember { mutableStateOf("no publish yet") }
    var publishDebugStamp by remember { mutableStateOf(0L) }
    var publishPreviewLines by remember { mutableStateOf(listOf<String>()) }

    fun rebuildPreview() {
        val slots = MasterScheduleSync.buildBusySlots(appState.appointments.toList())
        publishPreviewLines = slots.mapIndexed { index, slot ->
            "${index + 1}) ${slot.date} | ${slot.startTime}-${slot.endTime}"
        }
        publishDebugStamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    }

    fun syncProfileOnly() {
        scope.launch {
            MasterProfileSync.syncIfAuthenticated()
                .onFailure {
                    CloudSyncLogger.log("syncMasterProfile: failed: ${it.message}")
                }
        }
    }

    fun syncBusySlotsNow(reason: String) {
        scope.launch {
            val localAppointments = appState.appointments.toList()
            val localBusySlots = MasterScheduleSync.buildBusySlots(localAppointments)

            publishDebugStatus =
                "start reason='$reason' | appointments=${localAppointments.size} | visibleBusySlots=${localBusySlots.size} | interactionsEnabled=${AppSettings.clientInteractionsEnabled} | autoPublishBusySlots=${AppSettings.autoPublishBusySlots}"
            rebuildPreview()

            MasterScheduleSync.syncIfEligible(localAppointments)
                .onSuccess { result ->
                    publishDebugStatus =
                        "success reason='$reason' | response=$result | appointments=${localAppointments.size} | visibleBusySlots=${localBusySlots.size} | interactionsEnabled=${AppSettings.clientInteractionsEnabled} | autoPublishBusySlots=${AppSettings.autoPublishBusySlots}"
                    rebuildPreview()
                }
                .onFailure {
                    publishDebugStatus =
                        "failure reason='$reason' | message=${it.message} | appointments=${localAppointments.size} | visibleBusySlots=${localBusySlots.size}"
                    rebuildPreview()
                    CloudSyncLogger.log("syncMyPublicSchedule: failed: ${it.message}")
                }
        }
    }

    if (showEnableConfirm) {
        AlertDialog(
            onDismissRequest = {
                showEnableConfirm = false
            },
            title = {
                Text(
                    text = "Подтверждение активации",
                    color = onSurface,
                    fontSize = (18 * fontScale).sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Активировав режим взаимодействия с клиентами, пользователи смогут видеть информацию о вас, ваш рейтинг, оставлять отзывы и взаимодействовать с вашим профилем.",
                    color = onSurface.copy(alpha = 0.85f),
                    fontSize = (14 * fontScale).sp,
                    lineHeight = (20 * fontScale).sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        AppSettings.clientInteractionsEnabled = true
                        AppSettings.persist()
                        showEnableConfirm = false
                        syncProfileOnly()
                        syncBusySlotsNow("enable client interactions")
                    }
                ) {
                    Text("Подтвердить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEnableConfirm = false
                    }
                ) {
                    Text(
                        text = "Отмена",
                        color = onSurface.copy(alpha = 0.85f)
                    )
                }
            }
        )
    }

    CenteredNarrowContentContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Взаимодействие с клиентами",
                fontSize = (22 * fontScale).sp,
                fontWeight = FontWeight.Bold,
                color = onBg
            )

            Text(
                text = "Управление видимостью мастера для клиентов и будущими сценариями взаимодействия.",
                fontSize = (14 * fontScale).sp,
                color = onBg.copy(alpha = 0.7f)
            )

            Divider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Активировать взаимодействие с клиентами",
                    fontSize = (16 * fontScale).sp,
                    color = onSurface,
                    modifier = Modifier.weight(1f)
                )

                AppSwitch(
                    checked = AppSettings.clientInteractionsEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            showEnableConfirm = true
                        } else {
                            AppSettings.clientInteractionsEnabled = false
                            AppSettings.autoPublishBusySlots = false
                            AppSettings.persist()
                            syncProfileOnly()
                            syncBusySlotsNow("disable client interactions")
                        }
                    }
                )
            }

            Divider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Разрешить сервису отображать клиентам ваши свободные ячейки автоматически",
                    fontSize = (16 * fontScale).sp,
                    color = onSurface,
                    modifier = Modifier.weight(1f)
                )

                AppSwitch(
                    checked = AppSettings.autoPublishBusySlots,
                    onCheckedChange = { enabled ->
                        AppSettings.autoPublishBusySlots = enabled
                        AppSettings.persist()
                        syncBusySlotsNow("toggle auto publish")
                    },
                    enabled = AppSettings.clientInteractionsEnabled
                )
            }

            Text(
                text = if (AppSettings.clientInteractionsEnabled) {
                    "Режим взаимодействия с клиентами включён."
                } else {
                    "Режим взаимодействия с клиентами выключен."
                },
                fontSize = (13 * fontScale).sp,
                color = onSurface.copy(alpha = 0.72f),
                lineHeight = (20 * fontScale).sp
            )

            if (AppSettings.clientInteractionsEnabled) {
                Text(
                    text = if (AppSettings.autoPublishBusySlots) {
                        "Автоматическая публикация занятых интервалов включена. Клиенты будут видеть только свободные окна между уже занятыми слотами."
                    } else {
                        "Автоматическая публикация занятых интервалов выключена."
                    },
                    fontSize = (13 * fontScale).sp,
                    color = onSurface.copy(alpha = 0.72f),
                    lineHeight = (20 * fontScale).sp
                )
            }

            Divider()

            ClientInteractionsLoggerCard(
                publishDebugStamp = publishDebugStamp,
                publishDebugStatus = publishDebugStatus,
                clientInteractionsEnabled = AppSettings.clientInteractionsEnabled,
                autoPublishBusySlots = AppSettings.autoPublishBusySlots,
                appointments = appState.appointments.toList(),
                previewLines = publishPreviewLines,
                cloudLogLines = CloudSyncLogger.entries.toList()
            )
        }
    }
}

@Composable
private fun ClientInteractionsLoggerCard(
    publishDebugStamp: Long,
    publishDebugStatus: String,
    clientInteractionsEnabled: Boolean,
    autoPublishBusySlots: Boolean,
    appointments: List<Appointment>,
    previewLines: List<String>,
    cloudLogLines: List<String>
) {
    val fontScale = AppSettings.getFontScale()
    val onSurface = MaterialTheme.colors.onSurface

    val visibleAppointments = appointments.count { !it.isDeleted }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, onSurface.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "LOGGER",
            fontSize = (15 * fontScale).sp,
            fontWeight = FontWeight.Bold,
            color = onSurface
        )

        Text(
            text = "stamp=$publishDebugStamp",
            fontSize = (12 * fontScale).sp,
            color = onSurface.copy(alpha = 0.75f)
        )

        Text(
            text = "clientInteractionsEnabled=$clientInteractionsEnabled | autoPublishBusySlots=$autoPublishBusySlots",
            fontSize = (12 * fontScale).sp,
            color = onSurface.copy(alpha = 0.75f)
        )

        Text(
            text = "appointments=${appointments.size} | visibleAppointments=$visibleAppointments | busySlotsPreview=${previewLines.size}",
            fontSize = (12 * fontScale).sp,
            color = onSurface.copy(alpha = 0.75f)
        )

        Text(
            text = "publishStatus=$publishDebugStatus",
            fontSize = (12 * fontScale).sp,
            color = onSurface.copy(alpha = 0.75f)
        )

        Divider()

        Text(
            text = "Busy slots preview",
            fontSize = (13 * fontScale).sp,
            fontWeight = FontWeight.SemiBold,
            color = onSurface
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (previewLines.isEmpty()) {
                Text(
                    text = "No busy slots prepared",
                    fontSize = (12 * fontScale).sp,
                    color = onSurface.copy(alpha = 0.55f)
                )
            } else {
                previewLines.forEach { line ->
                    Text(
                        text = line,
                        fontSize = (11 * fontScale).sp,
                        color = onSurface.copy(alpha = 0.78f)
                    )
                }
            }
        }

        Divider()

        Text(
            text = "Cloud sync log",
            fontSize = (13 * fontScale).sp,
            fontWeight = FontWeight.SemiBold,
            color = onSurface
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (cloudLogLines.isEmpty()) {
                Text(
                    text = "No logs yet",
                    fontSize = (12 * fontScale).sp,
                    color = onSurface.copy(alpha = 0.55f)
                )
            } else {
                cloudLogLines.forEach { line ->
                    Text(
                        text = line,
                        fontSize = (11 * fontScale).sp,
                        color = onSurface.copy(alpha = 0.78f)
                    )
                }
            }
        }
    }
}
