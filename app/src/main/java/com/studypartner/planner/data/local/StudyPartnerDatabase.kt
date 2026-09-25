package com.studypartner.planner.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [EventEntity::class, TaskEntity::class], version = 2, exportSchema = false)
@TypeConverters(RoomTypeConverters::class)
abstract class StudyPartnerDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun taskDao(): TaskDao
}
