package com.example.first

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.first.ActivityTransitionData.TRANSITIONS_EXTRA
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionClient

class ActivityTransitionService : Service() {

    private lateinit var activityClient: ActivityRecognitionClient
    private lateinit var pendingIntent: PendingIntent
    private val notificationChannelId = "ActivityTransitionChannel"
    private val notificationId = 1

    override fun onCreate() {
        super.onCreate()

        activityClient = ActivityRecognition.getClient(this)

        initPendingIntent()

        startForegroundService(
            "Обнаружение активности", "Он обнаруживает активность в фоновом режиме"
        )
    }

    private fun startForegroundService(title: String, content: String) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            notificationChannelId,
            "Activity Transition Detection",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Служба, которая обнаруживает активность пользователя."
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification: Notification = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()

        startForeground(notificationId, notification)
    }

    private fun updateNotification(title: String, content: String) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun initPendingIntent() {
        val intent = Intent(this, ActivityTransitionsReceiver::class.java)
        pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.takeIf { it.action == TRANSITIONS_EXTRA }?.let { i ->
            val info = i.getStringExtra(TRANSITIONS_EXTRA)
            info?.let {
                updateNotification("Обнаружение изменений активности", "Текущая активность: $it")
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
