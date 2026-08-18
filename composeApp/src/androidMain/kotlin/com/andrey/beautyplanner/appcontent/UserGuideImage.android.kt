package com.andrey.beautyplanner.appcontent

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

@Composable
actual fun guidePainterOrNull(
    imageName: String
): Painter? {
    val context = LocalContext.current
    val resId = context.resources.getIdentifier(
        imageName,
        "drawable",
        context.packageName
    )

    return if (resId != 0) {
        painterResource(id = resId)
    } else {
        null
    }
}