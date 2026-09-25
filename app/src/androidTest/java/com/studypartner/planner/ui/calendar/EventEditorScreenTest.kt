package com.studypartner.planner.ui.calendar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventEditorScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun eventEditorContent_inputTitleAndSave_triggersSaveCallback() {
        var savedTitle = ""
        var savedDescription = ""
        var savedLocation = ""

        composeTestRule.setContent {
            EventEditorContent(
                isEditing = false,
                initialEvent = null,
                onSave = { title, description, location, _, _ ->
                    savedTitle = title
                    savedDescription = description
                    savedLocation = location
                },
                onNavigateBack = {}
            )
        }

        // Verify TopBar title
        composeTestRule.onNodeWithText("New Event").assertIsDisplayed()

        // Input title, description, location
        composeTestRule.onNodeWithText("Title").performTextInput("Group Study Session")
        composeTestRule.onNodeWithText("Description").performTextInput("Room 302 Library")
        composeTestRule.onNodeWithText("Location").performTextInput("Campus Library")

        // Click Save
        composeTestRule.onNodeWithText("Save").performClick()

        // Verify values received in callback
        assertEquals("Group Study Session", savedTitle)
        assertEquals("Room 302 Library", savedDescription)
        assertEquals("Campus Library", savedLocation)
    }

    @Test
    fun eventEditorContent_allDayCheckbox_canBeToggled() {
        var savedAllDay = false

        composeTestRule.setContent {
            EventEditorContent(
                isEditing = false,
                initialEvent = null,
                onSave = { _, _, _, _, allDay ->
                    savedAllDay = allDay
                },
                onNavigateBack = {}
            )
        }

        // Toggle All Day checkbox
        composeTestRule.onNodeWithText("All Day Event").performClick()

        // Click Save
        composeTestRule.onNodeWithText("Save").performClick()

        assertEquals(true, savedAllDay)
    }
}
