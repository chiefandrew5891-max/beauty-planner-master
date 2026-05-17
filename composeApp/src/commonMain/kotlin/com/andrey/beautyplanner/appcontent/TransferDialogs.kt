package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Locales
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

@Composable
fun TransferPickDialog(
    initialSelectedDate: LocalDate,
    initialMonthDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, String) -> Unit
) {
    val fontScale = AppSettings.getFontScale()
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }

    var monthDate by remember { mutableStateOf(LocalDate(initialMonthDate.year, initialMonthDate.month, 1)) }
    var selectedDate by remember { mutableStateOf(initialSelectedDate) }
    var selectedTime by remember { mutableStateOf("08:00") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Locales.t("transfer_title")) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${monthDate.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${monthDate.year}",
                        fontWeight = FontWeight.Bold,
                        fontSize = (16 * fontScale).sp
                    )

                    Row {
                        IconButton(onClick = { monthDate = monthDate.minus(1, DateTimeUnit.MONTH) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null)
                        }
                        IconButton(onClick = { monthDate = monthDate.plus(1, DateTimeUnit.MONTH) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                MonthCalendarGrid(
                    monthDate = monthDate,
                    today = today,
                    selectedDate = selectedDate,
                    onDateClick = { selectedDate = it }
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = Locales.t("transfer_choose_time"),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (14 * fontScale).sp,
                    color = Color.Gray
                )

                Spacer(Modifier.height(8.dp))

                TimeSlotGrid(
                    selectedTime = selectedTime,
                    onTimeSelected = { selectedTime = it }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedDate, selectedTime) }) {
                Text(Locales.t("transfer_confirm"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Locales.t("cancel")) }
        },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    )
}

/**
 * ВАЖНО:
 * Заголовок делаем без Locales-ключа "reschedule_title_for", чтобы у тебя НЕ показывало ключ.
 * (Locales.t("...") если ключа нет — возвращает сам ключ.)
 *
 * Поэтому: "Переназначить запись для <Имя>" собираем прямо строкой + жирное имя.
 */
@Composable
fun RescheduleClientBDialog(
    clientName: String,
    initialSelectedDate: LocalDate,
    initialMonthDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, String) -> Unit
) {
    val fontScale = AppSettings.getFontScale()
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }

    var monthDate by remember { mutableStateOf(LocalDate(initialMonthDate.year, initialMonthDate.month, 1)) }
    var selectedDate by remember { mutableStateOf(initialSelectedDate) }
    var selectedTime by remember { mutableStateOf("08:00") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                buildAnnotatedString {
                    append("Переназначить запись для ")
                    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                        append(clientName)
                    }
                }
            )
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${monthDate.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${monthDate.year}",
                        fontWeight = FontWeight.Bold,
                        fontSize = (16 * fontScale).sp
                    )

                    Row {
                        IconButton(onClick = { monthDate = monthDate.minus(1, DateTimeUnit.MONTH) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null)
                        }
                        IconButton(onClick = { monthDate = monthDate.plus(1, DateTimeUnit.MONTH) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                MonthCalendarGrid(
                    monthDate = monthDate,
                    today = today,
                    selectedDate = selectedDate,
                    onDateClick = { selectedDate = it }
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = Locales.t("reschedule_choose_time"),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (14 * fontScale).sp,
                    color = Color.Gray
                )

                Spacer(Modifier.height(8.dp))

                TimeSlotGrid(
                    selectedTime = selectedTime,
                    onTimeSelected = { selectedTime = it }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedDate, selectedTime) }) {
                Text(Locales.t("reschedule_confirm"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Locales.t("cancel")) }
        },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    )
}

@Composable
private fun TimeSlotGrid(
    selectedTime: String,
    onTimeSelected: (String) -> Unit
) {
    val timeSlots = remember { (8..20).map { "${if (it < 10) "0$it" else it}:00" } }
    val interactionSource = remember { MutableInteractionSource() }
    val fontScale = AppSettings.getFontScale()

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp, max = 220.dp)
    ) {
        items(timeSlots.size) { idx ->
            val t = timeSlots[idx]
            val isSelected = t == selectedTime

            Card(
                elevation = 0.dp,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                backgroundColor = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.15f) else Color.Transparent,
                border = BorderStroke(
                    1.dp,
                    if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.6f) else Color.LightGray.copy(alpha = 0.25f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = { onTimeSelected(t) }
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = t,
                        fontSize = (16 * fontScale).sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
                    )
                }
            }
        }
    }
}