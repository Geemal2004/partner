package com.studypartner.planner.data.repository

import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.studypartner.planner.data.local.TaskDao
import com.studypartner.planner.data.local.toDto
import com.studypartner.planner.data.local.toEntity
import com.studypartner.planner.data.model.TaskDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val taskDao: TaskDao
) {

    fun getTasks(groupId: String): Flow<List<TaskDto>> {
        return taskDao.getTasks(groupId).map { entities ->
            entities.map { it.toDto() }
        }
    }

    fun startRealtimeSync(groupId: String): Flow<List<TaskDto>> = callbackFlow {
        if (groupId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = firestore.collection("groups").document(groupId)
            .collection("tasks")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    launch(Dispatchers.IO) {
                        try {
                            for (change in snapshot.documentChanges) {
                                when (change.type) {
                                    DocumentChange.Type.ADDED,
                                    DocumentChange.Type.MODIFIED -> {
                                        val dto = change.document.toObject(TaskDto::class.java)
                                        dto?.let { taskDao.insertTask(it.toEntity()) }
                                    }
                                    DocumentChange.Type.REMOVED -> {
                                        taskDao.deleteTask(change.document.id)
                                    }
                                }
                            }
                            val remoteTasks = snapshot.documents.mapNotNull { it.toObject(TaskDto::class.java) }
                            trySend(remoteTasks)
                        } catch (e: Exception) {
                            close(e)
                        }
                    }
                }
            }

        awaitClose {
            registration.remove()
        }
    }

    suspend fun syncTasks(groupId: String) {
        if (groupId.isEmpty()) return
        val snapshot = firestore.collection("groups").document(groupId)
            .collection("tasks")
            .get()
            .await()

        val remoteTasks = snapshot.documents.mapNotNull { it.toObject(TaskDto::class.java) }
        taskDao.insertTasks(remoteTasks.map { it.toEntity() })
    }

    suspend fun saveTask(task: TaskDto) {
        // Save locally
        taskDao.insertTask(task.toEntity())

        // Sync remote
        if (task.groupId.isNotEmpty()) {
            firestore.collection("groups").document(task.groupId)
                .collection("tasks").document(task.id)
                .set(task, SetOptions.merge())
                .await()
        }
    }

    suspend fun deleteTask(task: TaskDto) {
        // Delete locally
        taskDao.deleteTask(task.id)

        // Sync remote
        if (task.groupId.isNotEmpty()) {
            firestore.collection("groups").document(task.groupId)
                .collection("tasks").document(task.id)
                .delete()
                .await()
        }
    }
}
