package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Appointment
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

    var customFromDate by remember { mutableStateOf(today.minus(30, DateTimeUnit.DAY)) }
    var customToDate by remember { mutableStateOf(today) }

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

    val filtered = remember(appointments, fromDate, toDateInclusive) {
        appointments
            .asSequence()
            .mapNotNull { a ->
                val d = runCatching { LocalDate.parse(a.dateString) }.getOrNull() ?: return@mapNotNull null
                d to a
            }
            .filter { (d, _) -> d >= fromDate && d <= toDateInclusive }
            .map { it.second }
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

    data class ServiceStat(val service: String, val count: Int, val revenue: Double)

    val byService = remember(filtered) {
        filtered
            .groupBy { a ->
                val s = a.serviceName
                if (s.startsWith("service_")) Locales.t(s) else s
            }
            .map { (service, list) ->
                val count = list.size
                val serviceRevenue = list.sumOf { it.price.trim().replace(",", ".").toDoubleOrNull() ?: 0.0 }
                ServiceStat(service = service, count = count, revenue = serviceRevenue)
            }
            .sortedWith(compareByDescending<ServiceStat> { it.revenue }.thenByDescending { it.count })
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

        Text(
            text = "${Locales.t("stats_range")}: $fromDate — $toDateInclusive",
            fontSize = (13 * fontScale).sp,
            color = hintText
        )

        Divider()

        StatRow(Locales.t("stats_revenue"), formatMoneyEur(revenue), primaryText)
        StatRow(Locales.t("stats_count"), totalCount.toString(), primaryText)
        StatRow(Locales.t("stats_hours"), Locales.hoursCount(totalHours), primaryText)

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
private fun PeriodChip(text: String, selected: Boolean, onClick: () -> Unit) {
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
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = FontWeight.Medium, color = primaryText)
        Text(value, fontWeight = FontWeight.SemiBold, color = primaryText)
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
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(service, fontWeight = FontWeight.SemiBold, color = primaryText)
            Text(formatMoneyEur(revenue), color = primaryText)
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
    val s = if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
    return "$s €"
}