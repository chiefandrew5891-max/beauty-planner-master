package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Appointment
import com.andrey.beautyplanner.ClientDatabase
import com.andrey.beautyplanner.ClientProfileStatus
import com.andrey.beautyplanner.Locales
import com.andrey.beautyplanner.appcontent.ClientNameWithIndicators

@Composable
fun BlacklistScreen(
    appointments: List<Appointment>
) {
    val fontScale = AppSettings.getFontScale()

    val entries = remember(appointments, AppSettings.clientProfiles) {
        ClientDatabase.build(
            appointments = appointments,
            profiles = AppSettings.clientProfiles
        )
    }

    val blacklistedClients = remember(entries) {
        entries
            .filter { it.status == ClientProfileStatus.DO_NOT_BOOK.name }
            .sortedBy { it.displayName.lowercase() }
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
                text = Locales.t("client_blacklist_title"),
                fontSize = (22 * fontScale).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onBackground
            )

            Text(
                text = Locales.t("client_blacklist_hint"),
                fontSize = (14 * fontScale).sp,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f)
            )

            if (blacklistedClients.isEmpty()) {
                Text(
                    text = Locales.t("client_blacklist_empty"),
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
                )
            } else {
                blacklistedClients.forEach { client ->
                    val titleText = AppSettings.clientDisplayName(
                        name = client.displayName,
                        phone = client.phone
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                ClientNameWithIndicators(
                                    name = client.displayName,
                                    phone = client.phone,
                                    fontSize = (16 * fontScale).sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colors.onSurface,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(
                                    onClick = {
                                        val existingProfile = AppSettings.findClientProfile(
                                            name = client.displayName,
                                            phone = client.phone
                                        )

                                        if (existingProfile != null) {
                                            AppSettings.upsertClientProfile(
                                                existingProfile.copy(
                                                    status = ClientProfileStatus.NONE.name
                                                )
                                            )
                                        }
                                    },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = Locales.t("delete_btn"),
                                        tint = MaterialTheme.colors.error
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

                            if (client.colorTag.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(
                                            color = colorTagToColor(client.colorTag),
                                            shape = RoundedCornerShape(50)
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}