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
import com.andrey.beautyplanner.appcontent.ServiceTemplatesScreen
import com.andrey.beautyplanner.appcontent.WorkScheduleScreen
import com.andrey.beautyplanner.appcontent.AppearanceSettingsScreen
import com.andrey.beautyplanner.appcontent.DeveloperAccessScreen
import com.andrey.beautyplanner.appcontent.BackupSettingsScreen
import androidx.compose.runtime.saveable.rememberSaveable
import com.andrey.beautyplanner.appcontent.AuthWelcomeScreen
import com.andrey.beautyplanner.appcontent.AuthEmailScreen
import kotlinx.coroutines.launch
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState


@OptIn(androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun AppRootContent(
    state: AppRootState,
    padding: PaddingValues
) {
    var showSplash by rememberSaveable { mutableStateOf(true) }
    var pendingPinAfterSplash by rememberSaveable { mutableStateOf(false) }
    val ownerName = AppSettings.ownerName.trim()

    var viewingAppt by remember { mutableStateOf<Appointment?>(null) }
    var viewingStartHm by remember { mutableStateOf("") }
    var viewingEndHm by remember { mutableStateOf("") }
    var viewingStatus by remember { mutableStateOf<LiveStatusKey?>(null) }

    if (showSplash) {
        AnimatedSplashScreen(
            ownerName = ownerName,
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
                onOpenPrivacyPolicy = {
                    state.navigateTo(Screen.PRIVACY_POLICY)
                },
                onOpenPremiumScreen = {
                    state.showPremiumRequired(
                        message = Locales.t("premium_required_default"),
                        returnTo = Screen.SETTINGS
                    )
                },
                onOpenServiceTemplates = {
                    val nowMillis = Clock.System.now().toEpochMilliseconds()
                    if (!AccessManager.hasFeature(PremiumFeature.CUSTOM_SERVICES, nowMillis)) {
                        state.showPremiumRequired(
                            message = Locales.t("premium_required_services"),
                            returnTo = Screen.SETTINGS
                        )
                        return@SettingsPage
                    }

                    state.navigateTo(Screen.SERVICE_TEMPLATES)
                },
                onOpenWorkSchedule = {
                    val nowMillis = Clock.System.now().toEpochMilliseconds()
                    if (!AccessManager.hasFeature(PremiumFeature.WORK_SCHEDULE, nowMillis)) {
                        state.showPremiumRequired(
                            message = Locales.t("premium_required_work_schedule"),
                            returnTo = Screen.SETTINGS
                        )
                        return@SettingsPage
                    }

                    state.navigateTo(Screen.WORK_SCHEDULE)
                },
                onOpenNotificationSettings = {
                    state.navigateTo(Screen.NOTIFICATION_SETTINGS)
                },
                onOpenAppearanceSettings = {
                    state.navigateTo(Screen.APPEARANCE_SETTINGS)
                },
                onOpenBackupSettings = {
                    val nowMillis = Clock.System.now().toEpochMilliseconds()
                    if (
                        !AccessManager.hasFeature(PremiumFeature.BACKUP_EXPORT, nowMillis) ||
                        !AccessManager.hasFeature(PremiumFeature.BACKUP_IMPORT, nowMillis)
                    ) {
                        state.showPremiumRequired(
                            message = Locales.t("premium_required_backup"),
                            returnTo = Screen.SETTINGS
                        )
                        return@SettingsPage
                    }

                    state.navigateTo(Screen.BACKUP_SETTINGS)
                },
                onOpenDeveloperAccess = {
                    state.navigateTo(Screen.DEVELOPER_ACCESS)
                }
            )

            Screen.AUTH_WELCOME -> AuthWelcomeScreen(
                errorMessage = state.authErrorMessage,
                onContinueWithGoogle = {
                    state.continueWithGoogle()
                },
                onContinueWithApple = {
                    state.continueWithApple()
                },
                onContinueWithEmail = {
                    state.openEmailSignInScreen()
                },
                onRegisterWithEmail = {
                    state.openEmailRegisterScreen()
                },
                onContinueAnonymously = {
                    state.continueAnonymously()
                }
            )

            Screen.AUTH_EMAIL -> AuthEmailScreen(
                isRegisterMode = state.authEmailRegisterMode,
                errorMessage = state.authErrorMessage,
                onSubmit = { email, password, confirmPassword ->
                    state.submitEmailAuth(email, password, confirmPassword)
                },
                onForgotPassword = { email ->
                    state.sendPasswordReset(email)
                }
            )

            Screen.STATS -> {
                val nowMillis = Clock.System.now().toEpochMilliseconds()
                val statsMessage = Locales.t("premium_required_stats")

                if (AccessManager.hasFeature(PremiumFeature.STATS, nowMillis)) {
                    StatsPage(
                        appointments = AppointmentSyncUtils.visibleAppointments(state.appointments),
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
                                    state.showPremiumRequired(
                                        message = statsMessage,
                                        returnTo = Screen.STATS
                                    )
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

            Screen.UNPAID_APPOINTMENTS -> UnpaidAppointmentsScreen(
                appointments = AppointmentSyncUtils.visibleAppointments(state.appointments),
                onConfirmPayment = { appt ->
                    state.confirmDeferredPayment(appt)
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
                            appointments = AppointmentSyncUtils.visibleAppointments(state.appointments),
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
                val pullRefreshState = rememberPullRefreshState(
                    refreshing = state.isRefreshing,
                    onRefresh = { state.manualRefresh() }
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pullRefresh(pullRefreshState)
                    ) {
                        CenteredContentContainer(maxWidth = 980.dp) {
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
                                            state.navigateTo(Screen.DAY_DETAILS)
                                        }
                                    }

                                    item {
                                        Divider(
                                            modifier = Modifier.padding(horizontal = 40.dp, vertical = 16.dp),
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
                                                    .padding(top = 12.dp, bottom = 40.dp),
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
                                                nowDate = state.today,
                                                nowMinutes = nowMin,
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
                    PullRefreshIndicator(
                        refreshing = state.isRefreshing,
                        state = pullRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }

            Screen.DAY_DETAILS -> DayDetailsView(
                date = state.selectedDate,
                appointments = AppointmentSyncUtils.visibleAppointments(state.appointments),
                onDateChange = { state.selectedDate = it },
                onTimeClick = { time ->
                    val nowMillis = Clock.System.now().toEpochMilliseconds()
                    val canCreate = AccessManager.canCreateAppointment(
                        currentAppointmentsCount = state.appointments.size,
                        nowMillis = nowMillis
                    )

                    if (!canCreate) {
                        state.showPremiumRequired(
                            message = Locales.t("premium_required_limit"),
                            returnTo = Screen.DAY_DETAILS
                        )
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
                    state.navigateBack()
                }
            )
            Screen.PREMIUM_ACCESS -> PremiumAccessScreen(
                accessState = state.accessState,
                message = state.premiumRequiredMessage,
                billingUiState = state.billingUiState,
                accountLabel = when {
                    state.currentAuthUser?.email?.isNotBlank() == true -> state.currentAuthUser?.email.orEmpty()
                    state.currentAuthUser?.displayName?.isNotBlank() == true -> state.currentAuthUser?.displayName.orEmpty()
                    else -> Locales.t("billing_account_binding_unknown")
                },
                onBack = {
                    state.closePremiumScreen()
                },
                onContinueFree = {
                    state.closePremiumScreen()
                },
                onUnlockPremium = {
                    state.buyPremium()
                },
                onRestorePurchases = {
                    state.restorePremium()
                }
            )
            Screen.BACKUP_SETTINGS -> BackupSettingsScreen(
                onExport = {
                    val nowMillis = Clock.System.now().toEpochMilliseconds()
                    if (!AccessManager.hasFeature(PremiumFeature.BACKUP_EXPORT, nowMillis)) {
                        state.showPremiumRequired(
                            message = Locales.t("premium_required_export"),
                            returnTo = Screen.BACKUP_SETTINGS
                        )
                        return@BackupSettingsScreen
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
                        state.showPremiumRequired(
                            message = Locales.t("premium_required_import"),
                            returnTo = Screen.BACKUP_SETTINGS
                        )
                        return@BackupSettingsScreen
                    }

                    state.runProtected(
                        title = Locales.t("pin_required"),
                        text = Locales.t("import_requires_pin"),
                        confirmText = Locales.t("confirm")
                    ) {
                        BackupFilePicker.importJson(
                            onPicked = { jsonText ->
                                val parsed = BackupCodec.parseBackupFile(jsonText)
                                if (parsed == null) {
                                    state.showImportError = Locales.t("backup_import_invalid_file")
                                    return@importJson
                                }

                                state.pendingImportText = jsonText
                                state.pendingImportPreview = when (parsed) {
                                    is ParsedBackupFile.LegacyPlainPayload -> {
                                        AppRootState.ImportPreviewInfo(
                                            isLegacy = true,
                                            isEncrypted = false,
                                            version = null,
                                            createdAtEpochMillis = null,
                                            appointmentsCount = DataManager.importBackupPayload(parsed.payloadJson).size
                                        )
                                    }
                                    is ParsedBackupFile.PlainContainer -> {
                                        AppRootState.ImportPreviewInfo(
                                            isLegacy = false,
                                            isEncrypted = false,
                                            version = parsed.container.version,
                                            createdAtEpochMillis = parsed.container.createdAtEpochMillis,
                                            appointmentsCount = parsed.container.appointmentsCount
                                        )
                                    }
                                    is ParsedBackupFile.EncryptedContainer -> {
                                        AppRootState.ImportPreviewInfo(
                                            isLegacy = false,
                                            isEncrypted = true,
                                            version = parsed.container.version,
                                            createdAtEpochMillis = parsed.container.createdAtEpochMillis,
                                            appointmentsCount = parsed.container.appointmentsCount
                                        )
                                    }
                                }
                                state.showImportBackupPrompt = true
                            },
                            onError = { errorText ->
                                state.showImportError = errorText
                            }
                        )
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
                dbOpsAllowed = AppSettings.pinEnabled && AppSettings.isPinSet()
            )
            Screen.DEVELOPER_ACCESS -> DeveloperAccessScreen(
                state = state,
                accessState = state.accessState,
                onEnablePremium = {
                    AppSettings.developerPremiumOverrideEnabled = true
                    AppSettings.persist()
                    state.refreshAccessState()
                },
                onDisablePremium = {
                    AppSettings.developerPremiumOverrideEnabled = false
                    AppSettings.persist()
                    state.refreshAccessState()
                },
                onLogoutDeveloperMode = {
                    AppSettings.lockDeveloperMode()
                    state.navigateBack()
                }
            )
            Screen.NOTIFICATION_SETTINGS -> NotificationsSettingsScreen()
            Screen.SERVICE_TEMPLATES -> ServiceTemplatesScreen()
            Screen.WORK_SCHEDULE -> WorkScheduleScreen()
            Screen.APPEARANCE_SETTINGS -> AppearanceSettingsScreen(state = state)
        }

        if (state.showBookingDialog) {
            BookingDialog(
                time = state.editingAppointment?.time ?: state.selectedTimeSlot,
                initialData = state.editingAppointment ?: state.transferA,
                readOnly = state.bookingReadOnly && state.editingAppointment != null,
                localClientSuggestions = ClientSuggestions.fromAppointments(
                    AppointmentSyncUtils.visibleAppointments(state.appointments)
                ),
                onDismiss = {
                    state.showBookingDialog = false
                    state.editingAppointment = null
                    state.transferA = null
                    state.bookingReadOnly = false
                },
                onSave = { startTime, durationMinutes, name, phone, service, price, currencyCode, notes, paymentDeferred ->
                    val existing = state.editingAppointment ?: state.transferA
                    val id = existing?.id ?: Clock.System.now().toEpochMilliseconds().toString()
                    val targetDate = state.selectedDate.toString()
                    val nowMillis = Clock.System.now().toEpochMilliseconds()

                    val newAppt = Appointment(
                        id = id,
                        dateString = targetDate,
                        time = startTime,
                        clientName = name,
                        phone = phone,
                        serviceName = service,
                        price = price,
                        durationMinutes = durationMinutes,
                        durationHours = ((durationMinutes + 59) / 60).coerceAtLeast(1),
                        notes = notes,
                        paymentDeferred = paymentDeferred,
                        paymentStatus = if (paymentDeferred) {
                            AppointmentPaymentStatus.PAYMENT_LATER.name
                        } else {
                            AppointmentPaymentStatus.PAID.name
                        },
                        updatedAtMillis = nowMillis,
                        isDeleted = existing?.isDeleted ?: false,
                        currency = currencyCode
                    )

                    state.replaceById(newAppt)
                    state.saveAll()

                    state.showBookingDialog = false
                    state.editingAppointment = null
                    state.transferA = null
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
            val apptDate = runCatching { kotlinx.datetime.LocalDate.parse(apptToView.dateString) }.getOrNull()
            val actionsEnabled = apptDate == null || apptDate >= state.today

            AppointmentDetailsDialog(
                appt = apptToView,
                startHm = viewingStartHm,
                endHm = viewingEndHm,
                status = statusToView,
                actionsEnabled = actionsEnabled,
                allowDeletePast = AppSettings.developerModeUnlocked,
                onDismiss = {
                    viewingAppt = null
                    viewingStatus = null
                },
                onEditClick = {
                    if (!actionsEnabled) return@AppointmentDetailsDialog
                    viewingAppt = null
                    viewingStatus = null
                    state.editingAppointment = apptToView
                    state.bookingReadOnly = false
                    state.showBookingDialog = true
                },
                onTransferClick = {
                    if (!actionsEnabled) return@AppointmentDetailsDialog
                    viewingAppt = null
                    viewingStatus = null
                    state.transferA = apptToView
                    state.showTransferPickDialog = true
                    state.bookingReadOnly = false
                },
                onDeleteClick = {
                    if (!(actionsEnabled || AppSettings.developerModeUnlocked)) {
                        return@AppointmentDetailsDialog
                    }
                    viewingAppt = null
                    viewingStatus = null
                    state.showDeleteConfirm = apptToView
                }
            )
        }
    }
}
//Create new Animation_screen fix11