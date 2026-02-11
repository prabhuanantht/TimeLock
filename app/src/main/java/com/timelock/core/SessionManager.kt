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

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun startSession(allowedApps: List<String>, durationMillis: Long, pin: String) {
        val endTime = System.currentTimeMillis() + durationMillis
        val pinHash = hashPin(pin)
        
        prefs.edit().apply {
            putBoolean(KEY_SESSION_ACTIVE, true)
            putLong(KEY_SESSION_END_TIME, endTime)
            putStringSet(KEY_ALLOWED_APPS, allowedApps.toSet())
            putString(KEY_PIN_HASH, pinHash)
            putLong(KEY_LAST_DURATION, durationMillis)
            apply()
        }
    }

    fun endSession() {
        prefs.edit().apply {
            putBoolean(KEY_SESSION_ACTIVE, false)
            remove(KEY_SESSION_END_TIME)
            remove(KEY_ALLOWED_APPS)
            remove(KEY_PIN_HASH)
            apply()
        }
    }

    fun isSessionActive(): Boolean {
        return prefs.getBoolean(KEY_SESSION_ACTIVE, false)
    }

    fun isAppAllowed(packageName: String): Boolean {
        // Always allow our own app
        if (packageName == "com.timelock") return true
        
        if (!isSessionActive()) return true
        
        // If session is active but time expired, block everything (except self)
        if (System.currentTimeMillis() > getSessionEndTime()) {
            return false
        }

        val allowed = prefs.getStringSet(KEY_ALLOWED_APPS, emptySet()) ?: emptySet()
        return allowed.contains(packageName)
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
