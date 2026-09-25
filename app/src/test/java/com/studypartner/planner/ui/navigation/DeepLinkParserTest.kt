package com.studypartner.planner.ui.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DeepLinkParserTest {

    @Test
    fun parse_joinGroupDeepLink_returnsJoinGroupRoute() {
        val uri = "studypartner://join/GROUP123"
        val route = DeepLinkParser.parse(uri)

        assertThat(route).isInstanceOf(JoinGroup::class.java)
        assertThat((route as JoinGroup).code).isEqualTo("GROUP123")
    }

    @Test
    fun parse_eventDetailDeepLink_returnsEventDetailRoute() {
        val uri = "studypartner://event/EVENT_ABC_123"
        val route = DeepLinkParser.parse(uri)

        assertThat(route).isInstanceOf(EventDetail::class.java)
        assertThat((route as EventDetail).id).isEqualTo("EVENT_ABC_123")
    }

    @Test
    fun parse_taskDetailDeepLink_returnsTaskDetailRoute() {
        val uri = "studypartner://task/TASK_XYZ_789"
        val route = DeepLinkParser.parse(uri)

        assertThat(route).isInstanceOf(TaskDetail::class.java)
        assertThat((route as TaskDetail).id).isEqualTo("TASK_XYZ_789")
    }

    @Test
    fun parse_invalidScheme_returnsNull() {
        val uri = "https://join/GROUP123"
        val route = DeepLinkParser.parse(uri)

        assertThat(route).isNull()
    }

    @Test
    fun parse_nullOrBlankUri_returnsNull() {
        assertThat(DeepLinkParser.parse(null)).isNull()
        assertThat(DeepLinkParser.parse("   ")).isNull()
    }

    @Test
    fun parse_unknownHost_returnsNull() {
        val uri = "studypartner://profile/123"
        val route = DeepLinkParser.parse(uri)

        assertThat(route).isNull()
    }
}
