package com.andrey.beautyplanner.appcontent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Appointment
import com.andrey.beautyplanner.ClientDatabase
import com.andrey.beautyplanner.ClientProfile
import com.andrey.beautyplanner.ClientProfileStatus
import com.andrey.beautyplanner.Locales
import com.andrey.beautyplanner.PhoneCaller
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

private enum class ClientDetailsFilter {
    NEWEST,
    OLDEST,
    COMPLETED,
    PENDING
}

@Composable
fun ClientDetailsScreen(
    clientId: String,
    appointments: List<Appointment>,
    onBack: () -> Unit
) {
    val fontScale = AppSettings.getFontScale()
    val onSurface = MaterialTheme.colors.onSurface
    val primary = MaterialTheme.colors.primary

    var showProfileDialog by remember(clientId) { mutableStateOf(false) }
    var selectedFilter by remember(clientId) { mutableStateOf(ClientDetailsFilter.NEWEST) }
    var sortExpanded by remember(clientId) { mutableStateOf(false) }
    var nowMillis by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }

    val visibleAppointments = remember(appointments) {
        appointments.filterNot { it.isDeleted }
    }

    val entries = remember(visibleAppointments, AppSettings.clientProfiles) {
        ClientDatabase.build(
            appointments = visibleAppointments,
            profiles = AppSettings.clientProfiles
        )
    }

    val clientEntry = remember(entries, clientId) {
        entries.firstOrNull { it.id == clientId }
    }

    if (clientEntry == null) {
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
                    fontSize = (20 * fontScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurface
                )

                Text(
                    text = Locales.t("client_details_no_records"),
                    color = onSurface.copy(alpha = 0.68f)
                )
            }
        }
        return
    }

    val clientAppointments = remember(visibleAppointments, clientEntry.displayName, clientEntry.phone) {
        visibleAppointments.filter { appointment ->
            appointment.clientName.trim().equals(clientEntry.displayName.trim(), ignoreCase = true) &&
                    appointment.phone.trim() == clientEntry.phone.trim()
        }
    }

    val existingProfile = AppSettings.clientProfiles
        .firstOrNull { it.id == clientEntry.id }
        ?: ClientProfile(
            id = clientEntry.id,
            displayName = clientEntry.displayName,
            phone = clientEntry.phone,
            notes = "",
            colorTag = "",
            status = ClientProfileStatus.NONE.name,
            updatedAtMillis = Clock.System.now().toEpochMilliseconds()
        )

    val sortedAppointments = remember(clientAppointments, selectedFilter, nowMillis) {
        when (selectedFilter) {
            ClientDetailsFilter.NEWEST -> {
                clientAppointments.sortedByDescending { appointmentDateTimeMillis(it) }
            }

            ClientDetailsFilter.OLDEST -> {
                clientAppointments.sortedBy { appointmentDateTimeMillis(it) }
            }

            ClientDetailsFilter.COMPLETED -> {
                clientAppointments
                    .filter { appointmentDateTimeMillis(it) in 1 until nowMillis }
                    .sortedByDescending { appointmentDateTimeMillis(it) }
            }

            ClientDetailsFilter.PENDING -> {
                clientAppointments
                    .filter { appointmentDateTimeMillis(it) >= nowMillis }
                    .sortedBy { appointmentDateTimeMillis(it) }
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
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = clientEntry.displayName,
                    fontSize = (22 * fontScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = AppSettings.clientDisplayColor(
                        name = clientEntry.displayName,
                        phone = clientEntry.phone,
                        defaultColor = onSurface
                    )
                )

                if (clientEntry.phone.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                PhoneCaller.call(clientEntry.phone)
                            }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = primary,
                            modifier = Modifier.size(19.dp)
                        )

                        Text(
                            text = clientEntry.phone,
                            fontSize = (14 * fontScale).sp,
                            color = primary,
                            textDecoration = TextDecoration.Underline
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = 0.dp,
                backgroundColor = MaterialTheme.colors.surface
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                sortExpanded = !sortExpanded
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Locales.t("client_details_sort_title"),
                            fontSize = (16 * fontScale).sp,
                            fontWeight = FontWeight.SemiBold,
                            color = onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            imageVector = if (sortExpanded) {
                                Icons.Default.KeyboardArrowUp
                            } else {
                                Icons.Default.KeyboardArrowDown
                            },
                            contentDescription = null,
                            tint = onSurface.copy(alpha = 0.75f)
                        )
                    }

                    AnimatedVisibility(visible = sortExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ClientDetailsRadioRow(
                                title = Locales.t("client_details_sort_newest"),
                                selected = selectedFilter == ClientDetailsFilter.NEWEST,
                                onSelect = { selectedFilter = ClientDetailsFilter.NEWEST }
                            )

                            ClientDetailsRadioRow(
                                title = Locales.t("client_details_sort_oldest"),
                                selected = selectedFilter == ClientDetailsFilter.OLDEST,
                                onSelect = { selectedFilter = ClientDetailsFilter.OLDEST }
                            )

                            ClientDetailsRadioRow(
                                title = Locales.t("client_details_sort_completed"),
                                selected = selectedFilter == ClientDetailsFilter.COMPLETED,
                                onSelect = {
                                    nowMillis = Clock.System.now().toEpochMilliseconds()
                                    selectedFilter = ClientDetailsFilter.COMPLETED
                                }
                            )

                            ClientDetailsRadioRow(
                                title = Locales.t("client_details_sort_pending"),
                                selected = selectedFilter == ClientDetailsFilter.PENDING,
                                onSelect = {
                                    nowMillis = Clock.System.now().toEpochMilliseconds()
                                    selectedFilter = ClientDetailsFilter.PENDING
                                }
                            )
                        }
                    }
                }
            }

            Button(
                onClick = { showProfileDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(Locales.t("client_details_notes_and_status"))
            }

            if (sortedAppointments.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = 1.dp,
                    backgroundColor = MaterialTheme.colors.surface,
                    border = BorderStroke(
                        width = 1.dp,
                        color = onSurface.copy(alpha = 0.08f)
                    )
                ) {
                    Text(
                        text = Locales.t("client_details_no_records"),
                        modifier = Modifier.padding(16.dp),
                        color = onSurface.copy(alpha = 0.68f)
                    )
                }
            } else {
                sortedAppointments.forEach { appointment ->
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
                            Text(
                                text = "${appointment.dateString} • ${appointment.time}",
                                fontSize = (15 * fontScale).sp,
                                fontWeight = FontWeight.SemiBold,
                                color = onSurface
                            )

                            if (appointment.serviceName.isNotBlank()) {
                                Text(
                                    text = "${Locales.t("service")}: ${appointment.serviceName}",
                                    color = onSurface.copy(alpha = 0.82f)
                                )
                            }

                            if (appointment.price.isNotBlank()) {
                                Text(
                                    text = "${Locales.t("price")}: ${appointment.price}",
                                    color = onSurface.copy(alpha = 0.82f)
                                )
                            }

                            if (appointment.notes.isNotBlank()) {
                                Text(
                                    text = appointment.notes,
                                    color = onSurface.copy(alpha = 0.72f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showProfileDialog) {
        ClientProfileDialog(
            initialProfile = existingProfile,
            visitCount = clientEntry.visitCount,
            lastVisitDate = clientEntry.lastVisitDate,
            onDismiss = { showProfileDialog = false },
            onSave = { updated ->
                AppSettings.upsertClientProfile(updated)
                showProfileDialog = false
            }
        )
    }
}

@Composable
private fun ClientDetailsRadioRow(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onSelect() }
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect
        )

        Text(
            text = title,
            color = MaterialTheme.colors.onSurface
        )
    }
}

private fun appointmentDateTimeMillis(appointment: Appointment): Long {
    val date = parseAppointmentDate(appointment.dateString) ?: return Long.MIN_VALUE
    val time = parseAppointmentTime(appointment.time) ?: LocalTime(0, 0)

    return LocalDateTime(
        year = date.year,
        month = date.month,
        dayOfMonth = date.dayOfMonth,
        hour = time.hour,
        minute = time.minute,
        second = 0,
        nanosecond = 0
    ).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
}

private fun parseAppointmentDate(value: String): LocalDate? {
    return runCatching {
        LocalDate.parse(value.trim())
    }.getOrNull()
}

private fun parseAppointmentTime(value: String): LocalTime? {
    val raw = value.trim()
    if (raw.isBlank()) return null

    val parts = raw.split(":")
    if (parts.size < 2) return null

    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null

    if (hour !in 0..23 || minute !in 0..59) return null

    return LocalTime(hour, minute)
}