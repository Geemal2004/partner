package com.studypartner.planner

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class StudyPartnerApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            
            val eventChannel = NotificationChannel(
                "event_reminders",
                "Event Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            
            val summaryChannel = NotificationChannel(
                "daily_summary",
                "Daily Summary",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            
            val updatesChannel = NotificationChannel(
                "partner_updates",
                "Partner Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            
            notificationManager.createNotificationChannels(listOf(eventChannel, summaryChannel, updatesChannel))
        }
    }
}
