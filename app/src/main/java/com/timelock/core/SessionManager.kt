package com.timelock.core

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

object SessionManager {
    private const val PREF_NAME = "timelock_session"
    private const val KEY_SESSION_ACTIVE = "session_active"
    private const val KEY_SESSION_END_TIME = "session_end_time"
    private const val KEY_ALLOWED_APPS = "allowed_apps"
    private const val KEY_PIN_HASH = "pin_hash"
    private const val KEY_LAST_DURATION = "last_duration"
    private const val KEY_SESSION_EXPIRED = "session_expired"

    private lateinit var prefs: SharedPreferences
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun startSession(allowedApps: List<String>, durationMillis: Long, pin: String) {
        val endTime = System.currentTimeMillis() + durationMillis
        val pinHash = hashPin(pin)
        
        prefs.edit().apply {
            putBoolean(KEY_SESSION_ACTIVE, true)
            putBoolean(KEY_SESSION_EXPIRED, false)
            putLong(KEY_SESSION_END_TIME, endTime)
            putStringSet(KEY_ALLOWED_APPS, allowedApps.toSet())
            putString(KEY_PIN_HASH, pinHash)
            putLong(KEY_LAST_DURATION, durationMillis)
            apply()
        }
    }

    fun expireSession() {
        prefs.edit().apply {
            putBoolean(KEY_SESSION_EXPIRED, true)
            apply()
        }
    }

    fun endSession() {
        prefs.edit().apply {
            putBoolean(KEY_SESSION_ACTIVE, false)
            putBoolean(KEY_SESSION_EXPIRED, false)
            remove(KEY_SESSION_END_TIME)
            remove(KEY_ALLOWED_APPS)
            remove(KEY_PIN_HASH)
            apply()
        }
    }

    fun isSessionActive(): Boolean {
        return prefs.getBoolean(KEY_SESSION_ACTIVE, false)
    }

    fun isSessionExpired(): Boolean {
        return prefs.getBoolean(KEY_SESSION_EXPIRED, false)
    }

    fun isAppAllowed(packageName: String): Boolean {
        if (!isSessionActive()) return true
        
        // Always allow self
        if (packageName == appContext.packageName) return true

        // If Expired, BLOCK EVERYTHING ELSE (even launcher)
        if (isSessionExpired()) {
            return false
        }
        
        // Always allow default launcher (Home screen) IF NOT EXPIRED
        val homeIntent = android.content.Intent(android.content.Intent.ACTION_MAIN)
        homeIntent.addCategory(android.content.Intent.CATEGORY_HOME)
        val defaultLauncher = appContext.packageManager.resolveActivity(homeIntent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        if (packageName == defaultLauncher) return true

        val allowedApps = prefs.getStringSet(KEY_ALLOWED_APPS, emptySet()) ?: emptySet()
        return allowedApps.contains(packageName)
    }

    fun validatePin(inputPin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return hashPin(inputPin) == storedHash
    }

    fun getSessionEndTime(): Long {
        return prefs.getLong(KEY_SESSION_END_TIME, 0)
    }
    
    fun getLastDuration(): Long {
        return prefs.getLong(KEY_LAST_DURATION, 0) // Default 0
    }

    private fun hashPin(pin: String): String {
        val bytes = pin.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
