package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Appointment
import com.andrey.beautyplanner.Locales
import com.andrey.beautyplanner.utils.LiveStatusKey
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import com.andrey.beautyplanner.utils.parseHmToMinutes
import kotlin.math.PI
import kotlin.math.sin

private fun ddMMyyyy(dateString: String): String {
    val p = dateString.split("-")
    return if (p.size == 3) "${p[2]}.${p[1]}.${p[0]}" else dateString
}

private fun apptServiceDisplay(appt: Appointment): String =
    if (appt.serviceName.startsWith("service_")) Locales.t(appt.serviceName) else appt.serviceName

private fun weekdayShortKey(date: LocalDate): String = when (date.dayOfWeek) {
    kotlinx.datetime.DayOfWeek.MONDAY -> "mon"
    kotlinx.datetime.DayOfWeek.TUESDAY -> "tue"
    kotlinx.datetime.DayOfWeek.WEDNESDAY -> "wed"
    kotlinx.datetime.DayOfWeek.THURSDAY -> "thu"
    kotlinx.datetime.DayOfWeek.FRIDAY -> "fri"
    kotlinx.datetime.DayOfWeek.SATURDAY -> "sat"
    kotlinx.datetime.DayOfWeek.SUNDAY -> "sun"
    else -> "mon"
}

private fun minutesUntilAppointment(
    appt: Appointment,
    today: LocalDate,
    nowMinutes: Int
): Int? {
    val apptDate = runCatching { LocalDate.parse(appt.dateString) }.getOrNull() ?: return null
    val apptStart = parseHmToMinutes(appt.time)

    val dayDiff = apptDate.toEpochDays() - today.toEpochDays()
    return dayDiff * 24 * 60 + (apptStart - nowMinutes)
}

private fun urgencyPulseSpec(minutesLeft: Int): Pair<Int, Float> {
    return when {
        minutesLeft <= 60 -> 850 to 0.30f
        minutesLeft <= 3 * 60 -> 1200 to 0.24f
        minutesLeft <= 6 * 60 -> 1600 to 0.19f
        minutesLeft <= 12 * 60 -> 2100 to 0.15f
        else -> 2700 to 0.11f
    }
}

/**
 * Единая карточка записи для разных экранов, но с сохранением "золотого стандарта" дизайна:
 * - showDateInCard=true  -> верстка как в UpcomingAppointmentCard (MonthViews)
 * - showDateInCard=false -> верстка как в DayDetailsView (заполненный слот)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppointmentCard(
    appt: Appointment,
    status: LiveStatusKey,
    showDateInCard: Boolean,
    startHm: String,
    endHm: String,
    nowDate: LocalDate? = null,
    nowMinutes: Int? = null,
    // Для DayDetails: цвета/фон уже были "канонические" — даём передать снаружи
    dayDetailsBackgroundColor: Color? = null,
    dayDetailsIsPastOrFinished: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val fontScale = AppSettings.getFontScale()
    val interactionSource = remember { MutableInteractionSource() }

    val serviceDisplay = apptServiceDisplay(appt)
    val priceText = appt.price.trim().let { p -> if (p.isBlank()) "" else "$p ${AppSettings.currencySymbol()}" }

    if (showDateInCard) {
        val formattedDate = ddMMyyyy(appt.dateString)
        val parsedDate = runCatching { LocalDate.parse(appt.dateString) }.getOrNull()
        val weekdayText = parsedDate?.let { Locales.t(weekdayShortKey(it)) }.orEmpty()

        val today = nowDate ?: Clock.System.todayIn(TimeZone.currentSystemDefault())
        val currentMinutes = nowMinutes ?: 0
        val minutesLeft = minutesUntilAppointment(appt, today, currentMinutes)

        val shouldPulse = minutesLeft != null && minutesLeft in 0..(24 * 60)

        val (pulseDuration, maxAlpha) = if (shouldPulse) {
            urgencyPulseSpec(minutesLeft!!)
        } else {
            2400 to 0f
        }

        val infiniteTransition = rememberInfiniteTransition(label = "upcomingUrgency")
        val cycleProgress = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = pulseDuration,
                    easing = androidx.compose.animation.core.LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "upcomingUrgencyCycle"
        )

        val wave = ((sin(cycleProgress.value * 2.0 * PI - PI / 2.0) + 1.0) / 2.0).toFloat()
        val animatedStrength = (maxAlpha * wave).coerceIn(0f, 1f)

        val urgentTint = if (MaterialTheme.colors.isLight) {
            Color(0xFFE86C8E)
        } else {
            Color(0xFFB85C7A)
        }

        val cardBackground =
            if (shouldPulse) {
                androidx.compose.ui.graphics.lerp(
                    MaterialTheme.colors.surface,
                    urgentTint,
                    animatedStrength
                )
            } else {
                MaterialTheme.colors.surface
            }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            shape = RoundedCornerShape(16.dp),
            elevation = 4.dp,
            backgroundColor = cardBackground
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (weekdayText.isNotBlank()) {
                            Text(
                                text = weekdayText,
                                fontSize = (13 * fontScale).sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.92f)
                            )
                            Spacer(Modifier.width(8.dp))
                        }

                        Text(
                            text = "$formattedDate  $startHm–$endHm",
                            fontSize = (13 * fontScale).sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.85f)
                        )
                    }

                    Text(
                        text = "${appt.price} ${AppSettings.currencySymbol()}",
                        fontSize = (13 * fontScale).sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.85f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = appt.clientName,
                            fontSize = (15 * fontScale).sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colors.onSurface
                        )

                        Spacer(Modifier.height(2.dp))

                        Text(
                            text = serviceDisplay,
                            fontSize = (13 * fontScale).sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (status == LiveStatusKey.DONE) {
                        Spacer(Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colors.primary.copy(alpha = 0.75f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    } else {
        // ====== EXACT DayDetails "filled slot" DESIGN (текущий) ======
        val bg = dayDetailsBackgroundColor ?: MaterialTheme.colors.surface

        Card(
            elevation = 0.dp,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .height(92.dp)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            backgroundColor = bg
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // time col (как было)
                val timeColWidth = 60.dp
                val timeFont = (16 * fontScale).sp
                val timeFontWeight = FontWeight.Bold

                val busyTimeColor = MaterialTheme.colors.primary
                val pastTimeColor = MaterialTheme.colors.onSurface.copy(alpha = 0.80f)

                val timeColor = if (dayDetailsIsPastOrFinished) pastTimeColor else busyTimeColor

                Column(
                    modifier = Modifier.width(timeColWidth),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = startHm,
                        fontSize = timeFont,
                        fontWeight = timeFontWeight,
                        color = timeColor
                    )
                    Divider(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .width(30.dp),
                        thickness = 1.dp,
                        color = timeColor.copy(alpha = 0.35f)
                    )
                    Text(
                        text = endHm,
                        fontSize = timeFont,
                        fontWeight = timeFontWeight,
                        color = timeColor
                    )
                }

                Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = appt.clientName,
                        fontSize = (17 * fontScale).sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 1f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = serviceDisplay,
                        fontSize = (13 * fontScale).sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.60f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (priceText.isNotBlank()) {
                    Text(
                        text = priceText,
                        fontSize = (14 * fontScale).sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 1f),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }

                if (status == LiveStatusKey.DONE) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary.copy(alpha = 0.75f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

/**
 * Общий попап просмотра записи.
 * Формат даты: DD.MM.YYYY HH:MM (строго).
 *
 * Кнопки Edit/Transfer должны быть disabled, если status == DONE.
 */
