package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.andrey.beautyplanner.AppSettings

val AppDialogShape = RoundedCornerShape(16.dp)

@Composable
fun AppDialogTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = ThemePalette.colors(AppSettings.isDarkMode, AppSettings.isCreamTheme)
    ) { content() }
}