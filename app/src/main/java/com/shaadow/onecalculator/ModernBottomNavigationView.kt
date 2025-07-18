package com.shaadow.onecalculator

import android.content.Context
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import android.graphics.Typeface

class ModernBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var currentSelectedPosition = 0
    private var onTabSelectedListener: ((Int) -> Unit)? = null

    // Tab views
    private lateinit var homeTab: View
    private lateinit var chatTab: View
    private lateinit var mathlyTab: View
    private lateinit var scannerTab: View

    // Tab icons
    private lateinit var homeIcon: ImageView
    private lateinit var chatIcon: ImageView
    private lateinit var mathlyIcon: ImageView
    private lateinit var scannerIcon: ImageView

    // Tab labels
    private lateinit var homeLabel: TextView
    private lateinit var chatLabel: TextView
    private lateinit var mathlyLabel: TextView
    private lateinit var scannerLabel: TextView

    // Tab containers
    private lateinit var homeContainer: FrameLayout
    private lateinit var chatContainer: FrameLayout
    private lateinit var mathlyContainer: FrameLayout
    private lateinit var scannerContainer: FrameLayout

    init {
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        // Inflate the bottom navigation layout
        LayoutInflater.from(context).inflate(R.layout.bottom_navigation, this, true)

        // Initialize tab views
        homeTab = findViewById(R.id.home_tab)
        chatTab = findViewById(R.id.category_tab) // Using same id for now
        mathlyTab = findViewById(R.id.mathly_tab)
        scannerTab = findViewById(R.id.scanner_tab)

        // Initialize icon views
        homeIcon = homeTab.findViewById(R.id.tab_icon)
        chatIcon = chatTab.findViewById(R.id.tab_icon)
        mathlyIcon = mathlyTab.findViewById(R.id.tab_icon)
        scannerIcon = scannerTab.findViewById(R.id.tab_icon)

        // Initialize label views
        homeLabel = homeTab.findViewById(R.id.tab_label)
        chatLabel = chatTab.findViewById(R.id.tab_label)
        mathlyLabel = mathlyTab.findViewById(R.id.tab_label)
        scannerLabel = scannerTab.findViewById(R.id.tab_label)

        // Initialize container views
        homeContainer = homeTab.findViewById(R.id.icon_container)
        chatContainer = chatTab.findViewById(R.id.icon_container)
        mathlyContainer = mathlyTab.findViewById(R.id.icon_container)
        scannerContainer = scannerTab.findViewById(R.id.icon_container)

        // Set up tab data
        setupTabData()

        // Set up click listeners
        setupClickListeners()

        // Set initial selection
        setSelectedTab(0)
    }

    private fun setupTabData() {
        // Home tab
        homeIcon.setImageResource(R.drawable.ic_home)
        homeLabel.text = context.getString(R.string.home_nav)

        // Mathly Voice tab
        mathlyIcon.setImageResource(R.drawable.ic_microphone)
        mathlyLabel.text = context.getString(R.string.voice)

        // Chat tab
        chatIcon.setImageResource(R.drawable.ic_chat)
        chatLabel.text = context.getString(R.string.chat)

        // Scanner tab
        scannerIcon.setImageResource(R.drawable.ic_scan)
        scannerLabel.text = context.getString(R.string.scanner)
    }

    private fun setupClickListeners() {
        homeTab.setOnClickListener {
            setSelectedTab(0)
            onTabSelectedListener?.invoke(0)
        }

        mathlyTab.setOnClickListener {
            setSelectedTab(1)
            onTabSelectedListener?.invoke(1)
        }

        chatTab.setOnClickListener {
            setSelectedTab(2)
            onTabSelectedListener?.invoke(2)
        }

        scannerTab.setOnClickListener {
            setSelectedTab(3)
            onTabSelectedListener?.invoke(3)
        }
    }

    private fun setSelectedTab(position: Int) {
        // Update previous selection
        updateTabAppearance(currentSelectedPosition, false)

        // Update new selection
        currentSelectedPosition = position
        updateTabAppearance(position, true)
    }

    private fun updateTabAppearance(position: Int, isSelected: Boolean) {
        val (container, icon, label) = when (position) {
            0 -> Triple(homeContainer, homeIcon, homeLabel)
            1 -> Triple(mathlyContainer, mathlyIcon, mathlyLabel)
            2 -> Triple(chatContainer, chatIcon, chatLabel)
            3 -> Triple(scannerContainer, scannerIcon, scannerLabel)
            else -> return
        }

        // Update container selection state
        container.isSelected = isSelected

        // Update icon and label colors
        val iconColor = if (isSelected) {
            ContextCompat.getColor(context, R.color.bottom_nav_active_icon)
        } else {
            ContextCompat.getColor(context, R.color.bottom_nav_inactive_icon)
        }

        val textColor = if (isSelected) {
            ContextCompat.getColor(context, R.color.bottom_nav_active_text)
        } else {
            ContextCompat.getColor(context, R.color.bottom_nav_inactive_text)
        }

        icon.setColorFilter(iconColor)
        label.setTextColor(textColor)
    }

    fun setOnTabSelectedListener(listener: (Int) -> Unit) {
        onTabSelectedListener = listener
    }

    fun setSelectedItem(position: Int) {
        setSelectedTab(position)
    }

    fun getCurrentSelectedPosition(): Int = currentSelectedPosition

    fun setEnabledTabs(enabledTabs: List<String>) {
        // Home tab is always visible
        homeTab.visibility = View.VISIBLE
        // Mathly Voice
        mathlyTab.visibility = if (enabledTabs.contains("mathly_voice")) View.VISIBLE else View.GONE
        // Chat
        chatTab.visibility = if (enabledTabs.contains("chat")) View.VISIBLE else View.GONE
        // Scanner
        scannerTab.visibility = if (enabledTabs.contains("scanner")) View.VISIBLE else View.GONE
    }
} 