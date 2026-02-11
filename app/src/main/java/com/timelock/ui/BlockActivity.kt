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

        val etPin = findViewById<EditText>(R.id.etPin)
        val btnUnlock = findViewById<Button>(R.id.btnUnlock)

        btnUnlock.setOnClickListener {
            val inputPin = etPin.text.toString()
            if (SessionManager.validatePin(inputPin)) {
                unlockSession()
            } else {
                Toast.makeText(this, "Invalid PIN", Toast.LENGTH_SHORT).show()
                etPin.text.clear()
            }
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
