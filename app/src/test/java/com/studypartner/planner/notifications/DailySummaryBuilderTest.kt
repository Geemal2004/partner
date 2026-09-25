package com.studypartner.planner.notifications

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DailySummaryBuilderTest {

    @Test
    fun buildSummaryTitle_eventsAndTasks_returnsCombinedTitle() {
        val title = DailySummaryBuilder.buildSummaryTitle(eventsCount = 3, tasksCount = 5)
        assertThat(title).contains("3 events")
        assertThat(title).contains("5 tasks")
    }

    @Test
    fun buildSummaryTitle_eventsOnly_returnsEventsTitle() {
        val title = DailySummaryBuilder.buildSummaryTitle(eventsCount = 2, tasksCount = 0)
        assertThat(title).contains("2 events")
        assertThat(title).doesNotContain("tasks")
    }

    @Test
    fun buildSummaryTitle_tasksOnly_returnsTasksTitle() {
        val title = DailySummaryBuilder.buildSummaryTitle(eventsCount = 0, tasksCount = 4)
        assertThat(title).contains("4 tasks")
        assertThat(title).doesNotContain("events")
    }

    @Test
    fun buildSummaryLines_bothEventsAndTasks_createsTwoLines() {
        val lines = DailySummaryBuilder.buildSummaryLines(eventsCount = 2, tasksCount = 3)

        assertThat(lines).hasSize(2)
        assertThat(lines[0]).isEqualTo("You have 2 event(s) today.")
        assertThat(lines[1]).isEqualTo("You have 3 open task(s).")
    }

    @Test
    fun buildSummaryLines_empty_returnsEmptyList() {
        val lines = DailySummaryBuilder.buildSummaryLines(eventsCount = 0, tasksCount = 0)
        assertThat(lines).isEmpty()
    }

    @Test
    fun buildSummaryText_empty_returnsDefaultNoUpcomingMessage() {
        val text = DailySummaryBuilder.buildSummaryText(eventsCount = 0, tasksCount = 0)
        assertThat(text).isEqualTo("No upcoming events or tasks for today.")
    }

    @Test
    fun buildSummaryText_withItems_returnsJoinedLines() {
        val text = DailySummaryBuilder.buildSummaryText(eventsCount = 1, tasksCount = 2)
        assertThat(text).contains("You have 1 event(s) today.")
        assertThat(text).contains("You have 2 open task(s).")
    }
}
