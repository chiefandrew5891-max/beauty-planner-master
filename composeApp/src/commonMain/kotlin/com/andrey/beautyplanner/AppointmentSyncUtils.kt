package com.andrey.beautyplanner

object AppointmentSyncUtils {

    fun touchForCreateOrUpdate(
        source: Appointment,
        nowMillis: Long
    ): Appointment {
        return source.copy(
            updatedAtMillis = nowMillis
        )
    }

    fun markDeleted(
        source: Appointment,
        nowMillis: Long
    ): Appointment {
        return source.copy(
            isDeleted = true,
            updatedAtMillis = nowMillis
        )
    }

    fun visibleAppointments(
        source: List<Appointment>
    ): List<Appointment> {
        return source.filterNot { it.isDeleted }
    }

    fun mergeAppointments(
        local: List<Appointment>,
        remote: List<Appointment>
    ): List<Appointment> {
        val byId = linkedMapOf<String, Appointment>()

        (local + remote).forEach { candidate ->
            val existing = byId[candidate.id]
            if (existing == null) {
                byId[candidate.id] = candidate
            } else {
                byId[candidate.id] =
                    if (candidate.updatedAtMillis >= existing.updatedAtMillis) {
                        candidate
                    } else {
                        existing
                    }
            }
        }

        return byId.values.toList()
    }
}