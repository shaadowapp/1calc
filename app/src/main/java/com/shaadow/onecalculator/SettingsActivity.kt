package com.shaadow.onecalculator

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.shaadow.onecalculator.databinding.ActivitySettingsBinding
import com.shaadow.onecalculator.databinding.DialogCalculatorModeBinding
import com.shaadow.onecalculator.databinding.DialogThemeModeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.CompoundButton
import android.content.Intent

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var preferenceDao: PreferenceDao
    
    private lateinit var mathlyVoiceSwitchListener: CompoundButton.OnCheckedChangeListener
    private lateinit var mathlyChatSwitchListener: CompoundButton.OnCheckedChangeListener
    private lateinit var mathlyScannerSwitchListener: CompoundButton.OnCheckedChangeListener
    
    companion object {
        private const val PREFS_NAME = "CalculatorSettings"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_CALCULATOR_MODE = "calculator_mode"
        private const val KEY_MATHLY_VOICE = "mathly_voice"
        private const val KEY_MATHLY_CHAT = "mathly_chat"
        private const val KEY_MATHLY_SCANNER = "mathly_scanner"
        private const val KEY_ADVANCED_CUSTOMIZATION_EXPANDED = "advanced_customization_expanded"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        preferenceDao = HistoryDatabase.getInstance(this).preferenceDao()
        
        setupSwitchListeners()
        setupClickListeners()
        loadCurrentPreferences()
        displayAppVersion()
    }
    
    private fun setupSwitchListeners() {
        mathlyVoiceSwitchListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            if (!buttonView.isPressed) return@OnCheckedChangeListener // Only react to user interaction
            if (!isChecked) {
                AlertDialog.Builder(this)
                    .setTitle("Turn Off Mathly Voice?")
                    .setMessage("Are you sure you want to turn off Mathly Voice? You can always turn it back on from settings.")
                    .setPositiveButton("Turn Off") { _, _ ->
                        lifecycleScope.launch {
                            setPrefBool(KEY_MATHLY_VOICE, false)
                            updateBottomTabBarVisibility()
                            Toast.makeText(this@SettingsActivity, getString(R.string.toast_mathly_voice_status, getString(R.string.off)), Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        binding.mathlyVoiceSwitch.isChecked = true
                    }
                    .setCancelable(false)
                    .show()
            } else {
                lifecycleScope.launch {
                    setPrefBool(KEY_MATHLY_VOICE, true)
                    updateBottomTabBarVisibility()
                    Toast.makeText(this@SettingsActivity, getString(R.string.toast_mathly_voice_status, getString(R.string.on)), Toast.LENGTH_SHORT).show()
                }
            }
        }
        mathlyChatSwitchListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            if (!buttonView.isPressed) return@OnCheckedChangeListener
            if (!isChecked) {
                AlertDialog.Builder(this)
                    .setTitle("Turn Off Mathly Chat?")
                    .setMessage("Are you sure you want to turn off Mathly Chat? You can always turn it back on from settings.")
                    .setPositiveButton("Turn Off") { _, _ ->
                        lifecycleScope.launch {
                            setPrefBool(KEY_MATHLY_CHAT, false)
                            updateBottomTabBarVisibility()
                            Toast.makeText(this@SettingsActivity, getString(R.string.toast_mathly_chat_status, getString(R.string.off)), Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        binding.mathlyChatSwitch.isChecked = true
                    }
                    .setCancelable(false)
                    .show()
            } else {
                lifecycleScope.launch {
                    setPrefBool(KEY_MATHLY_CHAT, true)
                    updateBottomTabBarVisibility()
                    Toast.makeText(this@SettingsActivity, getString(R.string.toast_mathly_chat_status, getString(R.string.on)), Toast.LENGTH_SHORT).show()
                }
            }
        }
        mathlyScannerSwitchListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            if (!buttonView.isPressed) return@OnCheckedChangeListener
            if (!isChecked) {
                AlertDialog.Builder(this)
                    .setTitle("Turn Off Mathly Scanner?")
                    .setMessage("Are you sure you want to turn off Mathly Scanner? You can always turn it back on from settings.")
                    .setPositiveButton("Turn Off") { _, _ ->
                        lifecycleScope.launch {
                            setPrefBool(KEY_MATHLY_SCANNER, false)
                            updateBottomTabBarVisibility()
                            Toast.makeText(this@SettingsActivity, getString(R.string.toast_mathly_scanner_status, getString(R.string.off)), Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        binding.mathlyScannerSwitch.isChecked = true
                    }
                    .setCancelable(false)
                    .show()
            } else {
                lifecycleScope.launch {
                    setPrefBool(KEY_MATHLY_SCANNER, true)
                    updateBottomTabBarVisibility()
                    Toast.makeText(this@SettingsActivity, getString(R.string.toast_mathly_scanner_status, getString(R.string.on)), Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.mathlyVoiceSwitch.setOnCheckedChangeListener(mathlyVoiceSwitchListener)
        binding.mathlyChatSwitch.setOnCheckedChangeListener(mathlyChatSwitchListener)
        binding.mathlyScannerSwitch.setOnCheckedChangeListener(mathlyScannerSwitchListener)
    }
    
    private fun setupClickListeners() {
        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        // Theme Mode
        binding.themeModeItem.setOnClickListener {
            showThemeModeDialog()
        }
        
        // Calculator Mode
        binding.calculatorModeItem.setOnClickListener {
            showCalculatorModeDialog()
        }
        
        // Advanced Customization Dropdown
        binding.advancedCustomizationHeader.setOnClickListener {
            toggleAdvancedCustomization()
        }
        
        // Privacy & Permissions
        binding.privacyPermissionsItem.setOnClickListener {
            startActivity(Intent(this, PrivacyPermissionsActivity::class.java))
        }
        // Check for Update
        binding.checkUpdatesItem.setOnClickListener {
            startActivity(Intent(this, CheckUpdateActivity::class.java))
        }
        // Feedback
        binding.feedbackItem.setOnClickListener {
            startActivity(Intent(this, FeedbackActivity::class.java))
        }
        // Rate Us
        binding.rateUsItem.setOnClickListener {
            startActivity(Intent(this, RateUsActivity::class.java))
        }
    }
    
    private fun showThemeModeDialog() {
        val dialogBinding = DialogThemeModeBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()
        
        // Set current selection
        lifecycleScope.launch {
            val currentTheme = getPrefString(KEY_THEME_MODE, "auto")
            when (currentTheme) {
                "light" -> dialogBinding.radioLight.isChecked = true
                "dark" -> dialogBinding.radioDark.isChecked = true
                "auto" -> dialogBinding.radioAuto.isChecked = true
            }
        }
        
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        dialogBinding.btnApply.setOnClickListener {
            val selectedTheme = when (dialogBinding.themeRadioGroup.checkedRadioButtonId) {
                R.id.radio_light -> "light"
                R.id.radio_dark -> "dark"
                R.id.radio_auto -> "auto"
                else -> "auto"
            }
            
            lifecycleScope.launch {
                setPrefString(KEY_THEME_MODE, selectedTheme)
                updateThemeModeDisplay(selectedTheme)
                Toast.makeText(this@SettingsActivity, getString(R.string.toast_theme_mode_updated), Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        
        dialog.show()
    }
    
    private fun showCalculatorModeDialog() {
        val dialogBinding = DialogCalculatorModeBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        lifecycleScope.launch {
            val currentMode = withContext(Dispatchers.IO) {
                preferenceDao.getPreference(KEY_CALCULATOR_MODE)?.value ?: "basic"
            }
            when (currentMode) {
                "basic" -> dialogBinding.radioBasic.isChecked = true
                "mathly_voice" -> dialogBinding.radioMathlyVoice.isChecked = true
                "chat" -> dialogBinding.radioChat.isChecked = true
                "scanner" -> dialogBinding.radioScanner.isChecked = true
                "todo" -> dialogBinding.radioTodo.isChecked = true
                "gallery" -> dialogBinding.radioGallery.isChecked = true
                else -> dialogBinding.radioBasic.isChecked = true
            }
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnApply.setOnClickListener {
            val selectedMode = when (dialogBinding.calculatorRadioGroup.checkedRadioButtonId) {
                R.id.radio_basic -> "basic"
                R.id.radio_mathly_voice -> "mathly_voice"
                R.id.radio_chat -> "chat"
                R.id.radio_scanner -> "scanner"
                R.id.radio_todo -> "todo"
                R.id.radio_gallery -> "gallery"
                else -> "basic"
            }
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    preferenceDao.setPreference(PreferenceEntity(KEY_CALCULATOR_MODE, selectedMode))
                }
                updateCalculatorModeDisplay(selectedMode)
                Toast.makeText(this@SettingsActivity, getString(R.string.toast_default_calculator_mode_updated), Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        dialog.show()
    }
    
    private fun loadCurrentPreferences() {
        lifecycleScope.launch {
            val themeMode = getPrefString(KEY_THEME_MODE, "auto")
            val calculatorMode = getPrefString(KEY_CALCULATOR_MODE, "basic")
            val mathlyVoice = getPrefBool(KEY_MATHLY_VOICE, true)
            val mathlyChat = getPrefBool(KEY_MATHLY_CHAT, true)
            val mathlyScanner = getPrefBool(KEY_MATHLY_SCANNER, true)
            val advancedExpanded = getPrefBool(KEY_ADVANCED_CUSTOMIZATION_EXPANDED, false)
            updateThemeModeDisplay(themeMode)
            updateCalculatorModeDisplay(calculatorMode)
            // Remove listeners before setting isChecked
            binding.mathlyVoiceSwitch.setOnCheckedChangeListener(null)
            binding.mathlyChatSwitch.setOnCheckedChangeListener(null)
            binding.mathlyScannerSwitch.setOnCheckedChangeListener(null)
            binding.mathlyVoiceSwitch.isChecked = mathlyVoice
            binding.mathlyChatSwitch.isChecked = mathlyChat
            binding.mathlyScannerSwitch.isChecked = mathlyScanner
            // Re-attach listeners
            binding.mathlyVoiceSwitch.setOnCheckedChangeListener(mathlyVoiceSwitchListener)
            binding.mathlyChatSwitch.setOnCheckedChangeListener(mathlyChatSwitchListener)
            binding.mathlyScannerSwitch.setOnCheckedChangeListener(mathlyScannerSwitchListener)
            // Set initial dropdown state
            if (advancedExpanded) {
                binding.advancedCustomizationContent.visibility = android.view.View.VISIBLE
                binding.advancedCustomizationArrow.rotation = 360f
            } else {
                binding.advancedCustomizationContent.visibility = android.view.View.GONE
                binding.advancedCustomizationArrow.rotation = 180f
            }
        }
    }
    
    private fun updateThemeModeDisplay(themeMode: String) {
        val themeText = when (themeMode) {
            "light" -> "Light"
            "dark" -> "Dark"
            "auto" -> "Auto"
            else -> "Auto"
        }
        binding.themeModeValue.text = themeText
    }
    
    private fun updateCalculatorModeDisplay(calculatorMode: String) {
        val modeText = when (calculatorMode) {
            "basic" -> "Basic Calculator"
            "mathly_voice" -> "Mathly Voice"
            "chat" -> "Chat"
            "scanner" -> "Scanner"
            "todo" -> "To-Do List"
            "gallery" -> "Hidden Gallery"
            else -> "Basic Calculator"
        }
        binding.calculatorModeValue.text = modeText
    }
    
    private fun displayAppVersion() {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionName = packageInfo.versionName
        binding.appVersion.text = versionName
        // Set Mathly version (hardcoded or from config)
        binding.mathlyVersion.text = "1.0"
    }
    
    private fun toggleAdvancedCustomization() {
        lifecycleScope.launch {
            val isExpanded = binding.advancedCustomizationContent.visibility == android.view.View.VISIBLE
            
            if (isExpanded) {
                // Collapse
                binding.advancedCustomizationContent.visibility = android.view.View.GONE
                binding.advancedCustomizationArrow.animate().rotation(180f).setDuration(200).start()
            } else {
                // Expand
                binding.advancedCustomizationContent.visibility = android.view.View.VISIBLE
                binding.advancedCustomizationArrow.animate().rotation(360f).setDuration(200).start()
            }
            
            setPrefBool(KEY_ADVANCED_CUSTOMIZATION_EXPANDED, !isExpanded)
        }
    }
    
    private fun updateBottomTabBarVisibility() {
        lifecycleScope.launch {
            val mathlyVoice = getPrefBool(KEY_MATHLY_VOICE, true)
            val mathlyChat = getPrefBool(KEY_MATHLY_CHAT, true)
            val mathlyScanner = getPrefBool(KEY_MATHLY_SCANNER, true)
            
            // If all three are disabled, hide the bottom tab bar
            val shouldHideTabBar = !mathlyVoice && !mathlyChat && !mathlyScanner
            
            // Send broadcast to update bottom tab bar visibility (explicit intent)
            val intent = android.content.Intent(this@SettingsActivity, TabBarVisibilityReceiver::class.java)
            intent.action = "com.shaadow.onecalculator.UPDATE_TAB_BAR_VISIBILITY"
            intent.putExtra("hide_tab_bar", shouldHideTabBar)
            intent.putExtra("mathly_voice_enabled", mathlyVoice)
            intent.putExtra("mathly_chat_enabled", mathlyChat)
            intent.putExtra("mathly_scanner_enabled", mathlyScanner)
            sendBroadcast(intent)
        }
    }

    private suspend fun getPrefBool(key: String, default: Boolean): Boolean {
        return withContext(Dispatchers.IO) {
            preferenceDao.getPreference(key)?.value?.toBooleanStrictOrNull() ?: default
        }
    }
    private suspend fun setPrefBool(key: String, value: Boolean) {
        withContext(Dispatchers.IO) {
            preferenceDao.setPreference(PreferenceEntity(key, value.toString()))
        }
    }
    private suspend fun getPrefString(key: String, default: String): String {
        return withContext(Dispatchers.IO) {
            preferenceDao.getPreference(key)?.value ?: default
        }
    }
    private suspend fun setPrefString(key: String, value: String) {
        withContext(Dispatchers.IO) {
            preferenceDao.setPreference(PreferenceEntity(key, value))
        }
    }
} 