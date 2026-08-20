package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Appointment
import com.andrey.beautyplanner.ClientDatabase
import com.andrey.beautyplanner.ClientProfileStatus
import com.andrey.beautyplanner.Locales

@Composable
fun ClientDatabaseScreen(
    appointments: List<Appointment>
) {
    val fontScale = AppSettings.getFontScale()
    var query by remember { mutableStateOf("") }

    val entries = remember(appointments, AppSettings.clientProfiles) {
        ClientDatabase.build(
            appointments = appointments,
            profiles = AppSettings.getClientProfiles()
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

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(Locales.t("client_database_search")) }
            )

            if (filtered.isEmpty()) {
                Text(
                    text = Locales.t("client_database_empty"),
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
                )
            } else {
                filtered.forEach { client ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                        elevation = 2.dp,
                        backgroundColor = MaterialTheme.colors.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = client.displayName,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = (16 * fontScale).sp,
                                color = MaterialTheme.colors.onSurface
                            )

                            if (client.phone.isNotBlank()) {
                                Text(
                                    text = client.phone,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.75f)
                                )
                            }

                            Text(
                                text = "${Locales.t("client_database_visits")}: ${client.visitCount}",
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                            )

                            if (client.lastVisitDate.isNotBlank()) {
                                Text(
                                    text = "${Locales.t("client_database_last_visit")}: ${client.lastVisitDate}",
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                                )
                            }

                            if (client.status != ClientProfileStatus.NONE.name) {
                                Text(
                                    text = Locales.t("client_status_${client.status.lowercase()}"),
                                    color = MaterialTheme.colors.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (client.notes.isNotBlank()) {
                                Text(
                                    text = client.notes,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}