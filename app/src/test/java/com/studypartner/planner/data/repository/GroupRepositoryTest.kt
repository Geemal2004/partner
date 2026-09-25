package com.studypartner.planner.data.repository

import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GroupRepositoryTest {

    private val firestore: FirebaseFirestore = mockk(relaxed = true)
    private val auth: FirebaseAuth = mockk(relaxed = true)
    private val functions: FirebaseFunctions = mockk(relaxed = true)
    private val userPreferencesRepository: UserPreferencesRepository = mockk(relaxed = true)

    private val currentGroupIdFlow = MutableStateFlow<String?>("initial-group-id")

    private lateinit var groupRepository: GroupRepository

    @Before
    fun setUp() {
        every { userPreferencesRepository.currentGroupIdFlow } returns currentGroupIdFlow
        groupRepository = GroupRepository(
            firestore = firestore,
            auth = auth,
            functions = functions,
            userPreferencesRepository = userPreferencesRepository
        )
    }

    @Test
    fun init_loadsCurrentGroupIdFromUserPreferences() = runTest {
        advanceUntilIdle()
        assertThat(groupRepository.currentGroupId.value).isEqualTo("initial-group-id")
    }

    @Test
    fun setCurrentGroupId_updatesStateAndPersistsToDataStore() = runTest {
        groupRepository.setCurrentGroupId("new-group-456")
        advanceUntilIdle()

        assertThat(groupRepository.currentGroupId.value).isEqualTo("new-group-456")
        coVerify(exactly = 1) { userPreferencesRepository.updateCurrentGroupId("new-group-456") }
    }
}
