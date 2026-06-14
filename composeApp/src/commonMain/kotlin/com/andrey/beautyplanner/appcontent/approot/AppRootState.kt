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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import com.andrey.beautyplanner.generated.resources.Res
import com.andrey.beautyplanner.generated.resources.inter_extralight
import com.andrey.beautyplanner.generated.resources.inter_regular
import com.andrey.beautyplanner.generated.resources.inter_medium
import com.andrey.beautyplanner.generated.resources.inter_bold

@Stable
class AppRootState(
    val appointments: SnapshotStateList<Appointment>,
    val today: LocalDate,
    val drawerState: DrawerState,
    private val scope: CoroutineScope,
) {
    private val billingManager = BillingManager()

    var currentScreen by mutableStateOf(Screen.MONTH)

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
                onSurface = Color.White
            )
        } else {
            lightColors(
                primary = Color(0xFF4285F4),
                background = Color.White,
                surface = Color.White,
                onPrimary = Color.White,
                onSurface = Color.Black
            )
        }

    var fontScale by mutableStateOf(AppSettings.getFontScale())

    val customTypography: Typography
    @Composable
    get() {
        val appFontFamily = FontFamily(
            org.jetbrains.compose.resources.Font(Res.font.inter_extralight, FontWeight.ExtraLight),
            org.jetbrains.compose.resources.Font(Res.font.inter_regular, FontWeight.Normal),
            org.jetbrains.compose.resources.Font(Res.font.inter_medium, FontWeight.Medium),
            org.jetbrains.compose.resources.Font(Res.font.inter_bold, FontWeight.Bold)
        )
        return Typography(
            body1 = TextStyle(
                fontFamily = appFontFamily,
                fontSize = (16 * fontScale).sp
            ),
            h6 = TextStyle(
                fontFamily = appFontFamily,
                fontSize = (20 * fontScale).sp,
                fontWeight = FontWeight.Bold
            ),
            subtitle1 = TextStyle(
                fontFamily = appFontFamily,
                fontSize = (14 * fontScale).sp
            )
        )
    }


    fun refreshAccessState(nowMillis: Long = Clock.System.now().toEpochMilliseconds()) {
        accessState = AccessManager.getAccessState(nowMillis)
        billingUiState = billingUiState.copy(
            ownedPremium = accessState.hasPremium
        )
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

    fun resetLivePreviews() {
        currentLiveDarkMode = AppSettings.isDarkMode
        fontScale = AppSettings.getFontScale()
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
    private suspend fun syncSubscriptionState() {
        val info = billingManager.getSubscriptionInfo()
        val now = Clock.System.now().toEpochMilliseconds()

        AppSettings.premiumSubscriptionState = info.state.name
        AppSettings.premiumSubscribedProductId = info.productId
        AppSettings.premiumSubscriptionToken = info.purchaseToken
        AppSettings.premiumSubscriptionStartMillis = info.startTimeMillis ?: 0L
        AppSettings.premiumSubscriptionExpiryMillis = info.expiryTimeMillis ?: 0L
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

            when (val result = billingManager.purchasePremium(product.productId)) {
                is PurchaseResult.Success -> {
                    refreshAccessState()
                    billingUiState = billingUiState.copy(
                        status = BillingStatus.PURCHASED,
                        errorMessage = null,
                        ownedPremium = accessState.hasPremium
                    )
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
        }
    }

    fun restorePremium(silent: Boolean = false) {
        scope.launch {
            if (!silent) {
                billingUiState = billingUiState.copy(
                    status = BillingStatus.RESTORING,
                    errorMessage = null
                )
            }

            when (val result = billingManager.restorePurchases()) {
                is RestoreResult.Restored -> {
                    refreshAccessState()
                    billingUiState = billingUiState.copy(
                        status = BillingStatus.READY,
                        errorMessage = if (silent) null else Locales.t("premium_restored"),
                        ownedPremium = accessState.hasPremium
                    )
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
        DataManager.saveToDatabase(appointments.toList())

        val nowMillis = Clock.System.now().toEpochMilliseconds()
        refreshAccessState(nowMillis)

        val mins = AppSettings.reminderMinutesComputed()
        if (AppSettings.notificationsEnabled && mins.isNotEmpty()) {
            Notifications.rescheduleAll(
                appointments = appointments.toList(),
                reminderMinutes = mins,
                soundType = AppSettings.notificationSoundType,
                soundId = AppSettings.notificationSoundId,
                nowEpochMillis = nowMillis
            )
        } else {
            Notifications.cancelAll()
        }
    }

    fun findAppointment(date: LocalDate, time: String): Appointment? =
        appointments.find { it.dateString == date.toString() && it.time == time }

    fun moveAppointment(appt: Appointment, toDate: LocalDate, toTime: String) {
        val idx = appointments.indexOfFirst { it.id == appt.id }
        if (idx >= 0) {
            appointments[idx] = appt.copy(dateString = toDate.toString(), time = toTime)
        } else {
            appointments.remove(appt)
            appointments.add(appt.copy(dateString = toDate.toString(), time = toTime))
        }
    }

    fun replaceById(updated: Appointment) {
        val idx = appointments.indexOfFirst { it.id == updated.id }
        if (idx >= 0) appointments[idx] = updated
        else {
            appointments.removeAll { it.id == updated.id }
            appointments.add(updated)
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
        val nowMillis = Clock.System.now().toEpochMilliseconds()

        AccessManager.ensureTrialInitialized(nowMillis)
        state.refreshAccessState(nowMillis)

        runCatching { DataManager.loadFromDatabase() }
            .onSuccess { loaded ->
                if (loaded.isNotEmpty()) {
                    appointments.clear()
                    appointments.addAll(loaded)
                }
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