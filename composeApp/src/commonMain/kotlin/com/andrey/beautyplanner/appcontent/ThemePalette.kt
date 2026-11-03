package com.andrey.beautyplanner.appcontent

import androidx.compose.material.Colors
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color

object ThemePalette {

    fun colors(dark: Boolean, cream: Boolean): Colors = when {
        dark -> darkColors(
            primary = Color(0xFF8AB4F8),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            onPrimary = Color.Black,
            onSurface = Color.White,
            onBackground = Color.White
        )

        cream -> lightColors(
            primary = Color(0xFFB98C6B),
            primaryVariant = Color(0xFF9A6F50),
            secondary = Color(0xFFD9B8A0),
            background = Color(0xFFFBF5EE),
            surface = Color(0xFFFFFFFF),
            onPrimary = Color.White,
            onSecondary = Color(0xFF3E2F27),
            onSurface = Color(0xFF3E2F27),
            onBackground = Color(0xFF3E2F27)
        )

        else -> lightColors(
            primary = Color(0xFF4285F4),
            background = Color.White,
            surface = Color.White,
            onPrimary = Color.White,
            onSurface = Color.Black,
            onBackground = Color.Black
        )
    }

    // Прошедший день в DayDetailsView
    fun dayPast(dark: Boolean, cream: Boolean): Color = when {
        dark -> Color(0xFF1F2A36)
        cream -> Color(0xFFEFE2D3)
        else -> Color(0xFFECECEC)
    }

    // Сегодняшний и будущие дни в DayDetailsView
    fun dayFuture(dark: Boolean, cream: Boolean): Color = when {
        dark -> Color(0xFF253548)
        cream -> Color(0xFFF7EDE2)
        else -> Color(0xFFF2F2F2)
    }
    // Фон шапки приложения
    fun headerBackground(dark: Boolean, cream: Boolean, surface: Color): Color = when {
        dark -> surface
        cream -> Color(0xFFF3E7DA)
        else -> surface
    }

    // Иконки шапки: домой, назад, меню, шестерёнка, поиск
    fun headerIcon(dark: Boolean, cream: Boolean, primary: Color): Color = when {
        dark -> primary
        cream -> Color(0xFF6F4E37)
        else -> primary
    }
}