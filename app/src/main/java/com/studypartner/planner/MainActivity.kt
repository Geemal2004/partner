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
import com.studypartner.planner.ui.navigation.Calendar
import com.studypartner.planner.ui.navigation.DeepLinkManager
import com.studypartner.planner.ui.navigation.EventDetail
import com.studypartner.planner.ui.navigation.GroupSetup
import com.studypartner.planner.ui.navigation.JoinGroup
import com.studypartner.planner.ui.navigation.Login
import com.studypartner.planner.ui.navigation.TaskDetail
import com.studypartner.planner.ui.navigation.rememberAppNavigator
import com.studypartner.planner.ui.theme.StudyPartnerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val groupViewModel: GroupViewModel by viewModels()

    internal val deepLinkManager = DeepLinkManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        deepLinkManager.restoreInstanceState(savedInstanceState)
        deepLinkManager.handleIntent(intent)

        setContent {
            StudyPartnerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val navigator = rememberAppNavigator()
                    val currentUser by authViewModel.currentUser.collectAsState()
                    val groups by groupViewModel.groups.collectAsState()
                    val hasLoadedGroups by groupViewModel.hasLoadedGroups.collectAsState()
                    val pendingRoute by deepLinkManager.pendingRoute.collectAsState()

                    LaunchedEffect(currentUser) {
                        if (currentUser == null) {
                            navigator.resetTo(Login)
                        } else {
                            authViewModel.ensureUserDocExists()
                            groupViewModel.fetchGroups()
                        }
                    }

                    LaunchedEffect(currentUser, pendingRoute, groups, hasLoadedGroups) {
                        if (currentUser != null) {
                            // 1. Consume pending deep link first
                            val routeToNavigate = deepLinkManager.consumePendingRoute()
                            if (routeToNavigate != null) {
                                navigator.navigate(routeToNavigate)
                                return@LaunchedEffect
                            }

                            // 2. Normal destination routing once groups have loaded
                            if (hasLoadedGroups) {
                                val current = navigator.currentDestination
                                if (groups.isNotEmpty()) {
                                    if (current == Login || current == GroupSetup) {
                                        navigator.resetTo(Calendar)
                                    }
                                } else {
                                    val isDeepLinkedOrSafe = current == GroupSetup ||
                                        current == Login ||
                                        current is JoinGroup ||
                                        current is EventDetail ||
                                        current is TaskDetail
                                    if (!isDeepLinkedOrSafe) {
                                        navigator.resetTo(GroupSetup)
                                    }
                                }
                            }
                        }
                    }

                    AppNavigationSuiteScaffold(navigator = navigator)
                }
            }
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        deepLinkManager.saveInstanceState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkManager.handleIntent(intent)
    }
}
