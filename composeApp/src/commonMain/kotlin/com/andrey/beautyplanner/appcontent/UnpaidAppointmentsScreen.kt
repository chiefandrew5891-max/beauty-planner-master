package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Appointment
import com.andrey.beautyplanner.AppointmentPaymentStatus
import com.andrey.beautyplanner.Locales
import com.andrey.beautyplanner.effectivePaymentStatus
import com.andrey.beautyplanner.getCurrentTimeHm
import com.andrey.beautyplanner.utils.getLiveStatus
import com.andrey.beautyplanner.utils.parseHmToMinutes
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

@Composable
fun UnpaidAppointmentsScreen(
    appointments: List<Appointment>,
    onConfirmPayment: (Appointment) -> Unit
) {
    val fontScale = AppSettings.getFontScale()

    var nowTimeHm by remember { mutableStateOf(getCurrentTimeHm()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowTimeHm = getCurrentTimeHm()
            delay(60_000)
        }
    }

    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val nowMinutes = remember(nowTimeHm) { parseHmToMinutes(nowTimeHm) ?: 0 }

    val unpaidAppointments = appointments
        .filter { it.effectivePaymentStatus() == AppointmentPaymentStatus.PAYMENT_LATER }
        .sortedWith(compareBy({ it.dateString }, { it.time }, { it.clientName.lowercase() }))

    CenteredNarrowContentContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = Locales.t("unpaid_title"),
                fontSize = (22 * fontScale).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onBackground
            )

            if (unpaidAppointments.isEmpty()) {
                Text(
                    text = Locales.t("unpaid_empty"),
                    fontSize = (14 * fontScale).sp,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(unpaidAppointments, key = { it.id }) { appt ->
                        val liveStatus = runCatching {
                            getLiveStatus(
                                appt = appt,
                                nowDate = today,
                                nowMinutes = nowMinutes
                            )
                        }.getOrDefault(com.andrey.beautyplanner.utils.LiveStatusKey.WAITING)

                        val serviceText = if (appt.serviceName.startsWith("service_")) {
                            Locales.t(appt.serviceName)
                        } else {
                            appt.serviceName
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                            elevation = 2.dp,
                            backgroundColor = MaterialTheme.colors.surface
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = appt.clientName,
                                    fontSize = (16 * fontScale).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colors.onSurface
                                )

                                Text(
                                    text = "${appt.dateString} • ${appt.time}",
                                    fontSize = (13 * fontScale).sp,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f)
                                )

                                Text(
                                    text = "${Locales.t("appt_status_label")}: ${Locales.t(liveStatus.localeKey)}",
                                    fontSize = (14 * fontScale).sp,
                                    color = MaterialTheme.colors.onSurface
                                )

                                Text(
                                    text = "${Locales.t("unpaid_service")}: $serviceText",
                                    fontSize = (14 * fontScale).sp,
                                    color = MaterialTheme.colors.onSurface
                                )

                                Text(
                                    text = "${Locales.t("unpaid_price")}: ${
                                        AppSettings.formatMoneyAmount(
                                            amount = appt.price,
                                            currencyCode = appt.currency
                                        )
                                    }",
                                    fontSize = (14 * fontScale).sp,
                                    color = MaterialTheme.colors.onSurface
                                )

                                if (appt.notes.isNotBlank()) {
                                    var commentExpanded by remember(appt.id) { mutableStateOf(false) }

                                    Text(
                                        text = "${Locales.t("view_comment")}: ${appt.notes}",
                                        fontSize = (13 * fontScale).sp,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.75f),
                                        maxLines = if (commentExpanded) Int.MAX_VALUE else 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                commentExpanded = !commentExpanded
                                            }
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                PrimaryActionButton(
                                    text = Locales.t("unpaid_mark_paid"),
                                    onClick = { onConfirmPayment(appt) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}