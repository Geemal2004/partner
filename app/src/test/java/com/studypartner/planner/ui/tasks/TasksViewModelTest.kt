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
    private val currentUserFlow = MutableStateFlow<FirebaseUser?>(null)
    private val mockUser: FirebaseUser = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { groupRepository.currentGroupId } returns currentGroupIdFlow
        every { authRepository.getCurrentUserSync() } returns mockUser
        every { authRepository.currentUser } returns currentUserFlow
        every { mockUser.uid } returns "user-me"
        currentUserFlow.value = mockUser

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
        every { taskRepository.startRealtimeSync(any()) } returns flowOf(emptyList())
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

    @Test
    fun filteredTasks_reactsToCurrentUserChange() = runTest {
        val viewModel = TasksViewModel(taskRepository, authRepository, groupRepository)
        viewModel.setFilter(TaskFilter.MINE)

        viewModel.filteredTasks.test {
            assertThat(awaitItem()).isEmpty()
            val initialFiltered = awaitItem()
            // With user-me, task 1 is mine
            assertThat(initialFiltered.map { it.id }).containsExactly("1")

            // Now current user switches to user-partner
            val partnerUser: FirebaseUser = mockk(relaxed = true)
            every { partnerUser.uid } returns "user-partner"
            every { authRepository.getCurrentUserSync() } returns partnerUser
            currentUserFlow.value = partnerUser

            val updatedFiltered = awaitItem()
            // With user-partner, task 2 is mine
            assertThat(updatedFiltered.map { it.id }).containsExactly("2")

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun realtimeSync_whenErrorOccurs_setsSyncErrorWithoutCrashing() = runTest {
        val errorFlow = kotlinx.coroutines.flow.flow<List<TaskDto>> {
            throw RuntimeException("Firestore task stream disconnected")
        }
        every { taskRepository.startRealtimeSync("group-1") } returns errorFlow

        val viewModel = TasksViewModel(taskRepository, authRepository, groupRepository)
        testScheduler.advanceUntilIdle()

        assertThat(viewModel.syncError.value).contains("Firestore task stream disconnected")

        // ViewModel scope is not dead, filter update works
        viewModel.setFilter(TaskFilter.COMPLETED)
        assertThat(viewModel.filter.value).isEqualTo(TaskFilter.COMPLETED)
    }

    @Test
    fun retrySync_clearsSyncErrorAndRestartsSync() = runTest {
        val errorFlow = kotlinx.coroutines.flow.flow<List<TaskDto>> {
            throw RuntimeException("Network error")
        }
        every { taskRepository.startRealtimeSync("group-1") } returns errorFlow

        val viewModel = TasksViewModel(taskRepository, authRepository, groupRepository)
        testScheduler.advanceUntilIdle()

        assertThat(viewModel.syncError.value).isNotNull()

        // Now network recovers
        every { taskRepository.startRealtimeSync("group-1") } returns flowOf(emptyList())

        viewModel.retrySync()
        testScheduler.advanceUntilIdle()

        assertThat(viewModel.syncError.value).isNull()
        io.mockk.verify(atLeast = 2) { taskRepository.startRealtimeSync("group-1") }
        io.mockk.coVerify(atLeast = 1) { taskRepository.syncTasks("group-1") }
    }
}
