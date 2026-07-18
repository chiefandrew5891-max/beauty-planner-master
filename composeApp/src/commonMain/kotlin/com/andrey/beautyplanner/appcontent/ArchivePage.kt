package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Appointment
import com.andrey.beautyplanner.AppointmentPaymentStatus
import com.andrey.beautyplanner.ClientSuggestion
import com.andrey.beautyplanner.ClientSuggestions
import com.andrey.beautyplanner.Locales
import com.andrey.beautyplanner.effectivePaymentStatus
import com.andrey.beautyplanner.getCurrentTimeHm
import com.andrey.beautyplanner.utils.LiveStatusKey
import com.andrey.beautyplanner.utils.getLiveStatus
import com.andrey.beautyplanner.utils.parseHmToMinutes
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.math.roundToInt

private enum class ArchiveSortMode {
    NEWEST,
    OLDEST
}

private enum class ArchivePaymentFilter {
    ALL,
    PAID,
    PAYMENT_LATER,
    PAID_AFTER_DELAY
}

@Composable
fun ArchivePage(
    appointments: List<Appointment>,
    premiumEnabled: Boolean,
    onOpenPremium: () -> Unit
) {
    val fontScale = AppSettings.getFontScale()
    val onBg = MaterialTheme.colors.onBackground
    val onSurface = MaterialTheme.colors.onSurface
    val scrollState = rememberScrollState()

    var sortMode by remember { mutableStateOf(ArchiveSortMode.NEWEST) }
    var paymentFilter by remember { mutableStateOf(ArchivePaymentFilter.ALL) }

    var periodFrom by remember { mutableStateOf<LocalDate?>(null) }
    var periodTo by remember { mutableStateOf<LocalDate?>(null) }

    var showClientPicker by remember { mutableStateOf(false) }
    var showPeriodExpanded by remember { mutableStateOf(false) }
    var showFromDatePicker by remember { mutableStateOf(false) }
    var showToDatePicker by remember { mutableStateOf(false) }

    var serviceFilter by remember { mutableStateOf("") }
    var selectedClient by remember { mutableStateOf<ClientSuggestion?>(null) }

    var viewingAppointment by remember { mutableStateOf<Appointment?>(null) }

    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val nowMinutes = runCatching { parseHmToMinutes(getCurrentTimeHm()) }.getOrNull() ?: 0

    val allClients = remember(appointments) {
        ClientSuggestions.fromAppointments(
            appointments = appointments.filterNot { it.isDeleted },
            limit = 1000
        )
    }

    val serviceOptions = remember(appointments) {
        buildList {
            add("")
            appointments
                .filterNot { it.isDeleted }
                .forEach { appointment ->
                    val title = serviceDisplay(appointment)
                    if (title.isNotBlank() && !contains(title)) {
                        add(title)
                    }
                }
        }.sortedBy { it.lowercase() }
    }

    val filtered = remember(
        appointments,
        periodFrom,
        periodTo,
        selectedClient,
        serviceFilter,
        sortMode,
        paymentFilter,
        today,
        nowMinutes
    ) {
        appointments
            .asSequence()
            .filter { !it.isDeleted }
            .filter { appointment ->
                runCatching {
                    getLiveStatus(
                        appt = appointment,
                        nowDate = today,
                        nowMinutes = nowMinutes
                    ) == LiveStatusKey.DONE
                }.getOrDefault(false)
            }
            .filter { appointment ->
                val appointmentDate = runCatching {
                    LocalDate.parse(appointment.dateString)
                }.getOrNull() ?: return@filter true

                val from = periodFrom
                val to = periodTo

                val fromOk = from?.let { appointmentDate >= it } ?: true
                val toOk = to?.let { appointmentDate <= it } ?: true
                fromOk && toOk
            }
            .filter { appointment ->
                val selected = selectedClient?.displayName?.trim().orEmpty()
                if (selected.isBlank()) {
                    true
                } else {
                    appointment.clientName.contains(selected, ignoreCase = true)
                }
            }
            .filter { appointment ->
                serviceFilter.isBlank() ||
                        serviceDisplay(appointment).equals(serviceFilter, ignoreCase = true) ||
                        appointment.serviceName.equals(serviceFilter, ignoreCase = true)
            }
            .filter { appointment ->
                when (paymentFilter) {
                    ArchivePaymentFilter.PAID ->
                        appointment.effectivePaymentStatus() == AppointmentPaymentStatus.PAID

                    ArchivePaymentFilter.PAYMENT_LATER ->
                        appointment.effectivePaymentStatus() == AppointmentPaymentStatus.PAYMENT_LATER

                    ArchivePaymentFilter.PAID_AFTER_DELAY ->
                        appointment.effectivePaymentStatus() == AppointmentPaymentStatus.PAID_AFTER_DELAY

                    ArchivePaymentFilter.ALL -> true
                }
            }
            .sortedWith(
                when (sortMode) {
                    ArchiveSortMode.NEWEST ->
                        compareByDescending<Appointment> { it.dateString }
                            .thenByDescending { it.time }

                    ArchiveSortMode.OLDEST ->
                        compareBy<Appointment> { it.dateString }
                            .thenBy { it.time }
                }
            )
            .toList()
    }

    val totalCount = filtered.size
    val totalRevenue = filtered.sumOf {
        it.price.trim().replace(",", ".").toDoubleOrNull() ?: 0.0
    }
    val totalHours = filtered.sumOf {
        val minutes = if (it.durationMinutes > 0) it.durationMinutes else it.durationHours * 60
        minutes / 60.0
    }

    val selectedSortLabel = when (sortMode) {
        ArchiveSortMode.NEWEST -> Locales.t("archive_sort_newest")
        ArchiveSortMode.OLDEST -> Locales.t("archive_sort_oldest")
    }

    CenteredContentContainer(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = Locales.t("archive_title"),
                fontSize = (22 * fontScale).sp,
                fontWeight = FontWeight.Bold,
                color = onBg
            )

            Text(
                text = Locales.t("archive_hint"),
                fontSize = (14 * fontScale).sp,
                color = onBg.copy(alpha = 0.7f)
            )

            if (!premiumEnabled) {
                Text(
                    text = Locales.t("premium_required_archive"),
                    color = MaterialTheme.colors.error,
                    fontSize = (14 * fontScale).sp,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedButton(
                    onClick = onOpenPremium,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(Locales.t("premium_open_screen_btn"))
                }
            }

            Text(
                text = Locales.t("archive_filters_title"),
                fontSize = (16 * fontScale).sp,
                fontWeight = FontWeight.SemiBold,
                color = if (premiumEnabled) onSurface else onSurface.copy(alpha = 0.55f)
            )

            CompactSelectionDialogField(
                label = Locales.t("archive_sort_label"),
                selected = selectedSortLabel,
                items = listOf(
                    Locales.t("archive_sort_newest"),
                    Locales.t("archive_sort_oldest")
                ),
                onSelect = { value ->
                    sortMode = when (value) {
                        Locales.t("archive_sort_oldest") -> ArchiveSortMode.OLDEST
                        else -> ArchiveSortMode.NEWEST
                    }
                },
                enabled = premiumEnabled
            )

            CompactSelectionDialogField(
                label = Locales.t("archive_service_filter_label"),
                selected = if (serviceFilter.isBlank()) {
                    Locales.t("archive_service_all")
                } else {
                    serviceFilter
                },
                items = serviceOptions.map {
                    if (it.isBlank()) Locales.t("archive_service_all") else it
                },
                onSelect = { value ->
                    serviceFilter = if (value == Locales.t("archive_service_all")) "" else value
                },
                enabled = premiumEnabled
            )

            ArchivePeriodPickerBlock(
                periodFrom = periodFrom,
                periodTo = periodTo,
                expanded = showPeriodExpanded,
                onToggle = { showPeriodExpanded = !showPeriodExpanded },
                onOpenFrom = { showFromDatePicker = true },
                onOpenTo = { showToDatePicker = true },
                onClear = {
                    periodFrom = null
                    periodTo = null
                },
                enabled = premiumEnabled
            )

            ClientFilterButton(
                selectedClientName = selectedClient?.displayName,
                onClick = { showClientPicker = true },
                onClear = { selectedClient = null },
                enabled = premiumEnabled
            )

            PaymentFilterRows(
                paymentFilter = paymentFilter,
                onSelect = { paymentFilter = it },
                enabled = premiumEnabled
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatLine(
                    label = Locales.t("archive_total_records"),
                    value = if (premiumEnabled) totalCount.toString() else "—",
                    fontScale = fontScale
                )
                StatLine(
                    label = Locales.t("archive_total_revenue"),
                    value = if (premiumEnabled) {
                        formatMoney(totalRevenue, AppSettings.selectedCurrency)
                    } else {
                        "—"
                    },
                    fontScale = fontScale
                )
                StatLine(
                    label = Locales.t("archive_total_hours"),
                    value = if (premiumEnabled) formatHours(totalHours) else "—",
                    fontScale = fontScale
                )
            }

            Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f))

            if (premiumEnabled) {
                if (filtered.isEmpty()) {
                    Text(
                        text = Locales.t("archive_no_results"),
                        color = onSurface.copy(alpha = 0.7f),
                        fontSize = (14 * fontScale).sp
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        filtered.forEach { appointment ->
                            ArchiveRowCard(
                                appointment = appointment,
                                onClick = { viewingAppointment = appointment }
                            )
                        }
                    }
                }
            }
        }
    }

    if (premiumEnabled && showClientPicker) {
        ClientPickerDialog(
            clients = allClients,
            selectedClientName = selectedClient?.displayName,
            onDismiss = { showClientPicker = false },
            onClear = {
                selectedClient = null
                showClientPicker = false
            },
            onSelect = { client ->
                selectedClient = client
                showClientPicker = false
            }
        )
    }

    if (premiumEnabled && showFromDatePicker) {
        val initialDate = periodFrom
            ?: periodTo
            ?: defaultArchiveDate(filtered, earliest = true)

        StatsDatePickerDialog(
            title = Locales.t("archive_period_from"),
            initialSelectedDate = initialDate,
            initialMonthDate = initialDate,
            onDismiss = { showFromDatePicker = false },
            onConfirm = { picked ->
                val currentTo = periodTo
                periodFrom = picked
                if (currentTo != null && picked > currentTo) {
                    periodTo = picked
                }
                showFromDatePicker = false
                showPeriodExpanded = true
            }
        )
    }

    if (premiumEnabled && showToDatePicker) {
        val initialDate = periodTo
            ?: periodFrom
            ?: defaultArchiveDate(filtered, earliest = false)

        StatsDatePickerDialog(
            title = Locales.t("archive_period_to"),
            initialSelectedDate = initialDate,
            initialMonthDate = initialDate,
            onDismiss = { showToDatePicker = false },
            onConfirm = { picked ->
                val currentFrom = periodFrom
                periodTo = picked
                if (currentFrom != null && picked < currentFrom) {
                    periodFrom = picked
                }
                showToDatePicker = false
                showPeriodExpanded = true
            }
        )
    }

    if (premiumEnabled) {
        viewingAppointment?.let { appointment ->
            ArchiveAppointmentViewDialog(
                appointment = appointment,
                onDismiss = { viewingAppointment = null }
            )
        }
    }
}

