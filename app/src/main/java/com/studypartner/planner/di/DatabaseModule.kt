package com.studypartner.planner.di

import android.content.Context
import androidx.room.Room
import com.studypartner.planner.data.local.EventDao
import com.studypartner.planner.data.local.StudyPartnerDatabase
import com.studypartner.planner.data.local.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): StudyPartnerDatabase {
        return Room.databaseBuilder(
            context,
            StudyPartnerDatabase::class.java,
            "study_partner_db"
        ).build()
    }

    @Provides
    fun provideEventDao(database: StudyPartnerDatabase): EventDao {
        return database.eventDao()
    }

    @Provides
    fun provideTaskDao(database: StudyPartnerDatabase): TaskDao {
        return database.taskDao()
    }
}
