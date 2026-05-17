package com.andrey.beautyplanner.appcontent

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material.LocalContentColor
import androidx.compose.material.ProvideTextStyle
import com.andrey.beautyplanner.appcontent.approot.AppRootChrome
import com.andrey.beautyplanner.appcontent.approot.AppRootContent
import com.andrey.beautyplanner.appcontent.approot.AppRootDialogs
import com.andrey.beautyplanner.appcontent.approot.rememberAppRootState

@Composable
fun AppRoot() {
    val state = rememberAppRootState()

    MaterialTheme(colors = state.colors, typography = state.customTypography) {
        // ✅ “железный” базовый цвет текста для всего дерева
        Surface(color = MaterialTheme.colors.background, contentColor = MaterialTheme.colors.onBackground) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colors.onBackground) {
                ProvideTextStyle(MaterialTheme.typography.body1) {
                    AppRootChrome(state = state) { padding ->
                        AppRootContent(state = state, padding = padding)
                        AppRootDialogs(state = state)
                    }
                }
            }
        }
    }
}