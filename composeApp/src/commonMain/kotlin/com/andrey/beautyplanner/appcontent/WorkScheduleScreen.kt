package com.andrey.beautyplanner.appcontent

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Locales
import com.andrey.beautyplanner.WeeklyBlockedInterval
import kotlinx.datetime.Clock
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.BorderStroke

@OptIn(androidx.compose.material.ExperimentalMaterialApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun WorkScheduleScreen() {
    val fontScale = AppSettings.getFontScale()
    var deletingItem by remember { mutableStateOf<WeeklyBlockedInterval?>(null) }

    var selectedDays by remember { mutableStateOf(setOf<Int>()) }
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("09:00") }

    val hourOptions = remember { (0..23).map { "${it.toString().padStart(2, '0')}:00" } }

    val endOptions = remember(startTime) {
        val startHour = startTime.substringBefore(":").toIntOrNull() ?: 0
        ((startHour + 1)..24).map {
            if (it == 24) "24:00" else "${it.toString().padStart(2, '0')}:00"
        }
    }

    val intervals = AppSettings.getActiveWeeklyBlockedIntervals()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = Locales.t("work_schedule"),
            fontSize = (22 * fontScale).sp,
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
                        endTime = if (startHour + 1 >= 24) "24:00" else "${(startHour + 1).toString().padStart(2, '0')}:00"
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

        Button(
            onClick = {
                selectedDays.forEach { day ->
                    AppSettings.upsertWeeklyBlockedInterval(
                        WeeklyBlockedInterval(
                            id = "blocked_${day}_${startTime}_${endTime}_${Clock.System.now().toEpochMilliseconds()}",
                            dayOfWeek = day,
                            startTime = startTime,
                            endTime = endTime,
                            isActive = true
                        )
                    )
                }
                selectedDays = emptySet()
            },
            enabled = selectedDays.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(Locales.t("work_schedule_add_interval"))
        }

        if (intervals.isEmpty()) {
            Text(
                text = Locales.t("work_schedule_empty"),
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                intervals.forEach { item ->
                    androidx.compose.material.Card(
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
            }
        }
    }

    deletingItem?.let { item ->
        AlertDialog(
            onDismissRequest = { deletingItem = null },
            title = { Text(Locales.t("delete_title")) },
            text = { Text(Locales.t("work_schedule_delete_confirm")) },
            confirmButton = {
                Button(onClick = {
                    AppSettings.removeWeeklyBlockedInterval(item.id)
                    deletingItem = null
                }) {
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
                        color = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(
                        color = if (selected) MaterialTheme.colors.primary.copy(alpha = 0.15f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .noRippleClickable { onToggle(day) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
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
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current
                ) {
                    expanded = true
                },
            shape = RoundedCornerShape(14.dp),
            elevation = 0.dp,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colors.onSurface.copy(alpha = 0.22f)
            ),
            backgroundColor = MaterialTheme.colors.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = value,
                    fontSize = 16.sp,
                    color = MaterialTheme.colors.onSurface
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
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