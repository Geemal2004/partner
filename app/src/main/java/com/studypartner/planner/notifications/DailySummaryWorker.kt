package com.studypartner.planner.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.studypartner.planner.MainActivity
import com.studypartner.planner.R
import com.studypartner.planner.data.local.EventDao
import com.studypartner.planner.data.local.TaskDao
import com.studypartner.planner.data.local.TaskEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@HiltWorker
class DailySummaryWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val eventDao: EventDao,
    private val taskDao: TaskDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val startOfDay = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfDay = today.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()

        val todaysEvents = eventDao.getEventsForDay(startOfDay, endOfDay)
        val dueTasks = taskDao.getDueOrOverdueTasks(endOfDay)
        val recentUndatedTasks = taskDao.getRecentUndatedTasks(limit = 3)

        if (todaysEvents.isEmpty() && dueTasks.isEmpty() && recentUndatedTasks.isEmpty()) {
            return@withContext Result.success()
        }

        sendNotification(todaysEvents.size, dueTasks.size, recentUndatedTasks)
        Result.success()
    }

    private fun sendNotification(eventsCount: Int, dueTasksCount: Int, undatedTasks: List<TaskEntity>) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "daily_summary"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Daily summary",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(DailySummaryBuilder.buildSummaryTitle(eventsCount, dueTasksCount, undatedTasks.size))

        val lines = DailySummaryBuilder.buildSummaryLines(
            eventsCount = eventsCount,
            tasksCount = dueTasksCount,
            undatedTasksCount = undatedTasks.size,
            undatedTaskTitles = undatedTasks.map { it.title }
        )
        for (line in lines) {
            inboxStyle.addLine(line)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("StudyPartner Daily Summary")
            .setContentText("You have upcoming events and tasks today.")
            .setStyle(inboxStyle)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(DAILY_SUMMARY_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val DAILY_SUMMARY_NOTIFICATION_ID = 1001
    }
}
