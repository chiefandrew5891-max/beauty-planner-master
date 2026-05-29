package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Appointment
import com.andrey.beautyplanner.ClientSuggestion
import com.andrey.beautyplanner.ClientSuggestions
import com.andrey.beautyplanner.Locales
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.math.roundToInt

private enum class StatsPeriod {
    DAY, WEEK, MONTH, YEAR, CUSTOM
}

@Composable
fun StatsPage(
    appointments: List<Appointment>,
    today: LocalDate
) {
    val fontScale = AppSettings.getFontScale()

    val primaryText = UiColors.primaryText()
    val secondaryText = UiColors.secondaryText()
    val hintText = UiColors.hintText()

    var period by remember { mutableStateOf(StatsPeriod.MONTH) }

    var showFromDatePicker by remember { mutableStateOf(false) }
    var showToDatePicker by remember { mutableStateOf(false) }
    var showClientPickerDialog by remember { mutableStateOf(false) }

    var customFromDate by remember { mutableStateOf(today.minus(30, DateTimeUnit.DAY)) }
    var customToDate by remember { mutableStateOf(today) }

    var clientQuery by remember { mutableStateOf("") }
    var selectedClient by remember { mutableStateOf<ClientSuggestion?>(null) }

    val allClients = remember(appointments) {
        ClientSuggestions.fromAppointments(appointments, limit = 1000)
    }

    val (fromDate, toDateInclusive) = remember(today, period, customFromDate, customToDate) {
        when (period) {
            StatsPeriod.DAY -> today to today
            StatsPeriod.WEEK -> today.minus(6, DateTimeUnit.DAY) to today
            StatsPeriod.MONTH -> today.minus(30, DateTimeUnit.DAY) to today
            StatsPeriod.YEAR -> today.minus(365, DateTimeUnit.DAY) to today
            StatsPeriod.CUSTOM -> {
                val safeFrom = if (customFromDate <= customToDate) customFromDate else customToDate
                val safeTo = if (customToDate >= customFromDate) customToDate else customFromDate
                safeFrom to safeTo
            }
        }
    }

    val filtered = remember(
        appointments,
        fromDate,
        toDateInclusive,
        clientQuery,
        selectedClient
    ) {
        val clientFilter = selectedClient?.displayName?.trim()?.lowercase()
            ?: clientQuery.trim().lowercase().takeIf { it.isNotBlank() }

        appointments
            .asSequence()
            .mapNotNull { a ->
                val d = runCatching { LocalDate.parse(a.dateString) }.getOrNull()
                    ?: return@mapNotNull null
                d to a
            }
            .filter { (d, _) -> d >= fromDate && d <= toDateInclusive }
            .map { it.second }
            .filter { appt ->
                if (clientFilter.isNullOrBlank()) {
                    true
                } else {
                    appt.clientName.trim().lowercase().contains(clientFilter)
                }
            }
            .toList()
    }

    val totalCount = filtered.size

    val totalHours = filtered.sumOf { a ->
        if (a.durationMinutes > 0) a.durationMinutes / 60.0
        else a.durationHours.toDouble()
    }.roundToInt()

    val revenue = filtered.sumOf { a ->
        a.price.trim().replace(",", ".").toDoubleOrNull() ?: 0.0
    }

    data class ServiceStat(
        val service: String,
        val count: Int,
        val revenue: Double
    )

    val byService = remember(filtered) {
        filtered
            .groupBy { a ->
                val s = a.serviceName
                if (s.startsWith("service_")) Locales.t(s) else s
            }
            .map { (service, list) ->
                val count = list.size
                val serviceRevenue =
                    list.sumOf { it.price.trim().replace(",", ".").toDoubleOrNull() ?: 0.0 }

                ServiceStat(
                    service = service,
                    count = count,
                    revenue = serviceRevenue
                )
            }
            .sortedWith(
                compareByDescending<ServiceStat> { it.revenue }
                    .thenByDescending { it.count }
            )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = Locales.t("nav_stats"),
            fontSize = (22 * fontScale).sp,
            fontWeight = FontWeight.Bold,
            color = primaryText
        )

        Text(
            text = Locales.t("stats_filters"),
            fontSize = (16 * fontScale).sp,
            fontWeight = FontWeight.SemiBold,
            color = primaryText
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PeriodChip(Locales.t("stats_period_day"), period == StatsPeriod.DAY) {
                period = StatsPeriod.DAY
            }
            PeriodChip(Locales.t("stats_period_week"), period == StatsPeriod.WEEK) {
                period = StatsPeriod.WEEK
            }
            PeriodChip(Locales.t("stats_period_month"), period == StatsPeriod.MONTH) {
                period = StatsPeriod.MONTH
            }
            PeriodChip(Locales.t("stats_period_year"), period == StatsPeriod.YEAR) {
                period = StatsPeriod.YEAR
            }
            PeriodChip(Locales.t("stats_period_custom"), period == StatsPeriod.CUSTOM) {
                period = StatsPeriod.CUSTOM
            }
        }

        if (period == StatsPeriod.CUSTOM) {
            Text(
                text = Locales.t("stats_custom_range"),
                fontSize = (14 * fontScale).sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryText
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showFromDatePicker = true },
                    modifier = Modifier.weight(1f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${Locales.t("stats_date_from")}: $customFromDate",
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = { showToDatePicker = true },
                    modifier = Modifier.weight(1f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${Locales.t("stats_date_to")}: $customToDate",
                        maxLines = 1
                    )
                }
            }
        }

        OutlinedButton(
            onClick = { showClientPickerDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        ) {
            Text(Locales.t("stats_client_filter_button"))
        }

        if (clientQuery.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 0.dp,
                backgroundColor = MaterialTheme.colors.surface,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = Locales.t("stats_selected_client"),
                        fontSize = (12 * fontScale).sp,
                        color = secondaryText
                    )

                    Text(
                        text = clientQuery,
                        fontSize = (15 * fontScale).sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryText
                    )
                }
            }

            TextButton(
                onClick = {
                    clientQuery = ""
                    selectedClient = null
                },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(Locales.t("stats_clear_client_filter"))
            }
        }

        Text(
            text = "${Locales.t("stats_range")}: $fromDate — $toDateInclusive",
            fontSize = (13 * fontScale).sp,
            color = hintText
        )

        Divider()

        StatRow(
            label = Locales.t("stats_revenue"),
            value = formatMoneyEur(revenue),
            primaryText = primaryText
        )

        StatRow(
            label = Locales.t("stats_count"),
            value = totalCount.toString(),
            primaryText = primaryText
        )

        StatRow(
            label = Locales.t("stats_hours"),
            value = Locales.hoursCount(totalHours),
            primaryText = primaryText
        )

        Divider()

        Text(
            text = Locales.t("stats_top_services"),
            fontSize = (16 * fontScale).sp,
            fontWeight = FontWeight.SemiBold,
            color = primaryText
        )

        if (byService.isEmpty()) {
            Text(
                text = Locales.t("stats_empty"),
                color = secondaryText,
                fontSize = (14 * fontScale).sp
            )
        } else {
            byService.forEach { s ->
                ServiceRow(
                    service = s.service,
                    count = s.count,
                    revenue = s.revenue,
                    fontScale = fontScale,
                    primaryText = primaryText,
                    secondaryText = secondaryText
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showClientPickerDialog) {
        ClientPickerDialog(
            clients = allClients,
            selectedClientName = selectedClient?.displayName ?: clientQuery.takeIf { it.isNotBlank() },
            onDismiss = { showClientPickerDialog = false },
            onClear = {
                clientQuery = ""
                selectedClient = null
                showClientPickerDialog = false
            },
            onSelect = { client ->
                selectedClient = client
                clientQuery = client.displayName
                showClientPickerDialog = false
            }
        )
    }

    if (showFromDatePicker) {
        StatsDatePickerDialog(
            title = Locales.t("stats_pick_start_date"),
            initialSelectedDate = customFromDate,
            initialMonthDate = customFromDate,
            onDismiss = { showFromDatePicker = false },
            onConfirm = { picked ->
                customFromDate = picked
                showFromDatePicker = false
            }
        )
    }

    if (showToDatePicker) {
        StatsDatePickerDialog(
            title = Locales.t("stats_pick_end_date"),
            initialSelectedDate = customToDate,
            initialMonthDate = customToDate,
            onDismiss = { showToDatePicker = false },
            onConfirm = { picked ->
                customToDate = picked
                showToDatePicker = false
            }
        )
    }
}

@Composable
private fun PeriodChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val modifier = Modifier.height(38.dp)

    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.elevation(defaultElevation = 0.dp)
        ) {
            Text(text, maxLines = 1)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        ) {
            Text(text, maxLines = 1)
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    primaryText: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Medium,
            color = primaryText
        )
        Text(
            text = value,
            fontWeight = FontWeight.SemiBold,
            color = primaryText
        )
    }
}

@Composable
private fun ServiceRow(
    service: String,
    count: Int,
    revenue: Double,
    fontScale: Float,
    primaryText: Color,
    secondaryText: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = service,
                fontWeight = FontWeight.SemiBold,
                color = primaryText
            )
            Text(
                text = formatMoneyEur(revenue),
                color = primaryText
            )
        }

        Text(
            text = "${Locales.t("stats_procedures_done")}: $count",
            color = secondaryText,
            fontSize = (13 * fontScale).sp
        )
    }
}

private fun formatMoneyEur(v: Double): String {
    val rounded = (v * 100).roundToInt() / 100.0
    val s = if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        rounded.toString()
    }
    return "$s €"
}