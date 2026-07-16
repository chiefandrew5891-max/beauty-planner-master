package com.andrey.beautyplanner

object CloudSyncCoordinator {

    fun mergeLocalAndRemoteAppointments(
        local: List<Appointment>,
        remote: List<Appointment>
    ): List<Appointment> {
        CloudSyncLogger.log(
            "mergeLocalAndRemoteAppointments: local=${local.size}, remote=${remote.size}"
        )

        val merged = AppointmentSyncUtils.mergeAppointments(local, remote)

        val localVisible = AppointmentSyncUtils.visibleAppointments(local).size
        val remoteVisible = AppointmentSyncUtils.visibleAppointments(remote).size
        val mergedVisible = AppointmentSyncUtils.visibleAppointments(merged).size

        CloudSyncLogger.log(
            "merge result: merged=${merged.size}, localVisible=$localVisible, remoteVisible=$remoteVisible, mergedVisible=$mergedVisible"
        )

        return merged
    }

    fun shouldApplyRemoteSettings(
        localSettingsUpdatedAtMillis: Long,
        remoteSettings: CloudSettingsSnapshot?
    ): Boolean {
        if (remoteSettings == null) {
            CloudSyncLogger.log("remote settings absent -> skip apply")
            return false
        }

        val shouldApply = remoteSettings.updatedAtMillis > localSettingsUpdatedAtMillis

        CloudSyncLogger.log(
            "shouldApplyRemoteSettings: local=$localSettingsUpdatedAtMillis, remote=${remoteSettings.updatedAtMillis}, apply=$shouldApply"
        )

        return shouldApply
    }
}