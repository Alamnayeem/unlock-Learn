package com.unlockandlearn.ai.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.unlockandlearn.ai.UnlockActivity
import com.unlockandlearn.ai.service.ScreenUnlockService

class ScreenUnlockReceiver : BroadcastReceiver() {

    companion object {
        private var lastLaunchTime = 0L
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ScreenUnlockReceiver", "Received event: ${intent.action}")
        
        // When screen is unlocked (USER_PRESENT) or turned on (SCREEN_ON)
        if (intent.action == Intent.ACTION_USER_PRESENT || intent.action == Intent.ACTION_SCREEN_ON) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastLaunchTime < 1500) {
                Log.d("ScreenUnlockReceiver", "Ignored duplicate/rapid trigger within 1.5 seconds")
                return
            }
            lastLaunchTime = currentTime
            
            // 1. Restart the background monitor service if it was killed by OS
            val serviceIntent = Intent(context, ScreenUnlockService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                Log.d("ScreenUnlockReceiver", "Successfully ensured ScreenUnlockService is active")
            } catch (e: Exception) {
                Log.e("ScreenUnlockReceiver", "Could not restart ScreenUnlockService", e)
            }

            // 2. Start UnlockActivity to show the flashcard overlay
            val overlayIntent = Intent(context, UnlockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            }
            try {
                context.startActivity(overlayIntent)
                Log.d("ScreenUnlockReceiver", "Started UnlockActivity successfully")
            } catch (e: Exception) {
                Log.e("ScreenUnlockReceiver", "Could not start UnlockActivity", e)
            }
        }
    }
}
