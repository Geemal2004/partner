package com.studypartner.planner.ui.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: String,
    onNavigateToEditor: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val events by viewModel.events.collectAsState()
    val event = events.find { it.id == eventId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (event != null) {
                        IconButton(onClick = { onNavigateToEditor(event.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Event")
                        }
                        IconButton(onClick = {
                            viewModel.deleteEvent(event.id)
                            onNavigateBack()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Event", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (event == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Event not found or loading...", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = event.title, style = MaterialTheme.typography.headlineMedium)

                if (event.description.isNotBlank()) {
                    Text(text = event.description, style = MaterialTheme.typography.bodyLarge)
                }

                val start = Instant.ofEpochMilli(event.startTime).atZone(ZoneId.systemDefault()).toLocalDateTime()
                val end = Instant.ofEpochMilli(event.endTime).atZone(ZoneId.systemDefault()).toLocalDateTime()
                val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Start: ${start.format(formatter)}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "End: ${end.format(formatter)}", style = MaterialTheme.typography.bodyMedium)
                        if (event.location.isNotBlank()) {
                            Text(text = "Location: ${event.location}", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (event.allDay) {
                            Text(text = "All day event", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        viewModel.deleteEvent(event.id)
                        onNavigateBack()
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onNavigateToEditor(event.id) }) {
                        Text("Edit Event")
                    }
                }
            }
        }
    }
}
