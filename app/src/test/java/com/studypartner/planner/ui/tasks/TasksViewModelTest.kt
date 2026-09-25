package com.studypartner.planner.ui.tasks

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseUser
import com.studypartner.planner.data.model.TaskDto
import com.studypartner.planner.data.repository.AuthRepository
import com.studypartner.planner.data.repository.GroupRepository
import com.studypartner.planner.data.repository.TaskRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val taskRepository: TaskRepository = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val groupRepository: GroupRepository = mockk(relaxed = true)

    private val currentGroupIdFlow = MutableStateFlow<String?>("group-1")
    private val mockUser: FirebaseUser = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { groupRepository.currentGroupId } returns currentGroupIdFlow
        every { authRepository.getCurrentUserSync() } returns mockUser
        every { mockUser.uid } returns "user-me"

        val sampleTasks = listOf(
            TaskDto(
                id = "1",
                groupId = "group-1",
                title = "My open task",
                notes = "",
                dueDate = System.currentTimeMillis() + 86400000,
                assignedTo = listOf("user-me"),
                isDone = false,
                doneBy = null,
                createdBy = "user-me",
                updatedAt = System.currentTimeMillis()
            ),
            TaskDto(
                id = "2",
                groupId = "group-1",
                title = "Partner open task",
                notes = "",
                dueDate = System.currentTimeMillis() + 86400000,
                assignedTo = listOf("user-partner"),
                isDone = false,
                doneBy = null,
                createdBy = "user-partner",
                updatedAt = System.currentTimeMillis()
            ),
            TaskDto(
                id = "3",
                groupId = "group-1",
                title = "Completed task",
                notes = "",
                dueDate = null,
                assignedTo = listOf("user-me"),
                isDone = true,
                doneBy = "user-me",
                createdBy = "user-me",
                updatedAt = System.currentTimeMillis()
            )
        )

        every { taskRepository.getTasks("group-1") } returns flowOf(sampleTasks)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun setFilter_updatesFilterState() = runTest {
        val viewModel = TasksViewModel(taskRepository, authRepository, groupRepository)

        viewModel.filter.test {
            assertThat(awaitItem()).isEqualTo(TaskFilter.ALL)

            viewModel.setFilter(TaskFilter.MINE)
            assertThat(awaitItem()).isEqualTo(TaskFilter.MINE)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun filteredTasks_filterAll_returnsUncompletedTasks() = runTest {
        val viewModel = TasksViewModel(taskRepository, authRepository, groupRepository)

        viewModel.filteredTasks.test {
            assertThat(awaitItem()).isEmpty()

            val filtered = awaitItem()
            assertThat(filtered.map { it.id }).containsExactly("1", "2")

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun filteredTasks_filterMine_returnsOnlyMyTasks() = runTest {
        val viewModel = TasksViewModel(taskRepository, authRepository, groupRepository)

        viewModel.filteredTasks.test {
            assertThat(awaitItem()).isEmpty()
            val initialFiltered = awaitItem()
            assertThat(initialFiltered.map { it.id }).containsExactly("1", "2")

            viewModel.setFilter(TaskFilter.MINE)

            val mineFiltered = awaitItem()
            assertThat(mineFiltered.map { it.id }).containsExactly("1")

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun filteredTasks_filterCompleted_returnsCompletedTasks() = runTest {
        val viewModel = TasksViewModel(taskRepository, authRepository, groupRepository)

        viewModel.filteredTasks.test {
            assertThat(awaitItem()).isEmpty()
            val initialFiltered = awaitItem()
            assertThat(initialFiltered.map { it.id }).containsExactly("1", "2")

            viewModel.setFilter(TaskFilter.COMPLETED)

            val completedFiltered = awaitItem()
            assertThat(completedFiltered.map { it.id }).containsExactly("3")

            cancelAndIgnoreRemainingEvents()
        }
    }
}
