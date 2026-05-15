package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Appointment
import com.andrey.beautyplanner.Locales
import com.andrey.beautyplanner.getCurrentTimeHm
import com.andrey.beautyplanner.utils.getLiveStatus
import com.andrey.beautyplanner.utils.LiveStatusKey
import com.andrey.beautyplanner.utils.parseHmToMinutes
import kotlinx.coroutines.delay
import kotlinx.datetime.*

private fun minutesToHm(mins: Int): String {
    val safe = mins.coerceIn(0, 24 * 60 - 1)
    val h = safe / 60
    val m = safe % 60
    return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
}

private fun nextHourBoundary(mins: Int): Int {
    val mod = mins % 60
    return if (mod == 0) mins + 60 else mins + (60 - mod)
}

private data class Block(
    val kind: Kind,
    val startMin: Int,
    val endMin: Int,
    val appt: Appointment? = null
) {
    enum class Kind { FREE, APPOINTMENT }
}

@Composable
fun DayDetailsView(
    date: LocalDate,
    appointments: List<Appointment>,
    onDateChange: (LocalDate) -> Unit,
    onTimeClick: (String) -> Unit,
    onEditClick: (Appointment) -> Unit,
    onDeleteClick: (Appointment) -> Unit,
    onTransferClick: (Appointment) -> Unit
) {
    val fontScale = AppSettings.getFontScale()

    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }

    var nowTimeHm by remember { mutableStateOf(getCurrentTimeHm()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowTimeHm = getCurrentTimeHm()
            delay(60_000)
        }
    }
    val nowMin = remember(nowTimeHm) { parseHmToMinutes(nowTimeHm) ?: 0 }

    val monthKeyGen = when (date.monthNumber) {
        1 -> "month_jan_gen"
        2 -> "month_feb_gen"
        3 -> "month_mar_gen"
        4 -> "month_apr_gen"
        5 -> "month_may_gen"
        6 -> "month_jun_gen"
        7 -> "month_jul_gen"
        8 -> "month_aug_gen"
        9 -> "month_sep_gen"
        10 -> "month_oct_gen"
        11 -> "month_nov_gen"
        12 -> "month_dec_gen"
        else -> "month_jan_gen"
    }
    val formattedDate = "${date.dayOfMonth} ${Locales.t(monthKeyGen)} ${date.year}"

    val dayStart = 8 * 60
    val dayEnd = 21 * 60

    val apptsSnapshot = appointments.toList()

    val dayAppts = remember(apptsSnapshot, date) {
        apptsSnapshot
            .filter { it.dateString == date.toString() }
            .mapNotNull { a ->
                val s = parseHmToMinutes(a.time) ?: return@mapNotNull null
                val d = if (a.durationMinutes > 0) a.durationMinutes else a.durationHours.coerceAtLeast(1) * 60
                val e = s + d
                Triple(a, s, e)
            }
            .sortedBy { it.second }
    }

    val blocks = remember(dayAppts) {
        val out = mutableListOf<Block>()
        var cursor = dayStart
        var i = 0

        while (cursor < dayEnd) {
            val next = dayAppts.getOrNull(i)
            val nextStart = next?.second
            val nextEnd = next?.third

            if (next != null && nextStart != null && nextEnd != null && nextStart <= cursor) {
                val start = nextStart.coerceAtLeast(dayStart)
                val end = nextEnd.coerceAtMost(dayEnd)
                if (end > cursor) {
                    out.add(Block(Block.Kind.APPOINTMENT, startMin = start, endMin = end, appt = next.first))
                    cursor = end
                }
                i++
                continue
            }

            val freeStart = cursor
            val freeEndCandidate = nextHourBoundary(cursor).coerceAtMost(dayEnd)
            val freeEnd =
                if (nextStart != null) minOf(freeEndCandidate, nextStart.coerceAtMost(dayEnd)) else freeEndCandidate

            if (freeEnd > freeStart) {
                out.add(Block(Block.Kind.FREE, startMin = freeStart, endMin = freeEnd, appt = null))
                cursor = freeEnd
            } else {
                cursor += 10
            }
        }
        out
    }

    var viewingAppt by remember { mutableStateOf<Appointment?>(null) }
    var viewingStartHm by remember { mutableStateOf("") }
    var viewingEndHm by remember { mutableStateOf("") }
    var viewingStatus by remember { mutableStateOf<LiveStatusKey?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formattedDate,
                fontSize = (24 * fontScale).sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colors.onBackground
            )

            Row {
                val arrowTint = MaterialTheme.colors.primary

                IconButton(onClick = { onDateChange(date.minus(1, DateTimeUnit.DAY)) }) {
                    Icon(Icons.Filled.KeyboardArrowLeft, null, tint = arrowTint)
                }
                IconButton(onClick = { onDateChange(date.plus(1, DateTimeUnit.DAY)) }) {
                    Icon(Icons.Filled.KeyboardArrowRight, null, tint = arrowTint)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(blocks.size) { idx ->
                val b = blocks[idx]
                val interactionSource = remember { MutableInteractionSource() }

                val startHm = minutesToHm(b.startMin)
                val endHm = minutesToHm(b.endMin)

                if (b.kind == Block.Kind.APPOINTMENT && b.appt != null) {
                    val appt = b.appt

                    val durationMin =
                        if (appt.durationMinutes > 0) appt.durationMinutes else appt.durationHours.coerceAtLeast(1) * 60
                    val apptStartMin = parseHmToMinutes(appt.time) ?: b.startMin
                    val apptEndMin = apptStartMin + durationMin

                    val cardBg = when {
                        date < today -> if (AppSettings.isDarkMode) Color(0xFF1F2A36) else Color(0xFFECECEC)
                        date > today -> if (AppSettings.isDarkMode) Color(0xFF253548) else Color(0xFFF2F2F2)
                        else -> if (AppSettings.isDarkMode) Color(0xFF253548) else Color(0xFFF2F2F2)
                    }

                    val liveStatus = getLiveStatus(
                        appt = appt,
                        nowDate = today,
                        nowMinutes = nowMin
                    )

                    val isPastOrFinished = date < today || apptEndMin <= nowMin

                    AppointmentCard(
                        appt = appt,
                        status = liveStatus,
                        showDateInCard = false,
                        startHm = startHm,
                        endHm = endHm,
                        dayDetailsBackgroundColor = cardBg,
                        dayDetailsIsPastOrFinished = isPastOrFinished,
                        onClick = {
                            viewingAppt = appt
                            viewingStartHm = startHm
                            viewingEndHm = endHm
                            viewingStatus = liveStatus
                        },
                        onLongClick = {
                            viewingAppt = appt
                            viewingStartHm = startHm
                            viewingEndHm = endHm
                            viewingStatus = liveStatus
                        }
                    )
                } else {
                    Card(
                        elevation = 0.dp,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .height(78.dp)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = LocalIndication.current,
                                onClick = { onTimeClick(startHm) }
                            ),
                        backgroundColor = Color.Transparent,
                        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier.width(60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = startHm,
                                    fontSize = (16 * fontScale).sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f)
                                )
                            }
                            Text(
                                text = Locales.t("free"),
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.35f),
                                fontSize = (14 * fontScale).sp
                            )
                        }
                    }
                }
            }
        }
    }

    val apptToView = viewingAppt
    val liveStatusToView = viewingStatus
    if (apptToView != null && liveStatusToView != null) {
        AppointmentDetailsDialog(
            appt = apptToView,
            startHm = viewingStartHm,
            endHm = viewingEndHm,
            status = liveStatusToView,
            onDismiss = {
                viewingAppt = null
                viewingStatus = null
            },
            onEditClick = {
                viewingAppt = null
                viewingStatus = null
                onEditClick(apptToView)
            },
            onTransferClick = {
                viewingAppt = null
                viewingStatus = null
                onTransferClick(apptToView)
            },
            onDeleteClick = {
                viewingAppt = null
                viewingStatus = null
                onDeleteClick(apptToView)
            }
        )
    }
}