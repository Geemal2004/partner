package com.studypartner.planner.data.model

data class User(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val fcmToken: String = "",
    val groupIds: List<String> = emptyList()
)
