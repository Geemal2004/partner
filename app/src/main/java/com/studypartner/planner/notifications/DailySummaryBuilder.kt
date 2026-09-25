package com.studypartner.planner.notifications

object DailySummaryBuilder {

    fun buildSummaryTitle(eventsCount: Int, tasksCount: Int, undatedTasksCount: Int = 0): String {
        val totalTasks = tasksCount + undatedTasksCount
        return when {
            eventsCount > 0 && totalTasks > 0 -> "StudyPartner Daily Summary ($eventsCount events, $totalTasks tasks)"
            eventsCount > 0 -> "StudyPartner Daily Summary ($eventsCount events)"
            totalTasks > 0 -> "StudyPartner Daily Summary ($totalTasks tasks)"
            else -> "StudyPartner Daily Summary"
        }
    }

    fun buildSummaryLines(
        eventsCount: Int,
        tasksCount: Int,
        undatedTasksCount: Int = 0,
        undatedTaskTitles: List<String> = emptyList()
    ): List<String> {
        val lines = mutableListOf<String>()
        if (eventsCount > 0) {
            lines.add("You have $eventsCount event(s) today.")
        }
        if (tasksCount > 0) {
            lines.add("You have $tasksCount open task(s).")
        }
        if (undatedTasksCount > 0) {
            if (undatedTaskTitles.isNotEmpty()) {
                val preview = undatedTaskTitles.take(3).joinToString(", ")
                lines.add("Ongoing Tasks: $preview" + if (undatedTasksCount > 3) " (+${undatedTasksCount - 3} more)" else "")
            } else {
                lines.add("Ongoing Tasks: $undatedTasksCount ongoing task(s).")
            }
        }
        return lines
    }

    fun buildSummaryText(eventsCount: Int, tasksCount: Int, undatedTasksCount: Int = 0): String {
        val lines = buildSummaryLines(eventsCount, tasksCount, undatedTasksCount)
        return if (lines.isEmpty()) {
            "No upcoming events or tasks for today."
        } else {
            lines.joinToString(" ")
        }
    }
}
