package com.studypartner.planner.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE groupId = :groupId ORDER BY startTime ASC")
    fun getEventsForGroup(groupId: String): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :eventId")
    suspend fun getEventById(eventId: String): EventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EventEntity>)

    @Query("DELETE FROM events WHERE id = :eventId")
    suspend fun deleteEvent(eventId: String)

    @Query("DELETE FROM events WHERE groupId = :groupId")
    suspend fun deleteAllForGroup(groupId: String)

    @Query("SELECT * FROM events WHERE startTime >= :startOfDay AND startTime <= :endOfDay ORDER BY startTime ASC")
    suspend fun getEventsForDay(startOfDay: Long, endOfDay: Long): List<EventEntity>
}
