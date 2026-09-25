package com.studypartner.planner.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.studypartner.planner.R
import com.studypartner.planner.ui.navigation.Calendar
import com.studypartner.planner.ui.navigation.Settings
import com.studypartner.planner.ui.navigation.Tasks

enum class TopLevelDestination(
    val route: NavKey,
    val icon: ImageVector,
    val labelRes: Int,
) {
    CALENDAR(Calendar, Icons.Rounded.CalendarMonth, R.string.nav_calendar),
    TASKS(Tasks, Icons.Rounded.CheckCircle, R.string.nav_tasks),
    SETTINGS(Settings, Icons.Rounded.Settings, R.string.nav_settings),
}