@Composable
fun AppointmentDetailsDialog(
    appt: Appointment,
    startHm: String,
    endHm: String,
    status: LiveStatusKey,
    actionsEnabled: Boolean = true,
    allowDeletePast: Boolean = false,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onTransferClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val fontScale = AppSettings.getFontScale()

    val serviceDisplay = apptServiceDisplay(appt)
    val priceText = appt.price.trim().let { p -> if (p.isBlank()) "" else "$p ${AppSettings.currencySymbol()}" }

    val dateTextColor = MaterialTheme.colors.primary.copy(alpha = 0.95f)
    val timeTextColor = MaterialTheme.colors.onSurface.copy(alpha = 0.72f)

    val editTransferEnabled = actionsEnabled && status != LiveStatusKey.DONE
    val deleteEnabled = actionsEnabled || allowDeletePast

    val disabledButtonTextColor = MaterialTheme.colors.onSurface.copy(alpha = 0.35f)
    val enabledDeleteColor = Color.Red

    AppDialogTheme {
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
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        text = appt.clientName,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        fontSize = (22 * fontScale).sp,
                        color = MaterialTheme.colors.onSurface
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    color = dateTextColor,
                                    fontSize = (15 * fontScale).sp
                                )
                            ) {
                                append(ddMMyyyy(appt.dateString))
                            }

                            append("  ")

                            withStyle(
                                SpanStyle(
                                    fontWeight = FontWeight.Normal,
                                    color = timeTextColor,
                                    fontSize = (14 * fontScale).sp
                                )
                            ) {
                                append("$startHm–$endHm")
                            }
                        }
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "${Locales.t("service")}: $serviceDisplay",
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.85f)
                    )

                    if (priceText.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "${Locales.t("price")}: $priceText",
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.85f)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "${Locales.t("appt_status_label")}: ${Locales.t(status.localeKey)}",
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.70f),
                        fontSize = (13 * fontScale).sp
                    )

                    if (!actionsEnabled && !allowDeletePast) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = Locales.t("past_date_actions_disabled"),
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
                            fontSize = (12 * fontScale).sp
                        )
                    }

                    if (!actionsEnabled && allowDeletePast) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = Locales.t("past_date_actions_disabled"),
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
                            fontSize = (12 * fontScale).sp
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onEditClick,
                            enabled = editTransferEnabled,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                disabledBackgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.08f),
                                disabledContentColor = disabledButtonTextColor
                            )
                        ) {
                            Text(
                                text = Locales.t("edit"),
                                color = if (editTransferEnabled) {
                                    MaterialTheme.colors.onPrimary
                                } else {
                                    disabledButtonTextColor
                                }
                            )
                        }

                        OutlinedButton(
                            onClick = onTransferClick,
                            enabled = editTransferEnabled,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                disabledContentColor = disabledButtonTextColor
                            )
                        ) {
                            Text(
                                text = Locales.t("transfer_appt"),
                                color = if (editTransferEnabled) {
                                    MaterialTheme.colors.primary
                                } else {
                                    disabledButtonTextColor
                                }
                            )
                        }

                        OutlinedButton(
                            onClick = onDeleteClick,
                            enabled = deleteEnabled,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = enabledDeleteColor,
                                disabledContentColor = disabledButtonTextColor
                            )
                        ) {
                            Text(
                                text = Locales.t("delete_btn"),
                                color = if (deleteEnabled) {
                                    enabledDeleteColor
                                } else {
                                    disabledButtonTextColor
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(Locales.t("close"))
                }
            },
            shape = AppDialogShape
        )
    }
}