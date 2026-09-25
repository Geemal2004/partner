package com.studypartner.planner.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Type-safe Navigation 3 destinations. All keys are @Serializable NavKey
 * so [androidx.navigation3.runtime.rememberNavBackStack] can survive process death.
 */
sealed interface AppRoute : NavKey

@Serializable
data object Login : AppRoute

@Serializable
data object GroupSetup : AppRoute

@Serializable
data object Calendar : AppRoute

@Serializable
data object Tasks : AppRoute

@Serializable
data object Settings : AppRoute

@Serializable
data class EventDetail(val id: String) : AppRoute

@Serializable
data class EventEditor(val eventId: String? = null) : AppRoute

@Serializable
data class TaskDetail(val id: String) : AppRoute

@Serializable
data class TaskEditor(val taskId: String? = null) : AppRoute

@Serializable
data class JoinGroup(val code: String) : AppRoute

/** Top-level destinations that each own an independent back stack. */
val TopLevelRoutes: Set<AppRoute> = setOf(Calendar, Tasks, Settings)
