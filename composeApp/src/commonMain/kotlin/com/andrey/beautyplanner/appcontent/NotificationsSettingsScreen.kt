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
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
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
import com.andrey.beautyplanner.notifications.NotificationSound
import com.andrey.beautyplanner.notifications.Notifications
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlin.math.roundToInt

@Composable
fun NotificationsSettingsScreen() {
    val fontScale = AppSettings.getFontScale()
    val onSurface = MaterialTheme.colors.onSurface
    val onBg = MaterialTheme.colors.onBackground

    var daysSlider by remember { mutableStateOf(AppSettings.reminderDaysBefore.toFloat()) }
    var hoursSlider by remember { mutableStateOf(AppSettings.reminderHoursBefore.toFloat()) }

    val notificationsEnabled = AppSettings.notificationsEnabled
    val notificationSound = AppSettings.notificationSound
    val reminderDays = AppSettings.reminderDaysBefore
    val reminderHours = AppSettings.reminderHoursBefore

    LaunchedEffect(notificationsEnabled, notificationSound, reminderDays, reminderHours) {
        delay(600)
        val all = runCatching { DataManager.loadFromDatabase() }.getOrNull().orEmpty()
        val mins = AppSettings.reminderMinutesComputed()

        runCatching {
            if (AppSettings.notificationsEnabled && mins.isNotEmpty()) {
                Notifications.rescheduleAll(
                    appointments = all,
                    reminderMinutes = mins,
                    sound = AppSettings.notificationSound,
                    nowEpochMillis = Clock.System.now().toEpochMilliseconds()
                )
            } else {
                Notifications.cancelAll()
            }
        }
    }

    val soundItems = listOf(
        Locales.t("notif_sound_default") to NotificationSound.DEFAULT,
        Locales.t("notif_sound_silent") to NotificationSound.SILENT
    )

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
            color = onBg.copy(alpha = 0.7f)
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

            Switch(
                checked = AppSettings.notificationsEnabled,
                onCheckedChange = {
                    AppSettings.notificationsEnabled = it
                    AppSettings.persist()
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colors.primary,
                    checkedTrackColor = MaterialTheme.colors.primary.copy(alpha = 0.35f),
                    uncheckedThumbColor = onSurface.copy(alpha = 0.45f),
                    uncheckedTrackColor = onSurface.copy(alpha = 0.20f)
                )
            )
        }

        SettingsDropdown(
            label = Locales.t("notif_sound_label"),
            selected = soundItems.firstOrNull { it.second == AppSettings.notificationSound }?.first
                ?: Locales.t("notif_sound_default"),
            items = soundItems.map { it.first },
            onSelect = { selected ->
                val sound =
                    soundItems.firstOrNull { it.first == selected }?.second
                        ?: NotificationSound.DEFAULT
                AppSettings.notificationSound = sound
                AppSettings.persist()
            }
        )

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
                AppSettings.reminderDaysBefore = daysSlider.roundToInt().coerceIn(0, 3)
                AppSettings.persist()
            },
            valueRange = 0f..3f,
            steps = 0,
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
                AppSettings.reminderHoursBefore = hoursSlider.roundToInt().coerceIn(0, 12)
                AppSettings.persist()
            },
            valueRange = 0f..12f,
            steps = 0,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colors.primary,
                activeTrackColor = MaterialTheme.colors.primary.copy(alpha = 0.85f),
                inactiveTrackColor = onSurface.copy(alpha = 0.20f)
            )
        )

        val totalMinutes =
            AppSettings.reminderDaysBefore * 24 * 60 + AppSettings.reminderHoursBefore * 60

        val summary = if (totalMinutes <= 0) {
            Locales.t("remind_off")
        } else {
            "${Locales.daysCount(AppSettings.reminderDaysBefore)} • ${Locales.hoursCount(AppSettings.reminderHoursBefore)}"
        }

        Text(
            text = "${Locales.t("remind_summary")}: $summary",
            fontSize = (13 * fontScale).sp,
            color = onSurface.copy(alpha = 0.60f)
        )
    }
}