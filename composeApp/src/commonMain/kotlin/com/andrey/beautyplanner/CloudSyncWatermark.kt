package com.andrey.beautyplanner

data class CloudSyncWatermark(
    val userId: String = "",
    val pullSyncedAtMillis: Long = 0L,
    val lastPushAtMillis: Long = 0L,
    val lastFullPullAtMillis: Long = 0L
) {
    fun encode(): String =
        listOf(userId, pullSyncedAtMillis, lastPushAtMillis, lastFullPullAtMillis).joinToString("|")

    companion object {
        fun parse(raw: String, userId: String): CloudSyncWatermark {
            val p = raw.split("|")
            if (p.size != 4 || p[0] != userId) return CloudSyncWatermark(userId = userId)
            return CloudSyncWatermark(
                userId = userId,
                pullSyncedAtMillis = p[1].toLongOrNull() ?: 0L,
                lastPushAtMillis = p[2].toLongOrNull() ?: 0L,
                lastFullPullAtMillis = p[3].toLongOrNull() ?: 0L
            )
        }
    }
}