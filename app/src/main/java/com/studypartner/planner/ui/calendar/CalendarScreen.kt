package com.studypartner.planner.ui.calendar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.window.core.layout.WindowWidthSizeClass
import com.studypartner.planner.data.local.EventEntity
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun CalendarScreen(
    initialEventId: String? = null,
    onNavigateToEditor: (String?) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val events by viewModel.events.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val syncError by viewModel.syncError.collectAsState()

    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = navigator.canNavigateBack()) {
        coroutineScope.launch {
            navigator.navigateBack()
        }
    }

    LaunchedEffect(initialEventId) {
        if (initialEventId != null) {
            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, initialEventId)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.syncEvents()
    }

    val sortedEvents = events.sortedBy { it.startTime }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane {
                CalendarListPane(
                    events = sortedEvents,
                    selectedDate = selectedDate,
                    onDateSelected = viewModel::onDateSelected,
                    syncError = syncError,
                    onRetrySync = viewModel::retrySync,
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
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    syncError: String?,
    onRetrySync: () -> Unit,
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

        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (syncError != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Sync Error",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                syncError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        TextButton(onClick = onRetrySync) {
                            Text("Retry", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            if (isExpanded) {
                Row(modifier = Modifier.fillMaxSize()) {
                    MonthView(
                        events = events,
                        selectedDate = selectedDate,
                        onDateSelected = onDateSelected,
                        modifier = Modifier.weight(1f)
                    )
                    AgendaView(
                        events = events,
                        selectedDate = selectedDate,
                        onEventClick = onEventClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                if (isMonthView) {
                    MonthView(
                        events = events,
                        selectedDate = selectedDate,
                        onDateSelected = onDateSelected,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AgendaView(
                        events = events,
                        selectedDate = selectedDate,
                        onEventClick = onEventClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun AgendaView(
    events: List<EventEntity>,
    selectedDate: LocalDate,
    onEventClick: (EventEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateEvents = remember(events, selectedDate) {
        events.filter { event ->
            Instant.ofEpochMilli(event.startTime)
                .atZone(ZoneId.systemDefault())
                .toLocalDate() == selectedDate
        }
    }

    Column(modifier = modifier) {
        val formatter = remember { DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy") }
        Text(
            text = "Events for ${selectedDate.format(formatter)} (${dateEvents.size})",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        val displayEvents = if (dateEvents.isNotEmpty()) dateEvents else events

        if (displayEvents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (events.isEmpty()) "No events found. Add one!" else "No events on this day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayEvents) { event ->
                    EventItem(event = event, onClick = { onEventClick(event) })
                }
            }
        }
    }
}

@Composable
fun MonthView(
    events: List<EventEntity>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    val today = remember { LocalDate.now() }

    val eventsByDate = remember(events) {
        events.groupBy { event ->
            Instant.ofEpochMilli(event.startTime)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }
    }

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Month Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
            }

            Text(
                text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = {
                        currentMonth = YearMonth.from(today)
                        onDateSelected(today)
                    }
                ) {
                    Text("Today")
                }
                IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                }
            }
        }

        // Days of week header (Mon - Sun)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            val daysOfWeek = remember {
                listOf(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY,
                    DayOfWeek.SATURDAY,
                    DayOfWeek.SUNDAY
                )
            }
            for (day in daysOfWeek) {
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        HorizontalDivider()

        // Days Grid
        val firstDayOfMonth = currentMonth.atDay(1)
        val daysInMonth = currentMonth.lengthOfMonth()
        val leadingEmptyCells = firstDayOfMonth.dayOfWeek.value - 1
        val totalCells = leadingEmptyCells + daysInMonth
        val rows = (totalCells + 6) / 7

        for (rowIndex in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                for (colIndex in 0 until 7) {
                    val cellIndex = rowIndex * 7 + colIndex
                    val dayNumber = cellIndex - leadingEmptyCells + 1

                    if (dayNumber in 1..daysInMonth) {
                        val cellDate = currentMonth.atDay(dayNumber)
                        val isSelected = cellDate == selectedDate
                        val isToday = cellDate == today
                        val dayEvents = eventsByDate[cellDate] ?: emptyList()
                        val cellDescription = buildString {
                            append(cellDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")))
                            if (isToday) append(", Today")
                            if (isSelected) append(", Selected")
                            if (dayEvents.isNotEmpty()) {
                                append(", ${dayEvents.size} event${if (dayEvents.size > 1) "s" else ""}")
                            } else {
                                append(", No events")
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.primaryContainer
                                        else -> Color.Transparent
                                    }
                                )
                                .then(
                                    if (isToday && !isSelected) {
                                        Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    } else Modifier
                                )
                                .clickable { onDateSelected(cellDate) }
                                .semantics { contentDescription = cellDescription },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = dayNumber.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                if (dayEvents.isNotEmpty()) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        val dotCount = minOf(dayEvents.size, 3)
                                        repeat(dotCount) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .padding(horizontal = 0.5.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                        else MaterialTheme.colorScheme.tertiary
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )
                    }
                }
            }
        }
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
                title = { Text("Event Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
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
