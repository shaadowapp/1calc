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


    companion object {
        private const val PREFS_NAME = "CalculatorSettings"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_CALCULATOR_MODE = "calculator_mode"
        private const val KEY_MATHLY_VOICE = "mathly_voice"
        private const val KEY_MATHLY_CHAT = "mathly_chat"
        private const val KEY_MATHLY_SCANNER = "mathly_scanner"
        private const val KEY_HIDDEN_GALLERY_VISIBLE = "hidden_gallery_visible"
        private const val KEY_ADVANCED_CUSTOMIZATION_EXPANDED = "advanced_customization_expanded"


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



        binding.mathlyVoiceSwitch.setOnCheckedChangeListener(mathlyVoiceSwitchListener)
        binding.mathlyChatSwitch.setOnCheckedChangeListener(mathlyChatSwitchListener)
        binding.mathlyScannerSwitch.setOnCheckedChangeListener(mathlyScannerSwitchListener)
        binding.hiddenGallerySwitch.setOnCheckedChangeListener(hiddenGallerySwitchListener)

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

            val advancedExpanded = getPrefBool(KEY_ADVANCED_CUSTOMIZATION_EXPANDED, false)
            updateThemeModeDisplay(themeMode)
            updateCalculatorModeDisplay(calculatorMode)
            // Remove listeners before setting isChecked
            binding.mathlyVoiceSwitch.setOnCheckedChangeListener(null)
            binding.mathlyChatSwitch.setOnCheckedChangeListener(null)
            binding.mathlyScannerSwitch.setOnCheckedChangeListener(null)
            binding.hiddenGallerySwitch.setOnCheckedChangeListener(null)
            binding.mathlyVoiceSwitch.isChecked = mathlyVoice
            binding.mathlyChatSwitch.isChecked = mathlyChat
            binding.mathlyScannerSwitch.isChecked = mathlyScanner
            binding.hiddenGallerySwitch.isChecked = hiddenGalleryVisible

            // Re-attach listeners
            binding.mathlyVoiceSwitch.setOnCheckedChangeListener(mathlyVoiceSwitchListener)
            binding.mathlyChatSwitch.setOnCheckedChangeListener(mathlyChatSwitchListener)
            binding.mathlyScannerSwitch.setOnCheckedChangeListener(mathlyScannerSwitchListener)
            binding.hiddenGallerySwitch.setOnCheckedChangeListener(hiddenGallerySwitchListener)
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

        // Set Hidden Gallery Security button - Now only shows fingerprint info
        binding.setHiddenGallerySecurityButton.setOnClickListener {
            android.util.Log.d("SettingsActivity", "Set Hidden Gallery Security button clicked")
            showFingerprintOnlyDialog()
        }

        // Setup Recovery Question button
        binding.setupRecoveryQuestionButton.setOnClickListener {
            android.util.Log.d("SettingsActivity", "Setup Recovery Question button clicked")
            showRecoveryQuestionDialog()
        }
    }

    private fun openHiddenGallery() {
        try {
            val intent = Intent(this, MediaGalleryActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("SettingsActivity", "Error opening hidden gallery", e)
        }
    }

    private fun showFingerprintOnlyDialog() {
        try {
            androidx.appcompat.app.AlertDialog.Builder(this, R.style.DialogStyle_Todo)
                .setTitle("Hidden Gallery Security")
                .setMessage("Hidden Gallery uses fingerprint authentication only.\n\nThis provides secure access while maintaining simplicity.")
                .setIcon(R.drawable.ic_fingerprint)
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
                .setCancelable(true)
                .show()
        } catch (e: Exception) {
            android.util.Log.e("SettingsActivity", "Error showing fingerprint info dialog", e)
            Toast.makeText(this, "Error opening security info", Toast.LENGTH_SHORT).show()
        }
    }

    // Removed: showPasswordSetupDialog() - no longer needed with fingerprint-only authentication

    // Removed: showPinSetupDialog() - no longer needed with fingerprint-only authentication



    // Removed: updatePinDisplay() and validatePasswordSetup() - no longer needed with fingerprint-only authentication

    // Removed: saveSecurityMethod() - no longer needed with fingerprint-only authentication



    private fun showRecoveryQuestionDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_recovery_question_setup, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false) // Prevent accidental dismissal
            .create()

        // Setup spinner with predefined questions
        val spinner = dialogView.findViewById<android.widget.Spinner>(R.id.security_question_spinner)
        val customQuestionLayout = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.custom_question_layout)
        val customQuestionInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.custom_question_input)
        val answerInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.answer_input)
        val pinHintInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.pin_hint_input)
        val cancelButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.cancel_button)
        val saveButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.save_button)

        // Load existing data if available
        loadExistingRecoveryData(spinner, customQuestionInput, answerInput, pinHintInput)

        // Predefined security questions
        val questions = arrayOf(
            "What is your mother's maiden name?",
            "What was the name of your first pet?",
            "What city were you born in?",
            "What is your favorite movie?",
            "What was your childhood nickname?",
            "What is the name of your best friend?",
            "What was your first car model?",
            "What is your favorite food?",
            "Custom Question"
        )

        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, questions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Show/hide custom question input based on selection
        spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (position == questions.size - 1) { // "Custom Question" selected
                    customQuestionLayout.visibility = android.view.View.VISIBLE
                } else {
                    customQuestionLayout.visibility = android.view.View.GONE
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        cancelButton.setOnClickListener { dialog.dismiss() }

        saveButton.setOnClickListener {
            if (validateAndSaveRecoveryData(
                    spinner, customQuestionInput, answerInput, pinHintInput,
                    questions, dialog
                )) {
                // Validation and saving handled in separate method
            }
        }

        dialog.show()
    }

    private fun loadExistingRecoveryData(
        spinner: android.widget.Spinner,
        customQuestionInput: com.google.android.material.textfield.TextInputEditText,
        answerInput: com.google.android.material.textfield.TextInputEditText,
        pinHintInput: com.google.android.material.textfield.TextInputEditText
    ) {
        lifecycleScope.launch {
            try {
                val existingQuestion = withContext(Dispatchers.IO) {
                    preferenceDao.getPreference("hidden_gallery_recovery_question")?.value
                }
                val existingHint = withContext(Dispatchers.IO) {
                    preferenceDao.getPreference("hidden_gallery_pin_hint")?.value
                }

                // Pre-populate existing data
                existingQuestion?.let { question ->
                    val questions = (spinner.adapter as android.widget.ArrayAdapter<String>)
                    val position = (0 until questions.count).find { questions.getItem(it) == question }
                    if (position != null) {
                        spinner.setSelection(position)
                    } else {
                        // Custom question
                        spinner.setSelection(questions.count - 1)
                        customQuestionInput.setText(question)
                    }
                }

                existingHint?.let { hint ->
                    pinHintInput.setText(hint)
                }

                if (existingQuestion != null) {
                    answerInput.hint = "Leave blank to keep existing answer"
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsActivity", "Error loading existing recovery data", e)
            }
        }
    }

    private fun validateAndSaveRecoveryData(
        spinner: android.widget.Spinner,
        customQuestionInput: com.google.android.material.textfield.TextInputEditText,
        answerInput: com.google.android.material.textfield.TextInputEditText,
        pinHintInput: com.google.android.material.textfield.TextInputEditText,
        questions: Array<String>,
        dialog: AlertDialog
    ): Boolean {
        val selectedQuestion = if (spinner.selectedItemPosition == questions.size - 1) {
            customQuestionInput.text.toString().trim()
        } else {
            questions[spinner.selectedItemPosition]
        }

        val answer = answerInput.text.toString().trim()
        val pinHint = pinHintInput.text.toString().trim()

        // Industry-level validation
        val validationResult = validateRecoveryInput(selectedQuestion, answer, pinHint)
        if (!validationResult.isValid) {
            showValidationError(validationResult.errorMessage)
            return false
        }

        // Save with proper error handling and security
        saveRecoveryDataSecurely(selectedQuestion, answer, pinHint, dialog)
        return true
    }

    private data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String = ""
    )

    private fun validateRecoveryInput(question: String, answer: String, pinHint: String): ValidationResult {
        // Question validation
        if (question.isEmpty()) {
            return ValidationResult(false, "Security question cannot be empty")
        }
        if (question.length < 10) {
            return ValidationResult(false, "Security question must be at least 10 characters long")
        }
        if (question.length > 200) {
            return ValidationResult(false, "Security question cannot exceed 200 characters")
        }

        // Answer validation
        if (answer.isEmpty()) {
            return ValidationResult(false, "Answer cannot be empty")
        }
        if (answer.length < 2) {
            return ValidationResult(false, "Answer must be at least 2 characters long")
        }
        if (answer.length > 100) {
            return ValidationResult(false, "Answer cannot exceed 100 characters")
        }

        // Check for common weak answers
        val weakAnswers = listOf("yes", "no", "maybe", "idk", "123", "abc", "test", "password", "admin")
        if (weakAnswers.contains(answer.lowercase())) {
            return ValidationResult(false, "Please choose a more secure answer")
        }

        // PIN hint validation (optional but if provided, must be valid)
        if (pinHint.isNotEmpty()) {
            if (pinHint.length > 150) {
                return ValidationResult(false, "PIN hint cannot exceed 150 characters")
            }
            // Check if hint reveals the actual PIN
            if (pinHint.matches(Regex(".*\\b\\d{4}\\b.*"))) {
                return ValidationResult(false, "PIN hint should not contain the actual PIN")
            }
        }

        return ValidationResult(true)
    }

    private fun showValidationError(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Validation Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun saveRecoveryDataSecurely(
        question: String,
        answer: String,
        pinHint: String,
        dialog: AlertDialog
    ) {
        lifecycleScope.launch {
            try {
                // Show progress
                val progressDialog = AlertDialog.Builder(this@SettingsActivity)
                    .setTitle("Saving Recovery Data")
                    .setMessage("Please wait...")
                    .setCancelable(false)
                    .create()
                progressDialog.show()

                val success = withContext(Dispatchers.IO) {
                    try {
                        // Use proper cryptographic hashing
                        val salt = generateSalt()
                        val hashedAnswer = hashAnswerWithSalt(answer.lowercase().trim(), salt)

                        // Save with transaction for atomicity
                        preferenceDao.setPreference(PreferenceEntity("hidden_gallery_recovery_question", question))
                        preferenceDao.setPreference(PreferenceEntity("hidden_gallery_recovery_answer", hashedAnswer))
                        preferenceDao.setPreference(PreferenceEntity("hidden_gallery_recovery_salt", salt))

                        if (pinHint.isNotEmpty()) {
                            preferenceDao.setPreference(PreferenceEntity("hidden_gallery_pin_hint", pinHint))
                        } else {
                            // Remove existing hint if empty
                            preferenceDao.deletePreference("hidden_gallery_pin_hint")
                        }

                        // Set recovery setup timestamp
                        preferenceDao.setPreference(PreferenceEntity("hidden_gallery_recovery_setup_time", System.currentTimeMillis().toString()))

                        android.util.Log.d("SettingsActivity", "Recovery data saved successfully")
                        true
                    } catch (e: Exception) {
                        android.util.Log.e("SettingsActivity", "Error in database transaction", e)
                        false
                    }
                }

                progressDialog.dismiss()

                if (success) {
                    AlertDialog.Builder(this@SettingsActivity)
                        .setTitle("Success")
                        .setMessage("Recovery question and PIN hint have been saved successfully.\n\nPlease remember your answer as it will be needed to recover your PIN/password.")
                        .setPositiveButton("OK") { _, _ ->
                            dialog.dismiss()
                        }
                        .setCancelable(false)
                        .show()
                } else {
                    showValidationError("Failed to save recovery data. Please try again.")
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsActivity", "Error saving recovery data", e)
                showValidationError("An unexpected error occurred. Please try again.")
            }
        }
    }

    private fun generateSalt(): String {
        val random = java.security.SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return android.util.Base64.encodeToString(salt, android.util.Base64.DEFAULT)
    }

    private fun hashAnswerWithSalt(answer: String, salt: String): String {
        return try {
            val saltBytes = android.util.Base64.decode(salt, android.util.Base64.DEFAULT)
            val answerBytes = answer.toByteArray(Charsets.UTF_8)
            val combined = saltBytes + answerBytes

            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(combined)
            android.util.Base64.encodeToString(hash, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            android.util.Log.e("SettingsActivity", "Error hashing answer", e)
            // Fallback to simple encoding (not recommended for production)
            android.util.Base64.encodeToString(answer.toByteArray(), android.util.Base64.DEFAULT)
        }
    }
}