package com.andrey.beautyplanner.appcontent.approot

import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.*
import com.andrey.beautyplanner.billing.*
import com.andrey.beautyplanner.notifications.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import com.andrey.beautyplanner.appcontent.appFontFamily
import com.andrey.beautyplanner.auth.AuthGateway
import com.andrey.beautyplanner.auth.SignInProvider
import com.andrey.beautyplanner.auth.SignInResult
import com.andrey.beautyplanner.auth.AuthUser
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.andrey.beautyplanner.remote.MasterScheduleSync
import com.andrey.beautyplanner.AppointmentSyncUtils
import com.andrey.beautyplanner.appcontent.getUpcomingAppointments

@Stable
class AppRootState(
    val appointments: SnapshotStateList<Appointment>,
    val today: LocalDate,
    val drawerState: DrawerState,
    private val scope: CoroutineScope,
) {
    private val billingManager = BillingManager()
    private val authenticatedSessionTimeoutMillis =
        7L * 24L * 60L * 60L * 1000L

    var cloudSyncInProgress by mutableStateOf(false)
        private set

    private val cloudSyncMutex = Mutex()

    var currentAuthUser by mutableStateOf<AuthUser?>(null)

    var currentScreen by mutableStateOf(Screen.AUTH_WELCOME)

    var accessState by mutableStateOf(
        AccessManager.getAccessState(
            nowMillis = Clock.System.now().toEpochMilliseconds()
        )
    )

    var billingUiState by mutableStateOf(
        BillingUiState(
            ownedPremium = AppSettings.premiumUnlocked
        )
    )
    var authResolved by mutableStateOf(false)
    var authErrorMessage by mutableStateOf<String?>(null)
    var authInfoMessage by mutableStateOf<String?>(null)
    var authEmailRegisterMode by mutableStateOf(false)
    var authInfoDialogMessage by mutableStateOf<String?>(null)
    var hasCompletedInitialSplash by mutableStateOf(false)

    var backupEncryptEnabled by mutableStateOf(true)
    var backupPassword by mutableStateOf("")
    var backupPasswordConfirm by mutableStateOf("")
    var backupPasswordError by mutableStateOf<String?>(null)

    var pendingEncryptedImportText by mutableStateOf<String?>(null)
    var showImportPasswordDialog by mutableStateOf(false)
    var importPassword by mutableStateOf("")
    var importPasswordError by mutableStateOf<String?>(null)

    var calendarViewDate by mutableStateOf(LocalDate(today.year, today.month, 1))
    var selectedDate by mutableStateOf(today)

    var showBookingDialog by mutableStateOf(false)
    var showDeleteConfirm by mutableStateOf<Appointment?>(null)
    var selectedTimeSlot by mutableStateOf("")
    var editingAppointment by mutableStateOf<Appointment?>(null)
    var bookingReadOnly by mutableStateOf(false)

    var transferA by mutableStateOf<Appointment?>(null)
    var showTransferPickDialog by mutableStateOf(false)

    var showTransferConflictConfirm by mutableStateOf(false)
    var conflictB by mutableStateOf<Appointment?>(null)
    var pendingTargetDate by mutableStateOf<LocalDate?>(null)
    var pendingTargetTime by mutableStateOf("")

    var showRescheduleBDialog by mutableStateOf(false)

    var showExportNameDialog by mutableStateOf(false)
    var exportFileName by mutableStateOf("beautyplanner-backup")

    var pendingImportText by mutableStateOf<String?>(null)
    var pendingImportPreview by mutableStateOf<ImportPreviewInfo?>(null)
    var showImportConfirm by mutableStateOf(false)
    var showImportError by mutableStateOf<String?>(null)
    var showImportBackupPrompt by mutableStateOf(false)
    var pendingImportAfterBackup by mutableStateOf(false)
    var backupSuccessMessage by mutableStateOf<String?>(null)
    var premiumRequiredMessage by mutableStateOf("")
    var premiumReturnScreen by mutableStateOf(Screen.SETTINGS)

    var freeLimitPopupMessage by mutableStateOf<String?>(null)
    var hasShownTrialEndedFreeModePopup by mutableStateOf(false)

    var mustCreatePin by mutableStateOf(false)
    var locked by mutableStateOf(AppSettings.pinEnabled && AppSettings.isPinSet())

    var showPinDialog by mutableStateOf(false)
    var pinDialogTitle by mutableStateOf("")
    var pinDialogText by mutableStateOf("")
    var pinDialogConfirmText by mutableStateOf("")
    var pinDialogOnSuccess by mutableStateOf<(() -> Unit)?>(null)
    var pinErrorText by mutableStateOf<String?>(null)

    var showSetPinDialog by mutableStateOf(false)
    var showRemovePinConfirm by mutableStateOf(false)

    var showClearDbBackupPrompt by mutableStateOf(false)
    var showClearDbFinalConfirm by mutableStateOf(false)

    var showSaveError by mutableStateOf<String?>(null)

    var isGlobalLoading by mutableStateOf(false)
    var globalLoadingMessage by mutableStateOf<String?>(null)
    var isRefreshing by mutableStateOf(false)
    var isCheckingAppUpdates by mutableStateOf(false)

    data class ShiftItem(val apptId: String, val newStartMin: Int)
    data class ImportPreviewInfo(
        val isLegacy: Boolean,
        val isEncrypted: Boolean,
        val version: Int?,
        val createdAtEpochMillis: Long?,
        val appointmentsCount: Int?
    )
    var showAutoShiftConfirm by mutableStateOf(false)
    var pendingNewAppt by mutableStateOf<Appointment?>(null)
    var shiftChain by mutableStateOf<List<ShiftItem>>(emptyList())
    var shiftBlockedApptId by mutableStateOf<String?>(null)

    var currentLiveDarkMode by mutableStateOf(AppSettings.isDarkMode)

    var appUpdateStatus by mutableStateOf(
        AppUpdateStatus(
            checked = AppSettings.lastUpdateCheckAtMillis > 0L,
            updateAvailable = AppSettings.lastKnownUpdateAvailable,
            latestVersion = AppSettings.lastKnownLatestVersion,
            latestBuild = AppSettings.lastKnownLatestBuild,
            storeUrl = AppSettings.lastKnownStoreUrl,
            errorMessage = ""
        )
    )

    enum class GuestDiscardAction {
        SIGN_OUT,
        SWITCH_ACCOUNT
    }

    data class GuestMigrationSnapshot(
        val appointments: List<Appointment>,
        val ownerName: String,
        val profilePhone: String,
        val profilePhoneVisible: Boolean,
        val profileRating: Float,
        val profileAvatarUrl: String,
        val profileAvatarBase64: String,
        val profileAvatarStoragePath: String,
        val profileDisplayCustomName: Boolean,
        val profileSpecialization: String,
        val clientInteractionsEnabled: Boolean,
        val autoPublishBusySlots: Boolean,
        val serviceTemplates: List<ServiceTemplate>,
        val weeklyBlockedIntervals: List<WeeklyBlockedInterval>,
        val scheduleDateOverrides: List<ScheduleDateOverride>
    )

    var guestUpgradeMode by mutableStateOf(false)
    var showGuestDataLossDialog by mutableStateOf(false)
    var pendingGuestDiscardAction by mutableStateOf<GuestDiscardAction?>(null)

    val colors: Colors
        get() = if (currentLiveDarkMode) {
            darkColors(
                primary = Color(0xFF8AB4F8),
                background = Color(0xFF121212),
                surface = Color(0xFF1E1E1E),
                onPrimary = Color.Black,
                onSurface = Color.White,
                onBackground = Color.White
            )
        } else {
            lightColors(
                primary = Color(0xFF4285F4),
                background = Color.White,
                surface = Color.White,
                onPrimary = Color.White,
                onSurface = Color.Black,
                onBackground = Color.Black
            )
        }

    var fontScale by mutableStateOf(AppSettings.getFontScale())

    val customTypography: Typography
        @Composable
        get() {
            val fontFamily = appFontFamily()
            return Typography(
                defaultFontFamily = fontFamily,
                h5 = TextStyle(
                    fontFamily = fontFamily,
                    fontSize = (24 * fontScale).sp,
                    fontWeight = FontWeight.Bold
                ),
                h6 = TextStyle(
                    fontFamily = fontFamily,
                    fontSize = (20 * fontScale).sp,
                    fontWeight = FontWeight.Bold
                ),
                subtitle1 = TextStyle(
                    fontFamily = fontFamily,
                    fontSize = (14 * fontScale).sp,
                    fontWeight = FontWeight.Medium
                ),
                subtitle2 = TextStyle(
                    fontFamily = fontFamily,
                    fontSize = (13 * fontScale).sp,
                    fontWeight = FontWeight.Medium
                ),
                body1 = TextStyle(
                    fontFamily = fontFamily,
                    fontSize = (16 * fontScale).sp
                ),
                body2 = TextStyle(
                    fontFamily = fontFamily,
                    fontSize = (14 * fontScale).sp
                ),
                button = TextStyle(
                    fontFamily = fontFamily,
                    fontSize = (14 * fontScale).sp,
                    fontWeight = FontWeight.Medium
                ),
                caption = TextStyle(
                    fontFamily = fontFamily,
                    fontSize = (12 * fontScale).sp
                )
            )
        }

    fun reloadAppointmentsForProfile(profileKey: String) {
        CloudSyncLogger.log("reloadAppointmentsForProfile: profileKey=$profileKey")

        val loaded = runCatching {
            DataManager.loadFromDatabase(profileKey)
        }.getOrElse {
            CloudSyncLogger.log("reloadAppointmentsForProfile: failed: ${it.message}")
            emptyList()
        }

        appointments.clear()
        appointments.addAll(loaded)

        CloudSyncLogger.log("reloadAppointmentsForProfile: loaded=${loaded.size}")
    }

    fun refreshBillingLocalization() {
        billingUiState = billingUiState.copy(errorMessage = null)
        initBilling()
    }

    fun reloadAppointmentsForCurrentProfile() {
        reloadAppointmentsForProfile(LocalProfileManager.currentProfileKey())
    }

    fun reloadAppointmentsForGuestProfile() {
        reloadAppointmentsForProfile(LocalProfileManager.guestProfileKey())
    }

    fun hasGuestDataToPreserve(): Boolean {
        val guestAppointments = runCatching {
            DataManager.loadFromDatabase(LocalProfileManager.guestProfileKey())
        }.getOrDefault(emptyList())

        val hasAppointments = guestAppointments.isNotEmpty()

        val hasProfileDraft =
            AppSettings.ownerName.isNotBlank() ||
                    AppSettings.profilePhone.isNotBlank() ||
                    AppSettings.profileSpecialization.isNotBlank() ||
                    AppSettings.profileDisplayCustomName ||
                    AppSettings.profileAvatarUrl.isNotBlank() ||
                    AppSettings.profileAvatarBase64.isNotBlank() ||
                    AppSettings.profileAvatarStoragePath.isNotBlank()

        val hasCustomServices = AppSettings.serviceTemplates.isNotEmpty()
        val hasScheduleData =
            AppSettings.weeklyBlockedIntervals.isNotEmpty() ||
                    AppSettings.scheduleDateOverrides.isNotEmpty()

        return hasAppointments || hasProfileDraft || hasCustomServices || hasScheduleData
    }

    fun openGuestAccountRegistrationScreen() {
        guestUpgradeMode = true
        authErrorMessage = null
        authInfoMessage = null
        screenHistory = emptyList()
        currentScreen = Screen.GUEST_ACCOUNT_REGISTRATION
    }

    fun requestGuestSignOut() {
        if (hasGuestDataToPreserve()) {
            pendingGuestDiscardAction = GuestDiscardAction.SIGN_OUT
            showGuestDataLossDialog = true
        } else {
            discardGuestDataAndOpenAuth()
        }
    }

    fun requestGuestSwitchAccount() {
        if (hasGuestDataToPreserve()) {
            pendingGuestDiscardAction = GuestDiscardAction.SWITCH_ACCOUNT
            showGuestDataLossDialog = true
        } else {
            discardGuestDataAndOpenAuth()
        }
    }

    fun cancelGuestDataLossDialog() {
        showGuestDataLossDialog = false
        pendingGuestDiscardAction = null
    }

    fun confirmGuestDataDiscard() {
        showGuestDataLossDialog = false
        pendingGuestDiscardAction = null
        discardGuestDataAndOpenAuth()
    }

    private fun discardGuestDataAndOpenAuth() {
        clearSessionLocalState()

        runCatching {
            DataManager.saveToDatabase(
                data = emptyList(),
                profileKey = LocalProfileManager.guestProfileKey()
            )
        }

        AppSettings.clearMasterProfileLocalState(clearMasterData = true)
        AppSettings.trialStartedAtMillis = 0L
        AppSettings.persist()

        reloadAppointmentsForGuestProfile()
        authErrorMessage = null
        authInfoMessage = null
        screenHistory = emptyList()
        currentScreen = Screen.AUTH_WELCOME
    }

    private fun captureGuestMigrationSnapshot(): GuestMigrationSnapshot {
        val guestAppointments = runCatching {
            DataManager.loadFromDatabase(LocalProfileManager.guestProfileKey())
        }.getOrDefault(emptyList())

        return GuestMigrationSnapshot(
            appointments = guestAppointments,
            ownerName = AppSettings.ownerName,
            profilePhone = AppSettings.profilePhone,
            profilePhoneVisible = AppSettings.profilePhoneVisible,
            profileRating = AppSettings.profileRating,
            profileAvatarUrl = AppSettings.profileAvatarUrl,
            profileAvatarBase64 = AppSettings.profileAvatarBase64,
            profileAvatarStoragePath = AppSettings.profileAvatarStoragePath,
            profileDisplayCustomName = AppSettings.profileDisplayCustomName,
            profileSpecialization = AppSettings.profileSpecialization,
            clientInteractionsEnabled = AppSettings.clientInteractionsEnabled,
            autoPublishBusySlots = AppSettings.autoPublishBusySlots,
            serviceTemplates = AppSettings.serviceTemplates,
            weeklyBlockedIntervals = AppSettings.weeklyBlockedIntervals,
            scheduleDateOverrides = AppSettings.scheduleDateOverrides
        )
    }

    private fun applyGuestSnapshotToCurrentAuthenticatedProfile(
        snapshot: GuestMigrationSnapshot,
        userId: String
    ) {
        AppSettings.localProfileUserId = userId
        AppSettings.ownerName = snapshot.ownerName
        AppSettings.profilePhone = snapshot.profilePhone
        AppSettings.profilePhoneVisible = snapshot.profilePhoneVisible
        AppSettings.profileRating = snapshot.profileRating
        AppSettings.profileAvatarUrl = snapshot.profileAvatarUrl
        AppSettings.profileAvatarBase64 = snapshot.profileAvatarBase64
        AppSettings.profileAvatarStoragePath = snapshot.profileAvatarStoragePath
        AppSettings.profileDisplayCustomName = snapshot.profileDisplayCustomName
        AppSettings.profileSpecialization = snapshot.profileSpecialization
        AppSettings.clientInteractionsEnabled = snapshot.clientInteractionsEnabled
        AppSettings.autoPublishBusySlots = snapshot.autoPublishBusySlots
        AppSettings.serviceTemplates = snapshot.serviceTemplates
        AppSettings.weeklyBlockedIntervals = snapshot.weeklyBlockedIntervals
        AppSettings.scheduleDateOverrides = snapshot.scheduleDateOverrides
        AppSettings.persist()

        val userProfileKey = LocalProfileManager.profileKeyForUser(userId)

        runCatching {
            DataManager.saveToDatabase(
                data = snapshot.appointments,
                profileKey = userProfileKey
            )
        }

        appointments.clear()
        appointments.addAll(snapshot.appointments)
    }

    private fun clearGuestStorageAfterMigration() {
        runCatching {
            DataManager.saveToDatabase(
                data = emptyList(),
                profileKey = LocalProfileManager.guestProfileKey()
            )
        }
    }

    fun showGlobalLoading(message: String? = null) {
        globalLoadingMessage = message
        isGlobalLoading = true
    }

    fun hideGlobalLoading() {
        isGlobalLoading = false
        globalLoadingMessage = null
    }

    fun sendPasswordReset(email: String) {
        val cleanEmail = email.trim()
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            authErrorMessage = Locales.t("auth_email_invalid")
            return
        }
        scope.launch {
            showGlobalLoading(Locales.t("loading"))
            try {
                when (val result = AuthGateway.sendPasswordReset(cleanEmail)) {
                    is SignInResult.Success -> {
                        authErrorMessage = null
                        authInfoMessage = Locales.t("auth_password_reset_sent")
                    }
                    is SignInResult.Cancelled -> {
                        authErrorMessage = null
                        authInfoMessage = Locales.t("auth_sign_in_cancelled")
                    }
                    is SignInResult.Error -> {
                        val mapped = mapAuthErrorMessage(result.message)
                        if (mapped.isNullOrBlank()) {
                            authErrorMessage = null
                            authInfoMessage = Locales.t("auth_sign_in_cancelled")
                        } else {
                            authInfoMessage = null
                            authErrorMessage = mapped
                        }
                    }
                }
            } finally {
                hideGlobalLoading()
            }
        }
    }

    fun refreshAccessState(nowMillis: Long = Clock.System.now().toEpochMilliseconds()) {
        accessState = AccessManager.getAccessState(nowMillis)
        billingUiState = billingUiState.copy(
            ownedPremium = accessState.hasPremium
        )
    }

    fun maybeShowTrialEndedFreeModePopup() {
        val isFreeAfterTrial =
            accessState.tier != AccessTier.PREMIUM &&
                    !accessState.isTrialActive

        if (!isFreeAfterTrial || hasShownTrialEndedFreeModePopup) {
            return
        }

        val activeAppointmentsCount = getUpcomingAppointments(
            appointments = AppointmentSyncUtils.visibleAppointments(appointments),
            today = today,
            nowTime = getCurrentTimeHm()
        ).size

        if (activeAppointmentsCount > AccessManager.FREE_ACTIVE_APPOINTMENTS_LIMIT) {
            freeLimitPopupMessage = Locales.t("free_limit_trial_ended_backlog_explanation")
                .replace("{count}", activeAppointmentsCount.toString())
                .replace("{limit}", AccessManager.FREE_ACTIVE_APPOINTMENTS_LIMIT.toString())

            hasShownTrialEndedFreeModePopup = true
        }
    }

    suspend fun syncAccessStatusFromServerIfPossible() {
        val backendUserId = AppSettings.backendUserId.trim()
        val authUserId = currentAuthUser?.uid?.trim().orEmpty()

        if (backendUserId.isBlank() || authUserId.isBlank()) {
            return
        }

        runCatching {
            val remote = com.andrey.beautyplanner.remote.BackendBridge.getAccessStatus(
                backendUserId
            )
            com.andrey.beautyplanner.access.AccessRepository.applyRemoteStatus(
                remote = remote,
                currentAuthUserId = currentAuthUser?.uid
            )
            refreshAccessState()
            CloudSyncLogger.log(
                "syncAccessStatusFromServerIfPossible: tier=${remote.tier}, hasPremium=${remote.hasPremium}, state=${remote.subscriptionState}"
            )
        }.onFailure {
            CloudSyncLogger.log(
                "syncAccessStatusFromServerIfPossible: failed: ${it.message}"
            )
        }
    }

    fun mapAuthErrorMessage(raw: String?): String? {
        val text = raw?.trim().orEmpty()
        if (text.isBlank()) return Locales.t("auth_error_generic")

        val lower = text.lowercase()

        return when {
            lower.contains("user_cancelled") ->
                null

            lower.contains("user_not_completed") ->
                null

            lower == "internal" || lower.contains("internal") ->
                Locales.t("auth_error_generic")

            lower.contains("underlying tasks failed") ||
                    lower.contains("network") ||
                    lower.contains("unable to resolve host") ||
                    lower.contains("failed to connect") ||
                    lower.contains("timeout") ||
                    lower.contains("timed out") ||
                    lower.contains("unreachable") ->
                Locales.t("auth_error_no_internet")

            lower.contains("developer console is not set up correctly") ->
                Locales.t("auth_google_failed")

            lower.contains("no credentials available") ->
                Locales.t("auth_google_no_credentials")

            lower.contains("cancel") ->
                null

            lower.contains("auth_email_not_verified") ->
                Locales.t("auth_email_not_verified")

            lower.contains("password is invalid") ||
                    lower.contains("wrong-password") ||
                    lower.contains("invalid login credentials") ||
                    lower.contains("invalid_login_credentials") ->
                Locales.t("auth_email_wrong_password")

            lower.contains("no user record") ||
                    lower.contains("user-not-found") ||
                    lower.contains("cannot find user") ->
                Locales.t("auth_email_user_not_found")

            lower.contains("email address is badly formatted") ||
                    lower.contains("invalid-email") ->
                Locales.t("auth_email_invalid")

            lower.contains("email address is already in use") ||
                    lower.contains("email-already-in-use") ||
                    lower.contains("email is already in use") ->
                Locales.t("auth_email_already_in_use")

            lower.contains("password should be at least 6 characters") ||
                    lower.contains("weak-password") ->
                Locales.t("auth_password_too_short")

            lower.contains("email/password accounts are not enabled") ->
                Locales.t("auth_email_provider_disabled")

            lower.contains("supplied auth credential is malformed or has expired") ->
                Locales.t("auth_error_generic")

            else -> Locales.t("auth_error_sign_in_failed")
        }
    }

    fun checkForAppUpdates() {
        if (isCheckingAppUpdates) return

        scope.launch {
            isCheckingAppUpdates = true
            try {
                val status = runCatching {
                    AppUpdateChecker.check()
                }.getOrElse {
                    AppUpdateStatus(
                        checked = true,
                        updateAvailable = false,
                        errorMessage = Locales.t("about_app_update_failed")
                    )
                }

                appUpdateStatus = status
                persistUpdateStatus(status)
            } finally {
                isCheckingAppUpdates = false
            }
        }
    }

    fun restoreOfflineAuthenticatedSessionIfPossible(): Boolean {
        val savedUserId = AppSettings.localProfileUserId.trim()
        val savedProviderRaw = AppSettings.lastAuthProvider.trim().uppercase()
        val savedEmail = AppSettings.lastAuthEmail.trim()
        val savedDisplayName = AppSettings.lastAuthDisplayName.trim()

        if (savedUserId.isBlank() || savedProviderRaw.isBlank()) return false

        val provider = when (savedProviderRaw) {
            "GOOGLE" -> SignInProvider.GOOGLE
            "APPLE" -> SignInProvider.APPLE
            "EMAIL" -> SignInProvider.EMAIL
            else -> return false
        }

        currentAuthUser = AuthUser(
            uid = savedUserId,
            provider = provider,
            email = savedEmail,
            displayName = savedDisplayName
        )

        hasShownTrialEndedFreeModePopup = false

        AppSettings.lastAuthenticatedAppOpenAtMillis = Clock.System.now().toEpochMilliseconds()
        AppSettings.persist()

        reloadAppointmentsForCurrentProfile()
        refreshAccessState()
        maybeShowTrialEndedFreeModePopup()

        authResolved = true
        authErrorMessage = null
        currentScreen = Screen.MONTH
        return true
    }

    var screenHistory by mutableStateOf(listOf<Screen>())

    fun navigateTo(screen: Screen) {
        if (currentScreen != screen) {
            screenHistory = screenHistory + currentScreen
            currentScreen = screen
        }
    }

    fun navigateBack() {
        if (screenHistory.isNotEmpty()) {
            val previous = screenHistory.last()
            screenHistory = screenHistory.dropLast(1)
            currentScreen = previous
        } else {
            currentScreen = Screen.MONTH
        }
    }

    fun performHeaderBackAction() {
        if (currentScreen == Screen.AUTH_EMAIL) {
            currentScreen = Screen.AUTH_WELCOME
            return
        }

        if (currentScreen != Screen.MONTH) {
            navigateBack()
        }
    }

    fun navigateHome() {
        screenHistory = emptyList()
        currentScreen = Screen.MONTH
    }

    fun manualRefresh() {
        if (isRefreshing) return

        scope.launch {
            isRefreshing = true
            try {
                CloudSyncLogger.log("manualRefresh: started")

                val hasSavedAuthenticatedSession =
                    AppSettings.localProfileUserId.isNotBlank() &&
                            AppSettings.lastAuthProvider.isNotBlank()

                if (hasSavedAuthenticatedSession) {
                    runCatching {
                        bootstrapAuthenticatedUser()
                    }.onFailure {
                        CloudSyncLogger.log("manualRefresh: bootstrap failed: ${it.message}")
                    }
                }

                reloadAppointmentsForCurrentProfile()
                refreshAccessState()

                runCatching {
                    performCloudSyncIfEligible()
                }.onFailure {
                    CloudSyncLogger.log("manualRefresh: sync failed: ${it.message}")
                }

                CloudSyncLogger.log("manualRefresh: finished")
            } finally {
                isRefreshing = false
            }
        }
    }

    private suspend fun runPostLoginFullSync() {
        runCatching {
            com.andrey.beautyplanner.remote.MasterProfileSync.pullIfAuthenticated(force = true)
        }.onFailure {
            CloudSyncLogger.log("postLoginFullSync: profile pull failed: ${it.message}")
        }

        reloadAppointmentsForCurrentProfile()
        refreshAccessState()

        runCatching {
            performCloudSyncIfEligible()
        }.onFailure {
            CloudSyncLogger.log("postLoginFullSync: cloud sync failed: ${it.message}")
        }

        runCatching {
            val status = AppUpdateChecker.check()
            appUpdateStatus = status
            persistUpdateStatus(status)
        }.onFailure {
            CloudSyncLogger.log("postLoginFullSync: app update fetch failed: ${it.message}")
        }
    }

    fun resetLivePreviews() {
        currentLiveDarkMode = AppSettings.isDarkMode
        fontScale = AppSettings.getFontScale()
    }

    fun confirmDeferredPayment(appointment: Appointment) {
        val idx = appointments.indexOfFirst { it.id == appointment.id }
        if (idx < 0) return

        val nowMillis = Clock.System.now().toEpochMilliseconds()
        appointments[idx] = AppointmentSyncUtils.touchForCreateOrUpdate(
            source = appointment.markPaidAfterDelay(),
            nowMillis = nowMillis
        )
        saveAll()
    }

    fun openDrawer() = scope.launch { drawerState.open() }
    fun closeDrawer() = scope.launch { drawerState.close() }

    fun initBilling() {
        scope.launch {
            billingUiState = billingUiState.copy(
                status = BillingStatus.CONNECTING,
                errorMessage = null
            )

            val connected = billingManager.startConnection()
            if (!connected) {
                billingUiState = billingUiState.copy(
                    status = BillingStatus.ERROR,
                    errorMessage = Locales.t("premium_store_unavailable"),
                    ownedPremium = AppSettings.premiumUnlocked
                )
                return@launch
            }

            billingUiState = billingUiState.copy(
                status = BillingStatus.READY,
                errorMessage = null
            )

            loadBillingProducts()
            syncSubscriptionState()
        }
    }

    suspend fun enforceAuthenticatedSessionTimeoutIfNeeded() {
        val currentUser = AuthGateway.getCurrentUser() ?: return

        if (currentUser.provider == SignInProvider.ANONYMOUS) {
            return
        }

        val lastOpen = AppSettings.lastAuthenticatedAppOpenAtMillis
        val now = Clock.System.now().toEpochMilliseconds()

        if (lastOpen > 0L && now - lastOpen > authenticatedSessionTimeoutMillis) {
            AuthGateway.signOut()
            AuthGateway.clearCredentialState()
            currentAuthUser = null
            clearPersistedAuthenticatedSession()
            refreshAccessState(now)
            throw IllegalStateException("Authenticated session expired due to inactivity")
        }

        AppSettings.lastAuthenticatedAppOpenAtMillis = now
        AppSettings.persist()
    }

    suspend fun bootstrapAuthenticatedUser(
        providerOverride: SignInProvider? = null
    ) {
        hasShownTrialEndedFreeModePopup = false

        val installId = IdentityManager.getOrCreateInstallId()

        val currentUser = AuthGateway.getCurrentUser()
            ?: throw IllegalStateException("No authenticated user session found")
        if (currentUser.uid.isBlank()) {
            throw IllegalStateException("Authenticated user has blank uid")
        }

        if (providerOverride == null && currentUser.provider == SignInProvider.ANONYMOUS) {
            throw IllegalStateException("Anonymous session is not restored automatically")
        }

        val backendAuthProvider = when (providerOverride ?: currentUser.provider) {
            SignInProvider.GOOGLE -> "google"
            SignInProvider.EMAIL -> "password"
            SignInProvider.APPLE -> "apple"
            SignInProvider.ANONYMOUS -> "anonymous"
        }

        handleAuthenticatedUserChange(currentUser)

        val remote = com.andrey.beautyplanner.remote.BackendBridge.bootstrapUser(
            installId = installId,
            firebaseUid = currentUser.uid,
            platform = getPlatform().backendPlatform,
            authProvider = backendAuthProvider,
            email = currentUser.email,
            displayName = currentUser.displayName
        )

        AppSettings.clearMasterProfileLocalState()
        com.andrey.beautyplanner.access.AccessRepository.applyRemoteStatus(
            remote = remote,
            currentAuthUserId = currentAuthUser?.uid
        )

        syncAccessStatusFromServerIfPossible()

        runCatching {
            com.andrey.beautyplanner.remote.MasterProfileSync.pullIfAuthenticated(force = true)
        }.onFailure {
            CloudSyncLogger.log("bootstrapAuthenticatedUser: profile pull failed: ${it.message}")
        }

        reloadAppointmentsForCurrentProfile()
        refreshAccessState(Clock.System.now().toEpochMilliseconds())
        maybeShowTrialEndedFreeModePopup()

        runCatching {
            performCloudSyncIfEligible()
        }.onFailure {
            CloudSyncLogger.log("bootstrapAuthenticatedUser: cloud sync failed: ${it.message}")
        }

        runCatching {
            val status = AppUpdateChecker.check()
            appUpdateStatus = status
            persistUpdateStatus(status)
        }.onFailure {
            CloudSyncLogger.log("bootstrapAuthenticatedUser: app update fetch failed: ${it.message}")
        }

        authResolved = true
        authErrorMessage = null
        currentScreen = Screen.MONTH
    }

    fun continueWithGoogle() {
        scope.launch {
            showGlobalLoading(Locales.t("loading"))
            try {
                runCatching { AuthGateway.clearCredentialState() }
                runCatching { AuthGateway.prepareForNewSignIn() }

                when (val result = AuthGateway.signInWithGoogle()) {
                    is SignInResult.Success -> {
                        runCatching {
                            val guestSnapshot =
                                if (guestUpgradeMode) captureGuestMigrationSnapshot() else null

                            handleAuthenticatedUserChange(result.user)

                            if (!guestUpgradeMode) {
                                clearSessionLocalState()
                                AppSettings.clearMasterProfileLocalState(clearMasterData = false)
                            }

                            val remote = com.andrey.beautyplanner.remote.BackendBridge.bootstrapUser(
                                installId = IdentityManager.getOrCreateInstallId(),
                                firebaseUid = result.user.uid,
                                platform = getPlatform().backendPlatform,
                                authProvider = result.user.provider.name.lowercase(),
                                email = result.user.email,
                                displayName = result.user.displayName
                            )

                            com.andrey.beautyplanner.access.AccessRepository.applyRemoteStatus(
                                remote = remote,
                                currentAuthUserId = result.user.uid
                            )

                            syncAccessStatusFromServerIfPossible()

                            com.andrey.beautyplanner.remote.BackendBridge.syncIdentity(
                                firebaseUid = result.user.uid,
                                email = result.user.email,
                                displayName = result.user.displayName,
                                authProvider = result.user.provider.name.lowercase()
                            )

                            if (guestUpgradeMode && guestSnapshot != null) {
                                applyGuestSnapshotToCurrentAuthenticatedProfile(
                                    snapshot = guestSnapshot,
                                    userId = result.user.uid
                                )
                            }

                            runPostLoginFullSync()

                            if (guestUpgradeMode) {
                                clearGuestStorageAfterMigration()
                            }

                            guestUpgradeMode = false
                            authResolved = true
                            authErrorMessage = null
                            authInfoMessage = null
                            currentScreen = Screen.MONTH
                        }.onFailure { throwable ->
                            guestUpgradeMode = false
                            runCatching { AuthGateway.signOut() }
                            runCatching { AuthGateway.clearCredentialState() }

                            authInfoMessage = null
                            resetToSignedOutState(
                                keepAuthErrorMessage = mapAuthErrorMessage(throwable.message)
                            )
                        }
                    }

                    is SignInResult.Cancelled -> {
                        authErrorMessage = null
                        authInfoMessage = Locales.t("auth_sign_in_cancelled")
                    }

                    is SignInResult.Error -> {
                        val raw = result.message.orEmpty()
                        val lower = raw.lowercase()
                        val mapped = mapAuthErrorMessage(result.message)

                        if (mapped.isNullOrBlank()) {
                            authErrorMessage = null
                            authInfoMessage = when {
                                lower.contains("user_not_completed") ->
                                    Locales.t("auth_sign_in_not_completed")
                                else ->
                                    Locales.t("auth_sign_in_cancelled")
                            }
                        } else {
                            authInfoMessage = null
                            authErrorMessage = mapped
                        }
                    }
                }
            } finally {
                hideGlobalLoading()
            }
        }
    }

    fun continueWithApple() {
        scope.launch {
            showGlobalLoading(Locales.t("loading"))
            try {
                when (val result = AuthGateway.signInWithApple()) {
                    is SignInResult.Success -> {
                        runCatching {
                            val guestSnapshot =
                                if (guestUpgradeMode) captureGuestMigrationSnapshot() else null

                            handleAuthenticatedUserChange(result.user)

                            if (!guestUpgradeMode) {
                                clearSessionLocalState()
                                AppSettings.clearMasterProfileLocalState(clearMasterData = false)
                            }

                            val remote = com.andrey.beautyplanner.remote.BackendBridge.bootstrapUser(
                                installId = IdentityManager.getOrCreateInstallId(),
                                firebaseUid = result.user.uid,
                                platform = getPlatform().backendPlatform,
                                authProvider = result.user.provider.name.lowercase(),
                                email = result.user.email,
                                displayName = result.user.displayName
                            )

                            com.andrey.beautyplanner.access.AccessRepository.applyRemoteStatus(
                                remote = remote,
                                currentAuthUserId = result.user.uid
                            )

                            syncAccessStatusFromServerIfPossible()

                            com.andrey.beautyplanner.remote.BackendBridge.syncIdentity(
                                firebaseUid = result.user.uid,
                                email = result.user.email,
                                displayName = result.user.displayName,
                                authProvider = result.user.provider.name.lowercase()
                            )

                            if (guestUpgradeMode && guestSnapshot != null) {
                                applyGuestSnapshotToCurrentAuthenticatedProfile(
                                    snapshot = guestSnapshot,
                                    userId = result.user.uid
                                )
                            }

                            runPostLoginFullSync()

                            if (guestUpgradeMode) {
                                clearGuestStorageAfterMigration()
                            }

                            guestUpgradeMode = false
                            authResolved = true
                            authErrorMessage = null
                            authInfoMessage = null
                            currentScreen = Screen.MONTH
                        }.onFailure { error ->
                            guestUpgradeMode = false
                            runCatching { AuthGateway.signOut() }
                            runCatching { AuthGateway.clearCredentialState() }

                            authInfoMessage = null
                            resetToSignedOutState(
                                keepAuthErrorMessage = mapAuthErrorMessage(error.message)
                            )
                        }
                    }

                    is SignInResult.Cancelled -> {
                        authErrorMessage = null
                        authInfoMessage = Locales.t("auth_sign_in_cancelled")
                    }

                    is SignInResult.Error -> {
                        val raw = result.message.orEmpty()
                        val lower = raw.lowercase()
                        val mapped = mapAuthErrorMessage(result.message)

                        if (mapped.isNullOrBlank()) {
                            authErrorMessage = null
                            authInfoMessage = when {
                                lower.contains("user_not_completed") ->
                                    Locales.t("auth_sign_in_not_completed")
                                else ->
                                    Locales.t("auth_sign_in_cancelled")
                            }
                        } else {
                            authInfoMessage = null
                            authErrorMessage = mapped
                        }
                    }
                }
            } finally {
                hideGlobalLoading()
            }
        }
    }

    fun continueAnonymously() {
        scope.launch {
            showGlobalLoading(Locales.t("loading"))
            try {
                runCatching {
                    val signIn = AuthGateway.signInAnonymously()
                    val user = when (signIn) {
                        is SignInResult.Success -> signIn.user
                        is SignInResult.Cancelled -> {
                            throw IllegalStateException("Anonymous sign-in cancelled")
                        }
                        is SignInResult.Error -> {
                            throw IllegalStateException(signIn.message)
                        }
                    }
                    val remote = com.andrey.beautyplanner.remote.BackendBridge.bootstrapUser(
                        installId = IdentityManager.getOrCreateInstallId(),
                        firebaseUid = user.uid,
                        platform = getPlatform().backendPlatform,
                        authProvider = "anonymous",
                        email = user.email,
                        displayName = user.displayName
                    )
                    clearPersistedAuthenticatedSession()
                    currentAuthUser = user

                    com.andrey.beautyplanner.access.AccessRepository.applyRemoteStatus(
                        remote = remote,
                        currentAuthUserId = user.uid
                    )

                    clearSessionLocalState()
                    reloadAppointmentsForGuestProfile()

                    refreshAccessState()
                    authResolved = true
                    authErrorMessage = null
                    currentScreen = Screen.MONTH
                }.onFailure { error ->
                    val mapped = mapAuthErrorMessage(error.message)
                    if (mapped.isNullOrBlank()) {
                        authErrorMessage = null
                        authInfoMessage = Locales.t("auth_sign_in_cancelled")
                    } else {
                        authInfoMessage = null
                        authErrorMessage = mapped
                    }
                }
            } finally {
                hideGlobalLoading()
            }
        }
    }

    fun openSignInScreen() {
        guestUpgradeMode = false
        authErrorMessage = null
        authInfoMessage = null
        screenHistory = emptyList()
        currentScreen = Screen.AUTH_WELCOME
    }

    fun clearSessionLocalState() {
        appointments.clear()

        editingAppointment = null
        transferA = null
        conflictB = null
        pendingTargetDate = null
        pendingTargetTime = ""

        pendingImportText = null
        pendingImportPreview = null
        pendingEncryptedImportText = null

        showBookingDialog = false
        showTransferPickDialog = false
        showTransferConflictConfirm = false
        showRescheduleBDialog = false
        showDeleteConfirm = null

        bookingReadOnly = false
        selectedTimeSlot = ""
    }

    private fun resetToSignedOutState(
        keepAuthErrorMessage: String? = null
    ) {
        currentAuthUser = null
        clearPersistedAuthenticatedSession()
        IdentityManager.resetInstallId()
        clearSessionLocalState()
        AppSettings.clearMasterProfileLocalState(clearMasterData = false)
        reloadAppointmentsForGuestProfile()
        refreshAccessState()
        authResolved = true
        screenHistory = emptyList()
        currentScreen = Screen.AUTH_WELCOME
        authErrorMessage = keepAuthErrorMessage
    }

    private fun purgeDeletedAccountLocally() {
        hideGlobalLoading()
        clearSessionLocalState()

        runCatching {
            DataManager.saveToDatabase(
                data = emptyList(),
                profileKey = LocalProfileManager.currentProfileKey()
            )
        }

        currentAuthUser = null
        clearPersistedAuthenticatedSession()

        AppSettings.clearMasterProfileLocalState()
        AppSettings.lastAuthProvider = ""
        AppSettings.lastAuthEmail = ""
        AppSettings.lastAuthDisplayName = ""
        AppSettings.localProfileUserId = ""
        AppSettings.backendUserId = ""
        AppSettings.cachedTrialEndsAtMillis = 0L
        AppSettings.trialStartedAtMillis = 0L
        AppSettings.premiumUnlocked = false
        AppSettings.lastAuthenticatedAppOpenAtMillis = 0L
        AppSettings.persist()

        reloadAppointmentsForGuestProfile()
        refreshAccessState()

        authResolved = true
        authErrorMessage = null
        screenHistory = emptyList()
        currentScreen = Screen.AUTH_WELCOME
        hideGlobalLoading()
    }

    private suspend fun waitForServerConfirmedAccountDeletion(
        timeoutMillis: Long = 180_000L,
        pollIntervalMillis: Long = 30_000L
    ): Boolean {
        val startedAt = Clock.System.now().toEpochMilliseconds()

        while (Clock.System.now().toEpochMilliseconds() - startedAt < timeoutMillis) {
            val validationResult = runCatching {
                com.andrey.beautyplanner.remote.BackendBridge.validateCurrentSession()
            }

            if (validationResult.isFailure) {
                val message = validationResult.exceptionOrNull()?.message.orEmpty().lowercase()

                val deletedOrInvalid =
                    message.contains("not-found") ||
                            message.contains("user not found") ||
                            message.contains("unauthenticated") ||
                            message.contains("no authenticated") ||
                            message.contains("permission-denied")

                if (deletedOrInvalid) {
                    return true
                }
            } else {
                val result = validationResult.getOrNull().orEmpty()
                val ok = result["ok"].orEmpty().equals("true", ignoreCase = true)
                if (!ok) {
                    return true
                }
            }

            delay(pollIntervalMillis)
        }

        return false
    }

    fun switchAccount() {
        scope.launch {
            drawerState.close()
            showGlobalLoading(Locales.t("loading"))
            try {
                runCatching {
                    AuthGateway.signOut()
                    AuthGateway.clearCredentialState()
                    IdentityManager.resetInstallId()

                    currentAuthUser = null
                    clearPersistedAuthenticatedSession()

                    clearSessionLocalState()
                    AppSettings.clearMasterProfileLocalState(clearMasterData = false)
                    reloadAppointmentsForGuestProfile()
                    refreshAccessState()

                    screenHistory = emptyList()
                    currentScreen = Screen.AUTH_WELCOME
                    authErrorMessage = null
                }.onFailure { error ->
                    authErrorMessage = mapAuthErrorMessage(error.message)
                }
            } finally {
                hideGlobalLoading()
            }
        }
    }

    fun signOutCompletely() {
        scope.launch {
            drawerState.close()
            showGlobalLoading(Locales.t("loading"))
            try {
                runCatching {
                    AuthGateway.signOut()
                    AuthGateway.clearCredentialState()
                    IdentityManager.resetInstallId()

                    currentAuthUser = null
                    clearPersistedAuthenticatedSession()

                    clearSessionLocalState()
                    AppSettings.clearMasterProfileLocalState(clearMasterData = false)
                    reloadAppointmentsForGuestProfile()
                    refreshAccessState()

                    screenHistory = emptyList()
                    currentScreen = Screen.AUTH_WELCOME
                    authErrorMessage = null
                }.onFailure { error ->
                    authErrorMessage = mapAuthErrorMessage(error.message)
                }
            } finally {
                hideGlobalLoading()
            }
        }
    }

    fun startDeleteGoogleAccount(
        onError: (String) -> Unit
    ) {
        scope.launch {
            runCatching {
                reauthenticateAndDeleteGoogleAccount()
            }.onFailure { error ->
                hideGlobalLoading()
                onError(error.message ?: Locales.t("account_delete_failed"))
            }
        }
    }

    fun startDeleteAppleAccount(
        onError: (String) -> Unit
    ) {
        scope.launch {
            runCatching {
                reauthenticateAndDeleteAppleAccount()
            }.onFailure { error ->
                hideGlobalLoading()
                onError(error.message ?: Locales.t("account_delete_failed"))
            }
        }
    }

    fun startDeleteEmailAccount(
        email: String,
        password: String,
        onError: (String) -> Unit
    ) {
        scope.launch {
            runCatching {
                reauthenticateAndDeleteEmailAccount(email, password)
            }.onFailure { error ->
                hideGlobalLoading()
                onError(error.message ?: Locales.t("account_delete_failed"))
            }
        }
    }

    suspend fun reauthenticateAndDeleteEmailAccount(
        email: String,
        password: String
    ) {
        val cleanEmail = email.trim()
        val cleanPassword = password.trim()

        if (cleanEmail.isBlank()) {
            throw IllegalStateException(Locales.t("account_delete_email_missing"))
        }

        if (cleanPassword.isBlank()) {
            throw IllegalStateException(Locales.t("account_delete_wrong_password"))
        }

        when (val reauthResult = AuthGateway.signInWithEmail(cleanEmail, cleanPassword)) {
            is SignInResult.Success -> {
                currentAuthUser = reauthResult.user
                persistAuthenticatedSession(reauthResult.user)
            }

            is SignInResult.Cancelled -> {
                throw IllegalStateException(Locales.t("account_delete_requires_recent_login"))
            }

            is SignInResult.Error -> {
                val mapped = mapAuthErrorMessage(reauthResult.message)
                throw IllegalStateException(mapped)
            }
        }

        val deleted = withTimeoutOrNull(30_000L) {
            runCatching {
                val deleteResult = com.andrey.beautyplanner.remote.BackendBridge.deleteMyAccount()
                deleteResult["ok"].orEmpty().equals("true", ignoreCase = true)
            }.getOrElse { false }
        } ?: false

        val confirmedDeleted = if (deleted) {
            true
        } else {
            waitForServerConfirmedAccountDeletion()
        }

        if (!confirmedDeleted) {
            hideGlobalLoading()
            throw IllegalStateException(Locales.t("account_delete_failed"))
        }

        AuthGateway.signOut()
        AuthGateway.clearCredentialState()

        purgeDeletedAccountLocally()
    }

    suspend fun reauthenticateAndDeleteGoogleAccount() {
        when (val signInResult = AuthGateway.signInWithGoogle()) {
            is SignInResult.Success -> {
                currentAuthUser = signInResult.user
                persistAuthenticatedSession(signInResult.user)
            }

            is SignInResult.Cancelled -> {
                throw IllegalStateException(Locales.t("account_delete_requires_recent_login"))
            }

            is SignInResult.Error -> {
                val mapped = mapAuthErrorMessage(signInResult.message)
                throw IllegalStateException(mapped)
            }
        }

        val deleted = withTimeoutOrNull(30_000L) {
            runCatching {
                val deleteResult = com.andrey.beautyplanner.remote.BackendBridge.deleteMyAccount()
                deleteResult["ok"].orEmpty().equals("true", ignoreCase = true)
            }.getOrElse { false }
        } ?: false

        val confirmedDeleted = if (deleted) {
            true
        } else {
            waitForServerConfirmedAccountDeletion()
        }

        if (!confirmedDeleted) {
            hideGlobalLoading()
            throw IllegalStateException(Locales.t("account_delete_failed"))
        }

        AuthGateway.signOut()
        AuthGateway.clearCredentialState()

        purgeDeletedAccountLocally()
    }

    suspend fun reauthenticateAndDeleteAppleAccount() {
        val bridge = com.andrey.beautyplanner.auth.AppleDeletionBridgeConnector.reauthenticateAndRevoke
            ?: throw IllegalStateException(Locales.t("account_delete_failed"))

        try {
            val deferred = kotlinx.coroutines.CompletableDeferred<Map<String, String>>()
            bridge.invoke(deferred)
            val result = deferred.await()

            val uid = result["uid"].orEmpty().trim()
            if (uid.isBlank()) {
                throw IllegalStateException(Locales.t("account_delete_failed"))
            }

            currentAuthUser = currentAuthUser?.copy(uid = uid)
        } catch (error: Throwable) {
            throw IllegalStateException(
                error.message ?: Locales.t("account_delete_requires_recent_login")
            )
        }

        val deleted = withTimeoutOrNull(30_000L) {
            runCatching {
                val deleteResult = com.andrey.beautyplanner.remote.BackendBridge.deleteMyAccount()
                deleteResult["ok"].orEmpty().equals("true", ignoreCase = true)
            }.getOrElse { false }
        } ?: false
        val confirmedDeleted = if (deleted) {
            true
        } else {
            waitForServerConfirmedAccountDeletion()
        }

        if (!confirmedDeleted) {
            hideGlobalLoading()
            throw IllegalStateException(Locales.t("account_delete_failed"))
        }

        AuthGateway.signOut()
        AuthGateway.clearCredentialState()

        purgeDeletedAccountLocally()
    }

    fun openEmailSignInScreen() {
        authErrorMessage = null
        authInfoMessage = null
        authEmailRegisterMode = false
        screenHistory = emptyList()
        currentScreen = Screen.AUTH_EMAIL
    }

    fun openEmailRegisterScreen() {
        authErrorMessage = null
        authEmailRegisterMode = true
        screenHistory = emptyList()
        currentScreen = Screen.AUTH_EMAIL
    }

    fun submitEmailAuth(email: String, password: String, confirmPassword: String) {
        val cleanEmail = email.trim()
        val cleanPassword = password.trim()
        val cleanConfirmPassword = confirmPassword.trim()

        authErrorMessage = null
        authInfoMessage = null

        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            authErrorMessage = Locales.t("auth_email_invalid")
            return
        }

        if (cleanPassword.length < 6) {
            authErrorMessage = Locales.t("auth_password_too_short")
            return
        }

        if (authEmailRegisterMode && cleanPassword != cleanConfirmPassword) {
            authErrorMessage = Locales.t("auth_passwords_mismatch")
            return
        }

        scope.launch {
            showGlobalLoading(Locales.t("loading"))
            try {
                runCatching { AuthGateway.clearCredentialState() }
                runCatching { AuthGateway.prepareForNewSignIn() }

                if (authEmailRegisterMode) {
                    when (val result = AuthGateway.registerWithEmail(cleanEmail, cleanPassword)) {
                        is SignInResult.Success -> {
                            authEmailRegisterMode = false
                            authErrorMessage = null
                            authInfoMessage = null
                            authInfoDialogMessage =
                                Locales.t("auth_email_verification_sent") + " " + cleanEmail
                        }

                        is SignInResult.Cancelled -> {
                            authErrorMessage = null
                            authInfoMessage = Locales.t("auth_sign_in_cancelled")
                        }

                        is SignInResult.Error -> {
                            val mapped = mapAuthErrorMessage(result.message)
                            if (mapped.isNullOrBlank()) {
                                authErrorMessage = null
                                authInfoMessage = Locales.t("auth_sign_in_cancelled")
                            } else {
                                authInfoMessage = null
                                authErrorMessage = mapped
                            }
                        }
                    }
                } else {
                    when (val result = AuthGateway.signInWithEmail(cleanEmail, cleanPassword)) {
                        is SignInResult.Success -> {
                            try {
                                val guestSnapshot =
                                    if (guestUpgradeMode) captureGuestMigrationSnapshot() else null

                                handleAuthenticatedUserChange(result.user)

                                if (!guestUpgradeMode) {
                                    clearSessionLocalState()
                                    AppSettings.clearMasterProfileLocalState(clearMasterData = false)
                                }

                                val remote = try {
                                    com.andrey.beautyplanner.remote.BackendBridge.bootstrapUser(
                                        installId = IdentityManager.getOrCreateInstallId(),
                                        firebaseUid = result.user.uid,
                                        platform = getPlatform().backendPlatform,
                                        authProvider = "password",
                                        email = result.user.email,
                                        displayName = result.user.displayName
                                    )
                                } catch (e: Throwable) {
                                    runCatching { AuthGateway.signOut() }
                                    runCatching { AuthGateway.clearCredentialState() }
                                    throw e
                                }

                                com.andrey.beautyplanner.access.AccessRepository.applyRemoteStatus(
                                    remote = remote,
                                    currentAuthUserId = result.user.uid
                                )

                                syncAccessStatusFromServerIfPossible()

                                com.andrey.beautyplanner.remote.BackendBridge.syncIdentity(
                                    firebaseUid = result.user.uid,
                                    email = result.user.email,
                                    displayName = result.user.displayName,
                                    authProvider = "password"
                                )

                                if (guestUpgradeMode && guestSnapshot != null) {
                                    applyGuestSnapshotToCurrentAuthenticatedProfile(
                                        snapshot = guestSnapshot,
                                        userId = result.user.uid
                                    )
                                }

                                runPostLoginFullSync()

                                if (guestUpgradeMode) {
                                    clearGuestStorageAfterMigration()
                                }

                                guestUpgradeMode = false
                                authResolved = true
                                authErrorMessage = null
                                authInfoMessage = null
                                currentScreen = Screen.MONTH
                            } catch (e: Throwable) {
                                guestUpgradeMode = false
                                runCatching { AuthGateway.signOut() }
                                runCatching { AuthGateway.clearCredentialState() }

                                authInfoMessage = null
                                resetToSignedOutState(
                                    keepAuthErrorMessage = mapAuthErrorMessage(e.message)
                                )
                            }
                        }

                        is SignInResult.Cancelled -> {
                            authErrorMessage = null
                            authInfoMessage = Locales.t("auth_sign_in_cancelled")
                        }

                        is SignInResult.Error -> {
                            val mapped = mapAuthErrorMessage(result.message)
                            if (mapped.isNullOrBlank()) {
                                authErrorMessage = null
                                authInfoMessage = Locales.t("auth_sign_in_cancelled")
                            } else {
                                authInfoMessage = null
                                authErrorMessage = mapped
                            }
                        }
                    }
                }
            } finally {
                hideGlobalLoading()
            }
        }
    }

    suspend fun performCloudSyncIfEligible() {
        val authUser = currentAuthUser
        val userId = authUser?.uid?.trim().orEmpty()
        if (userId.isBlank()) {
            CloudSyncLogger.log("performCloudSyncIfEligible: skipped, blank auth uid")
            return
        }

        val authenticatedEligible =
            authUser?.provider != null &&
                    authUser.provider != SignInProvider.ANONYMOUS

        if (!authenticatedEligible) {
            CloudSyncLogger.log("performCloudSyncIfEligible: skipped, anonymous user")
            return
        }

        val nowMillis = Clock.System.now().toEpochMilliseconds()

        val premiumEligible =
            accessState.hasPremium || accessState.tier == AccessTier.PREMIUM

        CloudSyncLogger.log(
            "performCloudSyncIfEligible: start userId=$userId localAppointments=${appointments.size}"
        )

        val repository = CloudSyncRepositoryProvider.repository
        val remote = repository.pullAll(userId)

        val mergedAppointments = CloudSyncCoordinator.mergeLocalAndRemoteAppointments(
            local = appointments.toList(),
            remote = remote.appointments
        )

        appointments.clear()
        appointments.addAll(mergedAppointments)

        if (premiumEligible) {
            if (
                CloudSyncCoordinator.shouldApplyRemoteSettings(
                    localSettingsUpdatedAtMillis = AppSettings.cloudSettingsUpdatedAtMillis,
                    remoteSettings = remote.settings
                )
            ) {
                remote.settings?.let {
                    CloudSyncLogger.log("performCloudSyncIfEligible: applying remote settings")
                    AppSettings.applyCloudSettingsSnapshot(it)
                }
                currentLiveDarkMode = AppSettings.isDarkMode
                fontScale = AppSettings.getFontScale()
            } else {
                CloudSyncLogger.log("performCloudSyncIfEligible: keeping local settings")
            }
        } else {
            CloudSyncLogger.log("performCloudSyncIfEligible: skipping cloud settings sync for free user")
        }

        DataManager.saveToDatabase(
            data = appointments.toList(),
            profileKey = LocalProfileManager.currentProfileKey()
        )

        if (premiumEligible) {
            repository.pushAll(
                userId = userId,
                appointments = appointments.toList(),
                settings = AppSettings.exportCloudSettingsSnapshot(nowMillis)
            )
        } else {
            repository.pushAppointments(
                userId = userId,
                appointments = appointments.toList()
            )
        }

        val visibleAppointments = AppointmentSyncUtils.visibleAppointments(appointments.toList())
        val mins = AppSettings.reminderMinutesComputed()

        if (AppSettings.notificationsEnabled && mins.isNotEmpty()) {
            Notifications.rescheduleAll(
                appointments = visibleAppointments,
                reminderMinutes = mins,
                soundType = AppSettings.notificationSoundType,
                soundId = AppSettings.notificationSoundId,
                nowEpochMillis = nowMillis
            )
        } else {
            Notifications.cancelAll()
        }

        refreshAccessState(nowMillis)

        CloudSyncLogger.log(
            "performCloudSyncIfEligible: done userId=$userId merged=${appointments.size} visible=${visibleAppointments.size}"
        )
    }

    fun scheduleCloudSyncIfEligible() {
        scope.launch {
            cloudSyncMutex.withLock {
                cloudSyncInProgress = true
                CloudSyncLogger.log("scheduleCloudSyncIfEligible: launched")
                try {
                    runCatching { performCloudSyncIfEligible() }
                        .onFailure { CloudSyncLogger.log("scheduleCloudSyncIfEligible: failed: ${it.message}") }
                } finally {
                    cloudSyncInProgress = false
                    CloudSyncLogger.log("scheduleCloudSyncIfEligible: finished")
                }
            }
        }
    }

    fun forceCloudSyncFromDebug() {
        if (cloudSyncInProgress) {
            CloudSyncLogger.log("forceCloudSyncFromDebug: skipped, sync already running")
            return
        }

        scope.launch {
            cloudSyncInProgress = true
            CloudSyncLogger.log("forceCloudSyncFromDebug: launched manually")
            try {
                runCatching {
                    performCloudSyncIfEligible()
                }.onFailure {
                    CloudSyncLogger.log("forceCloudSyncFromDebug: failed: ${it.message}")
                }
            } finally {
                cloudSyncInProgress = false
                CloudSyncLogger.log("forceCloudSyncFromDebug: finished")
            }
        }
    }

    fun logCloudSyncSnapshot() {
        val premiumEligible = accessState.hasPremium || accessState.tier == AccessTier.PREMIUM
        val visibleCount = appointments.count { !it.isDeleted }

        CloudSyncLogger.log(
            "snapshot: authUid=${currentAuthUser?.uid ?: "—"}, provider=${currentAuthUser?.provider ?: "NONE"}, backendUserId=${AppSettings.backendUserId.ifBlank { "—" }}, premiumEligible=$premiumEligible, inProgress=$cloudSyncInProgress, appointments=${appointments.size}, visible=$visibleCount"
        )
    }

    private suspend fun syncSubscriptionState() {
        val info = billingManager.getSubscriptionInfo()
        val now = Clock.System.now().toEpochMilliseconds()

        if (info.state != SubscriptionState.NONE) {
            AppSettings.premiumSubscriptionState = info.state.name
        }

        if (info.productId.isNotBlank()) {
            AppSettings.premiumSubscribedProductId = info.productId
        }

        if (info.purchaseToken.isNotBlank()) {
            AppSettings.premiumSubscriptionToken = info.purchaseToken
        }

        if ((info.startTimeMillis ?: 0L) > 0L) {
            AppSettings.premiumSubscriptionStartMillis = info.startTimeMillis ?: 0L
        }

        if ((info.expiryTimeMillis ?: 0L) > 0L) {
            AppSettings.premiumSubscriptionExpiryMillis = info.expiryTimeMillis ?: 0L
        }

        AppSettings.premiumSubscriptionAutoRenewing = info.isAutoRenewing
        AppSettings.premiumLastVerifiedAtMillis = info.lastVerifiedAtMillis ?: now

        AppSettings.persist()
        refreshAccessState(now)
    }

    private suspend fun loadBillingProducts() {
        billingUiState = billingUiState.copy(
            status = BillingStatus.LOADING_PRODUCTS,
            errorMessage = null
        )

        val products = billingManager.loadProducts(
            listOf(PREMIUM_SUBS_PRODUCT_ID)
        )

        billingUiState = billingUiState.copy(
            status = BillingStatus.READY,
            products = products,
            errorMessage = if (products.isEmpty()) Locales.t("premium_product_not_found") else null,
            ownedPremium = AppSettings.premiumUnlocked
        )
    }

    fun buyPremium() {
        scope.launch {
            if (currentAuthUser?.provider == SignInProvider.ANONYMOUS) {
                billingUiState = billingUiState.copy(
                    status = BillingStatus.READY,
                    errorMessage = Locales.t("premium_guest_buy_requires_account")
                )
                return@launch
            }

            showGlobalLoading(Locales.t("loading"))
            try {
                val product = billingUiState.products.firstOrNull {
                    it.productId == PREMIUM_SUBS_PRODUCT_ID
                }

                if (product == null) {
                    billingUiState = billingUiState.copy(
                        status = BillingStatus.READY,
                        errorMessage = Locales.t("premium_product_not_found"),
                        ownedPremium = accessState.hasPremium
                    )
                    return@launch
                }

                billingUiState = billingUiState.copy(
                    status = BillingStatus.PURCHASING,
                    errorMessage = null
                )

                val accountId = currentAuthUser?.uid?.ifBlank { null }
                    ?: AppSettings.backendUserId.ifBlank { null }
                    ?: IdentityManager.getOrCreateInstallId()

                when (
                    val result = billingManager.purchasePremium(
                        productId = product.productId,
                        obfuscatedAccountId = accountId
                    )
                ) {
                    is PurchaseResult.Success -> {
                        val platformCode = getPlatform().backendPlatform.uppercase().let {
                            if (it == "IOS") "APP_STORE" else "PLAY"
                        }
                        val isIosPlatform = platformCode == "APP_STORE"

                        val info = billingManager.getSubscriptionInfo()
                        val localSubscriptionActive = info.state == SubscriptionState.ACTIVE

                        if (isIosPlatform && localSubscriptionActive) {
                            val applied = com.andrey.beautyplanner.access.AccessRepository.applyLocalPremiumFallback(
                                currentAuthUserId = currentAuthUser?.uid,
                                currentBackendUserId = AppSettings.backendUserId,
                                productId = info.productId.ifBlank { result.productId },
                                subscriptionState = info.state.name,
                                expiryMillis = info.expiryTimeMillis ?: 0L,
                                autoRenewing = info.isAutoRenewing
                            )

                            if (applied) {
                                refreshAccessState()
                            }
                        }

                        runCatching {
                            val remote = com.andrey.beautyplanner.remote.BackendBridge.verifySubscription(
                                userId = AppSettings.backendUserId,
                                productId = result.productId,
                                purchaseToken = result.purchaseToken,
                                platform = platformCode,
                                transactionId = result.transactionId
                            )
                            com.andrey.beautyplanner.access.AccessRepository.applyRemoteStatus(
                                remote = remote,
                                currentAuthUserId = currentAuthUser?.uid
                            )
                            refreshAccessState()
                            billingUiState = billingUiState.copy(
                                status = BillingStatus.PURCHASED,
                                errorMessage = null,
                                ownedPremium = accessState.hasPremium
                            )

                            scope.launch {
                                delay(2000L)
                                runCatching {
                                    val refreshed = com.andrey.beautyplanner.remote.BackendBridge.getAccessStatus(
                                        AppSettings.backendUserId
                                    )
                                    com.andrey.beautyplanner.access.AccessRepository.applyRemoteStatus(
                                        remote = refreshed,
                                        currentAuthUserId = currentAuthUser?.uid
                                    )
                                    refreshAccessState()
                                }
                            }
                        }.onFailure { e ->
                            if (isIosPlatform && localSubscriptionActive) {
                                billingUiState = billingUiState.copy(
                                    status = BillingStatus.PURCHASED,
                                    errorMessage = null,
                                    ownedPremium = true
                                )
                                refreshAccessState()

                                scope.launch {
                                    delay(2500L)
                                    runCatching {
                                        val refreshed = com.andrey.beautyplanner.remote.BackendBridge.getAccessStatus(
                                            AppSettings.backendUserId
                                        )
                                        com.andrey.beautyplanner.access.AccessRepository.applyRemoteStatus(
                                            remote = refreshed,
                                            currentAuthUserId = currentAuthUser?.uid
                                        )
                                        refreshAccessState()
                                    }
                                }
                            } else {
                                billingUiState = billingUiState.copy(
                                    status = BillingStatus.ERROR,
                                    errorMessage = e.message ?: "Backend verification failed",
                                    ownedPremium = accessState.hasPremium
                                )
                            }
                        }
                    }

                    is PurchaseResult.Cancelled -> {
                        billingUiState = billingUiState.copy(
                            status = BillingStatus.READY,
                            errorMessage = Locales.t("premium_purchase_cancelled"),
                            ownedPremium = accessState.hasPremium
                        )
                    }

                    is PurchaseResult.Error -> {
                        billingUiState = billingUiState.copy(
                            status = BillingStatus.ERROR,
                            errorMessage = result.message.ifBlank {
                                Locales.t("premium_purchase_failed")
                            },
                            ownedPremium = accessState.hasPremium
                        )
                    }
                }
            } finally {
                hideGlobalLoading()
            }
        }
    }

    fun restorePremium(silent: Boolean = false) {
        scope.launch {
            if (currentAuthUser?.provider == SignInProvider.ANONYMOUS) {
                if (!silent) {
                    billingUiState = billingUiState.copy(
                        status = BillingStatus.READY,
                        errorMessage = Locales.t("premium_guest_restore_requires_account")
                    )
                }
                return@launch
            }

            if (!silent) {
                showGlobalLoading(Locales.t("loading"))
            }

            try {
                if (!silent) {
                    billingUiState = billingUiState.copy(
                        status = BillingStatus.RESTORING,
                        errorMessage = null
                    )
                }

                when (billingManager.restorePurchases()) {
                    is RestoreResult.Restored -> {
                        val isIosPlatform = getPlatform().backendPlatform.uppercase() == "IOS"
                        val info = billingManager.getSubscriptionInfo()
                        val localSubscriptionActive = info.state == SubscriptionState.ACTIVE

                        if (isIosPlatform && localSubscriptionActive) {
                            val applied = com.andrey.beautyplanner.access.AccessRepository.applyLocalPremiumFallback(
                                currentAuthUserId = currentAuthUser?.uid,
                                currentBackendUserId = AppSettings.backendUserId,
                                productId = info.productId.ifBlank { PREMIUM_SUBS_PRODUCT_ID },
                                subscriptionState = info.state.name,
                                expiryMillis = info.expiryTimeMillis ?: 0L,
                                autoRenewing = info.isAutoRenewing
                            )

                            if (applied) {
                                refreshAccessState()
                            }
                        }

                        runCatching {
                            val remote = com.andrey.beautyplanner.remote.BackendBridge.getAccessStatus(
                                AppSettings.backendUserId
                            )
                            com.andrey.beautyplanner.access.AccessRepository.applyRemoteStatus(
                                remote = remote,
                                currentAuthUserId = currentAuthUser?.uid
                            )
                            refreshAccessState()
                            billingUiState = billingUiState.copy(
                                status = BillingStatus.READY,
                                errorMessage = if (silent) null else Locales.t("premium_restored"),
                                ownedPremium = accessState.hasPremium
                            )

                            scope.launch {
                                delay(2000L)
                                runCatching {
                                    val refreshed = com.andrey.beautyplanner.remote.BackendBridge.getAccessStatus(
                                        AppSettings.backendUserId
                                    )
                                    com.andrey.beautyplanner.access.AccessRepository.applyRemoteStatus(
                                        remote = refreshed,
                                        currentAuthUserId = currentAuthUser?.uid
                                    )
                                    refreshAccessState()
                                }
                            }
                        }.onFailure { e ->
                            if (isIosPlatform && localSubscriptionActive) {
                                billingUiState = billingUiState.copy(
                                    status = BillingStatus.READY,
                                    errorMessage = if (silent) null else Locales.t("premium_restored"),
                                    ownedPremium = true
                                )
                                refreshAccessState()

                                scope.launch {
                                    delay(2500L)
                                    runCatching {
                                        val refreshed = com.andrey.beautyplanner.remote.BackendBridge.getAccessStatus(
                                            AppSettings.backendUserId
                                        )
                                        com.andrey.beautyplanner.access.AccessRepository.applyRemoteStatus(
                                            remote = refreshed,
                                            currentAuthUserId = currentAuthUser?.uid
                                        )
                                        refreshAccessState()
                                    }
                                }
                            } else {
                                billingUiState = billingUiState.copy(
                                    status = BillingStatus.ERROR,
                                    errorMessage = e.message ?: Locales.t("premium_restore_failed"),
                                    ownedPremium = accessState.hasPremium
                                )
                            }
                        }
                    }

                    is RestoreResult.NothingToRestore -> {
                        billingUiState = billingUiState.copy(
                            status = BillingStatus.READY,
                            errorMessage = if (silent) null else Locales.t("premium_nothing_to_restore"),
                            ownedPremium = accessState.hasPremium
                        )
                    }

                    is RestoreResult.Error -> {
                        billingUiState = billingUiState.copy(
                            status = if (silent) BillingStatus.READY else BillingStatus.ERROR,
                            errorMessage = if (silent) null else Locales.t("premium_restore_failed"),
                            ownedPremium = accessState.hasPremium
                        )
                    }
                }
            } finally {
                if (!silent) {
                    hideGlobalLoading()
                }
            }
        }
    }

    fun runProtected(title: String, text: String, confirmText: String, action: () -> Unit) {
        if (!AppSettings.isPinSet()) {
            mustCreatePin = true
            return
        }
        if (!(AppSettings.pinEnabled && AppSettings.isPinSet())) {
            action()
            return
        }
        pinErrorText = null
        pinDialogTitle = title
        pinDialogText = text
        pinDialogConfirmText = confirmText
        pinDialogOnSuccess = action
        showPinDialog = true
    }

    fun closePremiumScreen() {
        currentScreen = premiumReturnScreen
    }

    fun showPremiumRequired(
        message: String,
        returnTo: Screen = currentScreen
    ) {
        premiumRequiredMessage = message
        premiumReturnScreen = returnTo

        if (currentScreen != Screen.PREMIUM_ACCESS) {
            screenHistory = screenHistory + currentScreen
        }

        currentScreen = Screen.PREMIUM_ACCESS
    }

    fun parseHmToMinutes(hm: String): Int? {
        val parts = hm.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23) return null
        if (m !in 0..59) return null
        return h * 60 + m
    }

    fun minutesToHm(mins: Int): String {
        val safe = mins.coerceIn(0, 24 * 60 - 1)
        val h = safe / 60
        val m = safe % 60
        return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
    }

    fun apptDurationMinutes(a: Appointment): Int =
        if (a.durationMinutes > 0) a.durationMinutes else a.durationHours.coerceAtLeast(1) * 60

    fun apptStartEndMinutes(a: Appointment): Pair<Int, Int>? {
        val start = parseHmToMinutes(a.time) ?: return null
        val end = start + apptDurationMinutes(a)
        return start to end
    }

    fun saveAll() {
        CloudSyncLogger.log("saveAll: appointments=${appointments.size}")
        DataManager.saveToDatabase(
            data = appointments.toList(),
            profileKey = LocalProfileManager.currentProfileKey()
        )

        val nowMillis = Clock.System.now().toEpochMilliseconds()
        refreshAccessState(nowMillis)

        val visibleAppointments = AppointmentSyncUtils.visibleAppointments(appointments.toList())
        val mins = AppSettings.reminderMinutesComputed()

        if (AppSettings.notificationsEnabled && mins.isNotEmpty()) {
            Notifications.rescheduleAll(
                appointments = visibleAppointments,
                reminderMinutes = mins,
                soundType = AppSettings.notificationSoundType,
                soundId = AppSettings.notificationSoundId,
                nowEpochMillis = nowMillis
            )
        } else {
            Notifications.cancelAll()
        }

        scope.launch {
            MasterScheduleSync.syncIfEligible(appointments.toList())
                .onFailure {
                    CloudSyncLogger.log("syncMyPublicSchedule: failed: ${it.message}")
                }
        }

        scheduleCloudSyncIfEligible()
    }

    fun findAppointment(date: LocalDate, time: String): Appointment? =
        appointments.find {
            !it.isDeleted &&
                    it.dateString == date.toString() &&
                    it.time == time
        }

    fun moveAppointment(appt: Appointment, toDate: LocalDate, toTime: String) {
        val idx = appointments.indexOfFirst { it.id == appt.id }
        val nowMillis = Clock.System.now().toEpochMilliseconds()

        if (idx >= 0) {
            appointments[idx] = AppointmentSyncUtils.touchForCreateOrUpdate(
                appt.copy(
                    dateString = toDate.toString(),
                    time = toTime
                ),
                nowMillis = nowMillis
            )
        } else {
            appointments.add(
                AppointmentSyncUtils.touchForCreateOrUpdate(
                    appt.copy(
                        dateString = toDate.toString(),
                        time = toTime
                    ),
                    nowMillis = nowMillis
                )
            )
        }
    }

    fun replaceById(updated: Appointment) {
        val idx = appointments.indexOfFirst { it.id == updated.id }
        val nowMillis = Clock.System.now().toEpochMilliseconds()
        val touched = AppointmentSyncUtils.touchForCreateOrUpdate(
            source = updated,
            nowMillis = nowMillis
        )

        if (idx >= 0) {
            appointments[idx] = touched
        } else {
            appointments.removeAll { it.id == updated.id }
            appointments.add(touched)
        }
    }

    fun tryBuildShiftChain(
        day: String,
        baseIgnoreId: String?,
        newStartMin: Int,
        newEndMin: Int,
        dayEnd: Int = 21 * 60
    ): Pair<List<ShiftItem>, String?> {
        val chain = mutableListOf<ShiftItem>()
        val movedStart = mutableMapOf<String, Int>()

        fun virtualStart(a: Appointment): Int =
            movedStart[a.id] ?: (parseHmToMinutes(a.time) ?: 0)

        fun virtualEnd(a: Appointment): Int =
            virtualStart(a) + apptDurationMinutes(a)

        var cursorStart = newStartMin
        var cursorEnd = newEndMin

        repeat(50) {
            val conflict = appointments
                .asSequence()
                .filter { it.dateString == day }
                .filter { baseIgnoreId == null || it.id != baseIgnoreId }
                .filter { it.id != baseIgnoreId }
                .mapNotNull { a ->
                    val s = virtualStart(a)
                    val e = virtualEnd(a)
                    Triple(a, s, e)
                }
                .firstOrNull { (_, s, e) -> cursorStart < e && s < cursorEnd }
                ?.first

            if (conflict == null) return chain to null

            val newS = cursorEnd
            val newE = newS + apptDurationMinutes(conflict)

            if (newE > dayEnd) return chain to conflict.id

            movedStart[conflict.id] = newS
            chain.add(ShiftItem(conflict.id, newS))

            cursorStart = newS
            cursorEnd = newE
        }

        return chain to null
    }

    fun applyShiftChain(day: String, chain: List<ShiftItem>) {
        chain.forEach { item ->
            val a = appointments.firstOrNull { it.id == item.apptId && it.dateString == day } ?: return@forEach
            val updated = a.copy(time = minutesToHm(item.newStartMin))
            replaceById(updated)
        }
    }

    fun dispose() {
        billingManager.dispose()
    }

    fun checkForAppUpdatesIfNeeded() {
        val now = Clock.System.now().toEpochMilliseconds()
        val lastCheck = AppSettings.lastUpdateCheckAtMillis
        val shouldCheck = now - lastCheck >= 24L * 60L * 60L * 1000L

        if (!shouldCheck || isCheckingAppUpdates) return

        scope.launch {
            isCheckingAppUpdates = true
            try {
                val status = runCatching {
                    AppUpdateChecker.check()
                }.getOrElse {
                    AppUpdateStatus(
                        checked = true,
                        updateAvailable = false,
                        errorMessage = Locales.t("about_app_update_failed")
                    )
                }

                appUpdateStatus = status
                persistUpdateStatus(status)
            } finally {
                isCheckingAppUpdates = false
            }
        }
    }

    private fun persistAuthenticatedSession(user: AuthUser) {
        AppSettings.localProfileUserId = user.uid
        AppSettings.lastAuthProvider = user.provider.name
        AppSettings.lastAuthEmail = user.email
        AppSettings.lastAuthDisplayName = user.displayName
        AppSettings.lastAuthenticatedAppOpenAtMillis = Clock.System.now().toEpochMilliseconds()
        AppSettings.persist()
    }

    private fun handleAuthenticatedUserChange(user: AuthUser) {
        val previousAuthUserId = AppSettings.localProfileUserId
        val authUserChanged =
            previousAuthUserId.isNotBlank() && previousAuthUserId != user.uid

        if (authUserChanged) {
            com.andrey.beautyplanner.access.AccessRepository.clearLocalPremiumState(
                blockAutoFallback = true
            )
            AppSettings.backendUserId = ""
        }

        currentAuthUser = user
        persistAuthenticatedSession(user)
        AppSettings.rememberRecentAuthEmail(user.email)
    }

    private fun clearPersistedAuthenticatedSession() {
        AppSettings.backendUserId = ""
        AppSettings.localProfileUserId = ""
        AppSettings.lastAuthProvider = ""
        AppSettings.lastAuthEmail = ""
        AppSettings.lastAuthDisplayName = ""
        AppSettings.lastAuthenticatedAppOpenAtMillis = 0L
        com.andrey.beautyplanner.access.AccessRepository.clearLocalPremiumState(
            blockAutoFallback = true
        )
        AppSettings.persist()
    }

    private fun persistUpdateStatus(status: AppUpdateStatus) {
        AppSettings.lastUpdateCheckAtMillis = Clock.System.now().toEpochMilliseconds()

        if (status.errorMessage.isBlank()) {
            AppSettings.lastKnownUpdateAvailable = status.updateAvailable
            AppSettings.lastKnownLatestVersion = status.latestVersion
            AppSettings.lastKnownLatestBuild = status.latestBuild
            AppSettings.lastKnownStoreUrl = status.storeUrl
            AppSettings.aboutDescription = status.aboutDescription
            AppSettings.aboutUpcoming = status.aboutUpcoming
        }

        AppSettings.persist()
    }
}

