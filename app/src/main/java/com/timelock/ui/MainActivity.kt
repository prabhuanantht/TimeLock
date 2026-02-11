package com.timelock.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.timelock.R
import com.timelock.core.SessionManager
import com.timelock.service.SessionTimerService

class MainActivity : AppCompatActivity() {

    private lateinit var appAdapter: AppListAdapter
    private lateinit var etDuration: EditText
    private lateinit var etPin: EditText
    private lateinit var btnStart: Button
    private lateinit var rvApps: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etDuration = findViewById(R.id.etDuration)
        etPin = findViewById(R.id.etPin)
        btnStart = findViewById(R.id.btnStart)
        rvApps = findViewById(R.id.rvApps)

        setupAppList()

        btnStart.setOnClickListener {
            startSession()
        }
        
        checkSessionStatus()
    }
    
    private fun checkSessionStatus() {
        if (SessionManager.isSessionActive()) {
            btnStart.isEnabled = false
            btnStart.text = "SESSION ACTIVE"
            // Ideally redirect to a status screen, but for now just disable
        }
    }

    private fun setupAppList() {
        // Simple async loader or just run on main thread for MVP if list is small. 
        // For production, use Coroutines/Threads.
        Thread {
            val intent = Intent(Intent.ACTION_MAIN, null)
            intent.addCategory(Intent.CATEGORY_LAUNCHER)
            val apps = packageManager.queryIntentActivities(intent, 0)
            
            // Filter out self?
            // val filteredApps = apps.filter { it.activityInfo.packageName != packageName } 

            runOnUiThread {
                appAdapter = AppListAdapter(packageManager, apps)
                rvApps.layoutManager = LinearLayoutManager(this)
                rvApps.adapter = appAdapter
            }
        }.start()
    }

    private fun startSession() {
        val durationStr = etDuration.text.toString()
        val pin = etPin.text.toString()
        val selectedApps = appAdapter.getSelectedPackages()

        if (durationStr.isEmpty() || pin.isEmpty()) {
            Toast.makeText(this, "Please enter duration and PIN", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (pin.length < 4) {
             Toast.makeText(this, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
             return
        }

        val durationMinutes = durationStr.toLongOrNull() ?: 0
        if (durationMinutes <= 0) {
            Toast.makeText(this, "Invalid duration", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedApps.isEmpty()) {
            Toast.makeText(this, "Select at least one allowed app", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Start Session in Manager
        SessionManager.startSession(selectedApps, durationMinutes * 60 * 1000, pin)

        // 2. Start Timer Service
        val serviceIntent = Intent(this, SessionTimerService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // 3. Prompt user for Accessibility service if not enabled (Simple check)
        // For now, assume user will enable it via Settings or we guide them in Phase 9
        
        Toast.makeText(this, "Session Started!", Toast.LENGTH_SHORT).show()
        
        // 4. Exit to Home
        val homeIntent = Intent(Intent.ACTION_MAIN)
        homeIntent.addCategory(Intent.CATEGORY_HOME)
        homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(homeIntent)
        
        finish()
    }
}
