package com.studypartner.planner.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studypartner.planner.data.model.TaskDto
import com.studypartner.planner.data.repository.AuthRepository
import com.studypartner.planner.data.repository.TaskRepository
import com.studypartner.planner.data.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    val currentUser = authRepository.getCurrentUserSync()

    private val _tasks = MutableStateFlow<List<TaskDto>>(emptyList())

    val filteredTasks: StateFlow<List<TaskDto>> = combine(_tasks, _filter) { tasks, filter ->
        val user = authRepository.getCurrentUserSync()
        val now = System.currentTimeMillis()
        val filtered = when (filter) {
            TaskFilter.ALL -> tasks.filter { !it.isDone }
            TaskFilter.MINE -> tasks.filter { !it.isDone && user != null && (it.assignedTo.isEmpty() || it.assignedTo.contains(user.uid)) }
            TaskFilter.PARTNERS -> tasks.filter { !it.isDone && user != null && it.assignedTo.isNotEmpty() && !it.assignedTo.contains(user.uid) }
            TaskFilter.OVERDUE -> tasks.filter { !it.isDone && it.dueDate != null && it.dueDate < now }
            TaskFilter.COMPLETED -> tasks.filter { it.isDone }
        }
        filtered.sortedBy { it.dueDate ?: Long.MAX_VALUE }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            groupRepository.currentGroupId.collect { groupId ->
                if (groupId != null) {
                    taskRepository.syncTasks(groupId)
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

    fun toggleTaskCompletion(task: TaskDto) {
        viewModelScope.launch {
            val user = authRepository.getCurrentUserSync() ?: return@launch
            val updatedTask = task.copy(
                isDone = !task.isDone,
                doneBy = if (!task.isDone) user.uid else null,
                updatedAt = System.currentTimeMillis()
            )
            taskRepository.saveTask(updatedTask)
        }
    }

    fun deleteTask(task: TaskDto) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
        }
    }

    fun saveTask(id: String?, title: String, notes: String, dueDate: Long?, assignedTo: List<String>) {
        viewModelScope.launch {
            val user = authRepository.getCurrentUserSync() ?: return@launch
            val groupId = groupRepository.currentGroupId.value ?: return@launch

            val task = if (id != null) {
                // Editing existing
                val existing = _tasks.value.find { it.id == id }
                if (existing != null) {
                    existing.copy(
                        title = title,
                        notes = notes,
                        dueDate = dueDate,
                        assignedTo = assignedTo,
                        updatedAt = System.currentTimeMillis()
                    )
                } else null
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
                taskRepository.saveTask(task)
            }
        }
    }
}
