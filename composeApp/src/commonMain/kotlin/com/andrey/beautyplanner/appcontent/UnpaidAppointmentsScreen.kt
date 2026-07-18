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
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

@Composable
fun UnpaidAppointmentsScreen(
    appointments: List<Appointment>,
    onConfirmPayment: (Appointment) -> Unit,
    premiumEnabled: Boolean,
    onOpenPremium: () -> Unit
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

            if (!premiumEnabled) {
                Text(
                    text = Locales.t("premium_required_default"),
                    fontSize = (14 * fontScale).sp,
                    color = MaterialTheme.colors.error,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedButton(
                    onClick = onOpenPremium,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Text(Locales.t("premium_open_screen_btn"))
                }
            } else if (unpaidAppointments.isEmpty()) {
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
                    items(unpaidAppointments, key = { it.id }) { appointment ->
                        val liveStatus = runCatching {
                            getLiveStatus(
                                appt = appointment,
                                nowDate = today,
                                nowMinutes = nowMinutes
                            )
                        }.getOrDefault(com.andrey.beautyplanner.utils.LiveStatusKey.WAITING)

                        val serviceText = if (appointment.serviceName.startsWith("service_")) {
                            Locales.t(appointment.serviceName)
                        } else {
                            appointment.serviceName
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
                                    text = appointment.clientName,
                                    fontSize = (16 * fontScale).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colors.onSurface
                                )

                                Text(
                                    text = "${appointment.dateString} • ${appointment.time}",
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
                                            amount = appointment.price,
                                            currencyCode = appointment.currency
                                        )
                                    }",
                                    fontSize = (14 * fontScale).sp,
                                    color = MaterialTheme.colors.onSurface
                                )

                                if (appointment.notes.isNotBlank()) {
                                    var commentExpanded by remember(appointment.id) { mutableStateOf(false) }

                                    Text(
                                        text = "${Locales.t("view_comment")}: ${appointment.notes}",
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
                                    onClick = { onConfirmPayment(appointment) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}