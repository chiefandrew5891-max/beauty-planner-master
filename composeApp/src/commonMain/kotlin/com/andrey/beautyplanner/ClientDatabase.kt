package com.andrey.beautyplanner

data class ClientDatabaseEntry(
    val id: String,
    val displayName: String,
    val phone: String,
    val visitCount: Int,
    val lastVisitDate: String,
    val notes: String,
    val colorTag: String,
    val status: String
)

object ClientDatabase {

    fun buildClientId(name: String, phone: String): String {
        return name.trim().lowercase() + "|" + phone.trim()
    }

    fun build(
        appointments: List<Appointment>,
        profiles: List<ClientProfile>
    ): List<ClientDatabaseEntry> {
        val profilesById = profiles.associateBy { it.id }

        return appointments
            .filterNot { it.isDeleted }
            .groupBy { buildClientId(it.clientName, it.phone) }
            .mapNotNull { (id, items) ->
                val first = items.firstOrNull() ?: return@mapNotNull null
                val profile = profilesById[id]

                ClientDatabaseEntry(
                    id = id,
                    displayName = first.clientName.trim(),
                    phone = items.map { it.phone.trim() }.firstOrNull { it.isNotBlank() }.orEmpty(),
                    visitCount = items.size,
                    lastVisitDate = items.maxOfOrNull { it.dateString }.orEmpty(),
                    notes = profile?.notes.orEmpty(),
                    colorTag = profile?.colorTag.orEmpty(),
                    status = profile?.status ?: ClientProfileStatus.NONE.name
                )
            }
            .sortedWith(
                compareByDescending<ClientDatabaseEntry> { it.lastVisitDate }
                    .thenBy { it.displayName.lowercase() }
            )
    }
}