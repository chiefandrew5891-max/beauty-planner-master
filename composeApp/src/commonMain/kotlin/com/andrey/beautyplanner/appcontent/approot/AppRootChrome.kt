package com.andrey.beautyplanner.appcontent.approot

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalDrawer
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Locales
import com.andrey.beautyplanner.Screen
import com.andrey.beautyplanner.auth.SignInProvider
import com.andrey.beautyplanner.appcontent.calculateSubscriptionDaysLeft
import com.andrey.beautyplanner.appcontent.formatSubscriptionExpiry
import com.andrey.beautyplanner.rememberProfileAvatarBitmap
import com.andrey.beautyplanner.appcontent.subscriptionStateLabel
import com.andrey.beautyplanner.AccessManager
import com.andrey.beautyplanner.PremiumFeature
import com.andrey.beautyplanner.AccessTier
import kotlinx.datetime.Clock
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.abs
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle


@Composable
fun AppRootChrome(
    state: AppRootState,
    content: @Composable (PaddingValues) -> Unit
) {
    val onBg = MaterialTheme.colors.onBackground
    val bg = MaterialTheme.colors.background
    val onSurface = MaterialTheme.colors.onSurface

    @Composable
    fun DrawerItem(title: String, selected: Boolean, onClick: () -> Unit) {
        val itemBg =
            if (selected) MaterialTheme.colors.primary.copy(alpha = 0.12f) else Color.Transparent

        TextButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            colors = ButtonDefaults.textButtonColors(
                backgroundColor = itemBg,
                contentColor = onSurface
            )
        ) {
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
                color = onSurface
            )
        }
    }

    @Composable
    fun DrawerActionItem(title: String, onClick: () -> Unit) {
        TextButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(
                backgroundColor = Color.Transparent,
                contentColor = onSurface
            )
        ) {
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
                color = onSurface
            )
        }
    }

    @Composable
    fun DrawerSectionTitle(title: String) {
        Text(
            text = title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 2.dp, start = 12.dp, end = 12.dp),
            color = onSurface.copy(alpha = 0.72f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }

    @Composable
    fun accountAvatarColors(provider: SignInProvider?): Pair<Color, Color> {
        return when (provider) {
            SignInProvider.GOOGLE -> (
                    MaterialTheme.colors.primary.copy(alpha = 0.18f) to
                            MaterialTheme.colors.primary
                    )

            SignInProvider.EMAIL -> (
                    Color(0xFF7E57C2).copy(alpha = 0.18f) to
                            Color(0xFF7E57C2)
                    )

            SignInProvider.APPLE -> (
                    MaterialTheme.colors.onSurface.copy(alpha = 0.14f) to
                            MaterialTheme.colors.onSurface.copy(alpha = 0.88f)
                    )

            SignInProvider.ANONYMOUS, null -> (
                    MaterialTheme.colors.onSurface.copy(alpha = 0.10f) to
                            MaterialTheme.colors.onSurface.copy(alpha = 0.65f)
                    )
        }
    }

    fun buildAccountInitials(
        displayName: String?,
        email: String?,
        provider: SignInProvider?
    ): String {
        val name = displayName.orEmpty().trim()
        if (name.isNotBlank()) {
            val parts = name.split(" ").filter { it.isNotBlank() }
            return when {
                parts.size >= 2 -> (parts[0].take(1) + parts[1].take(1)).uppercase()
                else -> name.take(2).uppercase()
            }
        }

        val cleanEmail = email.orEmpty().trim()
        if (cleanEmail.isNotBlank()) {
            val localPart = cleanEmail.substringBefore("@").trim()
            if (localPart.isNotBlank()) {
                return localPart.take(2).uppercase()
            }
        }

        return when (provider) {
            SignInProvider.GOOGLE -> "G"
            SignInProvider.EMAIL -> "E"
            SignInProvider.APPLE -> "A"
            SignInProvider.ANONYMOUS, null -> "G"
        }
    }

    @Composable
    fun DrawerAccountHeader(
        initials: String,
        title: String,
        subtitle: String? = null,
        provider: SignInProvider? = null,
        onProfileClick: (() -> Unit)? = null
    ) {
        val (avatarBg, avatarText) = accountAvatarColors(provider)

        val profileAvatarBitmap = rememberProfileAvatarBitmap(AppSettings.profileAvatarBase64)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onProfileClick != null) Modifier.clickable(onClick = onProfileClick)
                    else Modifier
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(avatarBg),
                contentAlignment = Alignment.Center
            ) {
                if (profileAvatarBitmap != null) {
                    Image(
                        bitmap = profileAvatarBitmap,
                        contentDescription = Locales.t("profile_avatar_cd"),
                        modifier = Modifier
                            .matchParentSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = initials,
                        color = avatarText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = onSurface.copy(alpha = 0.92f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        color = onSurface.copy(alpha = 0.62f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    @Composable
    fun DrawerSubscriptionInfo(
        state: AppRootState
    ) {
        val onSurface = MaterialTheme.colors.onSurface
        val accessState = state.accessState
        val fontScale = state.fontScale

        val stateLabel = AccessManager.getUnifiedAccessStatusLabel(accessState)

        val nowMillis = Clock.System.now().toEpochMilliseconds()
        val shouldShowDaysLine =
            accessState.isTrialActive && accessState.trialEndsAtMillis > nowMillis

        val daysLeft = if (shouldShowDaysLine) {
            val millisLeft = (accessState.trialEndsAtMillis - nowMillis).coerceAtLeast(0L)
            kotlin.math.ceil(
                millisLeft / (24.0 * 60.0 * 60.0 * 1000.0)
            ).toInt().coerceAtLeast(0)
        } else {
            0
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            androidx.compose.foundation.text.BasicText(
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = onSurface.copy(alpha = 0.72f),
                            fontWeight = FontWeight.Normal
                        )
                    ) {
                        append(Locales.t("premium_subscription_status_compact"))
                        append(": ")
                    }

                    withStyle(
                        style = SpanStyle(
                            color = onSurface.copy(alpha = 0.92f),
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append(stateLabel)
                    }
                },
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = (14 * fontScale).sp
                ),
                maxLines = 1
            )

            if (shouldShowDaysLine) {
                Spacer(Modifier.height(2.dp))

                Text(
                    text = "${Locales.t("premium_subscription_days_left")}: ${Locales.daysCount(daysLeft)}",
                    color = onSurface.copy(alpha = 0.54f),
                    fontSize = (11 * fontScale).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
    val drawerGesturesEnabled =
        state.currentScreen != Screen.AUTH_WELCOME &&
        state.currentScreen != Screen.AUTH_EMAIL

    Surface(
        color = bg,
        contentColor = onBg
    ) {
        ModalDrawer(
            drawerState = state.drawerState,
            gesturesEnabled = drawerGesturesEnabled,
            drawerContent = {
                val drawerScrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .verticalScroll(drawerScrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = Locales.t("nav_menu"),
                        fontWeight = FontWeight.Bold,
                        color = onSurface
                    )

                    Divider()

                    DrawerSectionTitle(Locales.t("account_current"))

                    val authUser = state.currentAuthUser
                    val isSignedInUser =
                        authUser != null && authUser.provider != SignInProvider.ANONYMOUS

                    if (!isSignedInUser) {
                        val guestInitials = AppSettings.ownerName.trim().take(1).ifBlank { "G" }.uppercase()
                        val guestTitle = if (
                            AppSettings.profileDisplayCustomName &&
                            AppSettings.ownerName.trim().isNotBlank()
                        ) {
                            AppSettings.ownerName.trim()
                        } else {
                            Locales.t("account_anonymous")
                        }

                        DrawerAccountHeader(
                            initials = guestInitials,
                            title = guestTitle,
                            subtitle = null,
                            provider = SignInProvider.ANONYMOUS,
                            onProfileClick = null
                        )

                        DrawerSubscriptionInfo(state)

                        DrawerActionItem(
                            title = Locales.t("guest_register_current_account")
                        ) {
                            state.closeDrawer()
                            state.openGuestAccountRegistrationScreen()
                        }

                        DrawerActionItem(
                            title = Locales.t("account_switch")
                        ) {
                            state.closeDrawer()
                            state.requestGuestSwitchAccount()
                        }

                        DrawerActionItem(
                            title = Locales.t("account_sign_out")
                        ) {
                            state.closeDrawer()
                            state.requestGuestSignOut()
                        }
                    } else {
                        val authDisplayName = authUser?.displayName?.takeIf { it.isNotBlank() }
                        val customName = AppSettings.ownerName.trim().takeIf { it.isNotBlank() }
                        val title = when {
                            AppSettings.profileDisplayCustomName && customName != null -> customName
                            authDisplayName != null -> authDisplayName
                            authUser?.email?.isNotBlank() == true -> authUser.email
                            authUser?.provider == SignInProvider.APPLE ->
                                Locales.t("account_label_apple_fallback")
                            else -> Locales.t("account_current")
                        }

                        val subtitle = when {
                            authUser?.email?.isNotBlank() == true &&
                                    authUser.displayName.isNotBlank() -> authUser.email
                            else -> null
                        }

                        val initials = if (AppSettings.profileDisplayCustomName && customName != null) {
                            val parts = customName.split(" ").filter { it.isNotBlank() }
                            when {
                                parts.size >= 2 -> (parts[0].take(1) + parts[1].take(1)).uppercase()
                                else -> customName.take(2).uppercase()
                            }
                        } else {
                            buildAccountInitials(
                                displayName = authUser?.displayName,
                                email = authUser?.email,
                                provider = authUser?.provider
                            )
                        }

                        DrawerAccountHeader(
                            initials = initials,
                            title = title,
                            subtitle = subtitle,
                            provider = authUser?.provider,
                            onProfileClick = {
                                state.navigateTo(Screen.PERSONAL_INFO_SETTINGS)
                                state.closeDrawer()
                            }
                        )

                        DrawerSubscriptionInfo(state)

                        DrawerActionItem(
                            title = Locales.t("account_switch")
                        ) {
                            state.closeDrawer()
                            state.switchAccount()
                        }

                        DrawerActionItem(
                            title = Locales.t("account_sign_out")
                        ) {
                            state.closeDrawer()
                            state.signOutCompletely()
                        }
                    }

                    Divider()

                    DrawerItem(
                        title = Locales.t("nav_main"),
                        selected = state.currentScreen == Screen.MONTH
                    ) {
                        state.navigateHome()
                        state.closeDrawer()
                    }

                    DrawerItem(
                        title = Locales.t("nav_stats"),
                        selected = state.currentScreen == Screen.STATS
                    ) {
                        state.screenHistory = emptyList()
                        state.currentScreen = Screen.STATS
                        state.closeDrawer()
                    }

                    DrawerItem(
                        title = Locales.t("nav_client_database"),
                        selected = state.currentScreen == Screen.CLIENT_DATABASE
                    ) {
                        state.screenHistory = emptyList()
                        state.currentScreen = Screen.CLIENT_DATABASE
                        state.closeDrawer()
                    }

                    DrawerItem(
                        title = Locales.t("nav_unpaid_appointments"),
                        selected = state.currentScreen == Screen.UNPAID_APPOINTMENTS
                    ) {
                        state.screenHistory = emptyList()
                        state.currentScreen = Screen.UNPAID_APPOINTMENTS
                        state.closeDrawer()
                    }

                    DrawerItem(
                        title = Locales.t("nav_archive"),
                        selected = state.currentScreen == Screen.ARCHIVE
                    ) {
                        state.screenHistory = emptyList()
                        state.currentScreen = Screen.ARCHIVE
                        state.closeDrawer()
                    }

                    // =========================================================
                    // TEMP HIDE FOR APP REVIEW: CLIENT INTERACTIONS NAV ENTRY
                    // Скрыто временно для review-сборки.
                    // Вернуть после возобновления работы над BeautyBooker integration.
                    // BEGIN TEMP HIDE
                    // =========================================================
                    /*
                    DrawerItem(
                        title = Locales.t("nav_client_interactions"),
                        selected = state.currentScreen == Screen.CLIENT_INTERACTIONS
                    ) {
                        val nowMillis = Clock.System.now().toEpochMilliseconds()
                        if (!AccessManager.hasFeature(PremiumFeature.STATS, nowMillis)) {
                            state.showPremiumRequired(
                                message = Locales.t("premium_required_client_interactions"),
                                returnTo = Screen.MONTH
                            )
                            state.closeDrawer()
                            return@DrawerItem
                        }

                        state.screenHistory = emptyList()
                        state.currentScreen = Screen.CLIENT_INTERACTIONS
                        state.closeDrawer()
                    }
                    */
                    // =========================================================
                    // END TEMP HIDE FOR APP REVIEW: CLIENT INTERACTIONS NAV ENTRY
                    // =========================================================

                    DrawerItem(
                        title = Locales.t("nav_about_app"),
                        selected = state.currentScreen == Screen.FEEDBACK
                    ) {
                        state.screenHistory = emptyList()
                        state.currentScreen = Screen.FEEDBACK
                        state.closeDrawer()
                    }
                }
            }
        ) {
            val isAuthWelcomeScreen = state.currentScreen == Screen.AUTH_WELCOME
            val isAuthEmailScreen = state.currentScreen == Screen.AUTH_EMAIL

            val isHomeScreen = state.currentScreen == Screen.MONTH
            val isNestedScreen =
                state.currentScreen == Screen.DAY_DETAILS ||
                        state.currentScreen == Screen.SERVICE_TEMPLATES ||
                        state.currentScreen == Screen.WORK_SCHEDULE ||
                        state.currentScreen == Screen.APPEARANCE_SETTINGS ||
                        state.currentScreen == Screen.PERSONAL_INFO_SETTINGS ||
                        state.currentScreen == Screen.CLIENT_INTERACTIONS ||
                        state.currentScreen == Screen.DEVELOPER_ACCESS ||
                        state.currentScreen == Screen.BACKUP_SETTINGS ||
                        state.currentScreen == Screen.USER_GUIDE ||
                        state.currentScreen == Screen.PRIVACY_POLICY ||
                        state.currentScreen == Screen.NOTIFICATION_SETTINGS ||
                        state.currentScreen == Screen.PREMIUM_ACCESS ||
                        state.currentScreen == Screen.GUEST_ACCOUNT_REGISTRATION ||
                        state.currentScreen == Screen.BLACKLIST

            val density = LocalDensity.current
            val edgeWidthPx = with(density) { 64.dp.toPx() }
            val backSwipeThresholdPx = with(density) { 72.dp.toPx() }

            var gestureStartedFromEdge by remember(state.currentScreen) { mutableStateOf(false) }
            var accumulatedHorizontalDrag by remember(state.currentScreen) { mutableFloatStateOf(0f) }
            var swipeBackTriggered by remember(state.currentScreen) { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(state.currentScreen, state.screenHistory) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset: Offset ->
                                val width = size.width.toFloat()
                                val fromLeftEdge = offset.x <= edgeWidthPx
                                val fromRightEdge = offset.x >= (width - edgeWidthPx)

                                gestureStartedFromEdge =
                                    !isAuthWelcomeScreen &&
                                            !state.isGlobalLoading &&
                                            state.currentScreen != Screen.MONTH &&
                                            (fromLeftEdge || fromRightEdge)

                                accumulatedHorizontalDrag = 0f
                                swipeBackTriggered = false
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                if (!gestureStartedFromEdge) return@detectHorizontalDragGestures
                                if (swipeBackTriggered) return@detectHorizontalDragGestures

                                accumulatedHorizontalDrag += dragAmount

                                if (abs(accumulatedHorizontalDrag) >= backSwipeThresholdPx) {
                                    swipeBackTriggered = true
                                    change.consume()
                                    state.performHeaderBackAction()
                                }
                            },
                            onDragEnd = {
                                gestureStartedFromEdge = false
                                accumulatedHorizontalDrag = 0f
                                swipeBackTriggered = false
                            },
                            onDragCancel = {
                                gestureStartedFromEdge = false
                                accumulatedHorizontalDrag = 0f
                                swipeBackTriggered = false
                            }
                        )
                    }
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        modifier = Modifier.statusBarsPadding(),
                        topBar = {
                            when {
                                isAuthWelcomeScreen -> {
                                    // no top bar on the root auth screen
                                }

                                isAuthEmailScreen -> {
                                    TopAppBar(
                                        backgroundColor = MaterialTheme.colors.surface,
                                        elevation = 2.dp,
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Box(Modifier.fillMaxSize()) {
                                            IconButton(
                                                onClick = {
                                                    state.performHeaderBackAction()
                                                },
                                                modifier = Modifier.align(Alignment.CenterStart)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Reply,
                                                    contentDescription = Locales.t("cd_back"),
                                                    tint = MaterialTheme.colors.primary
                                                )
                                            }
                                        }
                                    }
                                }

                                else -> {
                                    val showBackButton = !isHomeScreen

                                    TopAppBar(
                                        backgroundColor = MaterialTheme.colors.surface,
                                        elevation = 2.dp,
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Box(Modifier.fillMaxSize()) {
                                            IconButton(
                                                onClick = {
                                                    if (showBackButton) {
                                                        state.performHeaderBackAction()
                                                    } else {
                                                        state.openDrawer()
                                                    }
                                                },
                                                modifier = Modifier.align(Alignment.CenterStart)
                                            ) {
                                                Icon(
                                                    imageVector = if (showBackButton) {
                                                        Icons.AutoMirrored.Filled.Reply
                                                    } else {
                                                        Icons.Default.Menu
                                                    },
                                                    contentDescription = if (showBackButton) {
                                                        Locales.t("cd_back")
                                                    } else {
                                                        Locales.t("cd_menu")
                                                    },
                                                    tint = MaterialTheme.colors.primary
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.align(Alignment.CenterEnd),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (isNestedScreen) {
                                                    IconButton(
                                                        onClick = { state.navigateHome() }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Home,
                                                            contentDescription = Locales.t("nav_main"),
                                                            tint = MaterialTheme.colors.primary,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                    Spacer(Modifier.width(4.dp))
                                                }

                                                IconButton(
                                                    onClick = {
                                                        if (state.currentScreen == Screen.SETTINGS) {
                                                            state.navigateHome()
                                                        } else {
                                                            state.screenHistory = emptyList()
                                                            state.currentScreen = Screen.SETTINGS
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Settings,
                                                        contentDescription = Locales.t("cd_settings"),
                                                        tint = if (state.currentScreen == Screen.SETTINGS) {
                                                            MaterialTheme.colors.primary.copy(alpha = 0.5f)
                                                        } else {
                                                            MaterialTheme.colors.primary
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    ) { padding ->
                        content(padding)
                    }
                }

                if (state.isGlobalLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = {}
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colors.primary
                            )

                            state.globalLoadingMessage?.takeIf { it.isNotBlank() }?.let { message ->
                                Text(
                                    text = message,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}