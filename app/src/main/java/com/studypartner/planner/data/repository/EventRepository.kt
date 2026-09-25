package com.studypartner.planner.data.repository

import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.studypartner.planner.data.local.EventDao
import com.studypartner.planner.data.local.EventEntity
import com.studypartner.planner.data.model.EventDto
import com.studypartner.planner.notifications.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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

    fun startRealtimeSync(groupId: String): Flow<List<EventEntity>> = callbackFlow {
        if (groupId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = firestore.collection("groups").document(groupId)
            .collection("events")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    launch(Dispatchers.IO) {
                        try {
                            val prefs = userPreferencesRepository.userPreferencesFlow.first()
                            for (change in snapshot.documentChanges) {
                                when (change.type) {
                                    DocumentChange.Type.ADDED,
                                    DocumentChange.Type.MODIFIED -> {
                                        val dto = change.document.toObject(EventDto::class.java)
                                        val entity = dto.toEntity()
                                        eventDao.insertEvent(entity)
                                        reminderScheduler.scheduleReminder(entity, prefs.defaultReminderOffset)
                                    }
                                    DocumentChange.Type.REMOVED -> {
                                        eventDao.deleteEvent(change.document.id)
                                        reminderScheduler.cancelReminder(change.document.id)
                                    }
                                }
                            }
                            val remoteEvents = snapshot.documents.mapNotNull { it.toObject(EventDto::class.java) }
                            trySend(remoteEvents.map { it.toEntity() })
                        } catch (e: Exception) {
                            close(e)
                        }
                    }
                }
            }

        awaitClose {
            registration.remove()
        }
    }

    suspend fun syncEvents(groupId: String) {
        if (groupId.isEmpty()) return
        val snapshot = firestore.collection("groups").document(groupId)
            .collection("events")
            .get()
            .await()
        val remoteEvents = snapshot.documents.mapNotNull { it.toObject(EventDto::class.java) }
        val entities = remoteEvents.map { it.toEntity() }
        eventDao.insertEvents(entities)

        val prefs = userPreferencesRepository.userPreferencesFlow.first()
        entities.forEach { event ->
            reminderScheduler.scheduleReminder(event, prefs.defaultReminderOffset)
        }
    }

    suspend fun saveEvent(event: EventEntity) {
        val updatedEvent = event.copy(updatedAt = System.currentTimeMillis())
        // Save to Room first (offline-first)
        eventDao.insertEvent(updatedEvent)

        val prefs = userPreferencesRepository.userPreferencesFlow.first()
        reminderScheduler.scheduleReminder(updatedEvent, prefs.defaultReminderOffset)

        // Then sync to Firestore subcollection only
        if (updatedEvent.groupId.isNotEmpty()) {
            firestore.collection("groups").document(updatedEvent.groupId)
                .collection("events").document(updatedEvent.id)
                .set(updatedEvent.toDto())
                .await()
        }
    }

    suspend fun deleteEvent(eventId: String) {
        val existingEvent = eventDao.getEventById(eventId)
        // Delete from Room first
        eventDao.deleteEvent(eventId)
        reminderScheduler.cancelReminder(eventId)

        // Then delete from Firestore subcollection only
        if (existingEvent != null && existingEvent.groupId.isNotEmpty()) {
            firestore.collection("groups").document(existingEvent.groupId)
                .collection("events").document(eventId)
                .delete()
                .await()
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
