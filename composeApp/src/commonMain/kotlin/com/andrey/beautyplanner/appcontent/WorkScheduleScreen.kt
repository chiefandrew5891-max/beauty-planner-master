package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.DateRangeBlockedInterval
import com.andrey.beautyplanner.Locales
import com.andrey.beautyplanner.WeeklyBlockedInterval
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(
    androidx.compose.material.ExperimentalMaterialApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)
private enum class WorkScheduleMode {
    WEEKLY,
    RANGE
}

@Composable
fun WorkScheduleScreen() {
    val fontScale = AppSettings.getFontScale()

    var deletingItem by remember { mutableStateOf<WeeklyBlockedInterval?>(null) }
    var deletingRangeItem by remember { mutableStateOf<DateRangeBlockedInterval?>(null) }

    var selectedDays by remember { mutableStateOf(setOf<Int>()) }
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("09:00") }

    var mode by remember { mutableStateOf(WorkScheduleMode.WEEKLY) }
    var showFromDatePicker by remember { mutableStateOf(false) }
    var showToDatePicker by remember { mutableStateOf(false) }

    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    var dateFrom by remember { mutableStateOf(today) }
    var dateTo by remember { mutableStateOf(today) }

    val hourOptions = remember { (0..23).map { "${it.toString().padStart(2, '0')}:00" } }
    val endOptions = remember(startTime) {
        val startHour = startTime.substringBefore(":").toIntOrNull() ?: 0
        ((startHour + 1)..24).map {
            if (it == 24) "24:00" else "${it.toString().padStart(2, '0')}:00"
        }
    }

    val intervals = AppSettings
        .getActiveWeeklyBlockedIntervals()
        .sortedWith(
            compareBy<WeeklyBlockedInterval>({ it.dayOfWeek }, { it.startTime }, { it.endTime })
        )

    val todayDate = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date

    val rangeIntervals = AppSettings
        .getActiveDateRangeBlockedIntervals()
        .filter { item ->
            runCatching { LocalDate.parse(item.dateTo) }
                .getOrNull()
                ?.let { it >= todayDate }
                ?: false
        }
        .sortedWith(compareBy({ it.dateFrom }, { it.startTime }, { it.endTime }))

    LaunchedEffect(Unit) {
        AppSettings.getActiveDateRangeBlockedIntervals().forEach { item ->
            val endDate = runCatching {
                LocalDate.parse(item.dateTo)
            }.getOrNull()

            if (endDate != null && endDate < todayDate) {
                AppSettings.removeDateRangeBlockedInterval(item.id)
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
                text = Locales.t("work_schedule"),
                fontSize = (22 * fontScale).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onBackground
            )

            Text(
                text = Locales.t("work_schedule_hint"),
                fontSize = (14 * fontScale).sp,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f)
            )

            DaySelectorRow(
                selectedDays = selectedDays,
                onToggle = { day ->
                    selectedDays =
                        if (day in selectedDays) selectedDays - day
                        else selectedDays + day
                }
            )

            OutlinedButton(
                onClick = {
                    mode =
                        if (mode == WorkScheduleMode.RANGE) {
                            WorkScheduleMode.WEEKLY
                        } else {
                            WorkScheduleMode.RANGE
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = if (mode == WorkScheduleMode.RANGE) {
                        MaterialTheme.colors.primary.copy(alpha = 0.08f)
                    } else {
                        Color.Transparent
                    }
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (mode == WorkScheduleMode.RANGE) {
                        MaterialTheme.colors.primary
                    } else {
                        MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
                    }
                )
            ) {
                Text(
                    text = Locales.t("work_schedule_long_period"),
                    color = MaterialTheme.colors.primary
                )
            }

            if (mode == WorkScheduleMode.RANGE) {
                Text(
                    text = Locales.t("stats_custom_range"),
                    fontSize = (14 * fontScale).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colors.onBackground
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DateField(
                        label = Locales.t("stats_date_from"),
                        value = dateFrom.toString(),
                        modifier = Modifier.weight(1f),
                        onClick = { showFromDatePicker = true }
                    )

                    DateField(
                        label = Locales.t("stats_date_to"),
                        value = dateTo.toString(),
                        modifier = Modifier.weight(1f),
                        onClick = { showToDatePicker = true }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TimeDropdown(
                    label = Locales.t("work_schedule_from"),
                    value = startTime,
                    options = hourOptions,
                    modifier = Modifier.weight(1f),
                    onSelected = { chosen ->
                        startTime = chosen
                        val startHour = chosen.substringBefore(":").toIntOrNull() ?: 0
                        val currentEndHour = endTime.substringBefore(":").toIntOrNull() ?: 0
                        if (currentEndHour <= startHour) {
                            endTime =
                                if (startHour + 1 >= 24) "24:00"
                                else "${(startHour + 1).toString().padStart(2, '0')}:00"
                        }
                    }
                )

                TimeDropdown(
                    label = Locales.t("work_schedule_to"),
                    value = endTime,
                    options = endOptions,
                    modifier = Modifier.weight(1f),
                    onSelected = { chosen ->
                        endTime = chosen
                    }
                )
            }

            PrimaryActionButton(
                text = Locales.t("work_schedule_add_interval"),
                onClick = {
                    val nowMillis = Clock.System.now().toEpochMilliseconds()

                    if (mode == WorkScheduleMode.RANGE) {
                        val safeFrom = if (dateFrom <= dateTo) dateFrom else dateTo
                        val safeTo = if (dateTo >= dateFrom) dateTo else dateFrom

                        AppSettings.upsertDateRangeBlockedInterval(
                            DateRangeBlockedInterval(
                                id = "range_${safeFrom}_${safeTo}_${startTime}_${endTime}_$nowMillis",
                                dateFrom = safeFrom.toString(),
                                dateTo = safeTo.toString(),
                                startTime = startTime,
                                endTime = endTime,
                                isActive = true
                            )
                        )
                    } else {
                        selectedDays.forEach { day ->
                            AppSettings.upsertWeeklyBlockedInterval(
                                WeeklyBlockedInterval(
                                    id = "blocked_${day}_${startTime}_${endTime}_$nowMillis",
                                    dayOfWeek = day,
                                    startTime = startTime,
                                    endTime = endTime,
                                    isActive = true
                                )
                            )
                        }
                        selectedDays = emptySet()
                    }

                    startTime = "08:00"
                    endTime = "09:00"
                },
                enabled = if (mode == WorkScheduleMode.RANGE) {
                    true
                } else {
                    selectedDays.isNotEmpty()
                }
            )

            if (intervals.isEmpty() && rangeIntervals.isEmpty()) {
                Text(
                    text = Locales.t("work_schedule_empty"),
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    intervals.forEach { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            elevation = 2.dp,
                            backgroundColor = MaterialTheme.colors.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${dayLabel(item.dayOfWeek)} · ${item.startTime}–${item.endTime}",
                                        color = MaterialTheme.colors.onSurface,
                                        fontSize = (16 * fontScale).sp
                                    )
                                }

                                IconButton(onClick = { deletingItem = item }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = Locales.t("delete_btn")
                                    )
                                }
                            }
                        }
                    }

                    rangeIntervals.forEach { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            elevation = 2.dp,
                            backgroundColor = MaterialTheme.colors.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${item.dateFrom} — ${item.dateTo} · ${item.startTime}–${item.endTime}",
                                        color = MaterialTheme.colors.onSurface,
                                        fontSize = (16 * fontScale).sp
                                    )
                                }

                                IconButton(onClick = { deletingRangeItem = item }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = Locales.t("delete_btn")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showFromDatePicker) {
        StatsDatePickerDialog(
            title = Locales.t("stats_pick_start_date"),
            initialSelectedDate = dateFrom,
            initialMonthDate = dateFrom,
            onDismiss = { showFromDatePicker = false },
            onConfirm = { picked ->
                dateFrom = picked
                if (picked > dateTo) {
                    dateTo = picked
                }
                showFromDatePicker = false
                mode = WorkScheduleMode.RANGE
            }
        )
    }

    if (showToDatePicker) {
        StatsDatePickerDialog(
            title = Locales.t("stats_pick_end_date"),
            initialSelectedDate = dateTo,
            initialMonthDate = dateTo,
            onDismiss = { showToDatePicker = false },
            onConfirm = { picked ->
                dateTo = picked
                if (picked < dateFrom) {
                    dateFrom = picked
                }
                showToDatePicker = false
                mode = WorkScheduleMode.RANGE
            }
        )
    }

    deletingItem?.let { item ->
        AlertDialog(
            onDismissRequest = { deletingItem = null },
            title = { Text(Locales.t("delete_title")) },
            text = { Text(Locales.t("work_schedule_delete_confirm")) },
            confirmButton = {
                Button(
                    onClick = {
                        AppSettings.removeWeeklyBlockedInterval(item.id)
                        deletingItem = null
                    }
                ) {
                    Text(Locales.t("delete_btn"))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingItem = null }) {
                    Text(Locales.t("cancel"))
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    deletingRangeItem?.let { item ->
        AlertDialog(
            onDismissRequest = { deletingRangeItem = null },
            title = { Text(Locales.t("delete_title")) },
            text = { Text(Locales.t("work_schedule_delete_confirm")) },
            confirmButton = {
                Button(
                    onClick = {
                        AppSettings.removeDateRangeBlockedInterval(item.id)
                        deletingRangeItem = null
                    }
                ) {
                    Text(Locales.t("delete_btn"))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingRangeItem = null }) {
                    Text(Locales.t("cancel"))
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun DaySelectorRow(
    selectedDays: Set<Int>,
    onToggle: (Int) -> Unit
) {
    val items = listOf(
        1 to Locales.t("mon"),
        2 to Locales.t("tue"),
        3 to Locales.t("wed"),
        4 to Locales.t("thu"),
        5 to Locales.t("fri"),
        6 to Locales.t("sat"),
        7 to Locales.t("sun")
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { (day, label) ->
            val selected = day in selectedDays
            Box(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = if (selected) {
                            MaterialTheme.colors.primary
                        } else {
                            MaterialTheme.colors.onSurface.copy(alpha = 0.25f)
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(
                        color = if (selected) {
                            MaterialTheme.colors.primary.copy(alpha = 0.15f)
                        } else {
                            Color.Transparent
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .noRippleClickable { onToggle(day) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selected) {
                        MaterialTheme.colors.primary
                    } else {
                        MaterialTheme.colors.onSurface
                    }
                )
            }
        }
    }
}

@Composable
private fun TimeDropdown(
    label: String,
    value: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    expanded = true
                },
            label = { Text(label) },
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null
                    )
                }
            },
            shape = RoundedCornerShape(14.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = appFontFamily(),
                fontSize = 16.sp,
                color = MaterialTheme.colors.onSurface
            ),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = MaterialTheme.colors.onSurface,
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.28f),
                focusedLabelColor = MaterialTheme.colors.primary,
                unfocusedLabelColor = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
                cursorColor = MaterialTheme.colors.primary,
                backgroundColor = MaterialTheme.colors.surface,
                placeholderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.50f)
            )
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 320.dp)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onSelected(option)
                    }
                ) {
                    Text(option)
                }
            }
        }
    }
}

@Composable
private fun DateField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    onClick()
                },
            label = { Text(label) },
            trailingIcon = {
                IconButton(onClick = onClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null
                    )
                }
            },
            shape = RoundedCornerShape(14.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = appFontFamily(),
                fontSize = 16.sp,
                color = MaterialTheme.colors.onSurface
            ),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = MaterialTheme.colors.onSurface,
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.28f),
                focusedLabelColor = MaterialTheme.colors.primary,
                unfocusedLabelColor = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
                cursorColor = MaterialTheme.colors.primary,
                backgroundColor = MaterialTheme.colors.surface,
                placeholderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.50f)
            )
        )
    }
}

private fun dayLabel(dayOfWeek: Int): String = when (dayOfWeek) {
    1 -> Locales.t("mon")
    2 -> Locales.t("tue")
    3 -> Locales.t("wed")
    4 -> Locales.t("thu")
    5 -> Locales.t("fri")
    6 -> Locales.t("sat")
    7 -> Locales.t("sun")
    else -> "?"
}

@Composable
private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this.clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
    )
}