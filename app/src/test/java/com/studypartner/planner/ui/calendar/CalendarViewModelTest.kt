package com.studypartner.planner.ui.calendar

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.studypartner.planner.data.local.EventEntity
import com.studypartner.planner.data.repository.EventRepository
import com.studypartner.planner.data.repository.GroupRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val eventRepository: EventRepository = mockk(relaxed = true)
    private val groupRepository: GroupRepository = mockk(relaxed = true)
    private val auth: FirebaseAuth = mockk(relaxed = true)
    private val mockUser: FirebaseUser = mockk(relaxed = true)

    private val currentGroupIdFlow = MutableStateFlow<String?>("group-1")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { groupRepository.currentGroupId } returns currentGroupIdFlow
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "user-123"

        every { eventRepository.startRealtimeSync(any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun eventsFlow_emitsEventsForCurrentGroup() = runTest {
        val sampleEvents = listOf(
            EventEntity(
                id = "evt-1",
                groupId = "group-1",
                title = "Session 1",
                description = "Desc",
                startTime = 1000L,
                endTime = 2000L,
                allDay = false,
                location = "Room 1",
                reminderMinutes = 15,
                createdBy = "user-123",
                updatedAt = 1000L
            )
        )
        every { eventRepository.getEvents("group-1") } returns flowOf(sampleEvents)

        val viewModel = CalendarViewModel(eventRepository, groupRepository, auth)

        viewModel.events.test {
            assertThat(awaitItem()).isEmpty()
            assertThat(awaitItem()).isEqualTo(sampleEvents)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onDateSelected_updatesSelectedDate() = runTest {
        val viewModel = CalendarViewModel(eventRepository, groupRepository, auth)
        val targetDate = LocalDate.of(2026, 10, 15)

        viewModel.selectedDate.test {
            assertThat(awaitItem()).isEqualTo(LocalDate.now())

            viewModel.onDateSelected(targetDate)
            assertThat(awaitItem()).isEqualTo(targetDate)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun saveEvent_delegatesToRepository() = runTest {
        val viewModel = CalendarViewModel(eventRepository, groupRepository, auth)
        val event = EventEntity(
            id = "evt-new",
            groupId = "group-1",
            title = "New Event",
            description = "",
            startTime = 5000L,
            endTime = 6000L,
            allDay = false,
            location = "Library",
            reminderMinutes = null,
            createdBy = "user-123",
            updatedAt = 5000L
        )

        viewModel.saveEvent(event)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { eventRepository.saveEvent(event) }
    }

    @Test
    fun deleteEvent_delegatesToRepository() = runTest {
        val viewModel = CalendarViewModel(eventRepository, groupRepository, auth)

        viewModel.deleteEvent("evt-to-delete")
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { eventRepository.deleteEvent("evt-to-delete") }
    }

    @Test
    fun realtimeSync_whenErrorOccurs_setsSyncErrorWithoutCrashing() = runTest {
        val errorFlow = kotlinx.coroutines.flow.flow<List<EventEntity>> {
            throw RuntimeException("Firestore connection dropped")
        }
        every { eventRepository.startRealtimeSync("group-1") } returns errorFlow

        val viewModel = CalendarViewModel(eventRepository, groupRepository, auth)
        testScheduler.advanceUntilIdle()

        assertThat(viewModel.syncError.value).contains("Firestore connection dropped")

        // ViewModel scope is not dead, still handles user interactions
        val targetDate = LocalDate.of(2026, 12, 1)
        viewModel.onDateSelected(targetDate)
        assertThat(viewModel.selectedDate.value).isEqualTo(targetDate)
    }

    @Test
    fun retrySync_clearsSyncErrorAndRestartsSync() = runTest {
        val errorFlow = kotlinx.coroutines.flow.flow<List<EventEntity>> {
            throw RuntimeException("Network timeout")
        }
        every { eventRepository.startRealtimeSync("group-1") } returns errorFlow

        val viewModel = CalendarViewModel(eventRepository, groupRepository, auth)
        testScheduler.advanceUntilIdle()

        assertThat(viewModel.syncError.value).isNotNull()

        // Now network recovers
        every { eventRepository.startRealtimeSync("group-1") } returns flowOf(emptyList())

        viewModel.retrySync()
        testScheduler.advanceUntilIdle()

        assertThat(viewModel.syncError.value).isNull()
        verify(atLeast = 2) { eventRepository.startRealtimeSync("group-1") }
        coVerify(atLeast = 1) { eventRepository.syncEvents("group-1") }
    }
}
