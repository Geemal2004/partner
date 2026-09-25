package com.studypartner.planner.data.model

data class EventDto(
    val id: String = "",
    val groupId: String = "",
    val title: String = "",
    val description: String = "",
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val allDay: Boolean = false,
    val location: String = "",
    val reminderMinutes: Int? = null,
    val createdBy: String = "",
    val updatedAt: Long = 0L
)
