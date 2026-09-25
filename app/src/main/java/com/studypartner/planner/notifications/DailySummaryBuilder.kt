package com.studypartner.planner.notifications

object DailySummaryBuilder {

    fun buildSummaryTitle(eventsCount: Int, tasksCount: Int): String {
        return when {
            eventsCount > 0 && tasksCount > 0 -> "StudyPartner Daily Summary ($eventsCount events, $tasksCount tasks)"
            eventsCount > 0 -> "StudyPartner Daily Summary ($eventsCount events)"
            tasksCount > 0 -> "StudyPartner Daily Summary ($tasksCount tasks)"
            else -> "StudyPartner Daily Summary"
        }
    }

    fun buildSummaryLines(eventsCount: Int, tasksCount: Int): List<String> {
        val lines = mutableListOf<String>()
        if (eventsCount > 0) {
            lines.add("You have $eventsCount event(s) today.")
        }
        if (tasksCount > 0) {
            lines.add("You have $tasksCount open task(s).")
        }
        return lines
    }

    fun buildSummaryText(eventsCount: Int, tasksCount: Int): String {
        val lines = buildSummaryLines(eventsCount, tasksCount)
        return if (lines.isEmpty()) {
            "No upcoming events or tasks for today."
        } else {
            lines.joinToString(" ")
        }
    }
}
