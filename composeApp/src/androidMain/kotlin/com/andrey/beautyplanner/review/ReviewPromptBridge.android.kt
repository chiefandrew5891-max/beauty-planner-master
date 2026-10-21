package com.andrey.beautyplanner.review

import com.andrey.beautyplanner.AndroidAppContext
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.tasks.await

actual object ReviewPromptBridge {
    actual suspend fun requestInAppReview(): Boolean {
        val activity = AndroidAppContext.activity ?: return false
        return try {
            val manager = ReviewManagerFactory.create(activity)
            val reviewInfo = manager.requestReviewFlow().await()
            manager.launchReviewFlow(activity, reviewInfo).await()
            true
        } catch (_: Throwable) {
            false
        }
    }
}