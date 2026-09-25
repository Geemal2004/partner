package com.studypartner.planner.ui.tasks

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.studypartner.planner.data.model.TaskDto
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TasksScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun taskListPane_displaysTasksAndFilters() {
        val sampleTasks = listOf(
            TaskDto(
                id = "t1",
                groupId = "g1",
                title = "Study Calculus",
                notes = "Chapter 4 exercises",
                dueDate = null,
                assignedTo = listOf("u1"),
                isDone = false,
                doneBy = null,
                createdBy = "u1",
                updatedAt = System.currentTimeMillis()
            )
        )

        var selectedFilter = TaskFilter.ALL

        composeTestRule.setContent {
            TaskListPane(
                tasks = sampleTasks,
                currentFilter = selectedFilter,
                onFilterChange = { selectedFilter = it },
                onTaskClick = {},
                onTaskCheck = {},
                onTaskDelete = {},
                onAddClick = {}
            )
        }

        // Verify task title is displayed
        composeTestRule.onNodeWithText("Study Calculus").assertIsDisplayed()

        // Verify filter tabs are displayed
        composeTestRule.onNodeWithText("ALL").assertIsDisplayed()
        composeTestRule.onNodeWithText("MINE").assertIsDisplayed()
        composeTestRule.onNodeWithText("COMPLETED").assertIsDisplayed()

        // Perform click on MINE filter tab
        composeTestRule.onNodeWithText("MINE").performClick()
    }

    @Test
    fun taskListPane_emptyList_displaysNoTasksMessage() {
        composeTestRule.setContent {
            TaskListPane(
                tasks = emptyList(),
                currentFilter = TaskFilter.ALL,
                onFilterChange = {},
                onTaskClick = {},
                onTaskCheck = {},
                onTaskDelete = {},
                onAddClick = {}
            )
        }

        composeTestRule.onNodeWithText("No tasks found.").assertIsDisplayed()
    }
}
