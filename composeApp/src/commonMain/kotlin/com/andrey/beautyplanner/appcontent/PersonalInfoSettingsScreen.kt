package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.CloudSyncLogger
import com.andrey.beautyplanner.Locales
import com.andrey.beautyplanner.ProfileAvatarUrlProcessor
import com.andrey.beautyplanner.ProfileImagePicker
import com.andrey.beautyplanner.remote.MasterProfileSync
import com.andrey.beautyplanner.rememberProfileAvatarBitmap
import kotlinx.coroutines.launch
import androidx.compose.material.Button
import androidx.compose.material.TextButton
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.andrey.beautyplanner.auth.SignInProvider

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PersonalInfoSettingsScreen() {
    val fontScale = AppSettings.getFontScale()
    val onBg = MaterialTheme.colors.onBackground
    val onSurface = MaterialTheme.colors.onSurface

    var userNameDraft by remember { mutableStateOf(AppSettings.ownerName) }
    var phoneDraft by remember { mutableStateOf(AppSettings.profilePhone) }
    var avatarUrlDraft by remember { mutableStateOf(AppSettings.profileAvatarUrl) }
    var avatarBase64Draft by remember { mutableStateOf(AppSettings.profileAvatarBase64) }
    var phoneVisibleDraft by remember { mutableStateOf(AppSettings.profilePhoneVisible) }
    var displayCustomNameDraft by remember { mutableStateOf(AppSettings.profileDisplayCustomName) }
    var specializationDraft by remember { mutableStateOf(AppSettings.profileSpecialization) }
    var avatarUrlErrorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var pendingRawBase64 by remember { mutableStateOf<String?>(null) }
    var isRefreshingProfile by remember { mutableStateOf(false) }

    var showDeleteAccountWarningDialog by remember { mutableStateOf(false) }
    var showDeleteAccountPasswordDialog by remember { mutableStateOf(false) }
    var deleteAccountPasswordDraft by remember { mutableStateOf("") }
    var deleteAccountPasswordError by remember { mutableStateOf<String?>(null) }
    var isDeletingAccount by remember { mutableStateOf(false) }

    fun applySettingsToDrafts() {
        userNameDraft = AppSettings.ownerName
        phoneDraft = AppSettings.profilePhone
        avatarUrlDraft = AppSettings.profileAvatarUrl
        avatarBase64Draft = AppSettings.profileAvatarBase64
        phoneVisibleDraft = AppSettings.profilePhoneVisible
        displayCustomNameDraft = AppSettings.profileDisplayCustomName
        specializationDraft = AppSettings.profileSpecialization
    }

    val scope = rememberCoroutineScope()
    val appState = com.andrey.beautyplanner.appcontent.approot.rememberAppRootState()
    val authProvider = appState.currentAuthUser?.provider

    val hasChanges =
        userNameDraft.trim() != AppSettings.ownerName.trim() ||
                phoneDraft.trim() != AppSettings.profilePhone.trim() ||
                avatarUrlDraft.trim() != AppSettings.profileAvatarUrl.trim() ||
                avatarBase64Draft != AppSettings.profileAvatarBase64 ||
                phoneVisibleDraft != AppSettings.profilePhoneVisible ||
                displayCustomNameDraft != AppSettings.profileDisplayCustomName ||
                specializationDraft.trim() != AppSettings.profileSpecialization.trim()

    val avatarBitmap = rememberProfileAvatarBitmap(avatarBase64Draft)

    fun refreshMasterProfile(force: Boolean) {
        scope.launch {
            isRefreshingProfile = true
            MasterProfileSync.pullIfAuthenticated(force = force)
                .onSuccess {
                    applySettingsToDrafts()
                }
                .onFailure {
                    CloudSyncLogger.log("pullMasterProfile: failed: ${it.message}")
                }
            isRefreshingProfile = false
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshingProfile,
        onRefresh = { refreshMasterProfile(force = true) }
    )

    LaunchedEffect(Unit) {
        applySettingsToDrafts()
        refreshMasterProfile(force = true)
    }

    pendingRawBase64?.let { rawBase64 ->
        AvatarCropEditorDialog(
            rawBase64 = rawBase64,
            onConfirm = { cropped ->
                avatarBase64Draft = cropped
                pendingRawBase64 = null
            },
            onDismiss = {
                pendingRawBase64 = null
            }
        )
    }
    if (showDeleteAccountWarningDialog) {
        androidx.compose.material.AlertDialog(
            onDismissRequest = {
                if (!isDeletingAccount) {
                    showDeleteAccountWarningDialog = false
                }
            },
            title = {
                Text(
                    text = Locales.t("account_delete_confirm_title"),
                    fontSize = (18 * fontScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onSurface
                )
            },
            text = {
                Text(
                    text = Locales.t("account_delete_confirm_message"),
                    fontSize = (14 * fontScale).sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.85f),
                    lineHeight = (20 * fontScale).sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountWarningDialog = false
                        when (authProvider) {
                            com.andrey.beautyplanner.auth.SignInProvider.EMAIL -> {
                                deleteAccountPasswordDraft = ""
                                deleteAccountPasswordError = null
                                showDeleteAccountPasswordDialog = true
                            }
                            com.andrey.beautyplanner.auth.SignInProvider.GOOGLE -> {
                                // TODO: start Google reauthentication flow before calling deleteMyAccount
                            }
                            com.andrey.beautyplanner.auth.SignInProvider.APPLE -> {
                                // TODO: start Apple reauthentication flow before calling deleteMyAccount
                            }
                            else -> {
                                // TODO: handle anonymous / unsupported provider deletion path
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(Locales.t("account_delete_confirm_continue"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!isDeletingAccount) {
                            showDeleteAccountWarningDialog = false
                        }
                    }
                ) {
                    Text(
                        text = Locales.t("account_delete_confirm_cancel"),
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.85f)
                    )
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
    if (showDeleteAccountPasswordDialog) {
        androidx.compose.material.AlertDialog(
            onDismissRequest = {
                if (!isDeletingAccount) {
                    showDeleteAccountPasswordDialog = false
                    deleteAccountPasswordDraft = ""
                    deleteAccountPasswordError = null
                }
            },
            title = {
                Text(
                    text = Locales.t("account_delete_email_password_title"),
                    fontSize = (18 * fontScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onSurface
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = Locales.t("account_delete_email_password_description"),
                        fontSize = (14 * fontScale).sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.80f),
                        lineHeight = (20 * fontScale).sp
                    )

                    OutlinedTextField(
                        value = deleteAccountPasswordDraft,
                        onValueChange = {
                            deleteAccountPasswordDraft = it
                            deleteAccountPasswordError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        placeholder = {
                            Text(
                                text = Locales.t("account_delete_email_password_placeholder"),
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.50f)
                            )
                        },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        textStyle = TextStyle(
                            fontFamily = appFontFamily(),
                            fontSize = (15 * fontScale).sp,
                            color = MaterialTheme.colors.onSurface
                        ),
                        isError = !deleteAccountPasswordError.isNullOrBlank(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = MaterialTheme.colors.onSurface,
                            focusedBorderColor = MaterialTheme.colors.primary,
                            unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.28f),
                            focusedLabelColor = MaterialTheme.colors.primary,
                            unfocusedLabelColor = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
                            cursorColor = MaterialTheme.colors.primary,
                            backgroundColor = MaterialTheme.colors.surface,
                            placeholderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.50f),
                            errorBorderColor = MaterialTheme.colors.error,
                            errorCursorColor = MaterialTheme.colors.error
                        )
                    )

                    deleteAccountPasswordError?.takeIf { it.isNotBlank() }?.let { errorText ->
                        Text(
                            text = errorText,
                            fontSize = (12 * fontScale).sp,
                            color = MaterialTheme.colors.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // TODO:
                        // 1. reauthenticate email/password
                        // 2. call backend deleteMyAccount
                        // 3. clear local session and navigate to auth screen
                    },
                    enabled = deleteAccountPasswordDraft.isNotBlank() && !isDeletingAccount,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (isDeletingAccount) {
                            Locales.t("account_delete_in_progress")
                        } else {
                            Locales.t("account_delete_email_password_confirm")
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!isDeletingAccount) {
                            showDeleteAccountPasswordDialog = false
                            deleteAccountPasswordDraft = ""
                            deleteAccountPasswordError = null
                        }
                    }
                ) {
                    Text(
                        text = Locales.t("account_delete_confirm_cancel"),
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.85f)
                    )
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        CenteredNarrowContentContainer {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = Locales.t("profile_master_title"),
                    fontSize = (22 * fontScale).sp,
                    fontWeight = FontWeight.Bold,
                    color = onBg
                )

                Text(
                    text = Locales.t("profile_master_description"),
                    fontSize = (14 * fontScale).sp,
                    color = onBg.copy(alpha = 0.7f)
                )

                Divider()

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarBitmap != null) {
                            Image(
                                bitmap = avatarBitmap,
                                contentDescription = Locales.t("profile_avatar_cd"),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colors.onSurface.copy(alpha = 0.10f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userNameDraft.trim().take(1).ifBlank { "?" }.uppercase(),
                                    fontSize = (72 * fontScale).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = onSurface.copy(alpha = 0.65f)
                                )
                            }
                        }
                    }

                    if (userNameDraft.trim().isNotBlank()) {
                        Text(
                            text = userNameDraft.trim(),
                            fontSize = (24 * fontScale).sp,
                            fontWeight = FontWeight.Bold,
                            color = onBg,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (specializationDraft.trim().isNotBlank()) {
                        Text(
                            text = specializationDraft.trim(),
                            fontSize = (14 * fontScale).sp,
                            color = onSurface.copy(alpha = 0.72f),
                            textAlign = TextAlign.Center
                        )
                    }

                    if (phoneDraft.trim().isNotBlank() && phoneVisibleDraft) {
                        Text(
                            text = phoneDraft.trim(),
                            fontSize = (14 * fontScale).sp,
                            color = onSurface.copy(alpha = 0.72f),
                            textAlign = TextAlign.Center
                        )
                    }

                    // =========================================================
                    // TEMP HIDE FOR APP REVIEW: PROFILE RATING BLOCK
                    // Рейтинг мастера временно скрыт на экране профиля.
                    // BEGIN TEMP HIDE
                    // =========================================================
                    // ProfileRatingBlock(rating = AppSettings.profileRating)
                    // =========================================================
                    // END TEMP HIDE FOR APP REVIEW: PROFILE RATING BLOCK
                    // =========================================================
                }

                Divider()

                ProfileTextField(
                    title = Locales.t("user_name_label"),
                    value = userNameDraft,
                    onValueChange = { userNameDraft = it },
                    placeholder = Locales.t("user_name_hint")
                )

                if (userNameDraft.trim().isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Locales.t("profile_display_name_switch"),
                            fontSize = (14 * fontScale).sp,
                            color = onSurface.copy(alpha = 0.85f),
                            modifier = Modifier.weight(1f)
                        )
                        AppSwitch(
                            checked = displayCustomNameDraft,
                            onCheckedChange = { displayCustomNameDraft = it }
                        )
                    }
                }

                ProfileTextField(
                    title = Locales.t("profile_specialization_label"),
                    value = specializationDraft,
                    onValueChange = { specializationDraft = it },
                    placeholder = Locales.t("profile_specialization_hint")
                )

                ProfileTextField(
                    title = Locales.t("profile_phone_label"),
                    value = phoneDraft,
                    onValueChange = { phoneDraft = it },
                    placeholder = Locales.t("profile_phone_hint")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Locales.t("profile_show_phone"),
                        fontSize = (16 * fontScale).sp,
                        color = onSurface
                    )
                    AppSwitch(
                        checked = phoneVisibleDraft,
                        onCheckedChange = { phoneVisibleDraft = it }
                    )
                }

                ProfileTextField(
                    title = Locales.t("profile_avatar_url_label"),
                    value = avatarUrlDraft,
                    onValueChange = {
                        avatarUrlDraft = it
                        avatarUrlErrorMessage = null
                    },
                    placeholder = Locales.t("profile_avatar_url_hint")
                )

                avatarUrlErrorMessage?.takeIf { it.isNotBlank() }?.let { errorMessage ->
                    Text(
                        text = errorMessage,
                        fontSize = (13 * fontScale).sp,
                        color = MaterialTheme.colors.error
                    )
                }

                SecondaryActionButton(
                    text = Locales.t("profile_pick_photo"),
                    onClick = {
                        avatarUrlErrorMessage = null
                        ProfileImagePicker.pickImage { rawBase64 ->
                            if (!rawBase64.isNullOrBlank()) {
                                pendingRawBase64 = rawBase64
                            }
                        }
                    }
                )

                SecondaryActionButton(
                    text = Locales.t("profile_remove_photo"),
                    onClick = {
                        avatarUrlErrorMessage = null
                        avatarBase64Draft = ""
                    },
                    enabled = avatarBase64Draft.isNotBlank()
                )

                Spacer(Modifier.height(6.dp))

                PrimaryActionButton(
                    text = if (isSaving) Locales.t("loading") else Locales.t("save"),
                    onClick = {
                        if (isSaving) return@PrimaryActionButton

                        val nextOwnerName = userNameDraft.trim()
                        val nextPhone = phoneDraft.trim()
                        val nextAvatarUrl = avatarUrlDraft.trim()
                        val nextSpecialization = specializationDraft.trim()
                        val shouldProcessAvatarUrl =
                            nextAvatarUrl.isNotBlank() &&
                                    nextAvatarUrl != AppSettings.profileAvatarUrl

                        val persistProfile: (String) -> Unit = { finalAvatarBase64 ->
                            AppSettings.ownerName = nextOwnerName
                            AppSettings.profilePhone = nextPhone
                            AppSettings.profilePhoneVisible = phoneVisibleDraft
                            AppSettings.profileAvatarUrl = nextAvatarUrl
                            AppSettings.profileAvatarBase64 = finalAvatarBase64
                            AppSettings.profileDisplayCustomName = displayCustomNameDraft
                            AppSettings.profileSpecialization = nextSpecialization
                            AppSettings.persist()
                        }

                        avatarUrlErrorMessage = null

                        if (!shouldProcessAvatarUrl) {
                            persistProfile(avatarBase64Draft)
                            scope.launch {
                                MasterProfileSync.syncIfAuthenticated()
                                    .onFailure {
                                        CloudSyncLogger.log("syncMasterProfile: failed: ${it.message}")
                                    }
                            }
                            return@PrimaryActionButton
                        }

                        isSaving = true
                        runCatching {
                            ProfileAvatarUrlProcessor.processAvatar(nextAvatarUrl) { processedBase64 ->
                                isSaving = false
                                if (processedBase64.isNullOrBlank()) {
                                    avatarUrlErrorMessage = Locales.t("profile_avatar_url_error")
                                    return@processAvatar
                                }

                                avatarBase64Draft = processedBase64
                                persistProfile(processedBase64)
                                scope.launch {
                                    MasterProfileSync.syncIfAuthenticated()
                                        .onFailure {
                                            CloudSyncLogger.log("syncMasterProfile: failed: ${it.message}")
                                        }
                                }
                            }
                        }.onFailure {
                            isSaving = false
                            avatarUrlErrorMessage = Locales.t("profile_avatar_url_error")
                        }
                    },
                    enabled = hasChanges && !isSaving
                )
                Spacer(Modifier.height(10.dp))
                Divider()
                Spacer(Modifier.height(6.dp))

                Text(
                    text = Locales.t("account_management_title"),
                    fontSize = (18 * fontScale).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = onSurface.copy(alpha = 0.90f)
                )

                Text(
                    text = Locales.t("account_management_description"),
                    fontSize = (13 * fontScale).sp,
                    color = onSurface.copy(alpha = 0.72f),
                    lineHeight = (19 * fontScale).sp
                )

                AccountDeleteActionButton(
                    text = Locales.t("account_delete_button"),
                    onClick = {
                        deleteAccountPasswordDraft = ""
                        deleteAccountPasswordError = null
                        showDeleteAccountWarningDialog = true
                    },
                    enabled = !isDeletingAccount
                )
            }
        }

        PullRefreshIndicator(
            refreshing = isRefreshingProfile,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun ProfileTextField(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    val fontScale = AppSettings.getFontScale()
    val onSurface = MaterialTheme.colors.onSurface

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            fontSize = (16 * fontScale).sp,
            fontWeight = FontWeight.SemiBold,
            color = onSurface.copy(alpha = 0.85f)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            placeholder = {
                Text(
                    text = placeholder,
                    color = onSurface.copy(alpha = 0.50f)
                )
            },
            textStyle = TextStyle(
                fontFamily = appFontFamily(),
                fontSize = (16 * fontScale).sp,
                color = onSurface
            ),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = onSurface,
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = onSurface.copy(alpha = 0.28f),
                focusedLabelColor = MaterialTheme.colors.primary,
                unfocusedLabelColor = onSurface.copy(alpha = 0.68f),
                cursorColor = MaterialTheme.colors.primary,
                backgroundColor = MaterialTheme.colors.surface,
                placeholderColor = onSurface.copy(alpha = 0.50f)
            )
        )
    }
}

@Composable
private fun AccountDeleteActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    androidx.compose.material.Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = androidx.compose.material.ButtonDefaults.elevation(
            defaultElevation = 4.dp,
            pressedElevation = 6.dp,
            disabledElevation = 0.dp
        ),
        colors = androidx.compose.material.ButtonDefaults.buttonColors(
            backgroundColor = androidx.compose.ui.graphics.Color(0xFFDB4437),
            contentColor = androidx.compose.ui.graphics.Color.White,
            disabledBackgroundColor = androidx.compose.ui.graphics.Color(0xFFDB4437).copy(alpha = 0.45f),
            disabledContentColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f)
        ),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        Text(
            text = text,
            fontSize = (15 * AppSettings.getFontScale()).sp,
            fontWeight = FontWeight.Medium
        )
    }
}