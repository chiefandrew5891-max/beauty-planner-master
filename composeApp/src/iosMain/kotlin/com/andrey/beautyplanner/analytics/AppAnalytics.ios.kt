package com.andrey.beautyplanner.analytics

actual object AppAnalytics {
    var handler: ((String, Map<String, Any>) -> Unit)? = null
    actual fun log(name: String, params: Map<String, Any>) {
        handler?.invoke(name, params)
    }
}