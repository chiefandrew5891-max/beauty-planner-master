package com.andrey.beautyplanner

fun Appointment.isOnlineBooking(): Boolean {
    return bookingSource.trim().equals("online", ignoreCase = true)
}