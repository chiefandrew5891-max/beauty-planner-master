package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import androidx.compose.runtime.rememberCoroutineScope
import com.andrey.beautyplanner.CloudSyncLogger
import com.andrey.beautyplanner.remote.MasterProfileSync
import kotlinx.coroutines.launch

@Composable
fun ClientInteractionsScreen() {
    val fontScale = AppSettings.getFontScale()
    val onBg = MaterialTheme.colors.onBackground
    val onSurface = MaterialTheme.colors.onSurface
    val scope = rememberCoroutineScope()

    var showEnableConfirm by remember { mutableStateOf(false) }

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
                        scope.launch {
                            MasterProfileSync.syncIfAuthenticated()
                                .onFailure { CloudSyncLogger.log("syncMasterProfile: failed: ${it.message}") }
                        }
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
                            AppSettings.persist()
                            scope.launch {
                                MasterProfileSync.syncIfAuthenticated()
                                    .onFailure { CloudSyncLogger.log("syncMasterProfile: failed: ${it.message}") }
                            }
                        }
                    }
                )
                Divider()
                Text(
                    text = "Разрешить сервису отображать клиентам ваши свободные ячейки автоматически",
                    fontSize = (16 * fontScale).sp,
                    color = onSurface,
                    modifier = Modifier.weight(1f)
                )

                AppSwitch(
                    
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
        }
    }
}