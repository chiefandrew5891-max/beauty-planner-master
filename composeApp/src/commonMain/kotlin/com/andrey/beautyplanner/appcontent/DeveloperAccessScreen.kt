package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AccessState
import com.andrey.beautyplanner.AccessTier
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.ClipboardHelper
import com.andrey.beautyplanner.CloudSyncLogger
import com.andrey.beautyplanner.FirebaseDebugInfoProvider
import com.andrey.beautyplanner.LocalProfileManager
import com.andrey.beautyplanner.Locales
import com.andrey.beautyplanner.auth.SignInProvider
import com.andrey.beautyplanner.appcontent.approot.AppRootState

@Composable
fun DeveloperAccessScreen(
    state: AppRootState,
    accessState: AccessState,
    onEnablePremium: () -> Unit,
    onDisablePremium: () -> Unit,
    onResetTrialToNow: () -> Unit,
    onExpireTrial: () -> Unit,
    onLogoutDeveloperMode: () -> Unit
) {
    val fontScale = AppSettings.getFontScale()
    val onSurface = MaterialTheme.colors.onSurface
    val onBg = MaterialTheme.colors.onBackground

    var supportPhoneDraft by remember { mutableStateOf(AppSettings.servicePhone) }
    var copyStatusMessage by remember { mutableStateOf<String?>(null) }

    val tierText = when (accessState.tier) {
        AccessTier.TRIAL -> Locales.t("premium_status_trial")
        AccessTier.FREE_LIMITED -> Locales.t("premium_status_free")
        AccessTier.PREMIUM -> Locales.t("premium_status_premium")
    }

    val authUser = state.currentAuthUser
    val authProviderText = when (authUser?.provider) {
        SignInProvider.GOOGLE -> "GOOGLE"
        SignInProvider.EMAIL -> "EMAIL"
        SignInProvider.APPLE -> "APPLE"
        SignInProvider.ANONYMOUS -> "ANONYMOUS"
        null -> "NONE"
    }

    val premiumEligible = accessState.hasPremium || accessState.tier == AccessTier.PREMIUM
    val cloudLogs = CloudSyncLogger.entries.toList()
    val currentProfileKey = LocalProfileManager.currentProfileKey()
    val syncUserId = authUser?.uid?.trim().orEmpty()
    val firebaseDebugInfo = remember { FirebaseDebugInfoProvider.get() }

    CenteredNarrowContentContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = Locales.t("developer_mode_title"),
                fontSize = (22 * fontScale).sp,
                fontWeight = FontWeight.Bold,
                color = onBg
            )

            Text(
                text = Locales.t("developer_mode_hint"),
                fontSize = (14 * fontScale).sp,
                color = onBg.copy(alpha = 0.7f)
            )

            Divider()

            Text(
                text = "${Locales.t("developer_current_tier")}: $tierText",
                color = onSurface,
                fontSize = (16 * fontScale).sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "${Locales.t("developer_trial_active")}: ${accessState.isTrialActive}",
                color = onSurface.copy(alpha = 0.8f),
                fontSize = (14 * fontScale).sp
            )

            Text(
                text = "${Locales.t("developer_trial_days_left")}: ${accessState.trialDaysLeft}",
                color = onSurface.copy(alpha = 0.8f),
                fontSize = (14 * fontScale).sp
            )

            Text(
                text = "${Locales.t("developer_trial_started_at")}: ${AppSettings.trialStartedAtMillis}",
                color = onSurface.copy(alpha = 0.7f),
                fontSize = (13 * fontScale).sp
            )

            Text(
                text = "${Locales.t("developer_premium_unlocked")}: ${AppSettings.developerPremiumOverrideEnabled}",
                color = onSurface.copy(alpha = 0.7f),
                fontSize = (13 * fontScale).sp
            )
            Text(
                text = "Developer premium override: ${AppSettings.developerPremiumOverrideEnabled}",
                color = onSurface.copy(alpha = 0.7f),
                fontSize = (13 * fontScale).sp
            )

            Text(
                text = "Cached access tier: ${AppSettings.cachedAccessTier}",
                color = onSurface.copy(alpha = 0.7f),
                fontSize = (13 * fontScale).sp
            )

            Text(
                text = "Cached has premium: ${AppSettings.cachedHasPremium}",
                color = onSurface.copy(alpha = 0.7f),
                fontSize = (13 * fontScale).sp
            )

            Text(
                text = "Cached subscription state: ${AppSettings.cachedSubscriptionState}",
                color = onSurface.copy(alpha = 0.7f),
                fontSize = (13 * fontScale).sp
            )

            Text(
                text = "Premium subscription state: ${AppSettings.premiumSubscriptionState}",
                color = onSurface.copy(alpha = 0.7f),
                fontSize = (13 * fontScale).sp
            )

            Text(
                text = "Premium subscription expiry: ${AppSettings.premiumSubscriptionExpiryMillis}",
                color = onSurface.copy(alpha = 0.7f),
                fontSize = (13 * fontScale).sp
            )

            Text(
                text = "Access state tier (runtime): ${accessState.tier}",
                color = onSurface.copy(alpha = 0.7f),
                fontSize = (13 * fontScale).sp
            )

            Text(
                text = "Access state hasPremium (runtime): ${accessState.hasPremium}",
                color = onSurface.copy(alpha = 0.7f),
                fontSize = (13 * fontScale).sp
            )

            Text(
                text = "Premium gate result (stats/archive/unpaid): ${accessState.tier == AccessTier.PREMIUM || accessState.tier == AccessTier.TRIAL}",
                color = if (accessState.tier == AccessTier.PREMIUM || accessState.tier == AccessTier.TRIAL) {
                    MaterialTheme.colors.primary
                } else {
                    MaterialTheme.colors.error
                },
                fontSize = (13 * fontScale).sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            PrimaryActionButton(
                text = Locales.t("developer_enable_premium"),
                onClick = onEnablePremium
            )

            SecondaryActionButton(
                text = Locales.t("developer_disable_premium"),
                onClick = onDisablePremium
            )

            SecondaryActionButton(
                text = Locales.t("developer_reset_trial"),
                onClick = onResetTrialToNow
            )

            DangerActionButton(
                text = Locales.t("developer_expire_trial"),
                onClick = onExpireTrial
            )

            Spacer(Modifier.height(16.dp))
            Divider()
            Spacer(Modifier.height(8.dp))

            Text(
                text = Locales.t("support_section"),
                fontSize = (16 * fontScale).sp,
                fontWeight = FontWeight.SemiBold,
                color = onSurface.copy(alpha = 0.85f)
            )

            OutlinedTextField(
                value = supportPhoneDraft,
                onValueChange = { supportPhoneDraft = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(Locales.t("support_phone_label")) }
            )

            Text(
                text = Locales.t("support_phone_hint"),
                fontSize = (12 * fontScale).sp,
                color = onSurface.copy(alpha = 0.72f)
            )

            PrimaryActionButton(
                text = Locales.t("support_phone_save"),
                onClick = {
                    AppSettings.servicePhone = supportPhoneDraft.trim()
                    AppSettings.persist()
                }
            )

            Spacer(Modifier.height(16.dp))
            Divider()
            Spacer(Modifier.height(8.dp))

            Text(
                text = "Cloud Sync Debug",
                fontSize = (16 * fontScale).sp,
                fontWeight = FontWeight.SemiBold,
                color = onSurface.copy(alpha = 0.85f)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = onSurface.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .background(
                        color = MaterialTheme.colors.surface.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(14.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SelectionContainer {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Auth provider: $authProviderText",
                                fontSize = (13 * fontScale).sp,
                                color = onSurface
                            )

                            Text(
                                text = "Auth uid: ${authUser?.uid ?: "—"}",
                                fontSize = (13 * fontScale).sp,
                                color = onSurface
                            )

                            Text(
                                text = "Backend userId: ${AppSettings.backendUserId.ifBlank { "—" }}",
                                fontSize = (13 * fontScale).sp,
                                color = onSurface
                            )

                            Text(
                                text = "Current profile key: $currentProfileKey",
                                fontSize = (13 * fontScale).sp,
                                color = onSurface
                            )

                            Text(
                                text = "Cloud sync userId: ${if (syncUserId.isBlank()) "—" else syncUserId}",
                                fontSize = (13 * fontScale).sp,
                                color = onSurface
                            )

                            Text(
                                text = "Premium eligible: $premiumEligible",
                                fontSize = (13 * fontScale).sp,
                                color = onSurface
                            )

                            Text(
                                text = "Refresh in progress: ${state.isRefreshing}",
                                fontSize = (13 * fontScale).sp,
                                color = onSurface
                            )

                            Text(
                                text = "Appointments in memory: ${state.appointments.size}",
                                fontSize = (13 * fontScale).sp,
                                color = onSurface
                            )

                            Text(
                                text = "Visible appointments: ${state.appointments.count { !it.isDeleted }}",
                                fontSize = (13 * fontScale).sp,
                                color = onSurface
                            )

                            Text(
                                text = "Platform: ${firebaseDebugInfo.platform}",
                                fontSize = (13 * fontScale).sp,
                                color = onSurface
                            )

                            Text(
                                text = "Firebase projectId: ${firebaseDebugInfo.firebaseProjectId.ifBlank { "—" }}",
                                fontSize = (13 * fontScale).sp,
                                color = onSurface
                            )

                            Text(
                                text = "Firebase appId: ${firebaseDebugInfo.firebaseAppId.ifBlank { "—" }}",
                                fontSize = (13 * fontScale).sp,
                                color = onSurface
                            )

                            Text(
                                text = "Firestore databaseId: ${firebaseDebugInfo.firestoreDatabaseId.ifBlank { "—" }}",
                                fontSize = (13 * fontScale).sp,
                                color = onSurface
                            )

                            Text(
                                text = "Firestore host: ${firebaseDebugInfo.firestoreHost.ifBlank { "—" }}",
                                fontSize = (13 * fontScale).sp,
                                color = onSurface
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { state.logCloudSyncSnapshot() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Log sync snapshot")
                        }

                        OutlinedButton(
                            onClick = { state.forceCloudSyncFromDebug() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.cloudSyncInProgress
                        ) {
                            Text("Refresh")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Log entries: ${cloudLogs.size}",
                            fontSize = (12 * fontScale).sp,
                            color = onSurface.copy(alpha = 0.65f)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val fullText = buildString {
                                        appendLine("Auth provider: $authProviderText")
                                        appendLine("Auth uid: ${authUser?.uid ?: "—"}")
                                        appendLine("Backend userId: ${AppSettings.backendUserId.ifBlank { "—" }}")
                                        appendLine("Current profile key: $currentProfileKey")
                                        appendLine("Cloud sync userId: ${if (syncUserId.isBlank()) "—" else syncUserId}")
                                        appendLine("Premium eligible: $premiumEligible")
                                        appendLine("Sync in progress: ${state.cloudSyncInProgress}")
                                        appendLine("Appointments in memory: ${state.appointments.size}")
                                        appendLine("Visible appointments: ${state.appointments.count { !it.isDeleted }}")
                                        appendLine("Platform: ${firebaseDebugInfo.platform}")
                                        appendLine("Firebase projectId: ${firebaseDebugInfo.firebaseProjectId}")
                                        appendLine("Firebase appId: ${firebaseDebugInfo.firebaseAppId}")
                                        appendLine("Firestore databaseId: ${firebaseDebugInfo.firestoreDatabaseId}")
                                        appendLine("Firestore host: ${firebaseDebugInfo.firestoreHost}")
                                        appendLine()
                                        cloudLogs.forEach { appendLine(it) }
                                    }

                                    val copied = ClipboardHelper.copyText(
                                        label = "Cloud Sync Debug",
                                        text = fullText
                                    )
                                    copyStatusMessage = if (copied) {
                                        "Logs copied"
                                    } else {
                                        "Copy failed"
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy logs",
                                    tint = onSurface.copy(alpha = 0.75f)
                                )
                            }

                            Spacer(Modifier.width(4.dp))

                            OutlinedButton(
                                onClick = {
                                    CloudSyncLogger.clear()
                                    copyStatusMessage = null
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Clear logs")
                            }
                        }
                    }

                    if (copyStatusMessage != null) {
                        Text(
                            text = copyStatusMessage.orEmpty(),
                            fontSize = (12 * fontScale).sp,
                            color = onSurface.copy(alpha = 0.60f)
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 320.dp)
                            .background(
                                color = onSurface.copy(alpha = 0.04f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(10.dp)
                    ) {
                        if (cloudLogs.isEmpty()) {
                            Text(
                                text = "No sync logs yet.",
                                fontSize = (12 * fontScale).sp,
                                color = onSurface.copy(alpha = 0.55f)
                            )
                        } else {
                            SelectionContainer {
                                Column(
                                    modifier = Modifier.verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    cloudLogs.forEach { line ->
                                        Text(
                                            text = line,
                                            fontSize = (12 * fontScale).sp,
                                            color = onSurface.copy(alpha = 0.88f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Divider()
            Spacer(Modifier.height(8.dp))

            SecondaryActionButton(
                text = Locales.t("developer_logout"),
                onClick = onLogoutDeveloperMode
            )
        }
    }
}