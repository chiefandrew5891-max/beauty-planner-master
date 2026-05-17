package com.andrey.beautyplanner.appcontent

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrey.beautyplanner.AppSettings
import com.andrey.beautyplanner.Locales
import kotlinx.datetime.LocalDate
import kotlin.math.roundToInt

@Composable
fun CollapsingCalendarHeader(
    today: LocalDate,
    calendarViewDate: LocalDate,
    selectedDate: LocalDate,
    collapseProgress: Float, // 0..1 (0 = expanded, 1 = collapsed)
    onExpandRequest: () -> Unit,
    onDateClick: (LocalDate) -> Unit,
    expandedMonthRow: @Composable () -> Unit
) {
    val fontScale = AppSettings.getFontScale()
    val todayText = remember(today) { formatTodayHuman(today) }

    // Чтобы не было резких скачков — прогресс анимируем
    val pClamped = collapseProgress.coerceIn(0f, 1f)
    val p by animateFloatAsState(targetValue = pClamped)

    // Альфа для элементов:
    // - expanded часть исчезает при p -> 1
    // - collapsed строка появляется при p -> 1
    val expandedAlpha = (1f - p).coerceIn(0f, 1f)
    val collapsedAlpha = p.coerceIn(0f, 1f)

    // Высота expanded-контента меняется плавно за счёт alpha + постепенного уменьшения.
    // Делать точные dp-вычисления сложно, поэтому мы "сжимаем" контент scaleY-подобно через heightIn.
    // Надёжный способ: не вырезать из композиции резко, а держать оба слоя и менять прозрачность.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.background)
            .pointerInput(p) {
                detectTapGestures(
                    onDoubleTap = {
                        // если почти свернуто — даём развернуть
                        if (p > 0.85f) onExpandRequest()
                    }
                )
            }
    ) {
        // Collapsed row (появляется)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp)
                .alpha(collapsedAlpha),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = todayText,
                fontSize = (20 * fontScale).sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colors.onBackground
            )
        }

        // Expanded content (исчезает)
        // Важно: не делаем if(p==0) / else — иначе будет резкая смена высоты.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(expandedAlpha)
                // небольшая “компрессия” по мере схлопывания: уменьшаем нижний padding календаря
                .padding(bottom = ((1f - p) * 8f).roundToInt().dp)
        ) {
            expandedMonthRow()

            MonthCalendarGrid(
                monthDate = calendarViewDate,
                today = today,
                selectedDate = selectedDate,
                onDateClick = onDateClick
            )
        }
    }
}

private fun formatTodayHuman(today: LocalDate): String {
    val day = today.dayOfMonth
    val monthKeyGen = when (today.monthNumber) {
        1 -> "month_jan_gen"
        2 -> "month_feb_gen"
        3 -> "month_mar_gen"
        4 -> "month_apr_gen"
        5 -> "month_may_gen"
        6 -> "month_jun_gen"
        7 -> "month_jul_gen"
        8 -> "month_aug_gen"
        9 -> "month_sep_gen"
        10 -> "month_oct_gen"
        11 -> "month_nov_gen"
        12 -> "month_dec_gen"
        else -> ""
    }

    val gen = Locales.t(monthKeyGen)
    val monthText =
        if (gen != monthKeyGen) gen
        else {
            val nomKey = when (today.monthNumber) {
                1 -> "month_jan"
                2 -> "month_feb"
                3 -> "month_mar"
                4 -> "month_apr"
                5 -> "month_may"
                6 -> "month_jun"
                7 -> "month_jul"
                8 -> "month_aug"
                9 -> "month_sep"
                10 -> "month_oct"
                11 -> "month_nov"
                12 -> "month_dec"
                else -> ""
            }
            Locales.t(nomKey)
        }

    return "$day $monthText"
}