@Composable
private fun CompactSelectionDialogField(
    label: String,
    selected: String,
    items: List<String>,
    onSelect: (String) -> Unit,
    enabled: Boolean
) {
    var showDialog by remember { mutableStateOf(false) }
    val fontScale = AppSettings.getFontScale()
    val onSurface = MaterialTheme.colors.onSurface
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontSize = (14 * fontScale).sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) onSurface.copy(alpha = 0.85f) else onSurface.copy(alpha = 0.45f)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    enabled = enabled
                ) {
                    showDialog = true
                },
            shape = RoundedCornerShape(12.dp),
            elevation = 0.dp,
            border = BorderStroke(1.dp, onSurface.copy(alpha = 0.25f)),
            backgroundColor = MaterialTheme.colors.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selected,
                    fontSize = (15 * fontScale).sp,
                    color = if (enabled) onSurface else onSurface.copy(alpha = 0.45f),
                    maxLines = 1
                )

                Text(
                    text = "▼",
                    fontSize = (12 * fontScale).sp,
                    color = if (enabled) onSurface.copy(alpha = 0.65f) else onSurface.copy(alpha = 0.35f)
                )
            }
        }
    }

    if (enabled && showDialog) {
        SelectionDialog(
            title = label,
            items = items,
            onDismiss = { showDialog = false },
            onSelect = {
                onSelect(it)
                showDialog = false
            }
        )
    }
}

