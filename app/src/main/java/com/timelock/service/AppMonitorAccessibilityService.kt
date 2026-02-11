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
        
        if (!SessionManager.isAppAllowed(packageName)) {
            Log.w(TAG, "BLOCKED: $packageName")
            // TODO: Launch BlockActivity (Phase 5)
        }
    }

    override fun onInterrupt() {
        // Required method
    }
}
