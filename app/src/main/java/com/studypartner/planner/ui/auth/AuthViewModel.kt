package com.studypartner.planner.ui.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.studypartner.planner.data.repository.AuthRepository
import com.studypartner.planner.data.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val groupRepository: GroupRepository
) : ViewModel() {

    val currentUser: StateFlow<FirebaseUser?> = authRepository.currentUser.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = authRepository.getCurrentUserSync()
    )

    fun signInWithGoogle(activity: Activity, onResult: (Result<FirebaseUser>) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(activity)
            if (result.isSuccess) {
                // Ensure user doc exists in Firestore
                groupRepository.createUserIfNotExists()
            }
            onResult(result)
        }
    }

    fun ensureUserDocExists() {
        viewModelScope.launch {
            groupRepository.createUserIfNotExists()
        }
    }

    fun signOut() {
        authRepository.signOut()
    }
}
