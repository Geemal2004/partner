package com.studypartner.planner.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import com.studypartner.planner.data.model.Group
import com.studypartner.planner.data.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class GroupRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _currentGroupId = MutableStateFlow<String?>(null)
    val currentGroupId: StateFlow<String?> = _currentGroupId

    init {
        repositoryScope.launch {
            userPreferencesRepository.currentGroupIdFlow.collect { savedGroupId ->
                _currentGroupId.value = savedGroupId
            }
        }
    }

    fun setCurrentGroupId(groupId: String?) {
        _currentGroupId.value = groupId
        repositoryScope.launch {
            userPreferencesRepository.updateCurrentGroupId(groupId)
        }
    }

    suspend fun createUserIfNotExists() {
        val user = auth.currentUser ?: return
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                val userRef = firestore.collection("users").document(user.uid)
                val newUser = mapOf(
                    "uid" to user.uid,
                    "displayName" to (user.displayName ?: ""),
                    "email" to (user.email ?: ""),
                    "photoUrl" to (user.photoUrl?.toString() ?: "")
                )
                userRef.set(newUser, SetOptions.merge()).await()
            }
        } catch (_: Exception) {
            // Non-fatal exception handling for user doc creation
        }
    }

    suspend fun createGroup(name: String): Result<Group> {
        return try {
            val user = auth.currentUser ?: error("Not signed in")
            val groupId = UUID.randomUUID().toString()
            val inviteCode = generateInviteCode()

            val group = Group(
                groupId = groupId,
                name = name.trim(),
                memberIds = listOf(user.uid),
                inviteCode = inviteCode,
                createdBy = user.uid
            )

            withTimeout(QUERY_TIMEOUT_MS) {
                val batch = firestore.batch()
                val groupRef = firestore.collection("groups").document(groupId)
                val userRef = firestore.collection("users").document(user.uid)

                batch.set(groupRef, group)

                val userData = mapOf(
                    "uid" to user.uid,
                    "displayName" to (user.displayName ?: ""),
                    "email" to (user.email ?: ""),
                    "photoUrl" to (user.photoUrl?.toString() ?: ""),
                    "groupIds" to FieldValue.arrayUnion(groupId)
                )
                batch.set(userRef, userData, SetOptions.merge())

                batch.commit().await()
            }

            _currentGroupId.value = groupId
            userPreferencesRepository.updateCurrentGroupId(groupId)
            Result.success(group)
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("Group creation timed out. Please check your network connection.", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinGroupByCode(inviteCode: String): Result<Group> {
        val cleanCode = inviteCode.trim().uppercase()
        if (cleanCode.isBlank()) {
            return Result.failure(Exception("Invite code cannot be empty"))
        }

        return try {
            val user = auth.currentUser ?: error("Not signed in")

            val result = withTimeout(CALLABLE_TIMEOUT_MS) {
                functions
                    .getHttpsCallable("joinGroup")
                    .call(mapOf("inviteCode" to cleanCode))
                    .await()
            }

            val resultMap = result.data as? Map<*, *>
            val groupData = resultMap?.get("group") as? Map<*, *>

            if (groupData != null) {
                val groupId = groupData["groupId"] as? String ?: ""
                val name = groupData["name"] as? String ?: ""
                val code = groupData["inviteCode"] as? String ?: cleanCode
                val createdBy = groupData["createdBy"] as? String ?: ""
                val memberIds = (groupData["memberIds"] as? List<*>)?.mapNotNull { it as? String } ?: listOf(user.uid)

                val group = Group(
                    groupId = groupId,
                    name = name,
                    memberIds = memberIds,
                    inviteCode = code,
                    createdBy = createdBy
                )
                _currentGroupId.value = groupId
                userPreferencesRepository.updateCurrentGroupId(groupId)
                Result.success(group)
            } else {
                Result.failure(Exception("Failed to parse joined group details from response"))
            }
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("Joining group timed out. Please check your network connection.", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserGroups(): Result<List<Group>> {
        return try {
            val user = auth.currentUser ?: error("Not signed in")

            // Query groups directly where user is a member (1 roundtrip)
            val querySnapshot = withTimeout(QUERY_TIMEOUT_MS) {
                firestore.collection("groups")
                    .whereArrayContains("memberIds", user.uid)
                    .get().await()
            }

            val groups = querySnapshot.toObjects(Group::class.java)
            Result.success(groups)
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("Fetching groups timed out.", e))
        } catch (e: Exception) {
            // Fallback strategy if whereArrayContains is blocked by security rule
            try {
                val user = auth.currentUser ?: error("Not signed in")
                val userDoc = firestore.collection("users").document(user.uid).get().await()
                val userModel = userDoc.toObject(User::class.java) ?: return Result.success(emptyList())

                val groupIds = userModel.groupIds
                if (groupIds.isEmpty()) return Result.success(emptyList())

                val querySnapshot = firestore.collection("groups")
                    .whereIn("groupId", groupIds.take(30))
                    .get().await()

                val groups = querySnapshot.toObjects(Group::class.java)
                Result.success(groups)
            } catch (_: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun leaveGroup() {
        val user = auth.currentUser ?: return
        val groupId = currentGroupId.value ?: return
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                val batch = firestore.batch()
                val groupRef = firestore.collection("groups").document(groupId)
                val userRef = firestore.collection("users").document(user.uid)

                batch.update(groupRef, "memberIds", FieldValue.arrayRemove(user.uid))
                batch.update(userRef, "groupIds", FieldValue.arrayRemove(groupId))

                batch.commit().await()
            }
            _currentGroupId.value = null
            userPreferencesRepository.updateCurrentGroupId(null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to leave group", e)
        }
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    companion object {
        private const val TAG = "GroupRepository"
        private const val DEFAULT_TIMEOUT_MS = 5000L
        private const val QUERY_TIMEOUT_MS = 8000L
        private const val CALLABLE_TIMEOUT_MS = 10000L
    }
}
