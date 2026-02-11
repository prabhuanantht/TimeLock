package com.timelock.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.timelock.R
import com.timelock.core.SessionManager
import com.timelock.ui.MainActivity

class SessionTimerService : Service() {

    companion object {
        const val CHANNEL_ID = "session_timer_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_SESSION_ENDED = "com.timelock.ACTION_SESSION_ENDED"
    }

    private var countdownTimer: CountDownTimer? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!SessionManager.isSessionActive()) {
            stopSelf()
            return START_NOT_STICKY
        }

        val endTime = SessionManager.getSessionEndTime()
        val remainingTime = endTime - System.currentTimeMillis()

        if (remainingTime <= 0) {
            onSessionFinished()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification(remainingTime))
        startTimer(remainingTime)

        return START_STICKY
    }

    private fun startTimer(durationMillis: Long) {
        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Update notification every minute to save battery, or use a simpler update logic
                // For now, let's update every second for visibility, but typically minutes is better for background
                if (millisUntilFinished % 60000 < 1000) { // Update roughly every minute
                     updateNotification(millisUntilFinished)
                }
            }

            override fun onFinish() {
                onSessionFinished()
            }
        }.start()
    }

    private fun onSessionFinished() {
        SessionManager.endSession()
        sendBroadcast(Intent(ACTION_SESSION_ENDED))
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Session Timer",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun buildNotification(millisUntilFinished: Long): Notification {
        val minutesLeft = millisUntilFinished / 60000
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TimeLock Session Active")
            .setContentText("Time remaining: $minutesLeft minutes")
            .setSmallIcon(R.mipmap.ic_launcher_round) // verify this icon exists
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(millisUntilFinished: Long) {
        val notification = buildNotification(millisUntilFinished)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        countdownTimer?.cancel()
        super.onDestroy()
    }
}
