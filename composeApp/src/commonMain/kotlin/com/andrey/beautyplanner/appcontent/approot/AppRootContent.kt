package com.andrey.beautyplanner.appcontent.approot

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Card
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
import kotlin.math.abs
import kotlinx.coroutines.launch
import androidx.compose.material.Button
import com.andrey.beautyplanner.appcontent.ServiceTemplatesScreen
import com.andrey.beautyplanner.appcontent.WorkScheduleScreen
import com.andrey.beautyplanner.appcontent.AppearanceSettingsScreen
import com.andrey.beautyplanner.appcontent.DeveloperAccessScreen
import com.andrey.beautyplanner.appcontent.BackupSettingsScreen
import androidx.compose.runtime.saveable.rememberSaveable
import com.andrey.beautyplanner.appcontent.AuthWelcomeScreen
import com.andrey.beautyplanner.appcontent.AuthEmailScreen
import com.andrey.beautyplanner.appcontent.GuestAccountRegistrationScreen
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import com.andrey.beautyplanner.auth.SignInProvider
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.platform.LocalDensity

private const val APPOINTMENT_MANAGE_GRACE_PERIOD_MILLIS = 24L * 60L * 60L * 1000L

private fun canManageAppointment(
    appointment: Appointment,
    nowMillis: Long = Clock.System.now().toEpochMilliseconds()
): Boolean {
    val appointmentDate = runCatching { kotlinx.datetime.LocalDate.parse(appointment.dateString) }.getOrNull() ?: return true
    val timeParts = appointment.time.split(":")
    val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 0
    val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

    val appointmentStart = runCatching {
        kotlinx.datetime.LocalDateTime(
            year = appointmentDate.year,
            monthNumber = appointmentDate.monthNumber,
            dayOfMonth = appointmentDate.dayOfMonth,
            hour = hour,
            minute = minute
        ).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    }.getOrNull() ?: return true

    return nowMillis <= appointmentStart + APPOINTMENT_MANAGE_GRACE_PERIOD_MILLIS
}
@OptIn(androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun AppRootContent(
    state: AppRootState,
    padding: PaddingValues
) {
    var pendingPinAfterSplash by rememberSaveable { mutableStateOf(false) }
    var showStartupLoader by rememberSaveable { mutableStateOf(false) }
    var loaderStartedAtMillis by rememberSaveable { mutableStateOf(0L) }
    val ownerName = AppSettings.ownerName.trim()

    var viewingAppt by remember { mutableStateOf<Appointment?>(null) }
    var viewingStartHm by remember { mutableStateOf("") }
    var viewingEndHm by remember { mutableStateOf("") }
    var viewingStatus by remember { mutableStateOf<LiveStatusKey?>(null) }

    if (!state.hasCompletedInitialSplash) {
        AnimatedSplashScreen(
            ownerName = ownerName,
            onAnimationFinished = {
                state.hasCompletedInitialSplash = true
                showStartupLoader = true
                loaderStartedAtMillis = Clock.System.now().toEpochMilliseconds()

                if (state.mustCreatePin || (state.locked && !state.mustCreatePin)) {
                    pendingPinAfterSplash = true
                }
            }
        )
        return
    }

    LaunchedEffect(state.hasCompletedInitialSplash, state.authResolved, showStartupLoader) {
        if (state.hasCompletedInitialSplash && showStartupLoader && state.authResolved) {
            val now = Clock.System.now().toEpochMilliseconds()
            val elapsed = now - loaderStartedAtMillis
            val minLoaderDurationMillis = 2_000L
            val remaining = (minLoaderDurationMillis - elapsed).coerceAtLeast(0L)

            if (remaining > 0L) {
                delay(remaining)
            }

            showStartupLoader = false
        }
    }

    if (showStartupLoader || !state.authResolved) {
        val hasSavedAuthenticatedSession =
            AppSettings.localProfileUserId.isNotBlank() &&
                    AppSettings.lastAuthProvider.isNotBlank()

        val loaderSubtitle =
            if (hasSavedAuthenticatedSession) {
                Locales.t("session_loading_subtitle")
            } else {
                Locales.t("session_loading_guest_subtitle")
            }

        SessionLoadingScreen(subtitle = loaderSubtitle)
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
                onOpenPersonalInfoSettings = {
                    state.navigateTo(Screen.PERSONAL_INFO_SETTINGS)
                },
                onOpenBackupSettings = {
                    if (state.currentAuthUser?.provider == SignInProvider.ANONYMOUS) {
                        state.showPremiumRequired(
                            message = Locales.t("backup_guest_requires_account"),
                            returnTo = Screen.SETTINGS
                        )
                        return@SettingsPage
                    }

                    val nowMillis = Clock.System.now().toEpochMilliseconds()
                    if (
                        !AccessManager.hasFeature(PremiumFeature.BACKUP_EXPORT, nowMillis) ||
                        !AccessManager.hasFeature(PremiumFeature.BACKUP_IMPORT, nowMillis)
                    ) {
                        state.showPremiumRequired(
                            message = Locales.t("premium_required_import"),
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
                infoMessage = state.authInfoMessage,
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
                    state.openEmailSignInScreen()
                },
                onContinueAnonymously = {
                    state.continueAnonymously()
                }
            )

            Screen.AUTH_EMAIL -> AuthEmailScreen(
                isRegisterMode = state.authEmailRegisterMode,
                errorMessage = state.authErrorMessage,
                infoMessage = state.authInfoMessage,
                onModeChange = { isRegister ->
                    state.authEmailRegisterMode = isRegister
                    state.authErrorMessage = null
                    state.authInfoMessage = null
                },
                onSubmit = { email, password, confirmPassword ->
                    state.submitEmailAuth(email, password, confirmPassword)
                },
                onForgotPassword = { email ->
                    state.sendPasswordReset(email)
                }
            )

            Screen.GUEST_ACCOUNT_REGISTRATION -> GuestAccountRegistrationScreen(
                onContinueWithGoogle = { state.continueWithGoogle() },
                onContinueWithApple = { state.continueWithApple() },
                onRegisterWithEmail = {
                    state.authEmailRegisterMode = true
                    state.screenHistory = emptyList()
                    state.currentScreen = Screen.AUTH_EMAIL
                },
                onBack = {
                    state.screenHistory = emptyList()
                    state.currentScreen = Screen.MONTH
                }
            )

            Screen.STATS -> {
                val premiumEnabled =
                    state.accessState.tier == AccessTier.PREMIUM ||
                            state.accessState.isTrialActive

                StatsPage(
                    appointments = AppointmentSyncUtils.visibleAppointments(state.appointments),
                    today = state.today,
                    premiumEnabled = premiumEnabled,
                    onOpenPremium = {
                        state.showPremiumRequired(
                            message = Locales.t("premium_required_stats"),
                            returnTo = Screen.STATS
                        )
                    }
                )
            }

            Screen.FEEDBACK -> FeedbackPage(
                aboutDescriptionRaw = AppSettings.aboutDescription,
                aboutUpcomingRaw = AppSettings.aboutUpcoming,
                updateStatus = state.appUpdateStatus,
                isCheckingUpdates = state.isCheckingAppUpdates,
                onCheckUpdatesClick = {
                    state.checkForAppUpdates()
                },
                onOpenUserGuide = {
                    state.navigateTo(Screen.USER_GUIDE)
                }
            )

            Screen.USER_GUIDE -> UserGuideScreen()

            Screen.UNPAID_APPOINTMENTS -> {
                val premiumEnabled =
                    state.accessState.tier == AccessTier.PREMIUM ||
                            state.accessState.isTrialActive

                UnpaidAppointmentsScreen(
                    appointments = AppointmentSyncUtils.visibleAppointments(state.appointments),
                    onConfirmPayment = { appt ->
                        state.confirmDeferredPayment(appt)
                    },
                    premiumEnabled = premiumEnabled,
                    onOpenPremium = {
                        state.showPremiumRequired(
                            message = Locales.t("premium_required_default"),
                            returnTo = Screen.UNPAID_APPOINTMENTS
                        )
                    }
                )
            }
            // =========================================================
            // TEMP HIDE FOR APP REVIEW: CLIENT INTERACTIONS SCREEN
            // Экран временно скрыт из пользовательского UI.
            // Логику и файл экрана не удалять.
            // BEGIN TEMP HIDE
            // =========================================================
            Screen.CLIENT_INTERACTIONS -> {
             //   val nowMillis = Clock.System.now().toEpochMilliseconds()
             //   if (!AccessManager.hasFeature(PremiumFeature.STATS, nowMillis)) {
              //      state.showPremiumRequired(
              //          message = Locales.t("premium_required_client_interactions"),
              //          returnTo = Screen.MONTH
              //      )
              //  } else {
              //      ClientInteractionsScreen(appState = state)
             //   }
            }
            // =========================================================
            // END TEMP HIDE FOR APP REVIEW: CLIENT INTERACTIONS SCREEN
            // =========================================================

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
                val activeAppointmentsCount = getUpcomingAppointmentsCount(
                    appointments = AppointmentSyncUtils.visibleAppointments(state.appointments),
                    today = state.today,
                    nowTime = nowTimeHm
                )

                val appointmentLimitNotice = run {
                    val nowMs = Clock.System.now().toEpochMilliseconds()
                    val isPremiumActive = AccessManager.isPremiumAccessActive(nowMs)
                    val isEffectivelyFreeLimited = !isPremiumActive && !state.accessState.isTrialActive

                    if (!isEffectivelyFreeLimited) {
                        null
                    } else {
                        val remainingSlots = AccessManager.getRemainingFreeSlots(
                            currentAppointmentsCount = activeAppointmentsCount,
                            nowMillis = nowMs
                        )
                        if (AccessManager.shouldShowFreeLimitWarning(remainingSlots)) {
                            Locales.t("premium_free_limit_slots_warning")
                                .replace("{count}", remainingSlots.toString())
                        } else {
                            null
                        }
                    }
                }

                val listState = rememberLazyListState()

                val upcoming by remember(
                    nowTimeHm,
                    state.today,
                    state.appointments.size,
                    state.accessState.tier,
                    state.accessState.hasPremium,
                    state.accessState.isTrialActive
                ) {
                    derivedStateOf {
                        val upcomingAll = getUpcomingAppointments(
                            appointments = AppointmentSyncUtils.visibleAppointments(state.appointments),
                            today = state.today,
                            nowTime = nowTimeHm
                        )

                        val isPremiumActive = AccessManager.isPremiumAccessActive(
                            Clock.System.now().toEpochMilliseconds()
                        )

                        val shouldLimitUpcomingOnHome =
                            !isPremiumActive && !state.accessState.isTrialActive

                        if (shouldLimitUpcomingOnHome) {
                            upcomingAll.take(AccessManager.FREE_ACTIVE_APPOINTMENTS_LIMIT)
                        } else {
                            upcomingAll
                        }
                    }
                }

                val calendarCollapseThresholdPx = 40

                val isCollapsed by remember(listState) {
                    derivedStateOf {
                        listState.firstVisibleItemScrollOffset >= calendarCollapseThresholdPx
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
                                        if (!appointmentLimitNotice.isNullOrBlank()) {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                                backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.10f),
                                                elevation = 0.dp
                                            ) {
                                                Text(
                                                    text = appointmentLimitNotice,
                                                    modifier = Modifier.padding(14.dp),
                                                    fontSize = (14 * state.fontScale).sp,
                                                    color = MaterialTheme.colors.onSurface
                                                )
                                            }
                                        }
                                    }

                                    item {
                                        MonthCalendarGrid(
                                            monthDate = state.calendarViewDate,
                                            today = state.today,
                                            selectedDate = state.selectedDate,
                                            appointments = AppointmentSyncUtils.visibleAppointments(state.appointments)
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

                    val currentActiveAppointmentsCount = getUpcomingAppointmentsCount(
                        appointments = AppointmentSyncUtils.visibleAppointments(state.appointments),
                        today = state.today,
                        nowTime = getCurrentTimeHm()
                    )

                    val canCreate = AccessManager.canCreateAppointment(
                        currentAppointmentsCount = currentActiveAppointmentsCount,
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
                    state.currentAuthUser?.provider == SignInProvider.ANONYMOUS ->
                        Locales.t("premium_guest_account_label")

                    state.currentAuthUser?.email?.isNotBlank() == true ->
                        state.currentAuthUser?.email ?: ""

                    state.currentAuthUser?.displayName?.isNotBlank() == true ->
                        state.currentAuthUser?.displayName ?: ""

                    else ->
                        Locales.t("billing_account_binding_unknown")
                },
                isGuestUser = state.currentAuthUser?.provider == SignInProvider.ANONYMOUS,
                onContinueFree = {
                    state.closePremiumScreen()
                },
                onUnlockPremium = {
                    state.buyPremium()
                },
                onRestorePurchases = {
                    state.restorePremium()
                },
                onOpenPrivacyPolicy = {
                    state.navigateTo(Screen.PRIVACY_POLICY)
                }
            )
            Screen.BACKUP_SETTINGS -> BackupSettingsScreen(
                onExport = {
                    if (state.currentAuthUser?.provider == SignInProvider.ANONYMOUS) {
                        state.showPremiumRequired(
                            message = Locales.t("backup_guest_requires_account"),
                            returnTo = Screen.BACKUP_SETTINGS
                        )
                        return@BackupSettingsScreen
                    }
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
                    if (state.currentAuthUser?.provider == SignInProvider.ANONYMOUS) {
                        state.showPremiumRequired(
                            message = Locales.t("backup_guest_requires_account"),
                            returnTo = Screen.BACKUP_SETTINGS
                        )
                        return@BackupSettingsScreen
                    }
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
                    val currentUserId = state.currentAuthUser?.uid?.trim().orEmpty()
                    if (currentUserId.isBlank() || state.currentAuthUser?.provider == SignInProvider.ANONYMOUS) {
                        return@DeveloperAccessScreen
                    }

                    AppSettings.developerPremiumOverrideEnabled = true
                    AppSettings.developerPremiumOverrideOwnerUserId = currentUserId
                    AppSettings.persist()
                    state.refreshAccessState()
                },
                onDisablePremium = {
                    AppSettings.developerPremiumOverrideEnabled = false
                    AppSettings.developerPremiumOverrideOwnerUserId = ""
                    com.andrey.beautyplanner.access.AccessRepository.clearLocalPremiumState(
                        blockAutoFallback = true
                    )
                    AppSettings.persist()
                    state.refreshAccessState()
                },
                onResetTrialToNow = {
                    val now = Clock.System.now().toEpochMilliseconds()
                    val trialEnds = now + 14L * 24L * 60L * 60L * 1000L

                    com.andrey.beautyplanner.access.AccessRepository.clearLocalPremiumState(
                        blockAutoFallback = true
                    )

                    AppSettings.trialStartedAtMillis = now
                    AppSettings.cachedAccessTier = "TRIAL"
                    AppSettings.cachedTrialEndsAtMillis = trialEnds
                    AppSettings.cachedHasPremium = false
                    AppSettings.cachedSubscriptionState = "NONE"
                    AppSettings.persist()
                    state.refreshAccessState(now)
                },
                onExpireTrial = {
                    val now = Clock.System.now().toEpochMilliseconds()

                    com.andrey.beautyplanner.access.AccessRepository.clearLocalPremiumState(
                        blockAutoFallback = true
                    )

                    AppSettings.trialStartedAtMillis = 0L
                    AppSettings.cachedAccessTier = "FREE_LIMITED"
                    AppSettings.cachedTrialEndsAtMillis = 0L
                    AppSettings.cachedHasPremium = false
                    AppSettings.cachedSubscriptionState = "NONE"
                    AppSettings.persist()
                    state.refreshAccessState(now)
                },
                onLogoutDeveloperMode = {
                    AppSettings.lockDeveloperMode()
                    state.navigateBack()
                }
            )
            Screen.ARCHIVE -> {
                val premiumEnabled =
                    state.accessState.tier == AccessTier.PREMIUM ||
                            state.accessState.isTrialActive

                ArchivePage(
                    appointments = AppointmentSyncUtils.visibleAppointments(state.appointments),
                    premiumEnabled = premiumEnabled,
                    onOpenPremium = {
                        state.showPremiumRequired(
                            message = Locales.t("premium_required_archive"),
                            returnTo = Screen.ARCHIVE
                        )
                    }
                )
            }
            Screen.NOTIFICATION_SETTINGS -> NotificationsSettingsScreen()
            Screen.SERVICE_TEMPLATES -> ServiceTemplatesScreen()
            Screen.WORK_SCHEDULE -> WorkScheduleScreen()
            Screen.APPEARANCE_SETTINGS -> AppearanceSettingsScreen(state = state)
            Screen.PERSONAL_INFO_SETTINGS -> PersonalInfoSettingsScreen(appState = state)
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
                    val isNewAppointment = existing == null

                    if (isNewAppointment) {
                        val currentActiveAppointmentsCount = getUpcomingAppointmentsCount(
                            appointments = AppointmentSyncUtils.visibleAppointments(state.appointments),
                            today = state.today,
                            nowTime = getCurrentTimeHm()
                        )

                        val canCreate = AccessManager.canCreateAppointment(
                            currentAppointmentsCount = currentActiveAppointmentsCount,
                            nowMillis = nowMillis
                        )

                        if (!canCreate) {
                            state.showBookingDialog = false
                            state.editingAppointment = null
                            state.transferA = null
                            state.bookingReadOnly = false
                            state.showPremiumRequired(
                                message = Locales.t("premium_required_limit"),
                                returnTo = Screen.DAY_DETAILS
                            )
                            return@BookingDialog
                        }
                    }

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
                        currency = currencyCode,
                        bookingSource = existing?.bookingSource ?: "manual"
                    )

                    state.replaceById(newAppt)
                    state.saveAll()

                    if (isNewAppointment) {
                        val nowForAccess = Clock.System.now().toEpochMilliseconds()
                        val isPremiumActive = AccessManager.isPremiumAccessActive(nowForAccess)
                        val isTrialActive = state.accessState.isTrialActive

                        val newActiveCount = getUpcomingAppointmentsCount(
                            appointments = AppointmentSyncUtils.visibleAppointments(state.appointments),
                            today = state.today,
                            nowTime = getCurrentTimeHm()
                        )

                        if (isPremiumActive) {
                            state.freeLimitPopupMessage = null
                        } else if (!isTrialActive) {
                            val remaining =
                                (AccessManager.FREE_ACTIVE_APPOINTMENTS_LIMIT - newActiveCount).coerceAtLeast(0)

                            state.freeLimitPopupMessage =
                                if (newActiveCount >= AccessManager.FREE_ACTIVE_APPOINTMENTS_LIMIT) {
                                    Locales.t("free_limit_popup_limit_reached_free")
                                } else {
                                    Locales.t("free_limit_popup_slots_remaining_free")
                                        .replace("{count}", remaining.toString())
                                }
                        } else {
                            val threshold = AccessManager.getFreeLimitPopupThreshold(newActiveCount)
                            if (threshold != null) {
                                state.freeLimitPopupMessage = when (threshold) {
                                    1 -> Locales.t("free_limit_popup_after_first_trial")

                                    AccessManager.FREE_ACTIVE_APPOINTMENTS_LIMIT ->
                                        Locales.t("free_limit_popup_limit_reached_trial")

                                    else -> {
                                        val remaining =
                                            AccessManager.FREE_ACTIVE_APPOINTMENTS_LIMIT - newActiveCount
                                        Locales.t("free_limit_popup_slots_remaining_trial")
                                            .replace("{count}", remaining.toString())
                                    }
                                }
                            }
                        }
                    }

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
            val actionsEnabled =
                if (AppSettings.developerModeUnlocked) {
                    true
                } else {
                    canManageAppointment(apptToView)
                }

            AppointmentDetailsDialog(
                appt = apptToView,
                startHm = viewingStartHm,
                endHm = viewingEndHm,
                status = statusToView,
                actionsEnabled = actionsEnabled,
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
                    if (!actionsEnabled) {
                        return@AppointmentDetailsDialog
                    }
                    viewingAppt = null
                    viewingStatus = null
                    state.showDeleteConfirm = apptToView
                }
            )
        }

        val popupMessage = state.freeLimitPopupMessage
        if (popupMessage != null) {
            val popupScrollState = rememberScrollState()

            androidx.compose.material.AlertDialog(
                onDismissRequest = { state.freeLimitPopupMessage = null },
                title = {
                    Text(
                        text = Locales.t("free_limit_popup_title"),
                        fontSize = (17 * state.fontScale).sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(popupScrollState)
                                .padding(end = 4.dp)
                        ) {
                            Text(
                                text = popupMessage,
                                fontSize = (14 * state.fontScale).sp,
                                lineHeight = (20 * state.fontScale).sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (popupScrollState.maxValue > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(28.dp)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colors.surface.copy(alpha = 0f),
                                                MaterialTheme.colors.surface.copy(alpha = 0.92f)
                                            )
                                        )
                                    )
                            )
                        }
                    }
                },
                confirmButton = {
                    androidx.compose.material.TextButton(
                        onClick = { state.freeLimitPopupMessage = null }
                    ) {
                        Text(Locales.t("close"))
                    }
                },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            )
        }
    }
}
//Create new Animation_screen fix11