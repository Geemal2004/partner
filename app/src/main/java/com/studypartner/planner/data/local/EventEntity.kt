package com.studypartner.planner.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val title: String,
    val description: String,
    val startTime: Long,
    val endTime: Long,
    val allDay: Boolean,
    val location: String,
    val reminderMinutes: Int?,
    val createdBy: String,
    val updatedAt: Long
)
