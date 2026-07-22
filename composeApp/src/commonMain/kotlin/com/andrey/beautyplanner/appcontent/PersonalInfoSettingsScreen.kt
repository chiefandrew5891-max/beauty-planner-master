package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Locales
import com.andrey.beautyplanner.ProfileAvatarUrlProcessor
import com.andrey.beautyplanner.ProfileImagePicker
import com.andrey.beautyplanner.rememberProfileAvatarBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.ContentScale


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

    // Raw (uncropped) base64 returned by the picker; triggers the crop editor dialog
    var pendingRawBase64 by remember { mutableStateOf<String?>(null) }

    val hasChanges =
        userNameDraft.trim() != AppSettings.ownerName.trim() ||
                phoneDraft.trim() != AppSettings.profilePhone.trim() ||
                avatarUrlDraft.trim() != AppSettings.profileAvatarUrl.trim() ||
                avatarBase64Draft != AppSettings.profileAvatarBase64 ||
                phoneVisibleDraft != AppSettings.profilePhoneVisible ||
                displayCustomNameDraft != AppSettings.profileDisplayCustomName ||
                specializationDraft.trim() != AppSettings.profileSpecialization.trim()

    val avatarBitmap = rememberProfileAvatarBitmap(avatarBase64Draft)

    // Show avatar crop editor when a raw (uncropped) image has been picked
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

                ProfileRatingBlock(rating = AppSettings.profileRating)
            }

            Divider()

            ProfileTextField(
                title = Locales.t("user_name_label"),
                value = userNameDraft,
                onValueChange = { userNameDraft = it },
                placeholder = Locales.t("user_name_hint")
            )

            // Display name preference switch (show only when a name is set)
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
                        return@PrimaryActionButton
                    }

                    isSaving = true
                    ProfileAvatarUrlProcessor.processAvatar(nextAvatarUrl) { processedBase64 ->
                        isSaving = false
                        if (processedBase64.isNullOrBlank()) {
                            avatarUrlErrorMessage = Locales.t("profile_avatar_url_error")
                            return@processAvatar
                        }

                        avatarBase64Draft = processedBase64
                        persistProfile(processedBase64)
                    }
                },
                enabled = hasChanges && !isSaving
            )
        }
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
            colors = androidx.compose.material.TextFieldDefaults.outlinedTextFieldColors(
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