@Composable
private fun ArchivePeriodPickerBlock(
    periodFrom: LocalDate?,
    periodTo: LocalDate?,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenFrom: () -> Unit,
    onOpenTo: () -> Unit,
    onClear: () -> Unit,
    enabled: Boolean
) {
    val fontScale = AppSettings.getFontScale()
    val onSurface = MaterialTheme.colors.onSurface
    val interactionSource = remember { MutableInteractionSource() }
    val hasPeriod = periodFrom != null || periodTo != null

    val selectedText = if (!hasPeriod) {
        Locales.t("archive_period_label")
    } else {
        buildString {
            append(Locales.t("archive_period_label"))
            append(": ")
            append(periodFrom?.toString() ?: "—")
            append(" → ")
            append(periodTo?.toString() ?: "—")
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = Locales.t("archive_period_label"),
                fontSize = (14 * fontScale).sp,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) onSurface.copy(alpha = 0.85f) else onSurface.copy(alpha = 0.45f)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        enabled = enabled
                    ) {
                        onToggle()
                    },
                shape = RoundedCornerShape(12.dp),
                elevation = 0.dp,
                border = BorderStroke(1.dp, onSurface.copy(alpha = 0.25f)),
                backgroundColor = MaterialTheme.colors.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = selectedText,
                        fontSize = (15 * fontScale).sp,
                        color = if (enabled) onSurface else onSurface.copy(alpha = 0.45f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = if (expanded) "▲" else "▼",
                        fontSize = (12 * fontScale).sp,
                        color = if (enabled) onSurface.copy(alpha = 0.65f) else onSurface.copy(alpha = 0.35f)
                    )
                }
            }
        }

        if (expanded) {
            Text(
                text = Locales.t("stats_custom_range"),
                fontSize = (14 * fontScale).sp,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) onSurface else onSurface.copy(alpha = 0.45f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenFrom,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${Locales.t("archive_period_from")}: ${periodFrom ?: "—"}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onOpenTo,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${Locales.t("archive_period_to")}: ${periodTo ?: "—"}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (hasPeriod) {
                TextButton(
                    onClick = onClear,
                    enabled = enabled
                ) {
                    Text(Locales.t("archive_period_clear"))
                }
            }
        }
    }
}

