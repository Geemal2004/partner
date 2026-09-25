package com.studypartner.planner.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.studypartner.planner.data.local.EventDao
import com.studypartner.planner.data.repository.UserPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var eventDao: EventDao

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_TIMEZONE_CHANGED) {
            CoroutineScope(Dispatchers.IO).launch {
                val prefs = userPreferencesRepository.userPreferencesFlow.first()
                val now = System.currentTimeMillis()
                val startOfDay = now // We only need future events
                val endOfDay = now + 365L * 24 * 60 * 60 * 1000 // Look ahead 1 year
                
                val events = eventDao.getEventsForDay(startOfDay, endOfDay)
                events.forEach { event ->
                    reminderScheduler.scheduleReminder(event, prefs.defaultReminderOffset)
                }
            }
        }
    }
}
