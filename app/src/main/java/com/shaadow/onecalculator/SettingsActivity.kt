package com.shaadow.onecalculator

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.shaadow.onecalculator.databinding.ActivitySettingsBinding
import com.shaadow.onecalculator.databinding.DialogCalculatorModeBinding
import com.shaadow.onecalculator.databinding.DialogThemeModeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.CompoundButton
import java.io.File
import java.text.DecimalFormat

class SettingsActivity : androidx.fragment.app.FragmentActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var preferenceDao: PreferenceDao
    private lateinit var cacheManager: CacheManager
    
    private lateinit var mathlyVoiceSwitchListener: CompoundButton.OnCheckedChangeListener
    private lateinit var mathlyChatSwitchListener: CompoundButton.OnCheckedChangeListener
    private lateinit var mathlyScannerSwitchListener: CompoundButton.OnCheckedChangeListener
    private lateinit var hiddenGallerySwitchListener: CompoundButton.OnCheckedChangeListener
    private lateinit var fingerprintUnlockSwitchListener: CompoundButton.OnCheckedChangeListener

    companion object {
        private const val PREFS_NAME = "CalculatorSettings"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_CALCULATOR_MODE = "calculator_mode"
        private const val KEY_MATHLY_VOICE = "mathly_voice"
        private const val KEY_MATHLY_CHAT = "mathly_chat"
        private const val KEY_MATHLY_SCANNER = "mathly_scanner"
        private const val KEY_HIDDEN_GALLERY_VISIBLE = "hidden_gallery_visible"
        private const val KEY_ADVANCED_CUSTOMIZATION_EXPANDED = "advanced_customization_expanded"

        // Broadcast constants
        const val ACTION_FINGERPRINT_SETTING_CHANGED = "com.shaadow.onecalculator.FINGERPRINT_SETTING_CHANGED"
        const val EXTRA_FINGERPRINT_ENABLED = "fingerprint_enabled"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        preferenceDao = HistoryDatabase.getInstance(this).preferenceDao()
        cacheManager = CacheManager(this)
        
        setupSwitchListeners()
        setupClickListeners()
        loadCurrentPreferences()
        displayAppVersion()
        updateCacheSizeDisplay()
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

        hiddenGallerySwitchListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                android.util.Log.d("SettingsActivity", "Hidden gallery switch toggled: $isChecked")

                // Save preference to database - real-time observation will handle immediate UI updates
                withContext(Dispatchers.IO) {
                    preferenceDao.setPreference(PreferenceEntity(KEY_HIDDEN_GALLERY_VISIBLE, isChecked.toString()))
                }

                // Show user feedback
                Toast.makeText(this@SettingsActivity, "Hidden Gallery button ${if (isChecked) "shown" else "hidden"}", Toast.LENGTH_SHORT).show()
            }
        }

        fingerprintUnlockSwitchListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                android.util.Log.d("SettingsActivity", "Fingerprint unlock switch toggled: $isChecked")

                if (isChecked) {
                    // Check if biometric authentication is available before enabling
                    val biometricManager = BiometricManager.from(this@SettingsActivity)
                    when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
                        BiometricManager.BIOMETRIC_SUCCESS -> {
                            // Biometric authentication is available, save preference
                            withContext(Dispatchers.IO) {
                                preferenceDao.setPreference(PreferenceEntity("hidden_gallery_fingerprint_enabled", "true"))
                            }
                            // Send broadcast to notify other components
                            sendFingerprintSettingBroadcast(true)
                            Toast.makeText(this@SettingsActivity, "Fingerprint unlock enabled", Toast.LENGTH_SHORT).show()
                        }
                        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                            binding.fingerprintUnlockSwitch.setOnCheckedChangeListener(null)
                            binding.fingerprintUnlockSwitch.isChecked = false
                            binding.fingerprintUnlockSwitch.setOnCheckedChangeListener(fingerprintUnlockSwitchListener)
                            Toast.makeText(this@SettingsActivity, "No biometric features available on this device", Toast.LENGTH_SHORT).show()
                        }
                        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                            binding.fingerprintUnlockSwitch.setOnCheckedChangeListener(null)
                            binding.fingerprintUnlockSwitch.isChecked = false
                            binding.fingerprintUnlockSwitch.setOnCheckedChangeListener(fingerprintUnlockSwitchListener)
                            Toast.makeText(this@SettingsActivity, "Biometric features are currently unavailable", Toast.LENGTH_SHORT).show()
                        }
                        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                            binding.fingerprintUnlockSwitch.setOnCheckedChangeListener(null)
                            binding.fingerprintUnlockSwitch.isChecked = false
                            binding.fingerprintUnlockSwitch.setOnCheckedChangeListener(fingerprintUnlockSwitchListener)
                            Toast.makeText(this@SettingsActivity, "No fingerprints enrolled. Please add fingerprints in device settings", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            binding.fingerprintUnlockSwitch.setOnCheckedChangeListener(null)
                            binding.fingerprintUnlockSwitch.isChecked = false
                            binding.fingerprintUnlockSwitch.setOnCheckedChangeListener(fingerprintUnlockSwitchListener)
                            Toast.makeText(this@SettingsActivity, "Biometric authentication is not supported", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // Disable fingerprint unlock
                    withContext(Dispatchers.IO) {
                        preferenceDao.setPreference(PreferenceEntity("hidden_gallery_fingerprint_enabled", "false"))
                    }
                    // Send broadcast to notify other components
                    sendFingerprintSettingBroadcast(false)
                    Toast.makeText(this@SettingsActivity, "Fingerprint unlock disabled", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.mathlyVoiceSwitch.setOnCheckedChangeListener(mathlyVoiceSwitchListener)
        binding.mathlyChatSwitch.setOnCheckedChangeListener(mathlyChatSwitchListener)
        binding.mathlyScannerSwitch.setOnCheckedChangeListener(mathlyScannerSwitchListener)
        binding.hiddenGallerySwitch.setOnCheckedChangeListener(hiddenGallerySwitchListener)
        binding.fingerprintUnlockSwitch.setOnCheckedChangeListener(fingerprintUnlockSwitchListener)

        // Setup hidden gallery buttons
        setupHiddenGalleryButtons()
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
        
        // Clear Cache
        binding.clearCacheItem.setOnClickListener {
            showClearCacheDialog()
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
            val hiddenGalleryVisible = getPrefBool(KEY_HIDDEN_GALLERY_VISIBLE, true)
            val fingerprintEnabled = getPrefBool("hidden_gallery_fingerprint_enabled", false)
            val advancedExpanded = getPrefBool(KEY_ADVANCED_CUSTOMIZATION_EXPANDED, false)
            updateThemeModeDisplay(themeMode)
            updateCalculatorModeDisplay(calculatorMode)
            // Remove listeners before setting isChecked
            binding.mathlyVoiceSwitch.setOnCheckedChangeListener(null)
            binding.mathlyChatSwitch.setOnCheckedChangeListener(null)
            binding.mathlyScannerSwitch.setOnCheckedChangeListener(null)
            binding.hiddenGallerySwitch.setOnCheckedChangeListener(null)
            binding.fingerprintUnlockSwitch.setOnCheckedChangeListener(null)
            binding.mathlyVoiceSwitch.isChecked = mathlyVoice
            binding.mathlyChatSwitch.isChecked = mathlyChat
            binding.mathlyScannerSwitch.isChecked = mathlyScanner
            binding.hiddenGallerySwitch.isChecked = hiddenGalleryVisible
            binding.fingerprintUnlockSwitch.isChecked = fingerprintEnabled
            // Re-attach listeners
            binding.mathlyVoiceSwitch.setOnCheckedChangeListener(mathlyVoiceSwitchListener)
            binding.mathlyChatSwitch.setOnCheckedChangeListener(mathlyChatSwitchListener)
            binding.mathlyScannerSwitch.setOnCheckedChangeListener(mathlyScannerSwitchListener)
            binding.hiddenGallerySwitch.setOnCheckedChangeListener(hiddenGallerySwitchListener)
            binding.fingerprintUnlockSwitch.setOnCheckedChangeListener(fingerprintUnlockSwitchListener)
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
    
    // Cache Management Methods
    private fun showClearCacheDialog() {
        lifecycleScope.launch {
            val cacheSize = cacheManager.calculateTotalCacheSize()
            val formattedSize = cacheManager.formatFileSize(cacheSize)

            AlertDialog.Builder(this@SettingsActivity)
                .setTitle("Clear Cache")
                .setMessage("This will clear all cached data including temporary files, images, and app data. This action cannot be undone.\n\nCurrent cache size: $formattedSize")
                .setPositiveButton("Clear Cache") { _, _ ->
                    lifecycleScope.launch {
                        val result = cacheManager.clearAllCache()
                        val freed = cacheManager.formatFileSize(result.totalSize)
                        val message = if (result.totalSize > 0) {
                            "Cache cleared successfully!\nFreed: $freed\nFiles: ${result.clearedFiles}\nDirectories: ${result.clearedDirectories}"
                        } else {
                            "Cache is already clean!"
                        }
                        Toast.makeText(this@SettingsActivity, message, Toast.LENGTH_LONG).show()
                        updateCacheSizeDisplay()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun updateCacheSizeDisplay() {
        lifecycleScope.launch {
            val cacheSize = cacheManager.calculateTotalCacheSize()
            val formattedSize = cacheManager.formatFileSize(cacheSize)
            binding.cacheSizeText.text = formattedSize
        }
    }

    private fun setupHiddenGalleryButtons() {
        // Open Hidden Gallery button
        binding.openHiddenGalleryButton.setOnClickListener {
            android.util.Log.d("SettingsActivity", "Open Hidden Gallery button clicked")
            openHiddenGallery()
        }

        // Set Hidden Gallery Security button
        binding.setHiddenGallerySecurityButton.setOnClickListener {
            android.util.Log.d("SettingsActivity", "Set Hidden Gallery Security button clicked")
            showSecurityOptionsDialog()
        }
    }

    private fun openHiddenGallery() {
        try {
            val intent = Intent(this, MediaGalleryActivity::class.java)
            startActivity(intent)
            Toast.makeText(this, "Opening Hidden Gallery...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.util.Log.e("SettingsActivity", "Error opening hidden gallery", e)
            Toast.makeText(this, "Error opening Hidden Gallery", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSecurityOptionsDialog() {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_security_options, null)
            val dialog = androidx.appcompat.app.AlertDialog.Builder(this, R.style.DialogStyle_Todo)
                .setView(dialogView)
                .setCancelable(true)
                .create()

            val passwordOption = dialogView.findViewById<LinearLayout>(R.id.password_option)
            val pinOption = dialogView.findViewById<LinearLayout>(R.id.pin_option)
            val cancelButton = dialogView.findViewById<android.widget.Button>(R.id.cancel_button)

            passwordOption.setOnClickListener {
                dialog.dismiss()
                showPasswordSetupDialog()
            }

            pinOption.setOnClickListener {
                dialog.dismiss()
                showPinSetupDialog()
            }

            cancelButton.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        } catch (e: Exception) {
            android.util.Log.e("SettingsActivity", "Error showing security options dialog", e)
            Toast.makeText(this, "Error opening security options", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPasswordSetupDialog() {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
            val dialog = androidx.appcompat.app.AlertDialog.Builder(this, R.style.DialogStyle_Todo)
                .setView(dialogView)
                .setCancelable(true)
                .setOnCancelListener {
                    // Go back to security options when dialog is cancelled
                    showSecurityOptionsDialog()
                }
                .create()

            val passwordInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.password_input)
            val confirmPasswordInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.confirm_password_input)
            val setButton = dialogView.findViewById<android.widget.Button>(R.id.set_password_button)
            val cancelButton = dialogView.findViewById<android.widget.Button>(R.id.cancel_button)
            val backButton = dialogView.findViewById<android.widget.ImageView>(R.id.back_button)

            setButton.setOnClickListener {
                val password = passwordInput.text.toString()
                val confirmPassword = confirmPasswordInput.text.toString()

                if (validatePasswordSetup(password, confirmPassword)) {
                    saveSecurityMethod("password", password)
                    dialog.dismiss()
                    Toast.makeText(this, "Password security enabled", Toast.LENGTH_SHORT).show()
                }
            }

            backButton.setOnClickListener {
                dialog.dismiss()
                // Go back to security options dialog
                showSecurityOptionsDialog()
            }

            cancelButton.setOnClickListener {
                dialog.dismiss()
                // Go back to security options dialog
                showSecurityOptionsDialog()
            }

            dialog.show()
        } catch (e: Exception) {
            android.util.Log.e("SettingsActivity", "Error showing password setup dialog", e)
            Toast.makeText(this, "Error opening password setup", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPinSetupDialog() {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_set_pin, null)
            val dialog = androidx.appcompat.app.AlertDialog.Builder(this, R.style.DialogStyle_Todo)
                .setView(dialogView)
                .setCancelable(true)
                .setOnCancelListener {
                    // Go back to security options when dialog is cancelled
                    showSecurityOptionsDialog()
                }
                .create()

            val pinDigits = arrayOf(
                dialogView.findViewById<android.widget.TextView>(R.id.pin_digit_1),
                dialogView.findViewById<android.widget.TextView>(R.id.pin_digit_2),
                dialogView.findViewById<android.widget.TextView>(R.id.pin_digit_3),
                dialogView.findViewById<android.widget.TextView>(R.id.pin_digit_4)
            )

            val setPinButton = dialogView.findViewById<android.widget.Button>(R.id.set_pin_button)
            val cancelButton = dialogView.findViewById<android.widget.Button>(R.id.cancel_button)
            val backButton = dialogView.findViewById<android.widget.ImageView>(R.id.back_button)

            var currentPin = ""

            // Setup number buttons
            val numberButtons = arrayOf(
                dialogView.findViewById<android.widget.Button>(R.id.pin_0),
                dialogView.findViewById<android.widget.Button>(R.id.pin_1),
                dialogView.findViewById<android.widget.Button>(R.id.pin_2),
                dialogView.findViewById<android.widget.Button>(R.id.pin_3),
                dialogView.findViewById<android.widget.Button>(R.id.pin_4),
                dialogView.findViewById<android.widget.Button>(R.id.pin_5),
                dialogView.findViewById<android.widget.Button>(R.id.pin_6),
                dialogView.findViewById<android.widget.Button>(R.id.pin_7),
                dialogView.findViewById<android.widget.Button>(R.id.pin_8),
                dialogView.findViewById<android.widget.Button>(R.id.pin_9)
            )

            numberButtons.forEachIndexed { index, button ->
                button.setOnClickListener {
                    if (currentPin.length < 4) {
                        currentPin += if (index == 0) "0" else index.toString()
                        updatePinDisplay(pinDigits, currentPin)
                        setPinButton.isEnabled = currentPin.length == 4
                    }
                }
            }

            dialogView.findViewById<android.widget.Button>(R.id.pin_backspace).setOnClickListener {
                if (currentPin.isNotEmpty()) {
                    currentPin = currentPin.dropLast(1)
                    updatePinDisplay(pinDigits, currentPin)
                    setPinButton.isEnabled = currentPin.length == 4
                }
            }

            setPinButton.setOnClickListener {
                if (currentPin.length == 4) {
                    saveSecurityMethod("pin", currentPin)
                    dialog.dismiss()
                    Toast.makeText(this, "PIN security enabled", Toast.LENGTH_SHORT).show()
                }
            }

            backButton.setOnClickListener {
                dialog.dismiss()
                // Go back to security options dialog
                showSecurityOptionsDialog()
            }

            cancelButton.setOnClickListener {
                dialog.dismiss()
                // Go back to security options dialog
                showSecurityOptionsDialog()
            }

            dialog.show()
        } catch (e: Exception) {
            android.util.Log.e("SettingsActivity", "Error showing PIN setup dialog", e)
            Toast.makeText(this, "Error opening PIN setup", Toast.LENGTH_SHORT).show()
        }
    }



    private fun updatePinDisplay(pinDigits: Array<android.widget.TextView>, pin: String) {
        pinDigits.forEachIndexed { index, textView ->
            textView.text = if (index < pin.length) "●" else ""
        }
    }

    private fun validatePasswordSetup(password: String, confirmPassword: String): Boolean {
        if (password.length < 4) {
            Toast.makeText(this, "Password must be at least 4 characters", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun saveSecurityMethod(method: String, value: String) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Save security method type
                    preferenceDao.setPreference(PreferenceEntity("hidden_gallery_security_method", method))

                    // Save security value (hashed for password/pin, just enabled for fingerprint)
                    val hashedValue = if (method == "fingerprint") {
                        "enabled"
                    } else {
                        // Simple hash for demo - in production use proper hashing
                        android.util.Base64.encodeToString(value.toByteArray(), android.util.Base64.DEFAULT)
                    }
                    preferenceDao.setPreference(PreferenceEntity("hidden_gallery_security_value", hashedValue))
                }

                android.util.Log.d("SettingsActivity", "Security method saved: $method")
            } catch (e: Exception) {
                android.util.Log.e("SettingsActivity", "Error saving security method", e)
                Toast.makeText(this@SettingsActivity, "Error saving security settings", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendFingerprintSettingBroadcast(enabled: Boolean) {
        val intent = Intent(ACTION_FINGERPRINT_SETTING_CHANGED)
        intent.putExtra(EXTRA_FINGERPRINT_ENABLED, enabled)
        intent.setPackage(packageName) // Ensure it's sent within the app
        sendBroadcast(intent)
        android.util.Log.d("SettingsActivity", "Fingerprint setting broadcast sent: $enabled")
    }
}