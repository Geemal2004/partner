package com.studypartner.planner.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.studypartner.planner.data.model.TaskDto

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val title: String,
    val notes: String,
    val dueDate: Long?,
    val assignedTo: List<String>,
    val isDone: Boolean,
    val doneBy: String?,
    val createdBy: String,
    val updatedAt: Long
)

fun TaskEntity.toDto() = TaskDto(
    id = id,
    groupId = groupId,
    title = title,
    notes = notes,
    dueDate = dueDate,
    assignedTo = assignedTo,
    isDone = isDone,
    doneBy = doneBy,
    createdBy = createdBy,
    updatedAt = updatedAt
)

fun TaskDto.toEntity() = TaskEntity(
    id = id,
    groupId = groupId,
    title = title,
    notes = notes,
    dueDate = dueDate,
    assignedTo = assignedTo,
    isDone = isDone,
    doneBy = doneBy,
    createdBy = createdBy,
    updatedAt = updatedAt
)
