@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.andrey.beautyplanner.review

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual object ReviewPromptBridge {
    actual suspend fun requestInAppReview(): Boolean =
        withContext(Dispatchers.Main) {
            val request = ReviewPromptConnector.requestReview
                ?: return@withContext false

            request.invoke()
            true
        }
}