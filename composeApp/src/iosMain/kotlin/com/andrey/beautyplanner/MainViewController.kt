package com.andrey.beautyplanner

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController {
    AppSettings.load()
    App()
}