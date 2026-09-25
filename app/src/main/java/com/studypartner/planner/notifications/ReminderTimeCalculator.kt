package com.studypartner.planner.notifications

object ReminderTimeCalculator {
    /**
     * Calculates the trigger time in milliseconds for a reminder alarm.
     * @param startTimeMs Event start time in epoch millis.
     * @param offsetMinutes Minutes before start time to trigger the reminder.
     * @return Alarm trigger epoch millis.
     */
    fun calculateReminderTime(startTimeMs: Long, offsetMinutes: Int): Long {
        if (offsetMinutes <= 0) return startTimeMs
        return startTimeMs - (offsetMinutes * 60 * 1000L)
    }

    /**
     * Checks whether the calculated reminder time is in the past compared to current time.
     */
    fun isReminderInPast(reminderTimeMs: Long, currentTimeMs: Long): Boolean {
        return reminderTimeMs <= currentTimeMs
    }

    /**
     * Checks if the reminder offset in minutes is valid (positive integer).
     */
    fun isValidReminderOffset(offsetMinutes: Int?): Boolean {
        return offsetMinutes != null && offsetMinutes > 0
    }
}
