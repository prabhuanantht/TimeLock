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
 
         permissionPanel = findViewById(R.id.permissionPanel)
         controlPanel = findViewById(R.id.controlPanel)
         btnEnableAdmin = findViewById(R.id.btnEnableAdmin)
         btnEnableAccessibility = findViewById(R.id.btnEnableAccessibility)
         rvApps = findViewById(R.id.rvApps)
 
         setupAppList()
         setupButtons()
     }
 
     override fun onNavigationItemSelected(item: android.view.MenuItem): Boolean {
         when (item.itemId) {
             R.id.nav_home -> {
                 // Do nothing, we are here
             }
             R.id.nav_permissions -> {
                 checkPermissions(forceShow = true)
             }
             R.id.nav_theme_dark -> {
                 AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
             }
             R.id.nav_theme_light -> {
                 AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
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
             
             btnEnableAdmin.text = if (isAdmin) "Device Admin Enabled" else "Enable Device Admin"
             btnEnableAccessibility.text = if (isAccessibility) "Accessibility Enabled" else "Enable Accessibility"
             
             if (!isOverlay) {
                 if (forceShow) Toast.makeText(this, "Overlay Permission Missing!", Toast.LENGTH_SHORT).show()
             }
         }
     }
 
     private fun isAdminActive(): Boolean {
         val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
         val componentName = ComponentName(this, TimeLockDeviceAdminReceiver::class.java)
         return dpm.isAdminActive(componentName)
     }
 
     private fun isAccessibilityServiceEnabled(): Boolean {
         val accessibilityEnabled = Settings.Secure.getInt(
             contentResolver,
             Settings.Secure.ACCESSIBILITY_ENABLED, 0
         )
         if (accessibilityEnabled == 1) {
             val service = "$packageName/${AppMonitorAccessibilityService::class.java.canonicalName}"
             val settingValue = Settings.Secure.getString(
                 contentResolver,
                 Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
             )
             return settingValue?.contains(service) == true
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
 
         findViewById<Button>(R.id.btnStart).setOnClickListener {
             startSession()
         }
     }
 
     private fun startSession() {
         if (permissionPanel.visibility == View.VISIBLE) {
              Toast.makeText(this, "Please enable permissions first", Toast.LENGTH_SHORT).show()
              return
         }
 
         val etDuration = findViewById<EditText>(R.id.etDuration)
         val etPin = findViewById<EditText>(R.id.etPin)
 
         val durationStr = etDuration.text.toString()
         val pin = etPin.text.toString()
 
         if (durationStr.isEmpty() || pin.length != 4) {
             Toast.makeText(this, "Enter duration and 4-digit PIN", Toast.LENGTH_SHORT).show()
             return
         }
 
         val durationMins = durationStr.toLong()
         val selectedApps = appAdapter.getSelectedPackages()
 
         if (selectedApps.isEmpty()) {
             Toast.makeText(this, "Select at least one app", Toast.LENGTH_SHORT).show()
             return
         }
 
         // Start!!!
         SessionManager.startSession(selectedApps, durationMins * 60 * 1000, pin)
         
         val serviceIntent = Intent(this, SessionTimerService::class.java)
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
             startForegroundService(serviceIntent)
         } else {
             startService(serviceIntent)
         }
 
         Toast.makeText(this, "Session Started!", Toast.LENGTH_SHORT).show()
         
         // Go Home
         val homeIntent = Intent(Intent.ACTION_MAIN)
         homeIntent.addCategory(Intent.CATEGORY_HOME)
         homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
         startActivity(homeIntent)
         finish() // Optional: finish MainActivity so user can't just back into it easily
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
