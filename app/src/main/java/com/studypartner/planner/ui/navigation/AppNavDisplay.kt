package com.studypartner.planner.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.studypartner.planner.R
import com.studypartner.planner.ui.auth.LoginScreen
import com.studypartner.planner.ui.calendar.CalendarScreen
import com.studypartner.planner.ui.calendar.EventEditorScreen
import com.studypartner.planner.ui.group.GroupSetupScreen
import com.studypartner.planner.ui.group.JoinGroupScreen
import com.studypartner.planner.ui.settings.SettingsScreen
import com.studypartner.planner.ui.tasks.TasksScreen

@Composable
fun AppNavDisplay(
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
) {
    NavDisplay(
        backStack = navigator.activeBackStack,
        modifier = modifier,
        onBack = { navigator.goBack() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Login> {
                LoginScreen(
                    onLoginSuccess = { navigator.resetTo(Calendar) }
                )
            }
            entry<GroupSetup> {
                GroupSetupScreen(
                    onGroupSelected = { navigator.resetTo(Calendar) }
                )
            }
            entry<Calendar> {
                CalendarScreen(
                    onNavigateToEditor = { eventId ->
                        navigator.navigate(EventEditor(eventId))
                    }
                )
            }
            entry<Tasks> {
                TasksScreen()
            }
            entry<Settings> {
                SettingsScreen()
            }
            entry<EventDetail> { key ->
                PlaceholderScreen(
                    title = stringResource(R.string.placeholder_event_detail, key.id)
                )
            }
            entry<EventEditor> { key ->
                EventEditorScreen(
                    eventId = key.eventId,
                    onNavigateBack = { navigator.goBack() }
                )
            }
            entry<TaskDetail> { key ->
                PlaceholderScreen(
                    title = stringResource(R.string.placeholder_task_detail, key.id)
                )
            }
            entry<TaskEditor> { key ->
                PlaceholderScreen(
                    title = stringResource(
                        R.string.placeholder_task_editor,
                        key.taskId ?: stringResource(R.string.label_new)
                    )
                )
            }
            entry<JoinGroup> { key ->
                JoinGroupScreen(
                    code = key.code,
                    onComplete = { navigator.resetTo(Calendar) }
                )
            }
        },
    )
}

@Composable
internal fun PlaceholderScreen(
    title: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.widthIn(max = 840.dp),
        )
    }
}
