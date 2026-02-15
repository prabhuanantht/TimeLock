package com.timelock.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.timelock.core.SessionManager

class AppMonitorAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "TimeLockMonitor"
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val packageName = event.packageName?.toString() ?: return
        
        // Optimization: Don't check if it's our own app (handled in SessionManager, but good to have quick exit here too if needed)
        
        if (packageName == "android" || packageName == "com.android.systemui" || packageName.contains("inputmethod")) {
            return
        }
        
        if (!SessionManager.isAppAllowed(packageName)) {
            Log.w(TAG, "BLOCKED: $packageName")
            
            val intent = android.content.Intent(this, com.timelock.ui.BlockActivity::class.java)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION) // Optional
            startActivity(intent)
        }
    }

    override fun onInterrupt() {
        // Required method
    }
}
