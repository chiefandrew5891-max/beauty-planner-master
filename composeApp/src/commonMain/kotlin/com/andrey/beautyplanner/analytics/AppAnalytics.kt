package com.andrey.beautyplanner.analytics

expect object AppAnalytics {
    fun log(name: String, params: Map<String, Any> = emptyMap())
}

object AnalyticsEvents {
    const val APPOINTMENT_CREATED = "appointment_created"
    const val PREMIUM_SCREEN_VIEW = "premium_screen_view"
    const val BEGIN_CHECKOUT = "begin_checkout"
    const val PURCHASE = "purchase"
}