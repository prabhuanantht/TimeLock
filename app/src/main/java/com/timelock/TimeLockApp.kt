package com.timelock

import android.app.Application
import com.timelock.core.SessionManager

class TimeLockApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionManager.init(this)
    }
}
