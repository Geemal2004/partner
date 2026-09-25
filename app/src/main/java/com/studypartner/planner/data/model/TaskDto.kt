package com.studypartner.planner.data.model

data class TaskDto(
    val id: String = "",
    val groupId: String = "",
    val title: String = "",
    val notes: String = "",
    val dueDate: Long? = null,
    val assignedTo: List<String> = emptyList(), // uids, empty = everyone
    val isDone: Boolean = false,
    val doneBy: String? = null,
    val createdBy: String = "",
    val updatedAt: Long = 0L
)
