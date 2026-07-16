package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.DataManager
import com.andrey.beautyplanner.Locales
import com.andrey.beautyplanner.notifications.Notifications
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlin.math.roundToInt
import com.andrey.beautyplanner.notifications.NotificationSoundPreviewPlayer
import androidx.compose.runtime.DisposableEffect

@Composable
fun NotificationsSettingsScreen() {
    val fontScale = AppSettings.getFontScale()
    val onSurface = MaterialTheme.colors.onSurface
    val onBg = MaterialTheme.colors.onBackground

    var daysSlider by remember { mutableStateOf(AppSettings.reminderDaysBefore.toFloat()) }
    var hoursSlider by remember { mutableStateOf(AppSettings.reminderHoursBefore.toFloat()) }
    var minutesSlider by remember { mutableStateOf((AppSettings.reminderMinutesBefore / 10).toFloat()) }

    val notificationsEnabled = AppSettings.notificationsEnabled
    val notificationSoundType = AppSettings.notificationSoundType
    val reminderDays = AppSettings.reminderDaysBefore
    val reminderHours = AppSettings.reminderHoursBefore
    val reminderMinutes = AppSettings.reminderMinutesBefore
    LaunchedEffect(
        notificationsEnabled,
        notificationSoundType,
        AppSettings.notificationSoundId,
        reminderDays,
        reminderHours,
        reminderMinutes
    ) {
        delay(600)
        val all = runCatching { DataManager.loadFromDatabase() }.getOrNull().orEmpty()
        val mins = AppSettings.reminderMinutesComputed()

        runCatching {
            if (AppSettings.notificationsEnabled && mins.isNotEmpty()) {
                Notifications.rescheduleAll(
                    appointments = all,
                    reminderMinutes = mins,
                    soundType = AppSettings.notificationSoundType,
                    soundId = AppSettings.notificationSoundId,
                    nowEpochMillis = Clock.System.now().toEpochMilliseconds()
                )
            } else {
                Notifications.cancelAll()
            }
        }
    }

    val soundItems = listOf(
        Triple(Locales.t("notif_sound_default"), "DEFAULT", ""),
        Triple(Locales.t("notif_sound_silent"), "SILENT", ""),
        Triple(Locales.t("notif_sound_1"), "BUNDLED", "sound1.wav"),
        Triple(Locales.t("notif_sound_2"), "BUNDLED", "sound2.wav"),
        Triple(Locales.t("notif_sound_3"), "BUNDLED", "sound3.wav"),
        Triple(Locales.t("notif_sound_4"), "BUNDLED", "sound4.wav"),
        Triple(Locales.t("notif_sound_5"), "BUNDLED", "sound5.wav"),
        Triple(Locales.t("notif_sound_6"), "BUNDLED", "sound6.wav"),
        Triple(Locales.t("notif_sound_7"), "BUNDLED", "sound7.wav"),
        Triple(Locales.t("notif_sound_8"), "BUNDLED", "sound8.wav"),
        Triple(Locales.t("notif_sound_9"), "BUNDLED", "sound9.wav"),
        Triple(Locales.t("notif_sound_10"), "BUNDLED", "sound10.wav"),
        Triple(Locales.t("notif_sound_11"), "BUNDLED", "sound11.wav")
    )

    CenteredNarrowContentContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = Locales.t("notifications_section"),
                fontSize = (22 * fontScale).sp,
                fontWeight = FontWeight.Bold,
                color = onBg
            )
            Text(
                text = Locales.t("notifications_settings_hint"),
                fontSize = (14 * fontScale).sp,
                color = onBg.copy(alpha = 0.82f)
            )
            androidx.compose.material.Divider()
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = Locales.t("notifications_enabled"),
                    fontSize = (16 * fontScale).sp,
                    color = onSurface
                )
                AppSwitch(
                    checked = AppSettings.notificationsEnabled,
                    onCheckedChange = {
                        AppSettings.notificationsEnabled = it
                        AppSettings.persist()
                    }
                )
            }
            SettingsDropdown(
                label = Locales.t("notif_sound_label"),
                selected = soundItems.firstOrNull {
                    when {
                        AppSettings.notificationSoundType == "BUNDLED" ->
                            it.second == "BUNDLED" && it.third == AppSettings.notificationSoundId

                        else ->
                            it.second == AppSettings.notificationSoundType
                    }
                }?.first ?: Locales.t("notif_sound_default"),
                items = soundItems.map { it.first },
                onSelect = { selected ->
                    val chosen = soundItems.firstOrNull { it.first == selected }

                    if (chosen != null) {
                        AppSettings.notificationSoundType = chosen.second
                        AppSettings.notificationSoundId = chosen.third
                        AppSettings.notificationSoundDisplayName = chosen.first
                        AppSettings.persist()

                        NotificationSoundPreviewPlayer.play(
                            soundType = chosen.second,
                            soundId = chosen.third
                        )
                    }
                }
            )
            if (
                AppSettings.notificationSoundType == "BUNDLED" ||
                AppSettings.notificationSoundType == "IMPORTED"
            ) {
                Text(
                    text = "${Locales.t("notif_sound_source")}: ${
                        if (AppSettings.notificationSoundType == "BUNDLED") {
                            Locales.t("notif_sound_bundled")
                        } else {
                            Locales.t("notif_sound_imported")
                        }
                    }",
                    fontSize = (13 * fontScale).sp,
                    color = onSurface.copy(alpha = 0.65f)
                )

                Text(
                    text = if (AppSettings.notificationSoundDisplayName.isBlank()) {
                        Locales.t("notif_sound_not_selected")
                    } else {
                        AppSettings.notificationSoundDisplayName
                    },
                    fontSize = (13 * fontScale).sp,
                    color = onSurface.copy(alpha = 0.75f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = Locales.t("reminders_when"),
                fontSize = (14 * fontScale).sp,
                fontWeight = FontWeight.SemiBold,
                color = onSurface.copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${Locales.t("remind_days")}: ${Locales.daysCount(AppSettings.reminderDaysBefore)}",
                fontSize = (15 * fontScale).sp,
                fontWeight = FontWeight.SemiBold,
                color = onSurface
            )
            Slider(
                value = daysSlider,
                onValueChange = { daysSlider = it },
                onValueChangeFinished = {
                    AppSettings.reminderDaysBefore = daysSlider.roundToInt().coerceIn(0, 7)
                    AppSettings.persist()
                },
                valueRange = 0f..7f,
                steps = 6,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colors.primary,
                    activeTrackColor = MaterialTheme.colors.primary.copy(alpha = 0.85f),
                    inactiveTrackColor = onSurface.copy(alpha = 0.20f)
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${Locales.t("remind_hours")}: ${Locales.hoursCount(AppSettings.reminderHoursBefore)}",
                fontSize = (15 * fontScale).sp,
                fontWeight = FontWeight.SemiBold,
                color = onSurface
            )
            Slider(
                value = hoursSlider,
                onValueChange = { hoursSlider = it },
                onValueChangeFinished = {
                    AppSettings.reminderHoursBefore = hoursSlider.roundToInt().coerceIn(0, 23)
                    AppSettings.persist()
                },
                valueRange = 0f..23f,
                steps = 22,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colors.primary,
                    activeTrackColor = MaterialTheme.colors.primary.copy(alpha = 0.85f),
                    inactiveTrackColor = onSurface.copy(alpha = 0.20f)
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${Locales.t("remind_minutes")}: ${Locales.minutesCount(AppSettings.reminderMinutesBefore)}",
                fontSize = (15 * fontScale).sp,
                fontWeight = FontWeight.SemiBold,
                color = onSurface
            )
            Slider(
                value = minutesSlider,
                onValueChange = { minutesSlider = it },
                onValueChangeFinished = {
                    AppSettings.reminderMinutesBefore =
                        (minutesSlider.roundToInt().coerceIn(0, 5)) * 10
                    AppSettings.persist()
                },
                valueRange = 0f..5f,
                steps = 4,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colors.primary,
                    activeTrackColor = MaterialTheme.colors.primary.copy(alpha = 0.85f),
                    inactiveTrackColor = onSurface.copy(alpha = 0.20f)
                )
            )
            val totalMinutes =
                AppSettings.reminderDaysBefore * 24 * 60 +
                        AppSettings.reminderHoursBefore * 60 +
                        AppSettings.reminderMinutesBefore

            val summaryParts = buildList {
                if (AppSettings.reminderDaysBefore > 0) {
                    add(Locales.daysCount(AppSettings.reminderDaysBefore))
                }
                if (AppSettings.reminderHoursBefore > 0) {
                    add(Locales.hoursCount(AppSettings.reminderHoursBefore))
                }
                if (AppSettings.reminderMinutesBefore > 0) {
                    add(Locales.minutesCount(AppSettings.reminderMinutesBefore))
                }
            }

            val summary = if (totalMinutes <= 0 || summaryParts.isEmpty()) {
                Locales.t("remind_off")
            } else {
                summaryParts.joinToString(" • ")
            }
            Text(
                text = "${Locales.t("remind_summary")}: $summary",
                fontSize = (13 * fontScale).sp,
                color = onSurface.copy(alpha = 0.72f)
            )
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            NotificationSoundPreviewPlayer.stop()
        }
    }
}