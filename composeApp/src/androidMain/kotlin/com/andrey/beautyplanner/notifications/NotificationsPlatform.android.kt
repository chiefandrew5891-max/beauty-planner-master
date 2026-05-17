package com.andrey.beautyplanner.notifications

import android.content.Context

object NotificationsPlatform {
    fun init(context: Context) {
        NotificationsAndroidContext.init(context)
    }
}