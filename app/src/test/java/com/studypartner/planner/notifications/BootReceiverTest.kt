package com.studypartner.planner.notifications

import android.content.Intent
import com.studypartner.planner.data.local.EventDao
import com.studypartner.planner.data.local.EventEntity
import com.studypartner.planner.data.repository.UserPreferences
import com.studypartner.planner.data.repository.UserPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BootReceiverTest {

    private val eventDao: EventDao = mockk(relaxed = true)
    private val reminderScheduler: ReminderScheduler = mockk(relaxed = true)
    private val userPreferencesRepository: UserPreferencesRepository = mockk(relaxed = true)
    private val dailySummaryScheduler: DailySummaryScheduler = mockk(relaxed = true)

    private lateinit var receiver: BootReceiver

    @Before
    fun setUp() {
        receiver = BootReceiver()
        receiver.eventDao = eventDao
        receiver.reminderScheduler = reminderScheduler
        receiver.userPreferencesRepository = userPreferencesRepository
        receiver.dailySummaryScheduler = dailySummaryScheduler
    }

    @Test
    fun processIntent_bootCompleted_reschedulesEventsAndDailySummary() = runTest {
        receiver.ioDispatcher = StandardTestDispatcher(testScheduler)
        val prefs = UserPreferences(
            defaultReminderOffset = 20,
            dailySummaryEnabled = true,
            dailySummaryTime = "08:15",
            partnerUpdatesEnabled = true
        )
        every { userPreferencesRepository.userPreferencesFlow } returns flowOf(prefs)

        val sampleEvents = listOf(
            EventEntity(
                id = "boot-event-1",
                groupId = "group-1",
                title = "Morning Meeting",
                description = "",
                startTime = System.currentTimeMillis() + 100000,
                endTime = System.currentTimeMillis() + 200000,
                allDay = false,
                location = "Room A",
                reminderMinutes = null,
                createdBy = "user-1",
                updatedAt = System.currentTimeMillis()
            )
        )
        coEvery { eventDao.getEventsForDay(any(), any()) } returns sampleEvents

        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns Intent.ACTION_BOOT_COMPLETED

        val job = receiver.processIntent(intent, null)
        testScheduler.advanceUntilIdle()
        job?.join()

        verify(exactly = 1) {
            reminderScheduler.scheduleReminder(sampleEvents[0], defaultOffsetMinutes = 20)
        }
        verify(exactly = 1) {
            dailySummaryScheduler.scheduleDailySummary("08:15")
        }
    }

    @Test
    fun processIntent_unrelatedAction_doesNothing() = runTest {
        receiver.ioDispatcher = StandardTestDispatcher(testScheduler)
        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns "com.example.UNRELATED_ACTION"

        val job = receiver.processIntent(intent, null)
        testScheduler.advanceUntilIdle()
        job?.join()

        coVerify(exactly = 0) {
            eventDao.getEventsForDay(any(), any())
        }
    }
}
