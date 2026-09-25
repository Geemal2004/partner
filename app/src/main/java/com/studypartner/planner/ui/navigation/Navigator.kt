package com.studypartner.planner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

/**
 * Owns navigation state outside leaf composables.
 *
 * Top-level tabs (Calendar / Tasks / Settings) each keep an independent
 * [rememberNavBackStack] so switching tabs preserves nested navigation.
 * Auth gating (Phase 1) will call [resetTo] when the session changes.
 */
class AppNavigator(
    private val calendarStack: NavBackStack<NavKey>,
    private val tasksStack: NavBackStack<NavKey>,
    private val settingsStack: NavBackStack<NavKey>,
    initialTopLevel: NavKey = Calendar,
) {
    var selectedTopLevel: NavKey by mutableStateOf(
        if (initialTopLevel in TopLevelRoutes) initialTopLevel else Calendar
    )
        private set

    val activeBackStack: NavBackStack<NavKey>
        get() = stackFor(selectedTopLevel)

    val currentDestination: NavKey?
        get() = activeBackStack.lastOrNull()

    fun stackFor(topLevel: NavKey): NavBackStack<NavKey> = when (topLevel) {
        Calendar -> calendarStack
        Tasks -> tasksStack
        Settings -> settingsStack
        else -> calendarStack
    }

    fun navigate(destination: NavKey) {
        when (destination) {
            is Calendar, is Tasks, is Settings -> switchTopLevel(destination)
            else -> activeBackStack.add(destination)
        }
    }

    fun switchTopLevel(destination: NavKey) {
        require(destination in TopLevelRoutes) {
            "Only top-level routes can be switched: $destination"
        }
        selectedTopLevel = destination
        val stack = stackFor(destination)
        if (stack.isEmpty()) {
            stack.add(destination)
        }
    }

    /** Clears every tab stack and shows [root]. Used when auth state changes. */
    fun resetTo(root: NavKey) {
        calendarStack.clear()
        tasksStack.clear()
        settingsStack.clear()
        when (root) {
            Calendar, Tasks, Settings -> {
                selectedTopLevel = root
                stackFor(root).add(root)
            }
            else -> {
                selectedTopLevel = Calendar
                calendarStack.add(root)
            }
        }
    }

    fun goBack(): Boolean {
        val stack = activeBackStack
        if (stack.size > 1) {
            stack.removeLastOrNull()
            return true
        }
        return false
    }
}

@Composable
fun rememberAppNavigator(
    startDestination: NavKey = Calendar,
): AppNavigator {
    val calendarStack = rememberNavBackStack(Calendar)
    val tasksStack = rememberNavBackStack(Tasks)
    val settingsStack = rememberNavBackStack(Settings)
    return remember(calendarStack, tasksStack, settingsStack, startDestination) {
        AppNavigator(
            calendarStack = calendarStack,
            tasksStack = tasksStack,
            settingsStack = settingsStack,
            initialTopLevel = startDestination,
        )
    }
}
