package com.studypartner.planner.data.model

data class Group(
    val groupId: String = "",
    val name: String = "",
    val memberIds: List<String> = emptyList(),
    val inviteCode: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