@Composable
private fun PaymentFilterRows(
    paymentFilter: ArchivePaymentFilter,
    onSelect: (ArchivePaymentFilter) -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PaymentFilterChip(
            text = Locales.t("archive_payment_all"),
            selected = paymentFilter == ArchivePaymentFilter.ALL,
            onClick = { onSelect(ArchivePaymentFilter.ALL) },
            enabled = enabled
        )

        PaymentFilterChip(
            text = Locales.t("archive_status_paid"),
            selected = paymentFilter == ArchivePaymentFilter.PAID,
            onClick = { onSelect(ArchivePaymentFilter.PAID) },
            enabled = enabled
        )

        PaymentFilterChip(
            text = Locales.t("archive_status_payment_later"),
            selected = paymentFilter == ArchivePaymentFilter.PAYMENT_LATER,
            onClick = { onSelect(ArchivePaymentFilter.PAYMENT_LATER) },
            enabled = enabled
        )

        PaymentFilterChip(
            text = Locales.t("archive_status_paid_after_delay"),
            selected = paymentFilter == ArchivePaymentFilter.PAID_AFTER_DELAY,
            onClick = { onSelect(ArchivePaymentFilter.PAID_AFTER_DELAY) },
            enabled = enabled
        )
    }
}

@Composable
private fun PaymentFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean
) {
    val modifier = Modifier.height(38.dp)

    if (selected) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier,
            shape = RoundedCornerShape(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 14.dp,
                vertical = 0.dp
            ),
            elevation = ButtonDefaults.elevation(defaultElevation = 0.dp)
        ) {
            Text(text, maxLines = 1)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier,
            shape = RoundedCornerShape(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 14.dp,
                vertical = 0.dp
            )
        ) {
            Text(text, maxLines = 1)
        }
    }
}

@Composable
private fun ClientFilterButton(
    selectedClientName: String?,
    onClick: () -> Unit,
    onClear: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = if (selectedClientName.isNullOrBlank()) {
                    Locales.t("archive_client_filter_label")
                } else {
                    "${Locales.t("archive_client_filter_label")}: $selectedClientName"
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!selectedClientName.isNullOrBlank()) {
            TextButton(
                onClick = onClear,
                enabled = enabled
            ) {
                Text(Locales.t("archive_filter_clear"))
            }
        }
    }
}

@Composable
private fun StatLine(
    label: String,
    value: String,
    fontScale: Float
) {
    Column {
        Text(
            text = label,
            fontSize = (11 * fontScale).sp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f)
        )
        Text(
            text = value,
            fontSize = (14 * fontScale).sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colors.onSurface
        )
    }
}

