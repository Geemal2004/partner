package com.studypartner.planner.notifications

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReminderTimeCalculatorTest {

    @Test
    fun calculateReminderTime_15MinutesOffset_calculatesCorrectTime() {
        val startTimeMs = 1_700_000_000_000L
        val offsetMinutes = 15
        val expectedReminderTime = startTimeMs - (15 * 60 * 1000L)

        val actual = ReminderTimeCalculator.calculateReminderTime(startTimeMs, offsetMinutes)

        assertThat(actual).isEqualTo(expectedReminderTime)
    }

    @Test
    fun calculateReminderTime_zeroOrNegativeOffset_returnsStartTime() {
        val startTimeMs = 1_700_000_000_000L

        assertThat(ReminderTimeCalculator.calculateReminderTime(startTimeMs, 0)).isEqualTo(startTimeMs)
        assertThat(ReminderTimeCalculator.calculateReminderTime(startTimeMs, -10)).isEqualTo(startTimeMs)
    }

    @Test
    fun isReminderInPast_pastTime_returnsTrue() {
        val currentTimeMs = 100_000L
        val reminderTimeMs = 50_000L

        val isPast = ReminderTimeCalculator.isReminderInPast(reminderTimeMs, currentTimeMs)

        assertThat(isPast).isTrue()
    }

    @Test
    fun isReminderInPast_futureTime_returnsFalse() {
        val currentTimeMs = 100_000L
        val reminderTimeMs = 150_000L

        val isPast = ReminderTimeCalculator.isReminderInPast(reminderTimeMs, currentTimeMs)

        assertThat(isPast).isFalse()
    }

    @Test
    fun isValidReminderOffset_validations() {
        assertThat(ReminderTimeCalculator.isValidReminderOffset(15)).isTrue()
        assertThat(ReminderTimeCalculator.isValidReminderOffset(0)).isFalse()
        assertThat(ReminderTimeCalculator.isValidReminderOffset(-5)).isFalse()
        assertThat(ReminderTimeCalculator.isValidReminderOffset(null)).isFalse()
    }
}
