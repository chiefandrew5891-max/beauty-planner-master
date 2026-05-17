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
    val themeColors = if (AppSettings.isDarkMode) {
        darkColors(
            primary = Color(0xFF8AB4F8),
            onPrimary = Color.Black,
            surface = Color(0xFF1E1E1E),
            onSurface = Color.White
        )
    } else {
        lightColors(
            primary = Color(0xFF4285F4),
            onPrimary = Color.White,
            surface = Color.White,
            onSurface = Color.Black
        )
    }

    MaterialTheme(colors = themeColors) { content() }
}