package com.studypartner.planner.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studypartner.planner.data.repository.AuthRepository
import com.studypartner.planner.data.repository.GroupRepository
import com.studypartner.planner.data.repository.UserPreferences
import com.studypartner.planner.data.repository.UserPreferencesRepository
import com.studypartner.planner.notifications.DailySummaryScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository,
    private val groupRepository: GroupRepository,
    private val dailySummaryScheduler: DailySummaryScheduler
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = userPreferencesRepository.userPreferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserPreferences(true, "07:30", 15, true)
    )

    fun updateDailySummaryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateDailySummaryEnabled(enabled)
            if (enabled) {
                dailySummaryScheduler.scheduleDailySummary(userPreferences.value.dailySummaryTime)
            } else {
                dailySummaryScheduler.cancelDailySummary()
            }
        }
    }

    fun updateDailySummaryTime(time: String) {
        viewModelScope.launch {
            userPreferencesRepository.updateDailySummaryTime(time)
            if (userPreferences.value.dailySummaryEnabled) {
                dailySummaryScheduler.scheduleDailySummary(time)
            }
        }
    }

    fun updateDefaultReminderOffset(offset: Int) {
        viewModelScope.launch {
            userPreferencesRepository.updateDefaultReminderOffset(offset)
        }
    }

    fun updatePartnerUpdatesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updatePartnerUpdatesEnabled(enabled)
        }
    }

    fun leaveGroup() {
        viewModelScope.launch {
            groupRepository.leaveGroup()
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}
