package com.muni.radiomunicipalzapala

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.media3.common.util.UnstableApi

class NotificationActionReceiver : BroadcastReceiver() {
    @UnstableApi
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            Log.w("NotificationAction", "Context o Intent nulos")
            return
        }

        val serviceIntent = Intent(context, MusicService::class.java).apply {
            action = intent.action
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}