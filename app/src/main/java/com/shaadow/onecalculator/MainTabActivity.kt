package com.shaadow.onecalculator

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.shaadow.onecalculator.PreferenceDao
import com.shaadow.onecalculator.HistoryDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope

class MainTabActivity : BaseActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNav: ModernBottomNavigationView
    private lateinit var sharedPreferences: SharedPreferences
    
    companion object {
        private const val PREFS_NAME = "CalculatorSettings"
        private const val KEY_MATHLY_VOICE = "mathly_voice"
        private const val KEY_MATHLY_CHAT = "mathly_chat"
        private const val KEY_MATHLY_SCANNER = "mathly_scanner"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_tab)

        viewPager = findViewById(R.id.view_pager)
        bottomNav = findViewById(R.id.bottom_navigation)

        // Shake detection is now handled globally by GlobalShakeService

        // Observe preference changes in real time
        val dao = HistoryDatabase.getInstance(this).preferenceDao()
        lifecycleScope.launch {
            dao.observeAllPreferences().collect { prefs ->
                val enabledTabs = mutableListOf("home", "calculator")
                // Mathly features are hidden for launch - they are in development
                // val mathlyVoice = prefs.find { it.key == "mathly_voice" }?.value?.toBooleanStrictOrNull() != false
                // val mathlyChat = prefs.find { it.key == "mathly_chat" }?.value?.toBooleanStrictOrNull() != false
                val mathlyScanner = prefs.find { it.key == "mathly_scanner" }?.value?.toBooleanStrictOrNull() != false
                // if (mathlyVoice) enabledTabs.add("mathly_voice")
                // if (mathlyChat) enabledTabs.add("chat")
                if (mathlyScanner) enabledTabs.add("scanner")
                setupTabs(enabledTabs)
            }
        }
    }

    private fun setupTabs(enabledTabs: List<String>) {
        val adapter = ViewPagerAdapter(this, enabledTabs)
        viewPager.adapter = adapter
        bottomNav.setEnabledTabs(enabledTabs)
        // Sync tab selection between ViewPager and BottomNav
        bottomNav.setOnTabSelectedListener { position ->
            viewPager.currentItem = position
        }
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                bottomNav.setSelectedItem(position)
            }
        })
    }

    // Shake detection is now handled globally by GlobalShakeService
    // No need for manual lifecycle management

    // Bug report is now handled by BaseActivity via broadcast receiver
}