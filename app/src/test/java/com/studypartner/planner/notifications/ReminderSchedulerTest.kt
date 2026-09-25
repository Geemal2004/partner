package com.studypartner.planner.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import com.studypartner.planner.data.local.EventEntity
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class ReminderSchedulerTest {

    private val context: Context = mockk(relaxed = true)
    private val alarmManager: AlarmManager = mockk(relaxed = true)

    private lateinit var scheduler: ReminderScheduler

    @Before
    fun setUp() {
        mockkStatic(PendingIntent::class)
        every {
            PendingIntent.getBroadcast(any(), any(), any(), any())
        } returns mockk(relaxed = true)

        every { context.getSystemService(Context.ALARM_SERVICE) } returns alarmManager
        scheduler = ReminderScheduler(context)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun scheduleReminder_exactAllowed_callsSetExactAndAllowWhileIdle() {
        scheduler.sdkInt = 31 // Android S
        // Given exact alarms are permitted
        every { alarmManager.canScheduleExactAlarms() } returns true

        val futureTime = System.currentTimeMillis() + 3_600_000L
        val event = EventEntity(
            id = "evt-1",
            groupId = "grp-1",
            title = "Study Session",
            description = "Desc",
            startTime = futureTime,
            endTime = futureTime + 3600000,
            allDay = false,
            location = "Library",
            reminderMinutes = 15,
            createdBy = "user-1",
            updatedAt = System.currentTimeMillis()
        )

        // When
        scheduler.scheduleReminder(event, defaultOffsetMinutes = 15)

        // Then exact alarm is scheduled
        verify {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                any(),
                any()
            )
        }
    }

    @Test
    fun scheduleReminder_exactDenied_fallsBackToInexactSetAndAllowWhileIdle() {
        scheduler.sdkInt = 31 // Android S
        // Given exact alarms are not permitted (e.g. Android 12+ revoke)
        every { alarmManager.canScheduleExactAlarms() } returns false

        val futureTime = System.currentTimeMillis() + 3_600_000L
        val event = EventEntity(
            id = "evt-2",
            groupId = "grp-1",
            title = "Inexact Study Session",
            description = "Desc",
            startTime = futureTime,
            endTime = futureTime + 3600000,
            allDay = false,
            location = "Library",
            reminderMinutes = 15,
            createdBy = "user-1",
            updatedAt = System.currentTimeMillis()
        )

        // When
        scheduler.scheduleReminder(event, defaultOffsetMinutes = 15)

        // Then exact alarm is NOT called, fallback inexact alarm IS called
        verify(exactly = 0) {
            alarmManager.setExactAndAllowWhileIdle(any(), any(), any())
        }
        verify(exactly = 1) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                any(),
                any()
            )
        }
    }

    @Test
    fun scheduleReminder_preAndroid12_usesExactAlarmWithoutCheckingCanSchedule() {
        scheduler.sdkInt = 30 // Android R

        val futureTime = System.currentTimeMillis() + 3_600_000L
        val event = EventEntity(
            id = "evt-3",
            groupId = "grp-1",
            title = "Pre-S Study Session",
            description = "Desc",
            startTime = futureTime,
            endTime = futureTime + 3600000,
            allDay = false,
            location = "Library",
            reminderMinutes = 15,
            createdBy = "user-1",
            updatedAt = System.currentTimeMillis()
        )

        // When
        scheduler.scheduleReminder(event, defaultOffsetMinutes = 15)

        // Then exact alarm is scheduled directly without calling canScheduleExactAlarms()
        verify(exactly = 0) {
            alarmManager.canScheduleExactAlarms()
        }
        verify(exactly = 1) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                any(),
                any()
            )
        }
    }
}
