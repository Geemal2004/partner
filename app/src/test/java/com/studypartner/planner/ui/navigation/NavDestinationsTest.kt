package com.studypartner.planner.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavDestinationsTest {

    @Test
    fun topLevelRoutes_containsExactlyCalendarTasksSettings() {
        assertEquals(3, TopLevelRoutes.size)
        assertTrue(TopLevelRoutes.contains(Calendar))
        assertTrue(TopLevelRoutes.contains(Tasks))
        assertTrue(TopLevelRoutes.contains(Settings))
    }

    @Test
    fun eventEditor_defaultsToCreateMode() {
        val editor = EventEditor()
        assertEquals(null, editor.eventId)
    }

    @Test
    fun joinGroup_preservesInviteCode() {
        assertEquals("ABC123", JoinGroup("ABC123").code)
    }

    @Test
    fun topLevelNavKeySaver_savesAndRestoresCorrectly() {
        val scope = androidx.compose.runtime.saveable.SaverScope { true }
        val tasksState = androidx.compose.runtime.mutableStateOf<androidx.navigation3.runtime.NavKey>(Tasks)
        val savedTasks = with(TopLevelNavKeySaver) { scope.save(tasksState) }
        assertEquals("Tasks", savedTasks)

        val restoredTasks = TopLevelNavKeySaver.restore("Tasks")
        assertEquals(Tasks, restoredTasks?.value)

        val settingsState = androidx.compose.runtime.mutableStateOf<androidx.navigation3.runtime.NavKey>(Settings)
        val savedSettings = with(TopLevelNavKeySaver) { scope.save(settingsState) }
        assertEquals("Settings", savedSettings)

        val restoredSettings = TopLevelNavKeySaver.restore("Settings")
        assertEquals(Settings, restoredSettings?.value)
    }
}
