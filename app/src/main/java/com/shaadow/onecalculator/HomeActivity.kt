package com.shaadow.onecalculator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import android.widget.ImageButton
import androidx.viewpager2.widget.ViewPager2
import android.widget.PopupMenu
import android.content.Intent
import android.view.View

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_home)
        supportActionBar?.hide()

        val viewPager = findViewById<ViewPager2>(R.id.view_pager)
        val bottomNav = findViewById<ModernBottomNavigationView>(R.id.bottom_navigation)
        val adapter = ViewPagerAdapter(this, listOf("home", "calculator", "chat", "voice", "scanner"))
        viewPager.adapter = adapter
        viewPager.isUserInputEnabled = false // Disable swipe if you want only tab click

        // Set up bottom navigation listener
        bottomNav.setOnTabSelectedListener { position ->
            viewPager.currentItem = position
        }

        // Set up ViewPager page change callback
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                bottomNav.setSelectedItem(position)
            }
        })

        // Check if we should navigate to history tab
        if (intent.getBooleanExtra("navigate_to_history", false)) {
            viewPager.currentItem = 2 // History tab (categories tab)
            bottomNav.setSelectedItem(2)
        }
        // Check if we should navigate to scanner tab
        if (intent.getBooleanExtra("navigate_to_scanner", false)) {
            viewPager.currentItem = 4 // Scanner tab
            bottomNav.setSelectedItem(4)
        }

    }
    
    private fun showSettingsPopupMenu(view: android.view.View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.settings_popup_menu, popup.menu)
        
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_history -> {
                    val intent = Intent(this, HistoryActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.menu_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.menu_about -> {
                    Toast.makeText(this, "About", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }
}
