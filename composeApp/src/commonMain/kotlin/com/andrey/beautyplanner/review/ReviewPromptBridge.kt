@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.andrey.beautyplanner.review

expect object ReviewPromptBridge {
    suspend fun requestInAppReview(): Boolean
}