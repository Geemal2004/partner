package com.studypartner.planner.ui.group

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun GroupSetupScreen(
    viewModel: GroupViewModel = hiltViewModel(),
    onGroupSelected: () -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var isJoining by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (isJoining) "Join a Group" else "Create a Group",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            if (isJoining) {
                OutlinedTextField(
                    value = inviteCode,
                    onValueChange = { 
                        inviteCode = it.uppercase()
                        if (error != null) viewModel.clearError()
                    },
                    label = { Text("Invite Code") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { 
                        groupName = it
                        if (error != null) viewModel.clearError()
                    },
                    label = { Text("Group Name") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    if (isJoining) {
                        if (inviteCode.isNotBlank()) {
                            viewModel.joinGroup(inviteCode) { success ->
                                if (success) onGroupSelected()
                            }
                        }
                    } else {
                        if (groupName.isNotBlank()) {
                            viewModel.createGroup(groupName) { success ->
                                if (success) onGroupSelected()
                            }
                        }
                    }
                },
                enabled = !isLoading && if (isJoining) inviteCode.isNotBlank() else groupName.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (isJoining) "Join" else "Create")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(
                onClick = { 
                    isJoining = !isJoining
                    viewModel.clearError()
                },
                enabled = !isLoading
            ) {
                Text(if (isJoining) "Want to create a group instead?" else "Have an invite code? Join instead")
            }
            
            if (error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
