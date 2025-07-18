package com.shaadow.onecalculator

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Central traffic controller logic
        // Example: route based on intent extras (expand as needed)
        val target = intent.getStringExtra("target")
        when (target) {
            "basic" -> startActivity(Intent(this, BasicActivity::class.java))
            "tabs" -> startActivity(Intent(this, MainTabActivity::class.java))
            "history" -> startActivity(Intent(this, HistoryActivity::class.java))
            "settings" -> startActivity(Intent(this, SettingsActivity::class.java))
            "onboarding" -> startActivity(Intent(this, OnboardingActivity::class.java))
            else -> startActivity(Intent(this, BasicActivity::class.java)) // Default to calculator
        }
        finish()
    }
}
