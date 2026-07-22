package com.andrey.beautyplanner

import kotlin.math.roundToInt

const val DEFAULT_PROFILE_RATING = 4.7f
private const val PROFILE_RATING_SCALE = 5f

fun normalizedProfileRating(value: Float): Float =
    value.coerceIn(0f, PROFILE_RATING_SCALE)

fun profileRatingStarFillFractions(
    value: Float,
    starCount: Int = PROFILE_RATING_SCALE.toInt()
): List<Float> {
    val normalized = normalizedProfileRating(value)
    return List(starCount) { index ->
        val fill = (normalized - index).coerceIn(0f, 1f)
        (fill * 1000f).roundToInt() / 1000f
    }
}

fun formatProfileRating(value: Float): String {
    val tenths = (normalizedProfileRating(value) * 10f).roundToInt()
    return "${tenths / 10}.${tenths % 10} / 5.0"
}
