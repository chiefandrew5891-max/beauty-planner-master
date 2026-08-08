package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import com.andrey.beautyplanner.Locales
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Appointment
import kotlinx.datetime.LocalDate

private fun parseHmToMinutes(hm: String): Int {
    val parts = hm.trim().split(":")
    if (parts.size != 2) return 0
    val h = parts[0].toIntOrNull() ?: 0
    val m = parts[1].toIntOrNull() ?: 0
    return h * 60 + m
}

private fun minutesToHm(minutes: Int): String {
    val m = ((minutes % (24 * 60)) + (24 * 60)) % (24 * 60)
    val hPart = (m / 60).toString().padStart(2, '0')
    val mPart = (m % 60).toString().padStart(2, '0')
    return "$hPart:$mPart"
}

private fun apptDurationMinutes(appt: Appointment): Int {
    return if (appt.durationMinutes > 0) appt.durationMinutes else appt.durationHours.coerceAtLeast(1) * 60
}

private fun endTime(appt: Appointment): String {
    val startMin = parseHmToMinutes(appt.time)
    val endMin = startMin + apptDurationMinutes(appt)
    return minutesToHm(endMin)
}

fun getUpcomingAppointments(
    appointments: List<Appointment>,
    today: LocalDate,
    nowTime: String
): List<Appointment> {
    val nowMin = parseHmToMinutes(nowTime)

    return appointments
        .filter { appt ->
            val apptDate = runCatching { LocalDate.parse(appt.dateString) }.getOrNull()
                ?: return@filter false

            when {
                apptDate > today -> true
                apptDate < today -> false
                else -> {
                    val apptEndMin = parseHmToMinutes(endTime(appt))
                    apptEndMin > nowMin
                }
            }
        }
        .sortedWith(
            compareBy<Appointment>(
                { it.dateString },
                { parseHmToMinutes(it.time) }
            )
        )
}

fun getUpcomingAppointmentsCount(
    appointments: List<Appointment>,
    today: LocalDate,
    nowTime: String
): Int {
    return getUpcomingAppointments(
        appointments = appointments,
        today = today,
        nowTime = nowTime
    ).size
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UpcomingAppointmentItem(
    appt: Appointment,
    status: com.andrey.beautyplanner.utils.LiveStatusKey,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val end = endTime(appt)

    // Используем общий AppointmentCard, но он сохраняет старую вёрстку Upcoming
    // (showDateInCard = true)
    AppointmentCard(
        appt = appt,
        status = status,
        showDateInCard = true,
        startHm = appt.time,
        endHm = end,
        onClick = onClick,
        onLongClick = onLongClick
    )
}

@Composable
fun MonthCalendarGrid(
    monthDate: LocalDate,
    today: LocalDate,
    selectedDate: LocalDate,
    appointments: List<Appointment> = emptyList(),
    onDateClick: (LocalDate) -> Unit
) {
    val isLeap = monthDate.year % 4 == 0 && (monthDate.year % 100 != 0 || monthDate.year % 400 == 0)
    val daysInMonth = when (monthDate.monthNumber) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeap) 29 else 28
        else -> 30
    }

    val firstDayOfMonth = LocalDate(monthDate.year, monthDate.month, 1)
    val dayOfWeekOffset = firstDayOfMonth.dayOfWeek.ordinal
    val days = (1..daysInMonth).toList()
    val fontScale = AppSettings.getFontScale()

    val appointmentDates = remember(appointments) {
        appointments
            .filterNot { it.isDeleted }
            .mapNotNull { runCatching { LocalDate.parse(it.dateString) }.getOrNull() }
            .toSet()
    }

    val totalCells = dayOfWeekOffset + daysInMonth
    val rows = ((totalCells + 6) / 7).coerceAtLeast(5)

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        val compactCalendarWidth = 560.dp
        val horizontalOuterPadding = 24.dp
        val effectiveWidth = if (maxWidth > compactCalendarWidth) {
            compactCalendarWidth
        } else {
            maxWidth - horizontalOuterPadding * 2
        }

        val cellSize = effectiveWidth / 7
        val weekdayHeight = 24.dp
        val gridBottomPadding = 6.dp
        val gridHeight = cellSize * rows + gridBottomPadding

        Column(
            modifier = Modifier
                .width(effectiveWidth)
                .padding(bottom = 4.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                val weekdays = listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun")
                weekdays.forEach { day ->
                    Box(
                        modifier = Modifier
                            .width(cellSize)
                            .height(weekdayHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Locales.t(day),
                            fontSize = (13 * fontScale).sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.height(gridHeight),
                userScrollEnabled = false
            ) {
                items(dayOfWeekOffset) {
                    Spacer(modifier = Modifier.size(cellSize))
                }

                items(days) { day ->
                    val date = LocalDate(monthDate.year, monthDate.month, day)
                    val isSelected = date == selectedDate
                    val isToday = date == today
                    val isPastDay = date < today
                    val hasAppointments = date in appointmentDates

                    val textColor = when {
                        isSelected -> Color.White
                        isToday -> MaterialTheme.colors.primary
                        isPastDay -> MaterialTheme.colors.onSurface.copy(alpha = 0.38f)
                        else -> MaterialTheme.colors.onBackground
                    }

                    val backgroundColor = when {
                        isSelected -> MaterialTheme.colors.primary
                        isToday -> MaterialTheme.colors.primary.copy(alpha = 0.10f)
                        else -> Color.Transparent
                    }

                    Box(
                        modifier = Modifier
                            .size(cellSize)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(backgroundColor)
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = LocalIndication.current,
                                onClick = { onDateClick(date) },
                                onLongClick = { onDateClick(date) }
                            )
                    ) {
                        Text(
                            text = day.toString(),
                            fontSize = (18 * fontScale).sp,
                            fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal,
                            color = textColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center)
                        )

                        if (isPastDay && hasAppointments && !isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 8.dp)
                                    .width(16.dp)
                                    .height(2.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colors.primary.copy(alpha = 0.90f))
                            )
                        }
                    }
                }
            }
        }
    }
}