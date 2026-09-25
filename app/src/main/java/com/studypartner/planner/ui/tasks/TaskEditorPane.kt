package com.studypartner.planner.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.studypartner.planner.data.model.TaskDto
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorPane(
    taskId: String?,
    initialTask: TaskDto?,
    currentUserUid: String?,
    onSave: (id: String?, title: String, notes: String, dueDate: Long?, assignedTo: List<String>) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var notes by remember { mutableStateOf(initialTask?.notes ?: "") }
    var dueDate by remember { mutableStateOf(initialTask?.dueDate) }
    var assignToMe by remember { mutableStateOf(initialTask?.assignedTo?.isNotEmpty() == true) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDate)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (taskId == null) "New Task" else "Edit Task") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            // Due Date Selection
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = dueDate?.let {
                        val date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                        "Due: ${date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}"
                    } ?: "No Due Date",
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = { showDatePicker = true }) {
                    Text("Select Date")
                }
                if (dueDate != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { dueDate = null }) {
                        Text("Clear")
                    }
                }
            }

            // Assignee Selection
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (assignToMe) "Assigned to: Me" else "Assigned to: Everyone",
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = assignToMe,
                    onCheckedChange = { assignToMe = it }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val assignedTo = if (assignToMe && currentUserUid != null) listOf(currentUserUid) else emptyList()
                        onSave(taskId, title, notes, dueDate, assignedTo)
                    },
                    enabled = title.isNotBlank()
                ) {
                    Text("Save")
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dueDate = datePickerState.selectedDateMillis
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
