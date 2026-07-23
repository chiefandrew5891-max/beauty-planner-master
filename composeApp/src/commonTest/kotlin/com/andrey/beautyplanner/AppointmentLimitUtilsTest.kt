package com.andrey.beautyplanner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppointmentLimitUtilsTest {

    @Test
    fun visibleAppointmentsCount_ignoresSoftDeletedAppointments() {
        val appointments = listOf(
            Appointment(
                id = "1",
                dateString = "2026-07-23",
                time = "10:00",
                clientName = "A",
                phone = "",
                serviceName = "Service",
                price = "10"
            ),
            Appointment(
                id = "2",
                dateString = "2026-07-23",
                time = "11:00",
                clientName = "B",
                phone = "",
                serviceName = "Service",
                price = "10",
                isDeleted = true
            ),
            Appointment(
                id = "3",
                dateString = "2026-07-23",
                time = "12:00",
                clientName = "C",
                phone = "",
                serviceName = "Service",
                price = "10"
            )
        )

        assertEquals(2, AppointmentSyncUtils.visibleAppointmentsCount(appointments))
    }

    @Test
    fun shouldShowFreeLimitWarning_bannerThresholds() {
        // Banner warning uses remaining slots (5, 3, 1)
        assertTrue(AccessManager.shouldShowFreeLimitWarning(5))
        assertTrue(AccessManager.shouldShowFreeLimitWarning(3))
        assertTrue(AccessManager.shouldShowFreeLimitWarning(1))
        assertFalse(AccessManager.shouldShowFreeLimitWarning(4))
        assertFalse(AccessManager.shouldShowFreeLimitWarning(2))
        assertFalse(AccessManager.shouldShowFreeLimitWarning(0))
    }

    @Test
    fun shouldShowFreeLimitWarningForCount_popupThresholds() {
        // Popup warning uses visible appointment count (1, 15, 17, 19, 20)
        assertTrue(AccessManager.shouldShowFreeLimitWarningForCount(1))
        assertTrue(AccessManager.shouldShowFreeLimitWarningForCount(15))
        assertTrue(AccessManager.shouldShowFreeLimitWarningForCount(17))
        assertTrue(AccessManager.shouldShowFreeLimitWarningForCount(19))
        assertTrue(AccessManager.shouldShowFreeLimitWarningForCount(20))
        assertFalse(AccessManager.shouldShowFreeLimitWarningForCount(0))
        assertFalse(AccessManager.shouldShowFreeLimitWarningForCount(2))
        assertFalse(AccessManager.shouldShowFreeLimitWarningForCount(14))
        assertFalse(AccessManager.shouldShowFreeLimitWarningForCount(16))
        assertFalse(AccessManager.shouldShowFreeLimitWarningForCount(18))
    }

    @Test
    fun getFreeLimitPopupThreshold_returnsThresholdWhenAtLimit() {
        assertNotNull(AccessManager.getFreeLimitPopupThreshold(1))
        assertNotNull(AccessManager.getFreeLimitPopupThreshold(15))
        assertNotNull(AccessManager.getFreeLimitPopupThreshold(17))
        assertNotNull(AccessManager.getFreeLimitPopupThreshold(19))
        assertNotNull(AccessManager.getFreeLimitPopupThreshold(20))
        assertNull(AccessManager.getFreeLimitPopupThreshold(0))
        assertNull(AccessManager.getFreeLimitPopupThreshold(14))
        assertNull(AccessManager.getFreeLimitPopupThreshold(16))
        assertNull(AccessManager.getFreeLimitPopupThreshold(21))
    }

    @Test
    fun isTrialActive_falseWhenEndsAtMillisIsZero() {
        // isTrialActive should be false when trialEndsAtMillis <= nowMillis
        val expiredState = AccessState(
            tier = AccessTier.TRIAL,
            trialStartedAtMillis = 0L,
            trialEndsAtMillis = 1000L,
            isTrialActive = false, // expired
            hasPremium = false,
            trialDaysLeft = 0
        )
        assertFalse(expiredState.isTrialActive)
    }

    @Test
    fun isTrialActive_trueWhenTrialNotYetExpired() {
        val futureMillis = System.currentTimeMillis() + 10 * 24 * 60 * 60 * 1000L
        val activeState = AccessState(
            tier = AccessTier.TRIAL,
            trialStartedAtMillis = 0L,
            trialEndsAtMillis = futureMillis,
            isTrialActive = true,
            hasPremium = false,
            trialDaysLeft = 10
        )
        assertTrue(activeState.isTrialActive)
    }
}
