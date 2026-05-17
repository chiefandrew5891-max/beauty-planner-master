package com.andrey.beautyplanner

object ServicesCatalog {
    /**
     * Store these keys in Appointment.serviceName (not the translated text),
     * so that stats grouping stays stable across languages.
     */
    val keys: List<String> = listOf(
        "service_gel_polish",
        "service_gel_strengthening",
        "service_nail_extensions",
        "service_lash_extensions",

        // ✅ added
        "service_correction",
        "service_repair"
    )
}