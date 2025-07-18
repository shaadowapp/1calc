package com.shaadow.onecalculator

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.View

class TabBarVisibilityReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "com.shaadow.onecalculator.UPDATE_TAB_BAR_VISIBILITY") {
            val shouldHideTabBar = intent.getBooleanExtra("hide_tab_bar", false)
            val mathlyVoiceEnabled = intent.getBooleanExtra("mathly_voice_enabled", true)
            val mathlyChatEnabled = intent.getBooleanExtra("mathly_chat_enabled", true)
            val mathlyScannerEnabled = intent.getBooleanExtra("mathly_scanner_enabled", true)
            
            // Update the main activity's bottom navigation visibility
            updateBottomNavigationVisibility(context, shouldHideTabBar, mathlyVoiceEnabled, mathlyChatEnabled, mathlyScannerEnabled)
        }
    }
    
    private fun updateBottomNavigationVisibility(
        context: Context?,
        shouldHideTabBar: Boolean,
        mathlyVoiceEnabled: Boolean,
        mathlyChatEnabled: Boolean,
        mathlyScannerEnabled: Boolean
    ) {
        // Find the main activity and update its bottom navigation
        context?.let { ctx ->
            // This will be handled by the main activity
            val updateIntent = Intent(ctx, MainTabActivity::class.java)
            updateIntent.action = "UPDATE_TAB_BAR"
            updateIntent.putExtra("hide_tab_bar", shouldHideTabBar)
            updateIntent.putExtra("mathly_voice_enabled", mathlyVoiceEnabled)
            updateIntent.putExtra("mathly_chat_enabled", mathlyChatEnabled)
            updateIntent.putExtra("mathly_scanner_enabled", mathlyScannerEnabled)
            ctx.startActivity(updateIntent)
        }
    }
} 