package com.andrey.beautyplanner

/**
 * Возвращает текущее локальное время устройства в формате "HH:MM" (24h).
 * Реализуется в androidMain/iosMain через actual.
 */
expect fun getCurrentTimeHm(): String