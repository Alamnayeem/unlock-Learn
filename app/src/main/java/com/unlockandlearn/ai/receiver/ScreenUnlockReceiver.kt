package com.unlockandlearn.ai.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.unlockandlearn.ai.UnlockActivity

class ScreenUnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ScreenUnlockReceiver", "Received event: ${intent.action}")
        
        // When screen is unlocked (USER_PRESENT) or turned on (SCREEN_ON)
        if (intent.action == Intent.ACTION_USER_PRESENT || intent.action == Intent.ACTION_SCREEN_ON) {
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
