package com.andrey.beautyplanner.appcontent.approot

import androidx.compose.foundation.background
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
import androidx.compose.material.ButtonDefaults
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
import com.andrey.beautyplanner.appcontent.subscriptionStateLabel
import kotlinx.datetime.Clock

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
        provider: SignInProvider? = null
    ) {
        val (avatarBg, avatarText) = accountAvatarColors(provider)

        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                Text(
                    text = initials,
                    color = avatarText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
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
    fun DrawerSubscriptionInfo() {
        val nowMillis = Clock.System.now().toEpochMilliseconds()
        val isPremiumActive = AppSettings.cachedHasPremium
        val expiryMillis = AppSettings.premiumSubscriptionExpiryMillis
        val rawState = AppSettings.premiumSubscriptionState.trim().uppercase()

        val stateLabel = when {
            isPremiumActive -> subscriptionStateLabel(rawState)
            rawState.isNotBlank() && rawState != "NONE" -> subscriptionStateLabel(rawState)
            else -> Locales.t("premium_subscription_inactive")
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = "${Locales.t("premium_subscription_status_compact")}: $stateLabel",
                color = onSurface.copy(alpha = 0.72f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (isPremiumActive && expiryMillis > nowMillis) {
                val daysLeft = calculateSubscriptionDaysLeft(
                    expiryMillis = expiryMillis,
                    nowMillis = nowMillis
                )

                Spacer(Modifier.padding(top = 2.dp))

                Text(
                    text = "${Locales.t("premium_subscription_expires")}: ${formatSubscriptionExpiry(expiryMillis)} • ${Locales.daysCount(daysLeft)}",
                    color = onSurface.copy(alpha = 0.54f),
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    Surface(
        color = bg,
        contentColor = onBg
    ) {
        ModalDrawer(
            drawerState = state.drawerState,
            drawerContent = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        DrawerAccountHeader(
                            initials = "G",
                            title = Locales.t("account_anonymous"),
                            subtitle = null,
                            provider = SignInProvider.ANONYMOUS
                        )

                        DrawerSubscriptionInfo()

                        DrawerActionItem(
                            title = Locales.t("account_sign_in")
                        ) {
                            state.closeDrawer()
                            state.openSignInScreen()
                        }
                    } else {
                        val title = when {
                            authUser?.displayName?.isNotBlank() == true -> authUser.displayName
                            authUser?.email?.isNotBlank() == true -> authUser.email
                            else -> Locales.t("account_current")
                        }

                        val subtitle = when {
                            authUser?.email?.isNotBlank() == true &&
                                    authUser.displayName.isNotBlank() -> authUser.email
                            else -> null
                        }

                        val initials = buildAccountInitials(
                            displayName = authUser?.displayName,
                            email = authUser?.email,
                            provider = authUser?.provider
                        )

                        DrawerAccountHeader(
                            initials = initials,
                            title = title,
                            subtitle = subtitle,
                            provider = authUser?.provider
                        )

                        DrawerSubscriptionInfo()

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

                    DrawerItem(
                        title = Locales.t("nav_settings"),
                        selected = state.currentScreen == Screen.SETTINGS
                    ) {
                        state.screenHistory = emptyList()
                        state.currentScreen = Screen.SETTINGS
                        state.closeDrawer()
                    }

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
                        state.currentScreen == Screen.DEVELOPER_ACCESS ||
                        state.currentScreen == Screen.BACKUP_SETTINGS ||
                        state.currentScreen == Screen.PRIVACY_POLICY ||
                        state.currentScreen == Screen.NOTIFICATION_SETTINGS ||
                        state.currentScreen == Screen.PREMIUM_ACCESS

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
                                            state.currentScreen = Screen.AUTH_WELCOME
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
                                                state.navigateBack()
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
    }
}