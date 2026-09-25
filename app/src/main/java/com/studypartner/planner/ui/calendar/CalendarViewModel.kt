package com.studypartner.planner.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.studypartner.planner.data.local.EventEntity
import com.studypartner.planner.data.repository.EventRepository
import com.studypartner.planner.data.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val groupRepository: GroupRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    val currentGroupId = groupRepository.currentGroupId

    private val _retrySignal = MutableStateFlow(0)

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError

    fun clearSyncError() {
        _syncError.value = null
    }

    init {
        viewModelScope.launch {
            combine(currentGroupId, _retrySignal) { groupId, _ -> groupId }
                .collectLatest { groupId ->
                    if (!groupId.isNullOrEmpty()) {
                        eventRepository.startRealtimeSync(groupId)
                            .catch { e -> _syncError.value = e.localizedMessage ?: "Realtime sync error" }
                            .collect()
                    }
                }
        }
    }

    val events: StateFlow<List<EventEntity>> = currentGroupId.flatMapLatest { groupId ->
        if (groupId == null) flowOf(emptyList())
        else eventRepository.getEvents(groupId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }

    fun syncEvents() {
        viewModelScope.launch {
            _syncError.value = null
            val groupId = currentGroupId.value ?: return@launch
            try {
                eventRepository.syncEvents(groupId)
            } catch (e: Exception) {
                _syncError.value = e.localizedMessage ?: "Failed to sync events"
            }
        }
    }

    fun retrySync() {
        _syncError.value = null
        _retrySignal.value++
        syncEvents()
    }

    fun saveEvent(event: EventEntity) {
        viewModelScope.launch {
            try {
                eventRepository.saveEvent(event)
            } catch (e: Exception) {
                _syncError.value = e.localizedMessage ?: "Failed to save event"
            }
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            try {
                eventRepository.deleteEvent(eventId)
            } catch (e: Exception) {
                _syncError.value = e.localizedMessage ?: "Failed to delete event"
            }
        }
    }
    
    fun getUserId(): String = auth.currentUser?.uid ?: ""
}
