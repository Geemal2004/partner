package com.studypartner.planner.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studypartner.planner.data.model.Group
import com.studypartner.planner.data.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    val currentGroupId: StateFlow<String?> = groupRepository.currentGroupId

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _hasLoadedGroups = MutableStateFlow(false)
    val hasLoadedGroups: StateFlow<Boolean> = _hasLoadedGroups.asStateFlow()

    fun fetchGroups() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = groupRepository.getUserGroups()
            if (result.isSuccess) {
                val list = result.getOrDefault(emptyList())
                _groups.value = list
                if (list.isNotEmpty() && currentGroupId.value == null) {
                    groupRepository.setCurrentGroupId(list.first().groupId)
                }
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to fetch groups"
            }
            _isLoading.value = false
            _hasLoadedGroups.value = true
        }
    }

    fun createGroup(name: String, onComplete: (Boolean) -> Unit) {
        if (name.isBlank()) {
            _error.value = "Group name cannot be empty"
            onComplete(false)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = groupRepository.createGroup(name)
            if (result.isSuccess) {
                val createdGroup = result.getOrNull()
                if (createdGroup != null) {
                    val updatedList = _groups.value.filter { it.groupId != createdGroup.groupId } + createdGroup
                    _groups.value = updatedList
                    groupRepository.setCurrentGroupId(createdGroup.groupId)
                }
                _isLoading.value = false
                onComplete(true)
                // Refresh list in background
                fetchGroups()
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to create group"
                _isLoading.value = false
                onComplete(false)
            }
        }
    }

    fun joinGroup(inviteCode: String, onComplete: (Boolean) -> Unit) {
        val cleanCode = inviteCode.trim().uppercase()
        if (cleanCode.isBlank()) {
            _error.value = "Invite code cannot be empty"
            onComplete(false)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = groupRepository.joinGroupByCode(cleanCode)
            if (result.isSuccess) {
                val joinedGroup = result.getOrNull()
                if (joinedGroup != null) {
                    val updatedList = _groups.value.filter { it.groupId != joinedGroup.groupId } + joinedGroup
                    _groups.value = updatedList
                    groupRepository.setCurrentGroupId(joinedGroup.groupId)
                }
                _isLoading.value = false
                onComplete(true)
                // Refresh list in background
                fetchGroups()
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to join group"
                _isLoading.value = false
                onComplete(false)
            }
        }
    }

    fun setCurrentGroupId(groupId: String) {
        groupRepository.setCurrentGroupId(groupId)
    }
    
    fun clearError() {
        _error.value = null
    }
}
