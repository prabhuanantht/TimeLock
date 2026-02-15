package com.timelock.ui
 
 import android.app.admin.DevicePolicyManager
 import android.content.ComponentName
 import android.content.Context
 import android.content.Intent
 import android.content.pm.PackageManager
 import android.net.Uri
 import android.os.Build
 import android.os.Bundle
 import android.provider.Settings
 import android.view.View
 import android.widget.Button
 import android.widget.EditText
 import android.widget.LinearLayout
 import android.widget.TextView
 import android.widget.Toast
 import androidx.appcompat.app.ActionBarDrawerToggle
 import androidx.appcompat.app.AppCompatActivity
 import androidx.appcompat.app.AppCompatDelegate
 import androidx.appcompat.widget.Toolbar
 import androidx.core.view.GravityCompat
 import androidx.drawerlayout.widget.DrawerLayout
 import androidx.recyclerview.widget.LinearLayoutManager
 import androidx.recyclerview.widget.RecyclerView
 import com.google.android.material.navigation.NavigationView
 import com.timelock.R
 import com.timelock.core.SessionManager
 import com.timelock.receiver.TimeLockDeviceAdminReceiver
 import com.timelock.service.AppMonitorAccessibilityService
 import com.timelock.service.SessionTimerService
 
 class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
 
     private lateinit var appAdapter: AppListAdapter
     private lateinit var rvApps: RecyclerView
     private lateinit var permissionPanel: LinearLayout
     private lateinit var controlPanel: LinearLayout
     private lateinit var btnEnableAdmin: Button
     private lateinit var btnEnableAccessibility: Button
     private lateinit var drawerLayout: DrawerLayout
     private lateinit var navView: NavigationView
 
     private lateinit var btnEnableOverlay: Button

     override fun onCreate(savedInstanceState: Bundle?) {
         super.onCreate(savedInstanceState)
         setContentView(R.layout.activity_main)
 
         // Setup Toolbar
         val toolbar: Toolbar = findViewById(R.id.toolbar)
         setSupportActionBar(toolbar)
 
         // Setup Drawer
         drawerLayout = findViewById(R.id.drawer_layout)
         navView = findViewById(R.id.nav_view)
         
         val toggle = ActionBarDrawerToggle(
             this, drawerLayout, toolbar,
             R.string.navigation_drawer_open,
             R.string.navigation_drawer_close
         )
         drawerLayout.addDrawerListener(toggle)
         toggle.syncState()
         navView.setNavigationItemSelectedListener(this)

        // Setup Theme Switch
        val themeItem = navView.menu.findItem(R.id.nav_theme_toggle)
        val switchTheme = themeItem.actionView?.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_theme)
        
        // Set initial state
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        switchTheme?.isChecked = currentMode == AppCompatDelegate.MODE_NIGHT_YES
        
        switchTheme?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    

         permissionPanel = findViewById(R.id.permissionPanel)
         controlPanel = findViewById(R.id.controlPanel)
         btnEnableAdmin = findViewById(R.id.btnEnableAdmin)
         btnEnableAccessibility = findViewById(R.id.btnEnableAccessibility)
         btnEnableOverlay = findViewById(R.id.btnEnableOverlay)
         rvApps = findViewById(R.id.rvApps)
 
         setupAppList()
         setupButtons()
     }
 
     override fun onNavigationItemSelected(item: android.view.MenuItem): Boolean {
         when (item.itemId) {
             R.id.nav_home -> {
                 // Do nothing
             }
             R.id.nav_permissions -> {
                 checkPermissions(forceShow = true)
             }
             R.id.nav_about -> {
                 Toast.makeText(this, "TimeLock v1.0\nFocus on what matters.", Toast.LENGTH_LONG).show()
             }
         }
         drawerLayout.closeDrawer(GravityCompat.START)
         return true
     }
 
     override fun onBackPressed() {
         if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
             drawerLayout.closeDrawer(GravityCompat.START)
         } else {
             super.onBackPressed()
         }
     }
 
     override fun onResume() {
         super.onResume()
         checkPermissions(forceShow = false)
     }
 
     private fun checkPermissions(forceShow: Boolean = false) {
         val isAdmin = isAdminActive()
         val isAccessibility = isAccessibilityServiceEnabled()
         val isOverlay = Settings.canDrawOverlays(this)
 
         if (isAdmin && isAccessibility && isOverlay) {
             permissionPanel.visibility = View.GONE
             // If forcefully checking, show toast "All good"
             if (forceShow) Toast.makeText(this, "All Permissions Granted!", Toast.LENGTH_SHORT).show()
         } else {
             // Only show panel if we really need them or user asked
             permissionPanel.visibility = View.VISIBLE
             
             btnEnableAdmin.isEnabled = !isAdmin
             btnEnableAccessibility.isEnabled = !isAccessibility
             btnEnableOverlay.isEnabled = !isOverlay
             
             btnEnableAdmin.text = if (isAdmin) "Device Admin Enabled" else "Enable Device Admin"
             btnEnableAccessibility.text = if (isAccessibility) "Accessibility Enabled" else "Enable Accessibility"
             btnEnableOverlay.text = if (isOverlay) "Overlay Enabled" else "Enable Display Over Apps"
             
             if (!isOverlay && forceShow) {
                 Toast.makeText(this, "Overlay Permission Missing!", Toast.LENGTH_SHORT).show()
             }
         }
     }
 
     private fun isAdminActive(): Boolean {
         val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
         val componentName = ComponentName(this, TimeLockDeviceAdminReceiver::class.java)
         return dpm.isAdminActive(componentName)
     }
 
    // Fix isAccessibilityServiceEnabled check
    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = ComponentName(this, AppMonitorAccessibilityService::class.java)
        val enabledServicesSetting = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
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
 
     private fun setupButtons() {
         btnEnableAdmin.setOnClickListener {
             val componentName = ComponentName(this, TimeLockDeviceAdminReceiver::class.java)
             val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
             intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
             intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "TimeLock needs admin access to prevent uninstallation during sessions.")
             startActivity(intent)
         }
 
         btnEnableAccessibility.setOnClickListener {
             val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
             startActivity(intent)
         }
 
        btnEnableOverlay.setOnClickListener {
             val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
             startActivity(intent)
        }

        val rgTimerMode = findViewById<android.widget.RadioGroup>(R.id.rgTimerMode)
        // Logic: We need to hide the parent of etDuration.
        // Actually etDuration is inside controlPanel, but we need to hide JUST the duration part. 
        // In activity_main.xml, "Set Session Duration" + EditText are likely just direct children of controlPanel?
        // Let's check: "TextView" + "EditText" are children. We should wrap them or target them.
        // For now, let's target them by ID if possible, or assume they are children.
        val tvDurationLabel = findViewById<TextView>(R.id.tvDurationLabel) // We need to add ID to the label!
        val etDuration = findViewById<EditText>(R.id.etDuration)

        rgTimerMode.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbIndividual) {
                // Individual
                appAdapter.setTimerMode(true)
                tvDurationLabel.visibility = View.GONE
                etDuration.visibility = View.GONE
            } else {
                // Combined
                appAdapter.setTimerMode(false)
                tvDurationLabel.visibility = View.VISIBLE
                etDuration.visibility = View.VISIBLE
            }
        }

         findViewById<Button>(R.id.btnStart).setOnClickListener {
             startSession()
         }
     }
 
     private fun startSession() {
         if (permissionPanel.visibility == View.VISIBLE) {
              Toast.makeText(this, "Please enable permissions first", Toast.LENGTH_SHORT).show()
              return
         }
 
        val etPin = findViewById<EditText>(R.id.etPin)
        val pin = etPin.text.toString()

        if (pin.length != 4) {
            Toast.makeText(this, "Enter 4-digit PIN", Toast.LENGTH_SHORT).show()
            return
        }
        
        val selectedApps = appAdapter.getSelectedPackages()
        if (selectedApps.isEmpty()) {
            Toast.makeText(this, "Select at least one app", Toast.LENGTH_SHORT).show()
            return
        }

        var finalDurationMillis: Long = 0
        val isIndividual = findViewById<android.widget.RadioButton>(R.id.rbIndividual).isChecked

        if (isIndividual) {
            // Individual Mode: Calculate duration based on MAX of individual timers for now
            // Or sum? Usually "Lock session until X". If I set App A for 10m and App B for 20m, 
            // the session should effectively manage that. 
            // Current MVP: Take the MAXIMUM duration to keep session active.
            val durations = appAdapter.getAppDurations()
            if (durations.isEmpty()) {
                 Toast.makeText(this, "Set duration for selected apps", Toast.LENGTH_SHORT).show()
                 return
            }
            
            val maxDuration = durations.values.maxOrNull() ?: 0
            if (maxDuration <= 0) {
                Toast.makeText(this, "Invalid duration", Toast.LENGTH_SHORT).show()
                return
            }
            finalDurationMillis = maxDuration * 60 * 1000
        } else {
            // Combined Mode
            val etDuration = findViewById<EditText>(R.id.etDuration)
            val durationStr = etDuration.text.toString()
            if (durationStr.isEmpty()) {
                Toast.makeText(this, "Enter session duration", Toast.LENGTH_SHORT).show()
                return
            }
            finalDurationMillis = durationStr.toLong() * 60 * 1000
        }

        // 1. Start Session
        SessionManager.startSession(selectedApps, finalDurationMillis, pin)
        
        // 2. Start Timer Service
        val serviceIntent = Intent(this, SessionTimerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        Toast.makeText(this, "Session Started!", Toast.LENGTH_SHORT).show()
        
        // 4. Enter Kiosk Mode
        val lockIntent = Intent(this, BlockActivity::class.java)
        lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(lockIntent)
        finish()
    }
 
     private fun setupAppList() {
         // Initialize SearchView
         val searchView = findViewById<androidx.appcompat.widget.SearchView>(R.id.searchView)
         searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
             override fun onQueryTextSubmit(query: String?): Boolean {
                 appAdapter.filter.filter(query)
                 return false
             }
 
             override fun onQueryTextChange(newText: String?): Boolean {
                 appAdapter.filter.filter(newText)
                 return false
             }
         })
 
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
 }