@Composable
fun rememberAppRootState(): AppRootState {
    val appointments = remember { mutableStateListOf<Appointment>() }
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val state = remember { AppRootState(appointments, today, drawerState, scope) }

    LaunchedEffect(Unit) {
        val startCode = AppSettings.languageCodes[AppSettings.selectedLanguage] ?: "en"
        Locales.currentLanguage = startCode
        state.checkForAppUpdatesIfNeeded()
        Locales.init()

        val nowMillis = Clock.System.now().toEpochMilliseconds()

        runCatching {
            state.enforceAuthenticatedSessionTimeoutIfNeeded()
            state.bootstrapAuthenticatedUser()
        }.onSuccess {
            state.currentScreen = Screen.MONTH
        }.onFailure { error ->
            val raw = error.message.orEmpty()

            val isNoSavedAuthenticatedSession =
                raw.contains("No authenticated user session found", ignoreCase = true) ||
                        raw.contains("Anonymous session is not restored automatically", ignoreCase = true)

            val restoredOffline = if (!isNoSavedAuthenticatedSession) {
                state.restoreOfflineAuthenticatedSessionIfPossible()
            } else {
                false
            }

            if (!restoredOffline) {
                state.authResolved = false
                appointments.clear()
                state.reloadAppointmentsForGuestProfile()

                state.authErrorMessage =
                    if (isNoSavedAuthenticatedSession) {
                        null
                    } else {
                        state.mapAuthErrorMessage(raw)
                    }

                state.currentScreen = Screen.AUTH_WELCOME
                state.authResolved = true
            }
        }

        state.refreshAccessState(nowMillis)
        state.initBilling()
        state.checkForAppUpdatesIfNeeded()
    }

    DisposableEffect(Unit) {
        onDispose {
            state.dispose()
        }
    }

    return state
}