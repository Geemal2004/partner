package com.studypartner.planner.data.repository

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.studypartner.planner.data.local.EventDao
import com.studypartner.planner.notifications.ReminderScheduler
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat

@OptIn(ExperimentalCoroutinesApi::class)
class EventRepositoryTest {

    private val eventDao: EventDao = mockk(relaxed = true)
    private val firestore: FirebaseFirestore = mockk(relaxed = true)
    private val reminderScheduler: ReminderScheduler = mockk(relaxed = true)
    private val userPreferencesRepository: UserPreferencesRepository = mockk(relaxed = true)

    private val groupsCollection: CollectionReference = mockk(relaxed = true)
    private val groupDocument: DocumentReference = mockk(relaxed = true)
    private val eventsCollection: CollectionReference = mockk(relaxed = true)
    private val listenerRegistration: ListenerRegistration = mockk(relaxed = true)

    private val listenerSlot = slot<EventListener<QuerySnapshot>>()

    private lateinit var repository: EventRepository

    @Before
    fun setUp() {
        every { firestore.collection("groups") } returns groupsCollection
        every { groupsCollection.document(any()) } returns groupDocument
        every { groupDocument.collection("events") } returns eventsCollection
        every { eventsCollection.addSnapshotListener(capture(listenerSlot)) } returns listenerRegistration

        every { userPreferencesRepository.userPreferencesFlow } returns flowOf(
            UserPreferences(true, "07:30", 15, true)
        )

        repository = EventRepository(
            eventDao = eventDao,
            firestore = firestore,
            reminderScheduler = reminderScheduler,
            userPreferencesRepository = userPreferencesRepository
        )
    }

    @Test
    fun startRealtimeSync_cancellingScope_removesListenerRegistration() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val job = testScope.launch {
            repository.startRealtimeSync("group-123").collect()
        }

        testScope.advanceUntilIdle()

        // Listener should be registered, not yet removed
        verify(exactly = 1) { eventsCollection.addSnapshotListener(any()) }
        verify(exactly = 0) { listenerRegistration.remove() }

        // Scope / Job cancelled
        job.cancel()
        testScope.advanceUntilIdle()

        // Listener must be removed when collecting scope is cancelled
        verify(exactly = 1) { listenerRegistration.remove() }
    }

    @Test
    fun startRealtimeSync_whenFirestoreReturnsError_propagatesExceptionDownstream() = runTest {
        var caughtError: Throwable? = null

        val job = launch {
            repository.startRealtimeSync("group-123")
                .catch { e -> caughtError = e }
                .collect()
        }

        testScheduler.advanceUntilIdle()

        // Simulate Firestore permission denied error
        val exception = FirebaseFirestoreException("Permission denied", FirebaseFirestoreException.Code.PERMISSION_DENIED)
        listenerSlot.captured.onEvent(null, exception)

        testScheduler.advanceUntilIdle()

        assertThat(caughtError).isNotNull()
        assertThat(caughtError?.message).contains("Permission denied")

        job.cancel()
    }
}
