package com.shaadow.onecalculator

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Base activity class for the application
 * Handles theme mode application programmatically
 */
open class BaseActivity : AppCompatActivity() {

    private val PREFS_NAME = "CalculatorSettings"
    private val KEY_THEME_MODE = "theme_mode"
    private val THEME_CHANGED_ACTION = "com.shaadow.onecalculator.THEME_CHANGED"

    private lateinit var themeChangeReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        applyThemeMode()
        super.onCreate(savedInstanceState)
        registerThemeChangeReceiver()
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(themeChangeReceiver)
    }

    private fun registerThemeChangeReceiver() {
        themeChangeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == THEME_CHANGED_ACTION) {
                    applyThemeMode()
                }
            }
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(
            themeChangeReceiver,
            IntentFilter(THEME_CHANGED_ACTION)
        )
    }

    private fun applyThemeMode() {
        lifecycleScope.launch {
            val themeMode = getThemeModePreference()
            when (themeMode) {
                "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                "auto" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }

    private suspend fun getThemeModePreference(): String {
        return withContext(Dispatchers.IO) {
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            prefs.getString(KEY_THEME_MODE, "auto") ?: "auto"
        }
    }

    protected fun sendThemeChangeBroadcast() {
        val intent = Intent(THEME_CHANGED_ACTION)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
}