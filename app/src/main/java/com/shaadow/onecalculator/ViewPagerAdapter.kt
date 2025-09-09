package com.shaadow.onecalculator

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.shaadow.onecalculator.mathly.MathlyVoiceFragment
import com.shaadow.onecalculator.mathly.MathlyScannerFragment

class ViewPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val enabledTabs: List<String>
) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = enabledTabs.size

    override fun createFragment(position: Int): Fragment {
        return when (enabledTabs[position]) {
            "home" -> HomeFragment()
            "calculator" -> CalculatorFragment()
            "voice" -> com.shaadow.onecalculator.mathly.MathlyVoiceFragment()
            "chat" -> com.shaadow.onecalculator.mathly.ui.MathlyChatFragment()
            "scanner" -> com.shaadow.onecalculator.mathly.MathlyScannerFragment()
            else -> HomeFragment()
        }
    }
} 