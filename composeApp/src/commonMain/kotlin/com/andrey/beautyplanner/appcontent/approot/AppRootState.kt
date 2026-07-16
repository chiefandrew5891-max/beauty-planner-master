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
import kotlinx.coroutines.launch
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
    var authEmailRegisterMode by mutableStateOf(false)

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

    fun reloadAppointmentsForCurrentProfile() {
        reloadAppointmentsForProfile(LocalProfileManager.currentProfileKey())
    }

    fun reloadAppointmentsForGuestProfile() {
        reloadAppointmentsForProfile(LocalProfileManager.guestProfileKey())
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
                        authErrorMessage = Locales.t("auth_password_reset_sent")
                    }
                    is SignInResult.Cancelled -> {
                        authErrorMessage = Locales.t("auth_password_reset_failed")
                    }
                    is SignInResult.Error -> {
                        authErrorMessage = mapAuthErrorMessage(result.message)
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
    fun mapAuthErrorMessage(raw: String?): String {
        val text = raw?.trim().orEmpty()

        if (text.isBlank()) {
            return Locales.t("auth_error_generic")
        }

        val lower = text.lowercase()

        return when {
            lower == "internal" ||
                    lower.contains("internal") ->
                Locales.t("auth_error_generic")

            lower.contains("developer console is not set up correctly") ->
                Locales.t("auth_google_failed")

            lower.contains("no credentials available") ->
                Locales.t("auth_google_no_credentials")

            lower.contains("cancel") ->
                Locales.t("auth_google_cancelled")

            else ->
                Locales.t("auth_error_generic")
        }
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

                reloadAppointmentsForCurrentProfile()
                refreshAccessState()

                val premiumEligible = accessState.hasPremium || accessState.tier == AccessTier.PREMIUM
                if (premiumEligible) {
                    runCatching {
                        performCloudSyncIfEligible()
                    }.onFailure {
                        CloudSyncLogger.log("manualRefresh: sync failed: ${it.message}")
                    }
                } else {
                    CloudSyncLogger.log("manualRefresh: local reload only, no premium access")
                }

                CloudSyncLogger.log("manualRefresh: finished")
            } finally {
                isRefreshing = false
            }
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
            AppSettings.backendUserId = ""
            AppSettings.localProfileUserId = ""
            AppSettings.cachedAccessTier = "FREE_LIMITED"
            AppSettings.cachedTrialEndsAtMillis = 0L
            AppSettings.cachedHasPremium = false
            AppSettings.cachedSubscriptionState = "NONE"
            AppSettings.developerPremiumOverrideEnabled = false
            AppSettings.lastAuthenticatedAppOpenAtMillis = 0L
            AppSettings.persist()
            refreshAccessState(now)
            throw IllegalStateException("Authenticated session expired due to inactivity")
        }

        AppSettings.lastAuthenticatedAppOpenAtMillis = now
        AppSettings.persist()
    }
    suspend fun bootstrapAuthenticatedUser(
        providerOverride: SignInProvider? = null
    ) {
        val installId = IdentityManager.getOrCreateInstallId()

        val currentUser = AuthGateway.getCurrentUser()
            ?: throw IllegalStateException("No authenticated user session found")

        if (providerOverride == null && currentUser.provider == SignInProvider.ANONYMOUS) {
            throw IllegalStateException("Anonymous session is not restored automatically")
        }

        val backendAuthProvider = when (providerOverride ?: currentUser.provider) {
            SignInProvider.GOOGLE -> "google"
            SignInProvider.EMAIL -> "password"
            SignInProvider.APPLE -> "apple"
            SignInProvider.ANONYMOUS -> "anonymous"
        }

        val remote = com.andrey.beautyplanner.remote.BackendBridge.bootstrapUser(
            installId = installId,
            firebaseUid = currentUser.uid,
            platform = getPlatform().backendPlatform,
            authProvider = backendAuthProvider,
            email = currentUser.email,
            displayName = currentUser.displayName
        )

        currentAuthUser = currentUser
        AppSettings.localProfileUserId = currentUser.uid
        AppSettings.persist()
        com.andrey.beautyplanner.access.AccessRepository.applyRemoteStatus(remote)
        reloadAppointmentsForCurrentProfile()
        refreshAccessState(Clock.System.now().toEpochMilliseconds())
        performCloudSyncIfEligible()
        authResolved = true
        authErrorMessage = null
        currentScreen = Screen.MONTH
    }
    fun continueWithGoogle() {
        scope.launch {
            showGlobalLoading(Locales.t("loading"))
            try {
                when (val result = AuthGateway.signInWithGoogle()) {
                    is SignInResult.Success -> {
                        runCatching {
                            val remote = com.andrey.beautyplanner.remote.BackendBridge.bootstrapUser(
                                installId = IdentityManager.getOrCreateInstallId(),
                                firebaseUid = result.user.uid,
                                platform = getPlatform().backendPlatform,
                                authProvider = result.user.provider.name.lowercase(),
                                email = result.user.email,
                                displayName = result.user.displayName
                            )
                            com.andrey.beautyplanner.access.AccessRepository.applyRemoteStatus(remote)
                            com.andrey.beautyplanner.remote.BackendBridge.syncIdentity(
                                firebaseUid = result.user.uid,
                                email = result.user.email,
                                displayName = result.user.displayName,
                                authProvider = result.user.provider.name.lowercase()
                            )
                            currentAuthUser = result.user
                            AppSettings.localProfileUserId = result.user.uid
                            AppSettings.lastAuthenticatedAppOpenAtMillis = Clock.System.now().toEpochMilliseconds()
                            AppSettings.persist()

                            clearSessionLocalState()
                            reloadAppointmentsForCurrentProfile()
                            performCloudSyncIfEligible()
                            authResolved = true
                            authErrorMessage = null
                            currentScreen = Screen.MONTH
                        }.onFailure { throwable ->
                            authErrorMessage = mapAuthErrorMessage(throwable.message)
                        }
                    }
                    is SignInResult.Cancelled -> {
                        authErrorMessage = Locales.t("auth_google_cancelled")
                    }
                    is SignInResult.Error -> {
                        authErrorMessage = mapAuthErrorMessage(result.message)
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
                            val remote = com.andrey.beautyplanner.remote.BackendBridge.bootstrapUser(
                                installId = IdentityManager.getOrCreateInstallId(),
                                firebaseUid = result.user.uid,
                                platform = getPlatform().backendPlatform,
                                authProvider = result.user.provider.name.lowercase(),
                                email = result.user.email,
                                displayName = result.user.displayName
                            )
                            com.andrey.beautyplanner.access.AccessRepository.applyRemoteStatus(remote)
                            com.andrey.beautyplanner.remote.BackendBridge.syncIdentity(
                                firebaseUid = result.user.uid,
                                email = result.user.email,
                                displayName = result.user.displayName,
                                authProvider = result.user.provider.name.lowercase()
                            )
                            currentAuthUser = result.user
                            AppSettings.localProfileUserId = result.user.uid
                            AppSettings.lastAuthenticatedAppOpenAtMillis = Clock.System.now().toEpochMilliseconds()
                            AppSettings.persist()

                            clearSessionLocalState()
                            reloadAppointmentsForCurrentProfile()
                            refreshAccessState()
                            performCloudSyncIfEligible()
                            authResolved = true
                            authErrorMessage = null
                            currentScreen = Screen.MONTH
                        }.onFailure { error ->
                            authErrorMessage = mapAuthErrorMessage(error.message)
                        }
                    }

                    is SignInResult.Cancelled -> {
                        authErrorMessage = Locales.t("auth_error_generic")
                    }

                    is SignInResult.Error -> {
                        authErrorMessage = mapAuthErrorMessage(result.message)
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
                    currentAuthUser = user
                    com.andrey.beautyplanner.access.AccessRepository.applyRemoteStatus(remote)

                    AppSettings.backendUserId = ""
                    AppSettings.localProfileUserId = ""
                    AppSettings.persist()

                    clearSessionLocalState()
                    reloadAppointmentsForGuestProfile()

                    refreshAccessState()
                    authResolved = true
                    authErrorMessage = null
                    currentScreen = Screen.MONTH
                }.onFailure { error ->
                    authErrorMessage = mapAuthErrorMessage(error.message)
                }
            } finally {
                hideGlobalLoading()
            }
        }
    }
    fun openSignInScreen() {
        authErrorMessage = null
        clearSessionLocalState()
        reloadAppointmentsForGuestProfile()
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

    fun switchAccount() {
        scope.launch {
            showGlobalLoading(Locales.t("loading"))
            try {
                runCatching {
                    AuthGateway.signOut()
                    AuthGateway.clearCredentialState()

                    currentAuthUser = null
                    AppSettings.backendUserId = ""
                    AppSettings.localProfileUserId = ""
                    AppSettings.lastAuthenticatedAppOpenAtMillis = 0L
                    AppSettings.cachedAccessTier = "FREE_LIMITED"
                    AppSettings.cachedTrialEndsAtMillis = 0L
                    AppSettings.cachedHasPremium = false
                    AppSettings.cachedSubscriptionState = "NONE"
                    AppSettings.developerPremiumOverrideEnabled = false
                    AppSettings.persist()

                    clearSessionLocalState()
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
            showGlobalLoading(Locales.t("loading"))
            try {
                runCatching {
                    AuthGateway.signOut()
                    AuthGateway.clearCredentialState()

                    currentAuthUser = null
                    AppSettings.backendUserId = ""
                    AppSettings.localProfileUserId = ""
                    AppSettings.lastAuthenticatedAppOpenAtMillis = 0L
                    AppSettings.cachedAccessTier = "FREE_LIMITED"
                    AppSettings.cachedTrialEndsAtMillis = 0L
                    AppSettings.cachedHasPremium = false
                    AppSettings.cachedSubscriptionState = "NONE"
                    AppSettings.developerPremiumOverrideEnabled = false
                    AppSettings.persist()

                    clearSessionLocalState()
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
    fun openEmailSignInScreen() {
        authErrorMessage = null
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
    fun submitEmailAuth(
        email: String,
        password: String,
        confirmPassword: String
    ) {
        val cleanEmail = email.trim()
        val cleanPassword = password
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            authErrorMessage = Locales.t("auth_email_invalid")
            return
        }
        if (cleanPassword.length < 6) {
            authErrorMessage = Locales.t("auth_password_too_short")
            return
        }
        if (authEmailRegisterMode && cleanPassword != confirmPassword) {
            authErrorMessage = Locales.t("auth_passwords_mismatch")
            return
        }
        scope.launch {
            showGlobalLoading(Locales.t("loading"))
            try {
                val result = if (authEmailRegisterMode) {
                    AuthGateway.registerWithEmail(cleanEmail, cleanPassword)
                } else {
                    AuthGateway.signInWithEmail(cleanEmail, cleanPassword)
                }
                when (result) {
                    is SignInResult.Success -> {
                        runCatching {
                            val remote = com.andrey.beautyplanner.remote.BackendBridge.bootstrapUser(
                                installId = IdentityManager.getOrCreateInstallId(),
                                firebaseUid = result.user.uid,
                                platform = getPlatform().backendPlatform,
                                authProvider = "password",
                                email = result.user.email,
                                displayName = result.user.displayName
                            )
                            com.andrey.beautyplanner.access.AccessRepository.applyRemoteStatus(remote)
                            com.andrey.beautyplanner.remote.BackendBridge.syncIdentity(
                                firebaseUid = result.user.uid,
                                email = result.user.email,
                                displayName = result.user.displayName,
                                authProvider = "password"
                            )
                            currentAuthUser = result.user
                            AppSettings.localProfileUserId = result.user.uid
                            AppSettings.lastAuthenticatedAppOpenAtMillis = Clock.System.now().toEpochMilliseconds()
                            AppSettings.persist()

                            clearSessionLocalState()
                            reloadAppointmentsForCurrentProfile()
                            refreshAccessState()
                            performCloudSyncIfEligible()
                            authResolved = true
                            authErrorMessage = null
                            currentScreen = Screen.MONTH
                        }.onFailure { error ->
                            authErrorMessage = mapAuthErrorMessage(error.message)
                        }
                    }
                    is SignInResult.Cancelled -> {
                        authErrorMessage = Locales.t("auth_error_generic")
                    }
                    is SignInResult.Error -> {
                        authErrorMessage = mapAuthErrorMessage(result.message)
                    }
                }
            } finally {
                hideGlobalLoading()
            }
        }
    }

    suspend fun performCloudSyncIfEligible() {
        val userId = currentAuthUser?.uid?.trim().orEmpty()
        if (userId.isBlank()) {
            CloudSyncLogger.log("performCloudSyncIfEligible: skipped, blank auth uid")
            return
        }

        val nowMillis = Clock.System.now().toEpochMilliseconds()
        val premiumEligible = accessState.hasPremium || accessState.tier == AccessTier.PREMIUM
        if (!premiumEligible) {
            CloudSyncLogger.log("performCloudSyncIfEligible: skipped, no premium access")
            return
        }

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

        DataManager.saveToDatabase(
            data = appointments.toList(),
            profileKey = LocalProfileManager.currentProfileKey()
        )

        repository.pushAll(
            userId = userId,
            appointments = appointments.toList(),
            settings = AppSettings.exportCloudSettingsSnapshot(nowMillis)
        )

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
            showGlobalLoading(Locales.t("loading"))
            try {
                val product = billingUiState.products.firstOrNull {
                    it.productId == PREMIUM_SUBS_PRODUCT_ID
                }
                if (product == null) {
                    billingUiState = billingUiState.copy(
                        status = BillingStatus.ERROR,
                        errorMessage = Locales.t("premium_product_not_found")
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
                        runCatching {
                            val remote = com.andrey.beautyplanner.remote.BackendBridge.verifySubscription(
                                userId = AppSettings.backendUserId,
                                productId = result.productId,
                                purchaseToken = result.purchaseToken,
                                platform = getPlatform().backendPlatform.uppercase().let {
                                    if (it == "IOS") "APP_STORE" else "PLAY"
                                },
                                transactionId = result.transactionId
                            )
                            com.andrey.beautyplanner.access.AccessRepository.applyRemoteStatus(remote)
                            refreshAccessState()
                            billingUiState = billingUiState.copy(
                                status = BillingStatus.PURCHASED,
                                errorMessage = null,
                                ownedPremium = accessState.hasPremium
                            )
                        }.onFailure { e ->
                            billingUiState = billingUiState.copy(
                                status = BillingStatus.ERROR,
                                errorMessage = e.message ?: "Backend verification failed",
                                ownedPremium = accessState.hasPremium
                            )
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
                when (val result = billingManager.restorePurchases()) {
                    is RestoreResult.Restored -> {
                        runCatching {
                            val remote = com.andrey.beautyplanner.remote.BackendBridge.getAccessStatus(
                                AppSettings.backendUserId
                            )
                            com.andrey.beautyplanner.access.AccessRepository.applyRemoteStatus(remote)
                            refreshAccessState()
                            billingUiState = billingUiState.copy(
                                status = BillingStatus.READY,
                                errorMessage = if (silent) null else Locales.t("premium_restored"),
                                ownedPremium = accessState.hasPremium
                            )
                        }.onFailure { e ->
                            billingUiState = billingUiState.copy(
                                status = BillingStatus.ERROR,
                                errorMessage = e.message ?: Locales.t("premium_restore_failed"),
                                ownedPremium = accessState.hasPremium
                            )
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
                            errorMessage = if (silent) null else result.message.ifBlank {
                                Locales.t("premium_restore_failed")
                            },
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
        Locales.init()

        val nowMillis = Clock.System.now().toEpochMilliseconds()

        runCatching {
            state.enforceAuthenticatedSessionTimeoutIfNeeded()
            state.bootstrapAuthenticatedUser()
        }.onSuccess {
            state.currentScreen = Screen.MONTH
        }.onFailure {
            state.authResolved = false
            appointments.clear()
            state.reloadAppointmentsForGuestProfile()

            val raw = it.message.orEmpty()
            state.authErrorMessage =
                if (
                    raw.contains("No authenticated user session found", ignoreCase = true) ||
                    raw.contains("Anonymous session is not restored automatically", ignoreCase = true)
                ) {
                    null
                } else {
                    state.mapAuthErrorMessage(raw)
                }

            state.currentScreen = Screen.AUTH_WELCOME
        }

        state.refreshAccessState(nowMillis)
        state.initBilling()
    }

    DisposableEffect(Unit) {
        onDispose {
            state.dispose()
        }
    }

    return state
}