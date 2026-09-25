package com.studypartner.planner.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.studypartner.planner.data.local.EventEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    internal var sdkInt: Int = Build.VERSION.SDK_INT

    fun scheduleReminder(event: EventEntity, defaultOffsetMinutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val canExact = if (sdkInt >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        val reminderMinutes = event.reminderMinutes ?: defaultOffsetMinutes
        if (!ReminderTimeCalculator.isValidReminderOffset(reminderMinutes)) return

        val reminderTime = ReminderTimeCalculator.calculateReminderTime(event.startTime, reminderMinutes)
        if (ReminderTimeCalculator.isReminderInPast(reminderTime, System.currentTimeMillis())) return

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.studypartner.planner.ACTION_REMINDER"
            putExtra("EVENT_ID", event.id)
            putExtra("EVENT_TITLE", event.title)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )
        }
    }

    fun cancelReminder(eventId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.studypartner.planner.ACTION_REMINDER"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }
}
