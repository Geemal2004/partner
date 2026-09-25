package com.studypartner.planner.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.studypartner.planner.data.model.TaskDto
import com.studypartner.planner.data.repository.AuthRepository
import com.studypartner.planner.data.repository.GroupRepository
import com.studypartner.planner.data.repository.TaskRepository
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
import java.util.UUID
import javax.inject.Inject

enum class TaskFilter {
    ALL, MINE, PARTNERS, OVERDUE, COMPLETED
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val authRepository: AuthRepository,
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(TaskFilter.ALL)
    val filter: StateFlow<TaskFilter> = _filter

    val currentUser: StateFlow<FirebaseUser?> = authRepository.currentUser.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = authRepository.getCurrentUserSync()
    )

    private val _retrySignal = MutableStateFlow(0)

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError

    fun clearSyncError() {
        _syncError.value = null
    }

    private val _tasks = MutableStateFlow<List<TaskDto>>(emptyList())

    val filteredTasks: StateFlow<List<TaskDto>> = combine(_tasks, _filter, currentUser) { tasks, filter, user ->
        val now = System.currentTimeMillis()
        val filtered = when (filter) {
            TaskFilter.ALL -> tasks.filter { !it.isDone }
            TaskFilter.MINE -> tasks.filter {
                !it.isDone && user != null && (it.assignedTo.isEmpty() || it.assignedTo.contains(user.uid))
            }
            TaskFilter.PARTNERS -> tasks.filter {
                !it.isDone && user != null && it.assignedTo.isNotEmpty() && !it.assignedTo.contains(user.uid)
            }
            TaskFilter.OVERDUE -> tasks.filter { !it.isDone && it.dueDate != null && it.dueDate < now }
            TaskFilter.COMPLETED -> tasks.filter { it.isDone }
        }
        filtered.sortedBy { it.dueDate ?: Long.MAX_VALUE }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            combine(groupRepository.currentGroupId, _retrySignal) { groupId, _ -> groupId }
                .collectLatest { groupId ->
                    if (!groupId.isNullOrEmpty()) {
                        taskRepository.startRealtimeSync(groupId)
                            .catch { e -> _syncError.value = e.localizedMessage ?: "Realtime sync error" }
                            .collect()
                    }
                }
        }
        viewModelScope.launch {
            groupRepository.currentGroupId
                .flatMapLatest { groupId ->
                    if (groupId != null) {
                        taskRepository.getTasks(groupId)
                    } else {
                        flowOf(emptyList())
                    }
                }
                .collect { tasks ->
                    _tasks.value = tasks
                }
        }
    }

    fun setFilter(filter: TaskFilter) {
        _filter.value = filter
    }

    fun syncTasks() {
        viewModelScope.launch {
            _syncError.value = null
            val groupId = groupRepository.currentGroupId.value ?: return@launch
            try {
                taskRepository.syncTasks(groupId)
            } catch (e: Exception) {
                _syncError.value = e.localizedMessage ?: "Failed to sync tasks"
            }
        }
    }

    fun retrySync() {
        _syncError.value = null
        _retrySignal.value++
        syncTasks()
    }

    fun toggleTaskCompletion(task: TaskDto) {
        viewModelScope.launch {
            val user = authRepository.getCurrentUserSync() ?: currentUser.value ?: return@launch
            val updatedTask = task.copy(
                isDone = !task.isDone,
                doneBy = if (!task.isDone) user.uid else null,
                updatedAt = System.currentTimeMillis()
            )
            try {
                taskRepository.saveTask(updatedTask)
            } catch (e: Exception) {
                _syncError.value = e.localizedMessage ?: "Failed to update task"
            }
        }
    }

    fun deleteTask(task: TaskDto) {
        viewModelScope.launch {
            try {
                taskRepository.deleteTask(task)
            } catch (e: Exception) {
                _syncError.value = e.localizedMessage ?: "Failed to delete task"
            }
        }
    }

    fun saveTask(id: String?, title: String, notes: String, dueDate: Long?, assignedTo: List<String>) {
        viewModelScope.launch {
            val user = authRepository.getCurrentUserSync() ?: currentUser.value ?: return@launch
            val groupId = groupRepository.currentGroupId.value ?: return@launch

            val task = if (id != null) {
                // Editing existing
                val existing = _tasks.value.find { it.id == id }
                existing?.copy(
                    title = title,
                    notes = notes,
                    dueDate = dueDate,
                    assignedTo = assignedTo,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                // New task
                TaskDto(
                    id = UUID.randomUUID().toString(),
                    groupId = groupId,
                    title = title,
                    notes = notes,
                    dueDate = dueDate,
                    assignedTo = assignedTo,
                    isDone = false,
                    doneBy = null,
                    createdBy = user.uid,
                    updatedAt = System.currentTimeMillis()
                )
            }

            if (task != null) {
                try {
                    taskRepository.saveTask(task)
                } catch (e: Exception) {
                    _syncError.value = e.localizedMessage ?: "Failed to save task"
                }
            }
        }
    }
}
