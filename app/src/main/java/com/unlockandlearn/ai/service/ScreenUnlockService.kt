package com.unlockandlearn.ai.service

import android.app.*
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.unlockandlearn.ai.MainActivity
import com.unlockandlearn.ai.R
import com.unlockandlearn.ai.receiver.ScreenUnlockReceiver

class ScreenUnlockService : Service() {

    private var screenUnlockReceiver: ScreenUnlockReceiver? = null
    private val CHANNEL_ID = "UnlockAndLearnServiceChannel"
    private val NOTIFICATION_ID = 4242

    override fun onCreate() {
        super.onCreate()
        Log.d("ScreenUnlockService", "Creating ScreenUnlockService...")
        createNotificationChannel()
        startForegroundService()
        registerUnlockReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ScreenUnlockService", "Service started onStartCommand")
        return START_STICKY
    }

    private fun registerUnlockReceiver() {
        if (screenUnlockReceiver == null) {
            screenUnlockReceiver = ScreenUnlockReceiver()
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            registerReceiver(screenUnlockReceiver, filter)
            Log.d("ScreenUnlockService", "Dynamically registered ScreenUnlockReceiver")
        }
    }

    private fun unregisterUnlockReceiver() {
        screenUnlockReceiver?.let {
            try {
                unregisterReceiver(it)
                Log.d("ScreenUnlockService", "Unregistered ScreenUnlockReceiver")
            } catch (e: Exception) {
                Log.e("ScreenUnlockService", "Failed to unregister receiver", e)
            }
            screenUnlockReceiver = null
        }
    }

    private fun startForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_desc))
            .setSmallIcon(android.R.drawable.ic_dialog_info) // System default icon for compatibility
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        Log.d("ScreenUnlockService", "Service started in foreground")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Unlock & Learn Foreground Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the application running in background to capture screen unlock events"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("ScreenUnlockService", "Destroying ScreenUnlockService...")
        unregisterUnlockReceiver()
    }
}
