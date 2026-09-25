package com.studypartner.planner.ui.navigation

import android.content.Intent
import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test

class DeepLinkReplayTest {

    private lateinit var deepLinkManager: DeepLinkManager

    @Before
    fun setUp() {
        deepLinkManager = DeepLinkManager()
    }

    @Test
    fun handleIntent_validJoinLink_updatesPendingDeepLinkRoute() {
        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns Intent.ACTION_VIEW
        every { intent.dataString } returns "studypartner://join/TEST123"

        deepLinkManager.handleIntent(intent)

        assertThat(deepLinkManager.pendingRoute.value).isEqualTo(JoinGroup("TEST123"))
        assertThat(deepLinkManager.rawPendingUri).isEqualTo("studypartner://join/TEST123")
    }

    @Test
    fun handleIntent_onNewIntent_updatesPendingRouteReplay() {
        // Initial state is null
        assertThat(deepLinkManager.pendingRoute.value).isNull()

        val intent1 = mockk<Intent>(relaxed = true)
        every { intent1.action } returns Intent.ACTION_VIEW
        every { intent1.dataString } returns "studypartner://event/evt-99"

        deepLinkManager.handleIntent(intent1)
        assertThat(deepLinkManager.pendingRoute.value).isEqualTo(EventDetail("evt-99"))

        // Simulate onNewIntent with a new task link while app is already running
        val intent2 = mockk<Intent>(relaxed = true)
        every { intent2.action } returns Intent.ACTION_VIEW
        every { intent2.dataString } returns "studypartner://task/tsk-88"

        deepLinkManager.handleIntent(intent2)
        assertThat(deepLinkManager.pendingRoute.value).isEqualTo(TaskDetail("tsk-88"))

        // Consume route
        val consumed = deepLinkManager.consumePendingRoute()
        assertThat(consumed).isEqualTo(TaskDetail("tsk-88"))
        assertThat(deepLinkManager.pendingRoute.value).isNull()
    }

    @Test
    fun statePersistence_saveAndRestorePendingDeepLink_survivesProcessDeath() {
        val intent = mockk<Intent>(relaxed = true)
        every { intent.action } returns Intent.ACTION_VIEW
        every { intent.dataString } returns "studypartner://join/RESTORE_CODE"

        deepLinkManager.handleIntent(intent)

        // Save state into bundle
        val bundleMap = mutableMapOf<String, String>()
        val bundle = mockk<Bundle>(relaxed = true)
        every { bundle.putString(any(), any()) } answers {
            bundleMap[firstArg()] = secondArg()
        }
        every { bundle.getString(any()) } answers {
            bundleMap[firstArg()]
        }

        deepLinkManager.saveInstanceState(bundle)

        assertThat(bundle.getString("pending_deep_link_uri")).isEqualTo("studypartner://join/RESTORE_CODE")

        // Create new instance simulating process resurrection
        val recreatedManager = DeepLinkManager()
        recreatedManager.restoreInstanceState(bundle)

        assertThat(recreatedManager.pendingRoute.value).isEqualTo(JoinGroup("RESTORE_CODE"))
        assertThat(recreatedManager.rawPendingUri).isEqualTo("studypartner://join/RESTORE_CODE")
    }
}
