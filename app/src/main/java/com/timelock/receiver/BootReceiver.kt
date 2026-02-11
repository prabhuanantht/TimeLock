package com.timelock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.timelock.core.SessionManager
import com.timelock.service.SessionTimerService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Need to re-init SessionManager context as app just started
            SessionManager.init(context.applicationContext)

            if (SessionManager.isSessionActive()) {
                val endTime = SessionManager.getSessionEndTime()
                if (System.currentTimeMillis() < endTime) {
                    // Resume session
                    val serviceIntent = Intent(context, SessionTimerService::class.java)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } else {
                    // Session expired while off, cleanup
                    SessionManager.endSession()
                }
            }
        }
    }
}
