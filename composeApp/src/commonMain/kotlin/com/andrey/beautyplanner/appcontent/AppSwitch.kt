package com.andrey.beautyplanner.appcontent

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.runtime.Composable

@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colors.primary,
            checkedTrackColor = MaterialTheme.colors.primary.copy(alpha = 0.5f),

            uncheckedThumbColor = MaterialTheme.colors.onSurface.copy(alpha = 0.85f),
            uncheckedTrackColor = MaterialTheme.colors.onSurface.copy(alpha = 0.30f),

            disabledCheckedThumbColor = MaterialTheme.colors.primary.copy(alpha = 0.45f),
            disabledCheckedTrackColor = MaterialTheme.colors.primary.copy(alpha = 0.20f),

            disabledUncheckedThumbColor = MaterialTheme.colors.onSurface.copy(alpha = 0.35f),
            disabledUncheckedTrackColor = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
        )
    )
}