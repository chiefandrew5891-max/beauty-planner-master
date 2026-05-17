package com.andrey.beautyplanner.appcontent.approot

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import com.andrey.beautyplanner.*
import com.andrey.beautyplanner.appcontent.*
import com.andrey.beautyplanner.utils.LiveStatusKey
import com.andrey.beautyplanner.utils.getLiveStatus
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import androidx.compose.material.Button

@Composable
fun AppRootContent(
    state: AppRootState,
    padding: PaddingValues
) {
    var showSplash by remember { mutableStateOf(true) }
    var pendingPinAfterSplash by remember { mutableStateOf(false) }
    val ownerName = remember { AppSettings.ownerName ?: "" }

    var viewingAppt by remember { mutableStateOf<Appointment?>(null) }
    var viewingStartHm by remember { mutableStateOf("") }
    var viewingEndHm by remember { mutableStateOf("") }
    var viewingStatus by remember { mutableStateOf<LiveStatusKey?>(null) }

    if (showSplash) {
        AnimatedSplashScreen(
            ownerName = if (ownerName.isBlank()) "Evgi" else ownerName,
            onAnimationFinished = {
                showSplash = false
                if (state.mustCreatePin || (state.locked && !state.mustCreatePin)) {
                    pendingPinAfterSplash = true
                }
            }
        )
        return
    }

    if (pendingPinAfterSplash) {
        LaunchedEffect(Unit) {
            pendingPinAfterSplash = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        when (state.currentScreen) {
            Screen.SETTINGS -> SettingsPage(
                accessState = state.accessState,
                onExport = {
                    val nowMillis = Clock.System.now().toEpochMilliseconds()
                    if (!AccessManager.hasFeature(PremiumFeature.BACKUP_EXPORT, nowMillis)) {
                        state.showPremiumRequired(Locales.t("premium_required_export"))
                        return@SettingsPage
                    }

                    state.runProtected(
                        title = Locales.t("pin_required"),
                        text = Locales.t("export_requires_pin"),
                        confirmText = Locales.t("confirm")
                    ) {
                        state.exportFileName = "beautyplanner-backup"
                        state.showExportNameDialog = true
                    }
                },
                onImport = {
                    val nowMillis = Clock.System.now().toEpochMilliseconds()
                    if (!AccessManager.hasFeature(PremiumFeature.BACKUP_IMPORT, nowMillis)) {
                        state.showPremiumRequired(Locales.t("premium_required_import"))
                        return@SettingsPage
                    }

                    state.runProtected(
                        title = Locales.t("pin_required"),
                        text = Locales.t("import_requires_pin"),
                        confirmText = Locales.t("confirm")
                    ) {
                        BackupFilePicker.importJson(
                            onPicked = { jsonText ->
                                state.pendingImportText = jsonText
                                state.showImportConfirm = true
                            },
                            onError = { errorText ->
                                state.showImportError = errorText
                            }
                        )
                    }
                },
                onSetOrChangePin = {
                    state.showSetPinDialog = true
                },
                onRemovePin = {
                    state.runProtected(
                        title = Locales.t("pin_required"),
                        text = Locales.t("pin_required"),
                        confirmText = Locales.t("confirm")
                    ) {
                        state.showRemovePinConfirm = true
                    }
                },
                onClearDatabase = {
                    state.runProtected(
                        title = Locales.t("clear_db_title"),
                        text = Locales.t("clear_db_requires_pin"),
                        confirmText = Locales.t("confirm")
                    ) {
                        state.showClearDbBackupPrompt = true
                    }
                },
                onOpenPrivacyPolicy = {
                    state.currentScreen = Screen.PRIVACY_POLICY
                },
                onEnablePremiumForTesting = {
                    AppSettings.premiumUnlocked = true
                    AppSettings.persist()
                    state.refreshAccessState()
                },
                onDisablePremiumForTesting = {
                    AppSettings.premiumUnlocked = false
                    AppSettings.persist()
                    state.refreshAccessState()
                },
                onResetTrialForTesting = {
                    AppSettings.trialStartedAtMillis = Clock.System.now().toEpochMilliseconds()
                    AppSettings.premiumUnlocked = false
                    AppSettings.persist()
                    state.refreshAccessState()
                },
                onExpireTrialForTesting = {
                    val now = Clock.System.now().toEpochMilliseconds()
                    val fifteenDaysMillis = 15L * 24L * 60L * 60L * 1000L
                    AppSettings.trialStartedAtMillis = now - fifteenDaysMillis
                    AppSettings.premiumUnlocked = false
                    AppSettings.persist()
                    state.refreshAccessState()
                },
                onOpenPremiumScreen = {
                    state.premiumRequiredMessage = Locales.t("premium_required_default")
                    state.currentScreen = Screen.PREMIUM_ACCESS
                },
            )

            Screen.STATS -> {
                val nowMillis = Clock.System.now().toEpochMilliseconds()
                val statsMessage = Locales.t("premium_required_stats")

                if (AccessManager.hasFeature(PremiumFeature.STATS, nowMillis)) {
                    StatsPage(
                        appointments = state.appointments,
                        today = state.today
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = statsMessage,
                                color = MaterialTheme.colors.onBackground,
                                fontSize = (16 * state.fontScale).sp
                            )

                            Button(
                                onClick = {
                                    state.showPremiumRequired(statsMessage)
                                }
                            ) {
                                Text(Locales.t("premium_learn_more_btn"))
                            }
                        }
                    }
                }
            }

            Screen.FEEDBACK -> FeedbackPage(
                phone = AppSettings.servicePhone,
                onCallClick = { phone ->
                    if (phone.isNotBlank()) PhoneCaller.call(phone)
                }
            )

            Screen.MONTH -> {
                var nowTimeHm by remember { mutableStateOf(getCurrentTimeHm()) }
                LaunchedEffect(Unit) {
                    while (true) {
                        nowTimeHm = getCurrentTimeHm()
                        delay(60_000)
                    }
                }

                val nowMin = remember(nowTimeHm) {
                    com.andrey.beautyplanner.utils.parseHmToMinutes(nowTimeHm) ?: 0
                }

                val listState = rememberLazyListState()

                val upcoming by remember(nowTimeHm, state.today, state.appointments.size) {
                    derivedStateOf {
                        getUpcomingAppointments(
                            appointments = state.appointments,
                            today = state.today,
                            nowTime = nowTimeHm
                        )
                    }
                }

                val isCollapsed by remember {
                    derivedStateOf {
                        listState.firstVisibleItemIndex > 0 ||
                                listState.firstVisibleItemScrollOffset > 40
                    }
                }

                val headerText by remember(isCollapsed, state.calendarViewDate, state.today) {
                    derivedStateOf {
                        if (!isCollapsed) {
                            val monthKey = when (state.calendarViewDate.monthNumber) {
                                1 -> "month_jan"
                                2 -> "month_feb"
                                3 -> "month_mar"
                                4 -> "month_apr"
                                5 -> "month_may"
                                6 -> "month_jun"
                                7 -> "month_jul"
                                8 -> "month_aug"
                                9 -> "month_sep"
                                10 -> "month_oct"
                                11 -> "month_nov"
                                12 -> "month_dec"
                                else -> ""
                            }
                            "${Locales.t(monthKey)} ${state.calendarViewDate.year}"
                        } else {
                            val monthKeyGen = when (state.today.monthNumber) {
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
                                else -> ""
                            }
                            "${state.today.dayOfMonth} ${Locales.t(monthKeyGen)} ${state.calendarViewDate.year}"
                        }
                    }
                }

                Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = headerText,
                            fontSize = (24 * state.fontScale).sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.onBackground
                        )

                        Row {
                            val arrowsEnabled = !isCollapsed
                            val arrowTint = if (arrowsEnabled) {
                                MaterialTheme.colors.primary
                            } else {
                                MaterialTheme.colors.onSurface.copy(alpha = 0.35f)
                            }

                            IconButton(
                                enabled = arrowsEnabled,
                                onClick = { state.calendarViewDate = state.calendarViewDate.minus(1, DateTimeUnit.MONTH) }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowLeft,
                                    contentDescription = null,
                                    tint = arrowTint
                                )
                            }

                            IconButton(
                                enabled = arrowsEnabled,
                                onClick = { state.calendarViewDate = state.calendarViewDate.plus(1, DateTimeUnit.MONTH) }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = arrowTint
                                )
                            }
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            MonthCalendarGrid(
                                monthDate = state.calendarViewDate,
                                today = state.today,
                                selectedDate = state.selectedDate
                            ) { date ->
                                state.selectedDate = date
                                state.currentScreen = Screen.DAY_DETAILS
                            }
                        }

                        item {
                            Divider(
                                modifier = Modifier.padding(horizontal = 40.dp, vertical = 20.dp),
                                color = Color.LightGray.copy(alpha = 0.5f),
                                thickness = 1.dp
                            )

                            Text(
                                text = Locales.t("upcoming_appointments_list"),
                                modifier = Modifier.padding(start = 24.dp, bottom = 8.dp),
                                fontSize = (16 * state.fontScale).sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Gray
                            )
                        }

                        if (upcoming.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = Locales.t("no_upcoming_appointments"),
                                        color = Color.Gray,
                                        fontSize = (14 * state.fontScale).sp
                                    )
                                }
                            }
                        } else {
                            items(upcoming.size) { idx ->
                                val appt = upcoming[idx]

                                val durationMin =
                                    if (appt.durationMinutes > 0) appt.durationMinutes
                                    else appt.durationHours.coerceAtLeast(1) * 60
                                val startMin =
                                    com.andrey.beautyplanner.utils.parseHmToMinutes(appt.time) ?: 0
                                val endMin = startMin + durationMin
                                val endHour = ((endMin / 60) % 24).toString().padStart(2, '0')
                                val endMinute = (endMin % 60).toString().padStart(2, '0')
                                val endHm = "$endHour:$endMinute"

                                val status = getLiveStatus(
                                    appt = appt,
                                    nowDate = state.today,
                                    nowMinutes = nowMin
                                )

                                AppointmentCard(
                                    appt = appt,
                                    status = status,
                                    showDateInCard = true,
                                    startHm = appt.time,
                                    endHm = endHm,
                                    onClick = {
                                        viewingAppt = appt
                                        viewingStartHm = appt.time
                                        viewingEndHm = endHm
                                        viewingStatus = status
                                    },
                                    onLongClick = {
                                        viewingAppt = appt
                                        viewingStartHm = appt.time
                                        viewingEndHm = endHm
                                        viewingStatus = status
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Screen.DAY_DETAILS -> DayDetailsView(
                date = state.selectedDate,
                appointments = state.appointments,
                onDateChange = { state.selectedDate = it },
                onTimeClick = { time ->
                    val nowMillis = Clock.System.now().toEpochMilliseconds()
                    val canCreate = AccessManager.canCreateAppointment(
                        currentAppointmentsCount = state.appointments.size,
                        nowMillis = nowMillis
                    )

                    if (!canCreate) {
                        state.showPremiumRequired(Locales.t("premium_required_limit"))
                        return@DayDetailsView
                    }

                    state.selectedTimeSlot = time
                    state.editingAppointment = null
                    state.bookingReadOnly = false
                    state.showBookingDialog = true
                },
                onEditClick = { appt ->
                    state.editingAppointment = appt
                    state.bookingReadOnly = false
                    state.showBookingDialog = true
                },
                onDeleteClick = { appt ->
                    state.showDeleteConfirm = appt
                },
                onTransferClick = { appt ->
                    state.transferA = appt
                    state.showTransferPickDialog = true
                    state.bookingReadOnly = false
                }
            )
            Screen.PRIVACY_POLICY -> PrivacyPolicyScreen(
                languageCode = Locales.currentLanguage,
                onBack = {
                    state.currentScreen = Screen.SETTINGS
                }
            )
            Screen.PREMIUM_ACCESS -> PremiumAccessScreen(
                accessState = state.accessState,
                message = state.premiumRequiredMessage,
                onContinueFree = {
                    state.currentScreen = Screen.SETTINGS
                },
                onUnlockPremium = {
                    state.premiumRequiredMessage = Locales.t("premium_billing_coming_soon")
                }
            )
        }

        if (state.showBookingDialog) {
            BookingDialog(
                time = state.editingAppointment?.time ?: state.selectedTimeSlot,
                initialData = state.editingAppointment ?: state.transferA,
                readOnly = state.bookingReadOnly && state.editingAppointment != null,
                onDismiss = {
                    state.showBookingDialog = false
                    state.editingAppointment = null
                    state.transferA = null
                    state.bookingReadOnly = false
                },
                onSave = { startTime, durationMinutes, name, phone, service, price ->
                    val id = state.editingAppointment?.id
                        ?: state.transferA?.id
                        ?: Clock.System.now().toEpochMilliseconds().toString()

                    val targetDate = state.selectedDate.toString()

                    val newAppt = Appointment(
                        id = id,
                        dateString = targetDate,
                        time = startTime,
                        clientName = name,
                        phone = phone,
                        serviceName = service,
                        price = price,
                        durationMinutes = durationMinutes,
                        durationHours = ((durationMinutes + 59) / 60).coerceAtLeast(1)
                    )

                    state.transferA?.let { state.appointments.remove(it); state.transferA = null }
                    state.replaceById(newAppt)
                    state.saveAll()

                    state.showBookingDialog = false
                    state.editingAppointment = null
                    state.bookingReadOnly = false
                },
                onTransferRequest = { appt ->
                    state.transferA = appt
                    state.showBookingDialog = false
                    state.showTransferPickDialog = true
                    state.bookingReadOnly = false
                }
            )
        }

        if (state.showTransferPickDialog && state.transferA != null) {
            val a = state.transferA!!
            TransferPickDialog(
                initialSelectedDate = LocalDate.parse(a.dateString),
                initialMonthDate = LocalDate.parse(a.dateString),
                onDismiss = {
                    state.showTransferPickDialog = false
                    state.transferA = null
                },
                onConfirm = { newDate, newTime ->
                    state.pendingTargetDate = newDate
                    state.pendingTargetTime = newTime

                    val b = state.findAppointment(newDate, newTime)
                    if (b != null && b.id != a.id) {
                        state.conflictB = b
                        state.showTransferConflictConfirm = true
                    } else {
                        state.moveAppointment(a, newDate, newTime)
                        state.saveAll()
                        state.showTransferPickDialog = false
                        state.transferA = null
                    }
                }
            )
        }

        val apptToView = viewingAppt
        val statusToView = viewingStatus
        if (apptToView != null && statusToView != null) {
            AppointmentDetailsDialog(
                appt = apptToView,
                startHm = viewingStartHm,
                endHm = viewingEndHm,
                status = statusToView,
                onDismiss = {
                    viewingAppt = null
                    viewingStatus = null
                },
                onEditClick = {
                    viewingAppt = null
                    viewingStatus = null
                    state.editingAppointment = apptToView
                    state.bookingReadOnly = false
                    state.showBookingDialog = true
                },
                onTransferClick = {
                    viewingAppt = null
                    viewingStatus = null
                    state.transferA = apptToView
                    state.showTransferPickDialog = true
                    state.bookingReadOnly = false
                },
                onDeleteClick = {
                    viewingAppt = null
                    viewingStatus = null
                    state.showDeleteConfirm = apptToView
                }
            )
        }
    }
}
//Create new Animation_screen fix11