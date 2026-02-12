package com.timelock.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.timelock.R
import com.timelock.core.SessionManager
import com.timelock.service.SessionTimerService

class BlockActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block)

        // Disable Back Button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing, consume the event
            }
        })

        val btnGoHome = findViewById<Button>(R.id.btnGoHome)
        val tvEmergency = findViewById<android.widget.TextView>(R.id.tvEmergency)
        val layoutUnlock = findViewById<android.widget.LinearLayout>(R.id.layoutUnlock)
        val etPin = findViewById<EditText>(R.id.etPin)
        val btnUnlock = findViewById<Button>(R.id.btnUnlock)

        btnGoHome.setOnClickListener {
            // Just go home, keeping session active
            val homeIntent = Intent(Intent.ACTION_MAIN)
            homeIntent.addCategory(Intent.CATEGORY_HOME)
            homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(homeIntent)
            // We don't finish() here because we want to stay in back stack if needed, 
            // but actually, since this activity is excluded from recents, going home is fine.
            // But we shouldn't finish() because if the user comes back to the blocked app, 
            // the service will launch this activity again anyway.
        }

        tvEmergency.setOnClickListener {
            layoutUnlock.visibility = android.view.View.VISIBLE
            btnGoHome.visibility = android.view.View.GONE
            tvEmergency.visibility = android.view.View.GONE
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

    private fun checkExpiryState() {
        val btnGoHome = findViewById<Button>(R.id.btnGoHome)
        val tvEmergency = findViewById<android.widget.TextView>(R.id.tvEmergency)
        val layoutUnlock = findViewById<android.widget.LinearLayout>(R.id.layoutUnlock)
        val tvTitle = findViewById<android.widget.TextView>(R.id.tvTitle)

        if (SessionManager.isSessionExpired()) {
            tvTitle.text = "SESSION TIMER ENDED\nDEVICE LOCKED"
            tvTitle.setTextColor(android.graphics.Color.RED)
            
            btnGoHome.visibility = android.view.View.GONE
            tvEmergency.visibility = android.view.View.GONE
            layoutUnlock.visibility = android.view.View.VISIBLE
        }
    }

    private fun unlockSession() {
        // End session core logic
        SessionManager.endSession()
        
        // Stop the timer service
        val intent = Intent(this, SessionTimerService::class.java)
        stopService(intent)

        Toast.makeText(this, "Session Ended", Toast.LENGTH_SHORT).show()
        
        // Go back to Main or finish
        finish()
    }
}
