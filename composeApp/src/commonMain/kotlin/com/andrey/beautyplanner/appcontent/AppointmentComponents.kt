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

private fun ddMMyyyy(dateString: String): String {
    val p = dateString.split("-")
    return if (p.size == 3) "${p[2]}.${p[1]}.${p[0]}" else dateString
}

private fun apptServiceDisplay(appt: Appointment): String =
    if (appt.serviceName.startsWith("service_")) Locales.t(appt.serviceName) else appt.serviceName

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
    // Для DayDetails: передаем уже вычисленные значения (чтобы не ломать вёрстку)
    startHm: String,
    endHm: String,
    // Для DayDetails: цвета/фон уже были "канонические" — даём передать снаружи
    dayDetailsBackgroundColor: Color? = null,
    dayDetailsIsPastOrFinished: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val fontScale = AppSettings.getFontScale()
    val interactionSource = remember { MutableInteractionSource() }

    val serviceDisplay = apptServiceDisplay(appt)
    val priceText = appt.price.trim().let { p -> if (p.isBlank()) "" else "$p€" }

    if (showDateInCard) {
        // ====== EXACT UpcomingAppointmentCard DESIGN (старый) ======
        val formattedDate = ddMMyyyy(appt.dateString)

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
            backgroundColor = MaterialTheme.colors.surface
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$formattedDate  $startHm–$endHm",
                        fontSize = (13 * fontScale).sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.85f)
                    )

                    Text(
                        text = "${appt.price}€",
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
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onTransferClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val fontScale = AppSettings.getFontScale()

    val serviceDisplay = apptServiceDisplay(appt)
    val priceText = appt.price.trim().let { p -> if (p.isBlank()) "" else "$p €" }

    val dateTextColor = MaterialTheme.colors.primary.copy(alpha = 0.95f)
    val timeTextColor = MaterialTheme.colors.onSurface.copy(alpha = 0.72f)

    val editTransferEnabled = status != LiveStatusKey.DONE

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

                    Spacer(Modifier.height(18.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onEditClick,
                            enabled = editTransferEnabled,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(Locales.t("edit"))
                        }

                        OutlinedButton(
                            onClick = onTransferClick,
                            enabled = editTransferEnabled,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(Locales.t("transfer_appt"))
                        }

                        OutlinedButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                        ) {
                            Text(Locales.t("delete_btn"), color = Color.Red)
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