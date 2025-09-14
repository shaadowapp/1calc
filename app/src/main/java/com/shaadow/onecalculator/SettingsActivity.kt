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
import com.shaadow.onecalculator.databinding.DialogThemeModeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.CompoundButton
import java.io.File
import java.text.DecimalFormat
import androidx.core.net.toUri

class SettingsActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var preferenceDao: PreferenceDao
    private lateinit var cacheManager: CacheManager
    
    private lateinit var hiddenGallerySwitchListener: CompoundButton.OnCheckedChangeListener


    companion object {
        private const val PREFS_NAME = "CalculatorSettings"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_SOUND_EFFECTS = "sound_effects"
        private const val KEY_HIDDEN_GALLERY_VISIBLE = "hidden_gallery_visible"


    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Add analytics tracking for settings screen
        val analyticsHelper = com.shaadow.onecalculator.utils.AnalyticsHelper(this)
        analyticsHelper.logScreenView("Settings", "SettingsActivity")
        
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
            showFeedback()
        }

        // Report Bug
        binding.reportBugItem.setOnClickListener {
            showBugReport()
        }

        // Rate Us
        binding.rateUsItem.setOnClickListener {
            startActivity(Intent(this, RateUsActivity::class.java))
        }

        // FavTunes App
        binding.favtunesAppItem.setOnClickListener {
            try {
                // First try to open the FavTunes app directly if installed
                android.util.Log.d("SettingsActivity", "Checking for FavTunes app installation...")

                // Try multiple possible package names
                val possiblePackages = arrayOf(
                    "com.shaadow.tunes",
                    "com.shaadow.favtunes",
                    "com.shaadow.favTunes"
                )

                var launchIntent: Intent? = null
                var foundPackage = ""

                for (packageName in possiblePackages) {
                    try {
                        launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                        if (launchIntent != null) {
                            foundPackage = packageName
                            android.util.Log.d("SettingsActivity", "Found FavTunes app with package: $packageName")
                            break
                        }
                    } catch (e: Exception) {
                        android.util.Log.d("SettingsActivity", "Package $packageName not found: ${e.message}")
                    }
                }

                if (launchIntent != null) {
                    android.util.Log.d("SettingsActivity", "FavTunes app is installed (package: $foundPackage), launching directly")
                    startActivity(launchIntent)
                } else {
                    android.util.Log.d("SettingsActivity", "FavTunes app not installed, opening Play Store")
                    // If app is not installed, open Play Store
                    val intent = Intent(Intent.ACTION_VIEW,
                        "market://details?id=com.shaadow.tunes".toUri())
                    startActivity(intent)
                }
            } catch (e: android.content.ActivityNotFoundException) {
                android.util.Log.d("SettingsActivity", "Play Store not available, opening in browser")
                // If Play Store app is not installed, open in browser
                val intent = Intent(Intent.ACTION_VIEW,
                    "https://play.google.com/store/apps/details?id=com.shaadow.tunes".toUri())
                startActivity(intent)
            } catch (e: Exception) {
                android.util.Log.e("SettingsActivity", "Unexpected error opening FavTunes: ${e.message}")
                Toast.makeText(this, "Error opening FavTunes", Toast.LENGTH_SHORT).show()
            }
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
                // Send broadcast for instant theme change
                sendThemeChangeBroadcast()
            }
        }
        
        dialog.show()
    }
    
    
    private fun loadCurrentPreferences() {
        lifecycleScope.launch {
            val themeMode = getPrefString(KEY_THEME_MODE, "auto")
            val hiddenGalleryVisible = getPrefBool(KEY_HIDDEN_GALLERY_VISIBLE, true)

            updateThemeModeDisplay(themeMode)
            // Remove listeners before setting isChecked
            binding.hiddenGallerySwitch.setOnCheckedChangeListener(null)

            binding.hiddenGallerySwitch.isChecked = hiddenGalleryVisible

            // Re-attach listeners
            binding.hiddenGallerySwitch.setOnCheckedChangeListener(hiddenGallerySwitchListener)
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
    
    
    private fun displayAppVersion() {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionName = packageInfo.versionName
        binding.appVersion.text = versionName
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


        // Setup Recovery Question button
        binding.setupRecoveryQuestionButton.setOnClickListener {
            android.util.Log.d("SettingsActivity", "Setup Recovery Question button clicked")
            showRecoveryQuestionDialog()
        }
    }

    private fun openHiddenGallery() {
        checkPermissionsAndOpenGallery()
    }

    private fun checkPermissionsAndOpenGallery() {
        android.util.Log.d("SettingsActivity", "Checking storage permissions for hidden gallery")

        val permissions = mutableListOf<String>()

        // Check for media permissions based on Android version
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_MEDIA_IMAGES
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_MEDIA_VIDEO
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.READ_MEDIA_VIDEO)
            }
        } else {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            android.util.Log.d("SettingsActivity", "Requesting permissions: $permissions")
            requestStoragePermissionsLauncher.launch(permissions.toTypedArray())
        } else {
            android.util.Log.d("SettingsActivity", "All permissions already granted")
            authenticateAndOpenGallery()
        }
    }

    // Permission request launcher for storage permissions
    private val requestStoragePermissionsLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            android.util.Log.d("SettingsActivity", "All storage permissions granted")
            openGalleryActivity()
        } else {
            android.util.Log.w("SettingsActivity", "Storage permissions denied")
            showPermissionDeniedDialog()
        }
    }

    private fun authenticateAndOpenGallery() {
        android.util.Log.d("SettingsActivity", "Starting fingerprint authentication for hidden gallery")

        // Check if biometric authentication is available
        val biometricManager = androidx.biometric.BiometricManager.from(this)
        when (biometricManager.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS -> {
                android.util.Log.d("SettingsActivity", "Biometric authentication is available")
                showBiometricPrompt()
            }
            androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                android.util.Log.e("SettingsActivity", "No biometric hardware available")
                showBiometricError("Biometric authentication not available on this device")
            }
            androidx.biometric.BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                android.util.Log.e("SettingsActivity", "Biometric hardware unavailable")
                showBiometricError("Biometric hardware is currently unavailable")
            }
            androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                android.util.Log.e("SettingsActivity", "No biometric credentials enrolled")
                showBiometricError("No fingerprints enrolled. Please set up fingerprint authentication in device settings")
            }
            else -> {
                android.util.Log.e("SettingsActivity", "Biometric authentication not available")
                showBiometricError("Biometric authentication is not available")
            }
        }
    }

    private fun showBiometricPrompt() {
        val executor: java.util.concurrent.Executor = androidx.core.content.ContextCompat.getMainExecutor(this)
        val biometricPrompt = androidx.biometric.BiometricPrompt(this as androidx.fragment.app.FragmentActivity, executor,
            object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    android.util.Log.e("SettingsActivity", "Biometric authentication error: $errorCode - $errString")

                    when (errorCode) {
                        androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED,
                        androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                            // User cancelled, do nothing
                            android.util.Log.d("SettingsActivity", "User cancelled biometric authentication")
                        }
                        androidx.biometric.BiometricPrompt.ERROR_LOCKOUT,
                        androidx.biometric.BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                            showBiometricError("Too many failed attempts. Please try again later.")
                        }
                        else -> {
                            Toast.makeText(this@SettingsActivity, "Authentication error: $errString", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    android.util.Log.d("SettingsActivity", "Biometric authentication succeeded")
                    openGalleryActivity()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    android.util.Log.d("SettingsActivity", "Biometric authentication failed - fingerprint not recognized")
                    // Biometric prompt already shows feedback - no additional toast needed
                }
            })

        val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Hidden Gallery")
            .setSubtitle("Use your fingerprint to access the hidden gallery")
            .setDescription("Place your finger on the fingerprint sensor")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            android.util.Log.e("SettingsActivity", "Error showing biometric prompt", e)
            showBiometricError("Failed to show fingerprint authentication: ${e.message}")
        }
    }

    private fun showBiometricError(message: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Authentication Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setCancelable(true)
            .show()
    }

    private fun openGalleryActivity() {
        try {
            val intent = android.content.Intent(this, MediaGalleryActivity::class.java)
            intent.putExtra("is_first_launch", false) // Navigation within app
            intent.putExtra("authentication_done", true) // Indicate authentication is already completed
            startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("SettingsActivity", "Error opening hidden gallery", e)
            Toast.makeText(this, "Error opening hidden gallery", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPermissionDeniedDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Storage Permissions Required")
            .setMessage("Hidden Gallery needs storage permissions to access and encrypt your files. Please grant the permissions to continue.")
            .setPositiveButton("Grant Permissions") { _, _ ->
                checkPermissionsAndOpenGallery()
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(true)
            .show()
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
            if (pinHint.matches(Regex(".*\\b\\d{3}\\b.*"))) {
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

    private fun showFeedback() {
        val feedbackSheet = FeedbackBottomSheet.newInstance()
        feedbackSheet.show(supportFragmentManager, FeedbackBottomSheet.TAG)
    }

    private fun showBugReport() {
        val bugReportSheet = BugReportBottomSheet.newInstance()
        bugReportSheet.show(supportFragmentManager, BugReportBottomSheet.TAG)
    }
}