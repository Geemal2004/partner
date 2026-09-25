package com.studypartner.planner.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.studypartner.planner.data.local.EventDao
import com.studypartner.planner.data.local.EventEntity
import com.studypartner.planner.data.model.EventDto
import com.studypartner.planner.notifications.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(
    private val eventDao: EventDao,
    private val firestore: FirebaseFirestore,
    private val reminderScheduler: ReminderScheduler,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    fun getEvents(groupId: String): Flow<List<EventEntity>> {
        return eventDao.getEventsForGroup(groupId)
    }

    suspend fun syncEvents(groupId: String) {
        if (groupId.isEmpty()) return
        try {
            val snapshot = firestore.collection("groups").document(groupId)
                .collection("events")
                .get()
                .await()
            var remoteEvents = snapshot.documents.mapNotNull { it.toObject(EventDto::class.java) }
            if (remoteEvents.isEmpty()) {
                val rootSnapshot = firestore.collection("events")
                    .whereEqualTo("groupId", groupId)
                    .get()
                    .await()
                remoteEvents = rootSnapshot.toObjects(EventDto::class.java)
            }
            val entities = remoteEvents.map { it.toEntity() }
            eventDao.insertEvents(entities)

            val prefs = userPreferencesRepository.userPreferencesFlow.first()
            entities.forEach { event ->
                reminderScheduler.scheduleReminder(event, prefs.defaultReminderOffset)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun saveEvent(event: EventEntity) {
        val updatedEvent = event.copy(updatedAt = System.currentTimeMillis())
        // Save to Room first (offline-first)
        eventDao.insertEvent(updatedEvent)

        val prefs = userPreferencesRepository.userPreferencesFlow.first()
        reminderScheduler.scheduleReminder(updatedEvent, prefs.defaultReminderOffset)

        // Then sync to Firestore
        if (updatedEvent.groupId.isNotEmpty()) {
            try {
                firestore.collection("groups").document(updatedEvent.groupId)
                    .collection("events").document(updatedEvent.id)
                    .set(updatedEvent.toDto())
                    .await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                firestore.collection("events")
                    .document(updatedEvent.id)
                    .set(updatedEvent.toDto())
                    .await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun deleteEvent(eventId: String) {
        val existingEvent = eventDao.getEventById(eventId)
        // Delete from Room first
        eventDao.deleteEvent(eventId)
        reminderScheduler.cancelReminder(eventId)

        // Then delete from Firestore
        if (existingEvent != null && existingEvent.groupId.isNotEmpty()) {
            try {
                firestore.collection("groups").document(existingEvent.groupId)
                    .collection("events").document(eventId)
                    .delete()
                    .await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        try {
            firestore.collection("events")
                .document(eventId)
                .delete()
                .await()
        } catch (e: Exception) {
            // Ignore if not in top level collection
        }
    }
}

fun EventDto.toEntity(): EventEntity = EventEntity(
    id = id,
    groupId = groupId,
    title = title,
    description = description,
    startTime = startTime,
    endTime = endTime,
    allDay = allDay,
    location = location,
    reminderMinutes = reminderMinutes,
    createdBy = createdBy,
    updatedAt = updatedAt
)

fun EventEntity.toDto(): EventDto = EventDto(
    id = id,
    groupId = groupId,
    title = title,
    description = description,
    startTime = startTime,
    endTime = endTime,
    allDay = allDay,
    location = location,
    reminderMinutes = reminderMinutes,
    createdBy = createdBy,
    updatedAt = updatedAt
)
