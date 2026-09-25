package com.studypartner.planner.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventDaoTest {

    private lateinit var database: StudyPartnerDatabase
    private lateinit var eventDao: EventDao

    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            StudyPartnerDatabase::class.java
        ).allowMainThreadQueries().build()
        eventDao = database.eventDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndGetEventById(): Unit = runBlocking {
        val event = EventEntity(
            id = "evt-1",
            groupId = "group-1",
            title = "Study Group Session",
            description = "Math review",
            startTime = 1000L,
            endTime = 2000L,
            allDay = false,
            location = "Library",
            reminderMinutes = 15,
            createdBy = "user-1",
            updatedAt = 1000L
        )

        eventDao.insertEvent(event)

        val retrieved = eventDao.getEventById("evt-1")
        assertNotNull(retrieved)
        assertEquals("Study Group Session", retrieved?.title)
    }

    @Test
    fun getEventsForGroup(): Unit = runBlocking {
        val event1 = EventEntity("1", "g1", "Event 1", "", 100L, 200L, false, "", 15, "u1", 100L)
        val event2 = EventEntity("2", "g1", "Event 2", "", 300L, 400L, false, "", 15, "u1", 300L)
        val event3 = EventEntity("3", "g2", "Event 3", "", 200L, 300L, false, "", 15, "u1", 200L)

        eventDao.insertEvents(listOf(event1, event2, event3))

        val g1Events = eventDao.getEventsForGroup("g1").first()
        assertEquals(2, g1Events.size)
        assertEquals(listOf("1", "2"), g1Events.map { it.id })
    }

    @Test
    fun deleteEvent(): Unit = runBlocking {
        val event = EventEntity("1", "g1", "Event 1", "", 100L, 200L, false, "", 15, "u1", 100L)
        eventDao.insertEvent(event)

        eventDao.deleteEvent("1")

        val retrieved = eventDao.getEventById("1")
        assertNull(retrieved)
    }

    @Test
    fun getEventsForDay(): Unit = runBlocking {
        val event1 = EventEntity("1", "g1", "Morning Event", "", 1000L, 2000L, false, "", 15, "u1", 1000L)
        val event2 = EventEntity("2", "g1", "Evening Event", "", 5000L, 6000L, false, "", 15, "u1", 5000L)
        val event3 = EventEntity("3", "g1", "Next Day Event", "", 15000L, 16000L, false, "", 15, "u1", 15000L)

        eventDao.insertEvents(listOf(event1, event2, event3))

        val todaysEvents = eventDao.getEventsForDay(startOfDay = 500L, endOfDay = 10000L)
        assertEquals(2, todaysEvents.size)
        assertEquals(listOf("1", "2"), todaysEvents.map { it.id })
    }
}
