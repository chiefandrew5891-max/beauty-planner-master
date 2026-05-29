package com.andrey.beautyplanner

data class ClientSuggestion(
    val displayName: String,
    val phone: String = ""
)

object ClientSuggestions {

    fun fromAppointments(
        appointments: List<Appointment>,
        limit: Int = 200
    ): List<ClientSuggestion> {
        val map = linkedMapOf<String, ClientSuggestion>()

        appointments.forEach { appt ->
            val name = appt.clientName.trim()
            val phone = appt.phone.trim()

            if (name.isBlank()) return@forEach

            val key = buildKey(name, phone)
            if (!map.containsKey(key)) {
                map[key] = ClientSuggestion(
                    displayName = name,
                    phone = phone
                )
            }
        }

        return map.values
            .sortedBy { it.displayName.lowercase() }
            .take(limit)
    }

    fun filter(
        clients: List<ClientSuggestion>,
        query: String,
        limit: Int = 8
    ): List<ClientSuggestion> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return emptyList()

        return clients
            .asSequence()
            .filter { it.displayName.trim().lowercase().contains(q) }
            .sortedBy { it.displayName.lowercase() }
            .take(limit)
            .toList()
    }

    fun merge(
        local: List<ClientSuggestion>,
        contacts: List<ContactSuggestion>,
        limit: Int = 8
    ): List<ClientSuggestion> {
        val map = linkedMapOf<String, ClientSuggestion>()

        local.forEach { item ->
            val key = buildKey(item.displayName, item.phone)
            if (!map.containsKey(key)) {
                map[key] = item
            }
        }

        contacts.forEach { item ->
            val key = buildKey(item.displayName, item.phone)
            if (!map.containsKey(key)) {
                map[key] = ClientSuggestion(
                    displayName = item.displayName,
                    phone = item.phone
                )
            }
        }

        return map.values.take(limit)
    }

    private fun buildKey(name: String, phone: String): String {
        return name.trim().lowercase() + "|" + phone.trim()
    }
}