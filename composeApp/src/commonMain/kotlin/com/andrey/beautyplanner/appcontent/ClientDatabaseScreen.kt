package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.OutlinedButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Appointment
import com.andrey.beautyplanner.ClientDatabase
import com.andrey.beautyplanner.ClientProfileStatus
import com.andrey.beautyplanner.Locales
import kotlinx.datetime.Clock

@Composable
fun ClientDatabaseScreen(
    appointments: List<Appointment>,
    onOpenBlacklist: () -> Unit,
    onOpenClientDetails: (String) -> Unit
) {
    val fontScale = AppSettings.getFontScale()
    val onSurface = MaterialTheme.colors.onSurface

    var query by remember { mutableStateOf("") }

    val entries = remember(appointments, AppSettings.clientProfiles) {
        ClientDatabase.build(
            appointments = appointments,
            profiles = AppSettings.clientProfiles
        )
    }

    val filtered = remember(entries, query) {
        val q = query.trim().lowercase()

        if (q.isBlank()) {
            entries
        } else {
            entries.filter {
                it.displayName.lowercase().contains(q) ||
                        it.phone.contains(q)
            }
        }
    }

    val hasBlacklistedClients = remember(entries) {
        entries.any { it.status == ClientProfileStatus.DO_NOT_BOOK.name }
    }

    CenteredNarrowContentContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = Locales.t("client_database_title"),
                fontSize = (22 * fontScale).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onBackground
            )

            Text(
                text = Locales.t("client_database_hint"),
                fontSize = (14 * fontScale).sp,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f)
            )

            if (hasBlacklistedClients) {
                OutlinedButton(
                    onClick = onOpenBlacklist,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = Color(0xFFD32F2F).copy(alpha = 0.35f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        backgroundColor = Color(0xFFD32F2F).copy(alpha = 0.06f),
                        contentColor = Color(0xFFD32F2F)
                    )
                ) {
                    Text(
                        text = Locales.t("client_database_open_blacklist"),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(Locales.t("client_database_search")) },
                shape = RoundedCornerShape(14.dp),
                textStyle = TextStyle(
                    fontSize = (15 * fontScale).sp,
                    color = MaterialTheme.colors.onSurface
                ),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = MaterialTheme.colors.onSurface,
                    backgroundColor = MaterialTheme.colors.surface,
                    focusedBorderColor = MaterialTheme.colors.primary,
                    unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.22f),
                    focusedLabelColor = MaterialTheme.colors.primary,
                    unfocusedLabelColor = MaterialTheme.colors.onSurface.copy(alpha = 0.55f),
                    cursorColor = MaterialTheme.colors.primary
                )
            )

            if (filtered.isEmpty()) {
                Text(
                    text = Locales.t("client_database_empty"),
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
                )
            } else {
                filtered.forEach { client ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onOpenClientDetails(client.id)
                            },
                        shape = RoundedCornerShape(14.dp),
                        elevation = 2.dp,
                        backgroundColor = MaterialTheme.colors.surface,
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ClientNameWithIndicators(
                                    name = client.displayName,
                                    phone = client.phone,
                                    fontSize = (16 * fontScale).sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppSettings.clientDisplayColor(
                                        name = client.displayName,
                                        phone = client.phone,
                                        defaultColor = MaterialTheme.colors.onSurface
                                    ),
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2
                                )

                                if (client.colorTag.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .padding(start = 12.dp, top = 2.dp)
                                            .size(12.dp)
                                            .background(
                                                color = colorTagToColor(client.colorTag),
                                                shape = RoundedCornerShape(50)
                                            )
                                    )
                                }
                            }

                            if (client.phone.isNotBlank()) {
                                Text(
                                    text = client.phone,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.75f)
                                )
                            }

                            Text(
                                text = "${Locales.t("client_database_visits")}: ${client.visitCount}",
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.82f)
                            )

                            if (client.lastVisitDate.isNotBlank()) {
                                Text(
                                    text = "${Locales.t("client_database_last_visit")}: ${client.lastVisitDate}",
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.82f)
                                )
                            }

                            if (client.status != ClientProfileStatus.NONE.name) {
                                Text(
                                    text = Locales.t("client_status_${client.status.lowercase()}"),
                                    color = MaterialTheme.colors.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}