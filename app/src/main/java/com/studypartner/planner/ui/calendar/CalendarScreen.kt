package com.studypartner.planner.ui.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studypartner.planner.data.local.EventEntity
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun CalendarScreen(
    onNavigateToEditor: (String?) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val events by viewModel.events.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.syncEvents()
    }

    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    val coroutineScope = rememberCoroutineScope()
    
    val sortedEvents = events.sortedBy { it.startTime }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                CalendarListPane(
                    events = sortedEvents,
                    onEventClick = { event ->
                        coroutineScope.launch {
                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, event.id)
                        }
                    },
                    onAddClick = { onNavigateToEditor(null) }
                )
            }
        },
        detailPane = {
            AnimatedPane {
                val selectedId = navigator.currentDestination?.contentKey
                val selectedEvent = events.find { it.id == selectedId }
                if (selectedEvent != null) {
                    EventDetailPane(
                        event = selectedEvent,
                        onEditClick = { onNavigateToEditor(selectedEvent.id) },
                        onDeleteClick = {
                            viewModel.deleteEvent(selectedEvent.id)
                            coroutineScope.launch { navigator.navigateBack() }
                        },
                        onBackClick = {
                            coroutineScope.launch { navigator.navigateBack() }
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Select an event to view details")
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarListPane(
    events: List<EventEntity>,
    onEventClick: (EventEntity) -> Unit,
    onAddClick: () -> Unit
) {
    var isMonthView by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                actions = {
                    TextButton(onClick = { isMonthView = !isMonthView }) {
                        Text(if (isMonthView) "Agenda" else "Month")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Event")
            }
        }
    ) { padding ->
        val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
        val isExpanded = windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED
        
        if (isExpanded) {
            Row(modifier = Modifier.padding(padding).fillMaxSize()) {
                MonthView(
                    modifier = Modifier.weight(1f)
                )
                AgendaView(
                    events = events,
                    onEventClick = onEventClick,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            if (isMonthView) {
                MonthView(
                    modifier = Modifier.padding(padding).fillMaxSize()
                )
            } else {
                AgendaView(
                    events = events,
                    onEventClick = onEventClick,
                    modifier = Modifier.padding(padding).fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun AgendaView(
    events: List<EventEntity>,
    onEventClick: (EventEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (events.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text("No events found. Add one!")
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(events) { event ->
                EventItem(event = event, onClick = { onEventClick(event) })
            }
        }
    }
}

@Composable
fun MonthView(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Month Grid View (Mock)", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun EventItem(event: EventEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            val date = Instant.ofEpochMilli(event.startTime).atZone(ZoneId.systemDefault()).toLocalDate()
            val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
            Text(text = "Date: ${date.format(formatter)}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailPane(
    event: EventEntity,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event Details") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = event.title, style = MaterialTheme.typography.headlineMedium)
            
            Text(text = "Description: ${event.description}", style = MaterialTheme.typography.bodyLarge)
            
            val start = Instant.ofEpochMilli(event.startTime).atZone(ZoneId.systemDefault()).toLocalDateTime()
            val end = Instant.ofEpochMilli(event.endTime).atZone(ZoneId.systemDefault()).toLocalDateTime()
            val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
            
            Text(text = "Start: ${start.format(formatter)}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "End: ${end.format(formatter)}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Location: ${event.location}", style = MaterialTheme.typography.bodyLarge)
            
            Spacer(modifier = Modifier.weight(1f))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDeleteClick) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onEditClick) {
                    Text("Edit")
                }
            }
        }
    }
}
