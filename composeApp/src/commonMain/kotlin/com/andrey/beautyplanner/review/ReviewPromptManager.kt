package com.andrey.beautyplanner.review

import com.andrey.beautyplanner.AppSettings
import kotlinx.datetime.Clock

object ReviewPromptManager {
    private const val DAY_MS = 24L * 60L * 60L * 1000L

    private const val MIN_LAUNCHES_BEFORE_FIRST_PROMPT = 5L
    private const val MIN_DAYS_BEFORE_FIRST_PROMPT = 7L

    private const val REPEAT_INTERVAL_DAYS = 30L
    private const val MAX_PROMPT_COUNT = 3L

    fun onAppLaunch() {
        val now = Clock.System.now().toEpochMilliseconds()

        if (AppSettings.reviewPromptFirstLaunchAtMillis <= 0L) {
            AppSettings.reviewPromptFirstLaunchAtMillis = now
        }

        AppSettings.reviewPromptLaunchCount = AppSettings.reviewPromptLaunchCount + 1L
        AppSettings.persist()
    }

    fun shouldShowPrompt(): Boolean {
        if (AppSettings.reviewPromptDisabled) return false
        if (AppSettings.reviewPromptShownCount >= MAX_PROMPT_COUNT) return false
        if (AppSettings.reviewPromptLaunchCount < MIN_LAUNCHES_BEFORE_FIRST_PROMPT) return false

        val now = Clock.System.now().toEpochMilliseconds()
        val firstLaunchAt = AppSettings.reviewPromptFirstLaunchAtMillis
        if (firstLaunchAt <= 0L) return false

        val daysSinceFirstLaunch = (now - firstLaunchAt) / DAY_MS
        if (daysSinceFirstLaunch < MIN_DAYS_BEFORE_FIRST_PROMPT) return false

        val lastShownAt = AppSettings.reviewPromptLastShownAtMillis
        if (lastShownAt > 0L) {
            val daysSinceLastPrompt = (now - lastShownAt) / DAY_MS
            if (daysSinceLastPrompt < REPEAT_INTERVAL_DAYS) return false
        }

        return true
    }

    fun markPromptShown() {
        AppSettings.reviewPromptLastShownAtMillis = Clock.System.now().toEpochMilliseconds()
        AppSettings.reviewPromptShownCount = AppSettings.reviewPromptShownCount + 1L
        AppSettings.persist()
    }

    fun disableForever() {
        AppSettings.reviewPromptDisabled = true
        AppSettings.persist()
    }
}