package com.studypartner.planner

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.studypartner.planner.ui.auth.AuthViewModel
import com.studypartner.planner.ui.components.AppNavigationSuiteScaffold
import com.studypartner.planner.ui.group.GroupViewModel
import com.studypartner.planner.ui.navigation.GroupSetup
import com.studypartner.planner.ui.navigation.JoinGroup
import com.studypartner.planner.ui.navigation.EventDetail
import com.studypartner.planner.ui.navigation.TaskDetail
import com.studypartner.planner.ui.navigation.Login
import com.studypartner.planner.ui.navigation.Calendar
import com.studypartner.planner.ui.navigation.DeepLinkParser
import com.studypartner.planner.ui.navigation.rememberAppNavigator
import com.studypartner.planner.ui.theme.StudyPartnerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val groupViewModel: GroupViewModel by viewModels()

    private var pendingInviteCode: String? = null
    private var pendingEventId: String? = null
    private var pendingTaskId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            StudyPartnerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val navigator = rememberAppNavigator()
                    val currentUser by authViewModel.currentUser.collectAsState()
                    val groups by groupViewModel.groups.collectAsState()

                    LaunchedEffect(currentUser) {
                        if (currentUser == null) {
                            navigator.resetTo(Login)
                        } else {
                            authViewModel.ensureUserDocExists()
                            groupViewModel.fetchGroups()
                        }
                    }

                    LaunchedEffect(currentUser, groups) {
                        if (currentUser != null) {
                            if (pendingInviteCode != null) {
                                val code = pendingInviteCode!!
                                pendingInviteCode = null
                                navigator.navigate(JoinGroup(code))
                            } else if (pendingEventId != null) {
                                val eventId = pendingEventId!!
                                pendingEventId = null
                                navigator.navigate(EventDetail(eventId))
                            } else if (pendingTaskId != null) {
                                val taskId = pendingTaskId!!
                                pendingTaskId = null
                                navigator.navigate(TaskDetail(taskId))
                            } else if (groups.isNotEmpty()) {
                                if (navigator.currentDestination == Login || navigator.currentDestination == GroupSetup) {
                                    navigator.resetTo(Calendar)
                                }
                            } else if (groups.isEmpty() && navigator.currentDestination != GroupSetup && navigator.currentDestination != Login && navigator.currentDestination !is JoinGroup) {
                                navigator.resetTo(GroupSetup)
                            }
                        }
                    }

                    AppNavigationSuiteScaffold(navigator = navigator)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            val dataString = intent.dataString ?: intent.data?.toString()
            when (val route = DeepLinkParser.parse(dataString)) {
                is JoinGroup -> pendingInviteCode = route.code
                is EventDetail -> pendingEventId = route.id
                is TaskDetail -> pendingTaskId = route.id
                else -> {}
            }
        }
    }
}
