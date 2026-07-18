package com.andrey.beautyplanner

object AuthErrorMapper {
    fun toUserMessage(message: String?): String? {
        val raw = message.orEmpty().trim()
        val lower = raw.lowercase()

        if (
            "canceled" in lower ||
            "cancelled" in lower ||
            "12501" in lower
        ) {
            return null
        }

        if (
            "underlying tasks failed" in lower ||
            "network" in lower ||
            "unable to resolve host" in lower ||
            "timeout" in lower ||
            "timed out" in lower ||
            "failed to connect" in lower ||
            "unreachable" in lower
        ) {
            return Locales.t("auth_error_no_internet")
        }

        return Locales.t("auth_error_sign_in_failed")
    }
}