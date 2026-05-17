package com.andrey.beautyplanner

import java.time.LocalTime

actual fun getCurrentTimeHm(): String {
    val t = LocalTime.now()
    val hh = t.hour.toString().padStart(2, '0')
    val mm = t.minute.toString().padStart(2, '0')
    return "$hh:$mm"
}