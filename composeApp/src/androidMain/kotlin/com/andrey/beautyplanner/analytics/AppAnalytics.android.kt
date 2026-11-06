package com.andrey.beautyplanner.analytics

import android.os.Bundle
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
actual object AppAnalytics {
    actual fun log(name: String, params: Map<String, Any>) {
        val b = Bundle()
        params.forEach { (k, v) ->
            when (v) {
                is String -> b.putString(k, v)
                is Int -> b.putLong(k, v.toLong())
                is Long -> b.putLong(k, v)
                is Double -> b.putDouble(k, v)
                else -> b.putString(k, v.toString())
            }
        }
        Firebase.analytics.logEvent(name, b)
    }
}