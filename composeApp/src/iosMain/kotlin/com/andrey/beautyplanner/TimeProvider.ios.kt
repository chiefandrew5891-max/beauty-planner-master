package com.andrey.beautyplanner

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents

actual fun getCurrentTimeHm(): String {
    val now = NSDate()
    val calendar = NSCalendar.currentCalendar
    val comps = calendar.components(NSCalendarUnitHour or NSCalendarUnitMinute, fromDate = now) as NSDateComponents

    val hh = (comps.hour.toInt()).toString().padStart(2, '0')
    val mm = (comps.minute.toInt()).toString().padStart(2, '0')
    return "$hh:$mm"
}