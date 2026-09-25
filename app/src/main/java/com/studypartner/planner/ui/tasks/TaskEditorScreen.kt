package com.studypartner.planner.ui.tasks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun TaskEditorScreen(
    taskId: String?,
    onNavigateBack: () -> Unit,
    viewModel: TasksViewModel = hiltViewModel()
) {
    val tasks by viewModel.filteredTasks.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val taskToEdit = if (taskId != null) tasks.find { it.id == taskId } else null

    TaskEditorPane(
        taskId = taskId,
        initialTask = taskToEdit,
        currentUserUid = currentUser?.uid,
        onSave = { id, title, notes, dueDate, assignedTo ->
            viewModel.saveTask(id, title, notes, dueDate, assignedTo)
            onNavigateBack()
        },
        onCancel = onNavigateBack
    )
}
