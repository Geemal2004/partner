package com.studypartner.planner.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE groupId = :groupId ORDER BY dueDate ASC, title ASC")
    fun getTasks(groupId: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    @Query("DELETE FROM tasks")
    suspend fun clearTasks()

    @Query("SELECT * FROM tasks WHERE isDone = 0 AND (dueDate <= :endOfDay OR dueDate IS NULL) ORDER BY dueDate ASC")
    suspend fun getOpenOrOverdueTasks(endOfDay: Long): List<TaskEntity>
}
