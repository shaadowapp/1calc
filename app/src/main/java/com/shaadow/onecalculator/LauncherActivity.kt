package com.shaadow.onecalculator

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LauncherActivity : AppCompatActivity() {
    companion object {
        // Set to true for dev/test mode, false for production
        const val DEV_ONBOARDING_ALWAYS = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No UI, just logic
        lifecycleScope.launch {
            if (DEV_ONBOARDING_ALWAYS) {
                startActivity(Intent(this@LauncherActivity, OnboardingActivity::class.java))
                finish()
                return@launch
            }
            val prefDao = HistoryDatabase.getInstance(this@LauncherActivity).preferenceDao()
            val defaultPref = withContext(Dispatchers.IO) {
                prefDao.getPreference("default_screen")
            }
            when (defaultPref?.value) {
                "calculator" -> {
                    startActivity(Intent(this@LauncherActivity, BasicActivity::class.java))
                }
                else -> {
                    startActivity(Intent(this@LauncherActivity, OnboardingActivity::class.java))
                }
            }
            finish()
        }
    }
} 