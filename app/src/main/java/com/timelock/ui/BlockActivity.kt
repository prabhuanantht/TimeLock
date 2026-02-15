package com.timelock.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.timelock.R
import com.timelock.core.SessionManager
import com.timelock.service.SessionTimerService

class BlockActivity : AppCompatActivity() {

    private lateinit var rvKioskApps: RecyclerView
    private lateinit var adapter: KioskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block)

        // Disable Back Button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing
            }
        })

        rvKioskApps = findViewById(R.id.rvKioskApps)
        val tvEmergency = findViewById<android.widget.TextView>(R.id.tvEmergency)
        val layoutUnlock = findViewById<android.widget.LinearLayout>(R.id.layoutUnlock)
        val etPin = findViewById<EditText>(R.id.etPin)
        val btnUnlock = findViewById<Button>(R.id.btnUnlock)
        val bottomPanel = findViewById<android.widget.LinearLayout>(R.id.bottomPanel)

        setupKioskApps()

        tvEmergency.setOnClickListener {
            layoutUnlock.visibility = android.view.View.VISIBLE
            tvEmergency.visibility = android.view.View.GONE
            // Hide apps when unlocking? Maybe not needed, but cleaner.
            rvKioskApps.visibility = android.view.View.GONE
        }

        btnUnlock.setOnClickListener {
            val inputPin = etPin.text.toString()
            if (SessionManager.validatePin(inputPin)) {
                unlockSession()
            } else {
                Toast.makeText(this, "Invalid PIN", Toast.LENGTH_SHORT).show()
                etPin.text.clear()
            }
        }
        
        checkExpiryState()
    }

    override fun onResume() {
        super.onResume()
        checkExpiryState()
    }

    private fun setupKioskApps() {
        // Get allowed apps
        val allowedAppsSet = SessionManager.getAllowedApps()
        val allowedAppsList = allowedAppsSet.toList().sortedBy { packageName -> 
             try {
                 packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
             } catch (e: Exception) {
                 packageName
             }
        }

        adapter = KioskAdapter(this, packageManager, allowedAppsList) { packageName ->
            launchApp(packageName)
        }

        rvKioskApps.layoutManager = GridLayoutManager(this, 3) // 3 columns
        rvKioskApps.adapter = adapter
    }

    private fun launchApp(packageName: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Cannot launch app", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error launching app", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkExpiryState() {
        val tvEmergency = findViewById<android.widget.TextView>(R.id.tvEmergency)
        val layoutUnlock = findViewById<android.widget.LinearLayout>(R.id.layoutUnlock)
        val tvTitle = findViewById<android.widget.TextView>(R.id.tvTitle)

        if (SessionManager.isSessionExpired()) {
            tvTitle.text = "SESSION TIMER ENDED\nDEVICE LOCKED"
            tvTitle.setTextColor(android.graphics.Color.RED)
            
            rvKioskApps.visibility = android.view.View.GONE
            tvEmergency.visibility = android.view.View.GONE
            layoutUnlock.visibility = android.view.View.VISIBLE
        } else {
             // Ensure apps are visible if not expired (and not in emergency unlock mode)
             if (layoutUnlock.visibility != android.view.View.VISIBLE) {
                 rvKioskApps.visibility = android.view.View.VISIBLE
             }
        }
    }

    private fun unlockSession() {
        SessionManager.endSession()
        
        val intent = Intent(this, SessionTimerService::class.java)
        stopService(intent)

        Toast.makeText(this, "Session Ended", Toast.LENGTH_SHORT).show()
        finish()
    }
}
