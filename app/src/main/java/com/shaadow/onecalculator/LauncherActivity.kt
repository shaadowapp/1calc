package com.shaadow.onecalculator

import android.content.Intent
import android.content.SharedPreferences
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
        private const val ONBOARDING_PREFS_NAME = "onboarding_prefs"
        private const val KEY_IS_NEW_USER = "is_new_user"
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
            
            // Check if user has completed onboarding
            val onboardingPrefs = getSharedPreferences(ONBOARDING_PREFS_NAME, MODE_PRIVATE)
            val isNewUser = onboardingPrefs.getBoolean(KEY_IS_NEW_USER, true)
            
            if (isNewUser) {
                // User hasn't completed onboarding, show onboarding screen
                startActivity(Intent(this@LauncherActivity, OnboardingActivity::class.java))
            } else {
                // User has completed onboarding, check their default screen preference
                val prefDao = HistoryDatabase.getInstance(this@LauncherActivity).preferenceDao()
                val defaultPref = withContext(Dispatchers.IO) {
                    prefDao.getPreference("default_screen")
                }
                
                when (defaultPref?.value) {
                    "calculator" -> {
                        startActivity(Intent(this@LauncherActivity, BasicActivity::class.java))
                    }
                    "tabs" -> {
                        startActivity(Intent(this@LauncherActivity, MainTabActivity::class.java))
                    }
                    else -> {
                        // Default to calculator if no preference is set
                        startActivity(Intent(this@LauncherActivity, BasicActivity::class.java))
                    }
                }
            }
            finish()
        }
    }
} 