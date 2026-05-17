package com.andrey.beautyplanner.appcontent

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object UiColors {
    @Composable fun primaryText(): Color = MaterialTheme.colors.onSurface
    @Composable fun secondaryText(): Color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f)
    @Composable fun hintText(): Color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f)
}