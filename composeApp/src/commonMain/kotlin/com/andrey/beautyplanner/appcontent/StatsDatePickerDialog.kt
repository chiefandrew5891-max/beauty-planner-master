package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
fun StatsDatePickerDialog(
    title: String,
    initialSelectedDate: LocalDate,
    initialMonthDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    val fontScale = AppSettings.getFontScale()

    var monthDate by remember {
        mutableStateOf(
            LocalDate(
                initialMonthDate.year,
                initialMonthDate.month,
                1
            )
        )
    }

    var selectedDate by remember { mutableStateOf(initialSelectedDate) }
    var showYearPicker by remember { mutableStateOf(false) }

    val currentYear = Clock.System
        .todayIn(TimeZone.currentSystemDefault())
        .year

    val lastAvailableYear = maxOf(
        currentYear,
        monthDate.year
    ) + 50

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontSize = (19 * fontScale).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val fullMonthTitle = localizedMonthTitle(monthDate)
                    val monthName = fullMonthTitle.removeSuffix(" ${monthDate.year}")

                    Text(
                        text = monthName,
                        fontWeight = FontWeight.Bold,
                        fontSize = (16 * fontScale).sp,
                        color = MaterialTheme.colors.onSurface
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    TextButton(
                        onClick = {
                            showYearPicker = true
                        },
                        contentPadding = PaddingValues(
                            horizontal = 4.dp,
                            vertical = 0.dp
                        )
                    ) {
                        Text(
                            text = monthDate.year.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = (16 * fontScale).sp,
                            color = MaterialTheme.colors.primary
                        )

                        Spacer(modifier = Modifier.width(2.dp))

                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = {
                            monthDate = monthDate.minus(
                                1,
                                DateTimeUnit.MONTH
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = null,
                            tint = MaterialTheme.colors.onSurface
                        )
                    }

                    IconButton(
                        onClick = {
                            monthDate = monthDate.plus(
                                1,
                                DateTimeUnit.MONTH
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colors.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                MonthCalendarGrid(
                    monthDate = monthDate,
                    today = initialSelectedDate,
                    selectedDate = selectedDate,
                    useCompactWidth = false,
                    horizontalOuterPadding = 0.dp,
                    onDateClick = {
                        selectedDate = it
                    }
                )
            }
        },
        buttons = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = 8.dp,
                        bottom = 16.dp
                    ),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colors.onSurface.copy(
                            alpha = 0.75f
                        )
                    )
                ) {
                    Text(
                        text = Locales.t("cancel"),
                        fontSize = (14 * fontScale).sp
                    )
                }

                Spacer(modifier = Modifier.width(15.dp))

                Button(
                    onClick = {
                        onConfirm(selectedDate)
                    },
                    modifier = Modifier.height(44.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(
                        horizontal = 18.dp,
                        vertical = 10.dp
                    ),
                    elevation = ButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    ),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.primary,
                        contentColor = MaterialTheme.colors.onPrimary
                    )
                ) {
                    Text(
                        text = Locales.t("confirm"),
                        fontSize = (14 * fontScale).sp
                    )
                }
            }
        },
        shape = AppDialogShape,
        backgroundColor = MaterialTheme.colors.surface
    )

    if (showYearPicker) {
        YearPickerDialog(
            selectedYear = monthDate.year,
            lastAvailableYear = lastAvailableYear,
            onDismiss = {
                showYearPicker = false
            },
            onSelect = { selectedYear ->
                monthDate = LocalDate(
                    selectedYear,
                    monthDate.month,
                    1
                )
                showYearPicker = false
            }
        )
    }
}

@Composable
private fun YearPickerDialog(
    selectedYear: Int,
    lastAvailableYear: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    val fontScale = AppSettings.getFontScale()
    val onSurface = MaterialTheme.colors.onSurface
    val years = remember(lastAvailableYear) {
        (2000..lastAvailableYear).toList()
    }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier.width(160.dp),
            shape = AppDialogShape,
            backgroundColor = MaterialTheme.colors.surface,
            elevation = 8.dp
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .padding(vertical = 6.dp)
            ) {
                items(
                    items = years,
                    key = { year -> year }
                ) { year ->
                    TextButton(
                        onClick = {
                            onSelect(year)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(
                            horizontal = 12.dp,
                            vertical = 7.dp
                        )
                    ) {
                        Text(
                            text = year.toString(),
                            fontSize = (16 * fontScale).sp,
                            fontWeight = if (year == selectedYear) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            },
                            color = if (year == selectedYear) {
                                MaterialTheme.colors.primary
                            } else {
                                onSurface
                            }
                        )
                    }
                }
            }
        }
    }
}