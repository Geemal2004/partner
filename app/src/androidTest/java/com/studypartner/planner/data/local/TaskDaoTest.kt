package com.studypartner.planner.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskDaoTest {

    private lateinit var database: StudyPartnerDatabase
    private lateinit var taskDao: TaskDao

    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            StudyPartnerDatabase::class.java
        ).allowMainThreadQueries().build()
        taskDao = database.taskDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndGetTasks(): Unit = runBlocking {
        val task1 = TaskEntity(
            id = "t1",
            groupId = "g1",
            title = "Task 1",
            notes = "Note 1",
            dueDate = 1000L,
            assignedTo = listOf("u1"),
            isDone = false,
            doneBy = null,
            createdBy = "u1",
            updatedAt = 1000L
        )
        val task2 = TaskEntity(
            id = "t2",
            groupId = "g1",
            title = "Task 2",
            notes = "Note 2",
            dueDate = 2000L,
            assignedTo = listOf("u2"),
            isDone = true,
            doneBy = "u2",
            createdBy = "u1",
            updatedAt = 2000L
        )

        taskDao.insertTasks(listOf(task1, task2))

        val g1Tasks = taskDao.getTasks("g1").first()
        assertEquals(2, g1Tasks.size)
        assertEquals("Task 1", g1Tasks[0].title)
    }

    @Test
    fun deleteTask(): Unit = runBlocking {
        val task = TaskEntity(
            id = "t1",
            groupId = "g1",
            title = "Task 1",
            notes = "",
            dueDate = null,
            assignedTo = emptyList(),
            isDone = false,
            doneBy = null,
            createdBy = "u1",
            updatedAt = 1000L
        )

        taskDao.insertTask(task)
        taskDao.deleteTask("t1")

        val tasks = taskDao.getTasks("g1").first()
        assertTrue(tasks.isEmpty())
    }

    @Test
    fun getOpenOrOverdueTasks(): Unit = runBlocking {
        val openTask = TaskEntity("t1", "g1", "Open", "", 1000L, emptyList(), false, null, "u1", 1000L)
        val doneTask = TaskEntity("t2", "g1", "Done", "", 1000L, emptyList(), true, "u1", "u1", 1000L)
        val futureOpenTask = TaskEntity("t3", "g1", "Future", "", 20000L, emptyList(), false, null, "u1", 1000L)

        taskDao.insertTasks(listOf(openTask, doneTask, futureOpenTask))

        val openTasks = taskDao.getOpenOrOverdueTasks(endOfDay = 10000L)
        assertEquals(1, openTasks.size)
        assertEquals("t1", openTasks[0].id)
    }
}
