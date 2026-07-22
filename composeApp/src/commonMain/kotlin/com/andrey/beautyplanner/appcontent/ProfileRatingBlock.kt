package com.andrey.beautyplanner.appcontent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Locales
import com.andrey.beautyplanner.formatProfileRating
import com.andrey.beautyplanner.profileRatingStarFillFractions

private val RatingActiveColor = Color(0xFFFFA726)
private val RatingInactiveColor = Color(0xFFE3C08A)

@Composable
fun ProfileRatingBlock(
    rating: Float,
    modifier: Modifier = Modifier
) {
    val fontScale = AppSettings.getFontScale()
    val onSurface = MaterialTheme.colors.onSurface

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = Locales.t("profile_rating_label"),
            fontSize = (14 * fontScale).sp,
            fontWeight = FontWeight.Medium,
            color = onSurface.copy(alpha = 0.74f)
        )

        Text(
            text = formatProfileRating(rating),
            fontSize = (18 * fontScale).sp,
            fontWeight = FontWeight.Bold,
            color = onSurface
        )

        FractionalStarRatingRow(rating = rating)
    }
}

@Composable
private fun FractionalStarRatingRow(rating: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(0.46f),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        profileRatingStarFillFractions(rating).forEach { fillFraction ->
            FractionalStar(fillFraction = fillFraction)
        }
    }
}

@Composable
private fun FractionalStar(fillFraction: Float) {
    // Draw the muted star first, then clip a second full star to the desired width
    // so fractional ratings keep a clean silhouette and visible unfilled remainder.
    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = RatingInactiveColor.copy(alpha = 0.42f),
            modifier = Modifier.size(24.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fillFraction)
                .clipToBounds(),
            contentAlignment = Alignment.CenterStart
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = RatingActiveColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
