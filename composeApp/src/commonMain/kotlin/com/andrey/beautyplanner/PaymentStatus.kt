package com.andrey.beautyplanner

fun Appointment.effectivePaymentStatus(): AppointmentPaymentStatus {
    return when (paymentStatus.trim().uppercase()) {
        "PAYMENT_LATER" -> AppointmentPaymentStatus.PAYMENT_LATER
        "PAID_AFTER_DELAY" -> AppointmentPaymentStatus.PAID_AFTER_DELAY
        "PAID" -> AppointmentPaymentStatus.PAID
        else -> {
            if (paymentDeferred) {
                AppointmentPaymentStatus.PAYMENT_LATER
            } else {
                AppointmentPaymentStatus.PAID
            }
        }
    }
}

fun Appointment.withPaymentLaterStatus(enabled: Boolean): Appointment {
    return copy(
        paymentDeferred = enabled,
        paymentStatus = if (enabled) {
            AppointmentPaymentStatus.PAYMENT_LATER.name
        } else {
            AppointmentPaymentStatus.PAID.name
        }
    )
}

fun Appointment.markPaidAfterDelay(): Appointment {
    return copy(
        paymentDeferred = false,
        paymentStatus = AppointmentPaymentStatus.PAID_AFTER_DELAY.name
    )
}