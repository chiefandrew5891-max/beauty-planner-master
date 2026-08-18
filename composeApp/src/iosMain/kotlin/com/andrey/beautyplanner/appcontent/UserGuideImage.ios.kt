package com.andrey.beautyplanner.appcontent

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.andrey.beautyplanner.generated.resources.Res
import com.andrey.beautyplanner.generated.resources.allDrawableResources
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class)
@Composable
actual fun guidePainterOrNull(
    imageName: String
): Painter? {
    val resource = Res.allDrawableResources[imageName] ?: return null
    return painterResource(resource)
}