package com.timelock.ui

import android.content.ComponentName
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

    private lateinit var permissionPanel: android.widget.LinearLayout
    private lateinit var controlPanel: android.widget.LinearLayout
    private lateinit var btnEnableAdmin: Button
    private lateinit var btnEnableAccessibility: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        permissionPanel = findViewById(R.id.permissionPanel)
        controlPanel = findViewById(R.id.controlPanel)
        btnEnableAdmin = findViewById(R.id.btnEnableAdmin)
        btnEnableAccessibility = findViewById(R.id.btnEnableAccessibility)

        etDuration = findViewById(R.id.etDuration)
        etPin = findViewById(R.id.etPin)
        btnStart = findViewById(R.id.btnStart)
        rvApps = findViewById(R.id.rvApps)

        setupAppList()
        setupPermissionButtons()

        btnStart.setOnClickListener {
            startSession()
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
        checkSessionStatus()
    }

    private fun setupPermissionButtons() {
        btnEnableAdmin.setOnClickListener {
            val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, getAdminComponentName())
            intent.putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION, "TimeLock needs this to prevent uninstallation during sessions.")
            startActivity(intent)
        }

        btnEnableAccessibility.setOnClickListener {
            val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
    }

    private fun checkPermissions() {
        val isAdmin = isAdminActive()
        val isAccessibility = isAccessibilityServiceEnabled()

        if (isAdmin && isAccessibility) {
            permissionPanel.visibility = android.view.View.GONE
            controlPanel.visibility = android.view.View.VISIBLE
        } else {
            permissionPanel.visibility = android.view.View.VISIBLE
            controlPanel.visibility = android.view.View.GONE
            
            btnEnableAdmin.isEnabled = !isAdmin
            btnEnableAccessibility.isEnabled = !isAccessibility
            
            if (isAdmin) btnEnableAdmin.text = "Device Admin Enabled" else btnEnableAdmin.text = "Enable Device Admin"
            if (isAccessibility) btnEnableAccessibility.text = "Accessibility Enabled" else btnEnableAccessibility.text = "Enable Accessibility"
        }
    }

    private fun isAdminActive(): Boolean {
        val dpm = getSystemService(android.app.admin.DevicePolicyManager::class.java)
        return dpm.isAdminActive(getAdminComponentName())
    }

    private fun getAdminComponentName(): ComponentName {
        return ComponentName(this, com.timelock.receiver.TimeLockDeviceAdminReceiver::class.java)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = ComponentName(this, com.timelock.service.AppMonitorAccessibilityService::class.java)
        
        val enabledServicesSetting = android.provider.Settings.Secure.getString(
            contentResolver,
            android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)

        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledComponent = ComponentName.unflattenFromString(componentNameString)
            if (enabledComponent != null && enabledComponent == expectedComponentName)
                return true
        }
        return false
    }
    
    private fun checkSessionStatus() {
        if (SessionManager.isSessionActive()) {
            btnStart.isEnabled = false
            btnStart.text = "SESSION ACTIVE"
        }
    }

    private fun setupAppList() {
        Thread {
            val intent = Intent(Intent.ACTION_MAIN, null)
            intent.addCategory(Intent.CATEGORY_LAUNCHER)
            val apps = packageManager.queryIntentActivities(intent, 0)
            
            runOnUiThread {
                appAdapter = AppListAdapter(packageManager, apps)
                rvApps.layoutManager = LinearLayoutManager(this)
                rvApps.adapter = appAdapter
            }
        }.start()
    }

    private fun startSession() {
        if (permissionPanel.visibility == android.view.View.VISIBLE) {
             Toast.makeText(this, "Please enable permissions first", Toast.LENGTH_SHORT).show()
             return
        }

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
        
        Toast.makeText(this, "Session Started!", Toast.LENGTH_SHORT).show()
        
        // 4. Exit to Home
        val homeIntent = Intent(Intent.ACTION_MAIN)
        homeIntent.addCategory(Intent.CATEGORY_HOME)
        homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(homeIntent)
        
        finish()
    }
}
