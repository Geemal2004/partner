package com.studypartner.planner.ui.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studypartner.planner.data.local.EventEntity
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

@Composable
fun EventEditorScreen(
    eventId: String?,
    onNavigateBack: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val events by viewModel.events.collectAsState()
    val eventToEdit = events.find { it.id == eventId }

    EventEditorContent(
        isEditing = eventId != null,
        initialEvent = eventToEdit,
        onSave = { title, description, location, startTime, allDay ->
            val newEvent = EventEntity(
                id = eventId ?: UUID.randomUUID().toString(),
                groupId = viewModel.currentGroupId.value ?: "",
                title = title.takeIf { it.isNotBlank() } ?: "Untitled Event",
                description = description,
                startTime = startTime,
                endTime = startTime + 3600000,
                allDay = allDay,
                location = location,
                reminderMinutes = 15,
                createdBy = viewModel.getUserId(),
                updatedAt = System.currentTimeMillis()
            )
            viewModel.saveEvent(newEvent)
            onNavigateBack()
        },
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditorContent(
    isEditing: Boolean,
    initialEvent: EventEntity? = null,
    onSave: (title: String, description: String, location: String, startTime: Long, allDay: Boolean) -> Unit,
    onNavigateBack: () -> Unit
) {
    var title by remember { mutableStateOf(initialEvent?.title ?: "") }
    var description by remember { mutableStateOf(initialEvent?.description ?: "") }
    var location by remember { mutableStateOf(initialEvent?.location ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var selectedStartTime by remember {
        mutableStateOf(initialEvent?.startTime ?: System.currentTimeMillis())
    }
    var allDay by remember { mutableStateOf(initialEvent?.allDay ?: false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Event" else "New Event") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        onSave(title, description, location, selectedStartTime, allDay)
                    }) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = allDay, onCheckedChange = { allDay = it })
                Text("All Day Event")
            }

            val startDateStr = Instant.ofEpochMilli(selectedStartTime).atZone(ZoneId.systemDefault()).toLocalDate()
            Button(onClick = { showDatePicker = true }) {
                Text("Start Date: $startDateStr")
            }
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { selectedStartTime = it }
                        showDatePicker = false
                    }) {
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
}