@Composable
private fun ArchiveRowCard(
    appointment: Appointment,
    onClick: () -> Unit
) {
    val fontScale = AppSettings.getFontScale()
    val paymentLabel = when (appointment.effectivePaymentStatus()) {
        AppointmentPaymentStatus.PAID -> Locales.t("archive_status_paid")
        AppointmentPaymentStatus.PAYMENT_LATER -> Locales.t("archive_status_payment_later")
        AppointmentPaymentStatus.PAID_AFTER_DELAY -> Locales.t("archive_status_paid_after_delay")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = 2.dp,
        backgroundColor = MaterialTheme.colors.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colors.onSurface.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = appointment.clientName,
                fontSize = (16 * fontScale).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${appointment.dateString} • ${appointment.time}",
                fontSize = (13 * fontScale).sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f)
            )

            MetaLineCompact(
                label = Locales.t("archive_col_service"),
                value = serviceDisplay(appointment),
                fontScale = fontScale
            )

            MetaLineCompact(
                label = Locales.t("archive_col_price"),
                value = formatMoney(
                    appointment.price.trim().replace(",", ".").toDoubleOrNull() ?: 0.0,
                    appointment.currency
                ),
                fontScale = fontScale
            )

            MetaLineCompact(
                label = Locales.t("archive_col_payment"),
                value = paymentLabel,
                fontScale = fontScale
            )

            if (appointment.notes.isNotBlank()) {
                MetaLineCompact(
                    label = Locales.t("archive_col_comment"),
                    value = appointment.notes,
                    fontScale = fontScale,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun MetaLineCompact(
    label: String,
    value: String,
    fontScale: Float,
    maxLines: Int = 1
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label: ",
            fontSize = (13 * fontScale).sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colors.primary
        )
        Text(
            text = value,
            fontSize = (13 * fontScale).sp,
            color = MaterialTheme.colors.onSurface,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ArchiveAppointmentViewDialog(
    appointment: Appointment,
    onDismiss: () -> Unit
) {
    val fontScale = AppSettings.getFontScale()

    val paymentLabel = when (appointment.effectivePaymentStatus()) {
        AppointmentPaymentStatus.PAID -> Locales.t("archive_status_paid")
        AppointmentPaymentStatus.PAYMENT_LATER -> Locales.t("archive_status_payment_later")
        AppointmentPaymentStatus.PAID_AFTER_DELAY -> Locales.t("archive_status_paid_after_delay")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = Locales.t("view_appointment_title"),
                fontWeight = FontWeight.Bold,
                fontSize = (18 * fontScale).sp,
                color = MaterialTheme.colors.onSurface
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = appointment.clientName,
                    fontWeight = FontWeight.Bold,
                    fontSize = (22 * fontScale).sp,
                    color = MaterialTheme.colors.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "${appointment.dateString} • ${appointment.time}",
                    fontSize = (15 * fontScale).sp,
                    color = MaterialTheme.colors.primary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "${Locales.t("archive_col_service")}: ${serviceDisplay(appointment)}",
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${Locales.t("archive_col_price")}: ${
                        formatMoney(
                            appointment.price.trim().replace(",", ".").toDoubleOrNull() ?: 0.0,
                            appointment.currency
                        )
                    }",
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${Locales.t("archive_col_payment")}: $paymentLabel",
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.85f)
                )

                if (appointment.phone.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${Locales.t("phone")}: ${appointment.phone}",
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.85f)
                    )
                }

                if (appointment.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${Locales.t("archive_col_comment")}: ${appointment.notes}",
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.85f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Locales.t("close"))
            }
        },
        dismissButton = {},
        shape = RoundedCornerShape(16.dp)
    )
}

private fun serviceDisplay(appointment: Appointment): String {
    return if (appointment.serviceName.startsWith("service_")) {
        Locales.t(appointment.serviceName)
    } else {
        appointment.serviceName
    }
}

private fun formatMoney(value: Double, currencyCode: String): String {
    val rounded = (value * 100.0).roundToInt() / 100.0
    val text = if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        rounded.toString()
    }
    return AppSettings.formatMoneyAmount(text, currencyCode)
}

private fun formatHours(hours: Double): String {
    val rounded = (hours * 10.0).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        rounded.toString()
    }
}

private fun defaultArchiveDate(
    appointments: List<Appointment>,
    earliest: Boolean
): LocalDate {
    val parsedDates = appointments.mapNotNull {
        runCatching { LocalDate.parse(it.dateString) }.getOrNull()
    }

    return when {
        parsedDates.isEmpty() -> LocalDate(2026, 1, 1)
        earliest -> parsedDates.minOrNull() ?: LocalDate(2026, 1, 1)
        else -> parsedDates.maxOrNull() ?: LocalDate(2026, 1, 1)
    }
}