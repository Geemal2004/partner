package com.studypartner.planner.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavKey
import androidx.window.core.layout.WindowSizeClass
import com.studypartner.planner.R
import com.studypartner.planner.ui.navigation.AppNavDisplay
import com.studypartner.planner.ui.navigation.AppNavigator
import com.studypartner.planner.ui.navigation.Calendar
import com.studypartner.planner.ui.navigation.GroupSetup
import com.studypartner.planner.ui.navigation.Login
import com.studypartner.planner.ui.navigation.Settings
import com.studypartner.planner.ui.navigation.Tasks
import com.studypartner.planner.ui.navigation.rememberAppNavigator
import com.studypartner.planner.ui.theme.StudyPartnerTheme

enum class TopLevelDestination(
    val route: NavKey,
    val icon: ImageVector,
    val labelRes: Int,
) {
    CALENDAR(Calendar, Icons.Rounded.CalendarMonth, R.string.nav_calendar),
    TASKS(Tasks, Icons.Rounded.CheckCircle, R.string.nav_tasks),
    SETTINGS(Settings, Icons.Rounded.Settings, R.string.nav_settings),
}

@Composable
fun AppNavigationSuiteScaffold(
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
) {
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val layoutType = with(adaptiveInfo) {
        if (windowSizeClass.isWidthAtLeastBreakpoint(
                WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
            )
        ) {
            NavigationSuiteType.NavigationDrawer
        } else {
            NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)
        }
    }

    val isAuthOrSetup = navigator.currentDestination == Login || navigator.currentDestination == GroupSetup

    if (isAuthOrSetup) {
        AppNavDisplay(navigator = navigator, modifier = modifier.fillMaxSize())
    } else {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                TopLevelDestination.entries.forEach { destination ->
                    item(
                        selected = navigator.selectedTopLevel == destination.route,
                        onClick = { navigator.switchTopLevel(destination.route) },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = stringResource(destination.labelRes),
                            )
                        },
                        label = { Text(text = stringResource(destination.labelRes)) },
                    )
                }
            },
            layoutType = layoutType,
            modifier = modifier.fillMaxSize(),
        ) {
            AppNavDisplay(navigator = navigator)
        }
    }
}

@Preview(name = "Compact", widthDp = 360, heightDp = 740)
@Preview(name = "Medium", widthDp = 700, heightDp = 900)
@Preview(name = "Expanded", widthDp = 1200, heightDp = 800)
@Composable
private fun AppNavigationSuiteScaffoldPreview() {
    StudyPartnerTheme {
        AppNavigationSuiteScaffold(navigator = rememberAppNavigator())
    }
}
