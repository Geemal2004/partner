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
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

@HiltWorker
class DailySummaryWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val eventDao: EventDao,
    private val taskDao: TaskDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val now = Calendar.getInstance()
        val startOfDay = now.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfDay = now.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val todaysEvents = eventDao.getEventsForDay(startOfDay, endOfDay)
        val openTasks = taskDao.getOpenOrOverdueTasks(endOfDay)

        if (todaysEvents.isEmpty() && openTasks.isEmpty()) {
            return@withContext Result.success()
        }

        sendNotification(todaysEvents.size, openTasks.size)
        Result.success()
    }

    private fun sendNotification(eventsCount: Int, tasksCount: Int) {
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
            .setBigContentTitle(DailySummaryBuilder.buildSummaryTitle(eventsCount, tasksCount))

        val lines = DailySummaryBuilder.buildSummaryLines(eventsCount, tasksCount)
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

        notificationManager.notify(1001, notification)
    }
}
