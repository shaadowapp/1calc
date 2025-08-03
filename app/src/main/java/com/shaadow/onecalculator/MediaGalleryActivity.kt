package com.shaadow.onecalculator

import android.Manifest
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.shaadow.onecalculator.databinding.ActivityMediaGalleryBinding
import com.shaadow.onecalculator.utils.EncryptionUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executor
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.text.Charsets

class MediaGalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMediaGalleryBinding
    private lateinit var folderAdapter: EncryptedFolderAdapter
    private lateinit var database: HistoryDatabase
    private lateinit var encryptedFolderDao: EncryptedFolderDao

    // No longer needed since fingerprint button is now in keypad

    // Broadcast receiver for fingerprint setting changes
    private val fingerprintSettingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == SettingsActivity.ACTION_FINGERPRINT_SETTING_CHANGED) {
                val enabled = intent.getBooleanExtra(SettingsActivity.EXTRA_FINGERPRINT_ENABLED, false)
                android.util.Log.d("MediaGalleryActivity", "Received fingerprint setting broadcast: $enabled")
                updateFingerprintSectionVisibility(enabled)
            }
        }
    }

    // Permission launcher for media access
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            setupFolderList()
        } else {
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        initializeDatabase()

        // Register broadcast receiver for fingerprint setting changes
        val filter = IntentFilter(SettingsActivity.ACTION_FINGERPRINT_SETTING_CHANGED)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(fingerprintSettingReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(fingerprintSettingReceiver, filter)
        }

        // Check authentication before proceeding
        checkAuthentication()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister broadcast receiver
        try {
            unregisterReceiver(fingerprintSettingReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver was not registered
            android.util.Log.w("MediaGalleryActivity", "Broadcast receiver was not registered")
        }

        // Hide any open lock screen
        hideLockScreen()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun initializeDatabase() {
        database = HistoryDatabase.getInstance(this)
        encryptedFolderDao = database.encryptedFolderDao()
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        
        // Check for media permissions based on Android version
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        } else {
            setupFolderList()
        }
    }

    private fun setupFolderList() {
        folderAdapter = EncryptedFolderAdapter(
            onFolderClick = { folder ->
                // TODO: Open folder contents (implement in next phase)
                Toast.makeText(this, "Opening ${folder.name}", Toast.LENGTH_SHORT).show()
            },
            onFolderLongClick = { folder ->
                showFolderOptionsDialog(folder)
            },
            onFolderMenuClick = { folder, view ->
                showFolderPopupMenu(folder, view)
            },
            onAddFolderClick = {
                showCreateFolderDialog()
            }
        )

        binding.foldersRecyclerView.apply {
            layoutManager = GridLayoutManager(this@MediaGalleryActivity, 2)
            adapter = folderAdapter
        }

        // Check if we need to create default folders first
        lifecycleScope.launch {
            val existingFolders = withContext(Dispatchers.IO) {
                encryptedFolderDao.getAllFolders()
            }

            // Observe folders from database
            existingFolders.collect { folders ->
                if (folders.isEmpty()) {
                    // Create default folders on first access
                    withContext(Dispatchers.IO) {
                        createDefaultFolders()
                    }
                } else {
                    folderAdapter.submitFoldersWithAddButton(folders)
                    updateEmptyState(false)
                }
            }
        }
    }

    private fun checkAuthentication() {
        lifecycleScope.launch {
            val preferenceDao = database.preferenceDao()
            val securityMethod = withContext(Dispatchers.IO) {
                preferenceDao.getPreference("hidden_gallery_security_method")?.value
            }

            if (securityMethod != null) {
                // Security is set up, show lock screen
                showLockScreen(securityMethod)
            } else {
                // No security set up, set default PIN "0000" and show lock screen
                withContext(Dispatchers.IO) {
                    preferenceDao.setPreference(PreferenceEntity("hidden_gallery_security_method", "pin"))
                    val defaultPinHash = android.util.Base64.encodeToString("0000".toByteArray(), android.util.Base64.DEFAULT)
                    preferenceDao.setPreference(PreferenceEntity("hidden_gallery_security_value", defaultPinHash))
                }
                showLockScreen("pin")
            }
        }
    }

    private fun showLockScreen(securityMethod: String) {
        // Show the lock screen overlay
        val lockScreenOverlay = binding.lockScreenOverlay.root
        lockScreenOverlay.visibility = View.VISIBLE

        // Make activity full screen
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        // No longer needed since fingerprint button is now in keypad

        // Configure UI based on stored security method
        lifecycleScope.launch {
            val storedSecurityMethod = withContext(Dispatchers.IO) {
                database.preferenceDao().getPreference("hidden_gallery_security_method")?.value
            }

            when (storedSecurityMethod) {
                "password" -> setupPasswordLockScreen()
                "pin" -> setupPinLockScreen()
                else -> {
                    // Fallback to PIN (should not happen after our default setup)
                    setupPinLockScreen()
                }
            }
        }

        // Hide system UI for true full screen
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                window.insetsController?.let { controller ->
                    controller.hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                    controller.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                    android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
            }
        } catch (e: Exception) {
            android.util.Log.w("MediaGalleryActivity", "Failed to hide system UI: ${e.message}")
        }
    }

    private fun setupPasswordLockScreen() {
        val overlay = binding.lockScreenOverlay

        // Show password UI, hide PIN UI
        overlay.passwordInputLayout.visibility = View.VISIBLE
        overlay.pinDisplayLayout.visibility = View.GONE
        overlay.pinKeypadLayout.visibility = View.GONE
        overlay.actionButtonsLayout.visibility = View.VISIBLE
        overlay.lockSubtitle.text = "Enter your password to continue"

        // Always show fingerprint button to maintain layout, but enable/disable based on settings
        lifecycleScope.launch {
            val fingerprintEnabled = withContext(Dispatchers.IO) {
                database.preferenceDao().getPreference("hidden_gallery_fingerprint_enabled")?.value?.toBooleanStrictOrNull() ?: false
            }

            // Always keep button visible to maintain layout
            overlay.fingerprintButton.visibility = View.VISIBLE

            if (fingerprintEnabled) {
                // Enable fingerprint button and setup authentication
                overlay.fingerprintButton.isEnabled = true
                overlay.fingerprintButton.alpha = 1.0f
                setupFingerprintAuthentication(overlay.fingerprintButton)
            } else {
                // Disable fingerprint button but keep it visible for layout consistency
                overlay.fingerprintButton.isEnabled = false
                overlay.fingerprintButton.alpha = 0.3f
                // Set click listener to show toast when disabled
                overlay.fingerprintButton.setOnClickListener {
                    Toast.makeText(this@MediaGalleryActivity, "Fingerprint authentication is disabled", Toast.LENGTH_SHORT).show()
                }
            }
        }

        overlay.submitButton.setOnClickListener {
            val enteredPassword = overlay.passwordInput.text.toString()
            if (enteredPassword.isNotEmpty()) {
                verifyPassword(enteredPassword)
            } else {
                Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show()
            }
        }

        overlay.cancelButton.setOnClickListener {
            hideLockScreen()
            finish()
        }

        overlay.backButton.setOnClickListener {
            hideLockScreen()
            finish()
        }

        // Setup forgot password button (for password mode)
        overlay.forgotPasswordButton.setOnClickListener {
            showRecoveryDialog()
        }

        // Show forgot password button, hide forgot PIN button
        overlay.forgotPasswordButton.visibility = View.VISIBLE
        overlay.forgotPinButton.visibility = View.GONE
    }

    private fun setupClickListeners() {
        binding.fabCreateFolder.setOnClickListener {
            showCreateFolderDialog()
        }

        binding.createFirstFolderBtn.setOnClickListener {
            showCreateFolderDialog()
        }

        // Setup toolbar menu
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_select_all -> {
                    selectAllFolders()
                    true
                }
                R.id.action_delete_selected -> {
                    deleteSelectedFolders()
                    true
                }
                R.id.action_move_selected -> {
                    moveSelectedFolders()
                    true
                }
                R.id.action_export_selected -> {
                    exportSelectedFolders()
                    true
                }
                R.id.action_settings -> {
                    openSettings()
                    true
                }
                R.id.action_help -> {
                    showHelpDialog()
                    true
                }
                else -> false
            }
        }
    }

    private fun hideLockScreen() {
        binding.lockScreenOverlay.root.visibility = View.GONE

        // Restore normal UI
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)

        // Show system UI
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                window.insetsController?.show(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
            }
        } catch (e: Exception) {
            android.util.Log.w("MediaGalleryActivity", "Failed to show system UI: ${e.message}")
        }

        clearDialogReferences()
    }

    private fun setupPinLockScreen() {
        val overlay = binding.lockScreenOverlay
        // Show PIN UI, hide password UI
        overlay.passwordInputLayout.visibility = View.GONE
        overlay.pinDisplayLayout.visibility = View.VISIBLE
        overlay.pinKeypadLayout.visibility = View.VISIBLE
        overlay.actionButtonsLayout.visibility = View.GONE

        // Check if this is the default PIN and update subtitle accordingly
        lifecycleScope.launch {
            val storedPinHash = withContext(Dispatchers.IO) {
                database.preferenceDao().getPreference("hidden_gallery_security_value")?.value
            }
            val defaultPinHash = android.util.Base64.encodeToString("0000".toByteArray(), android.util.Base64.DEFAULT)

            if (storedPinHash == defaultPinHash) {
                overlay.lockSubtitle.text = "Enter PIN: 0000 (default)"
            } else {
                overlay.lockSubtitle.text = "Enter your PIN to continue"
            }
        }

        // Always show fingerprint button to maintain layout, but enable/disable based on settings
        lifecycleScope.launch {
            val fingerprintEnabled = withContext(Dispatchers.IO) {
                database.preferenceDao().getPreference("hidden_gallery_fingerprint_enabled")?.value?.toBooleanStrictOrNull() ?: false
            }

            // Always keep button visible to maintain layout
            overlay.fingerprintButton.visibility = View.VISIBLE

            if (fingerprintEnabled) {
                // Enable fingerprint button and setup authentication
                overlay.fingerprintButton.isEnabled = true
                overlay.fingerprintButton.alpha = 1.0f
                setupFingerprintAuthentication(overlay.fingerprintButton)
            } else {
                // Disable fingerprint button but keep it visible for layout consistency
                overlay.fingerprintButton.isEnabled = false
                overlay.fingerprintButton.alpha = 0.3f
                // Set click listener to show toast when disabled
                overlay.fingerprintButton.setOnClickListener {
                    Toast.makeText(this@MediaGalleryActivity, "Fingerprint authentication is disabled", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Setup PIN input
        var currentPin = ""
        val pinDigits = arrayOf(
            overlay.pinDigit1,
            overlay.pinDigit2,
            overlay.pinDigit3,
            overlay.pinDigit4
        )

        val numberButtons = arrayOf(
            overlay.pinBtn0,
            overlay.pinBtn1,
            overlay.pinBtn2,
            overlay.pinBtn3,
            overlay.pinBtn4,
            overlay.pinBtn5,
            overlay.pinBtn6,
            overlay.pinBtn7,
            overlay.pinBtn8,
            overlay.pinBtn9
        )

        numberButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                if (currentPin.length < 4) {
                    currentPin += index.toString()
                    updatePinDisplay(pinDigits, currentPin)

                    // Auto-verify when 4 digits are entered
                    if (currentPin.length == 4) {
                        verifyPin(currentPin, pinDigits) {
                            // Clear PIN on failure
                            currentPin = ""
                            updatePinDisplay(pinDigits, currentPin)
                        }
                    }
                }
            }
        }

        // Backspace button (RIGHT side - id="button_backspace" in your layout)
        overlay.buttonBackspace.setOnClickListener {
            if (currentPin.isNotEmpty()) {
                currentPin = currentPin.dropLast(1)
                updatePinDisplay(pinDigits, currentPin)
            }
        }

        overlay.cancelButton.setOnClickListener {
            hideLockScreen()
            finish()
        }

        overlay.backButton.setOnClickListener {
            hideLockScreen()
            finish()
        }

        // Setup forgot PIN button (for PIN mode)
        overlay.forgotPinButton.setOnClickListener {
            showRecoveryDialog()
        }

        // Show forgot PIN button, hide forgot password button
        overlay.forgotPinButton.visibility = View.VISIBLE
        overlay.forgotPasswordButton.visibility = View.GONE
    }

    private fun updatePinDisplay(pinDigits: Array<android.widget.TextView>, pin: String) {
        pinDigits.forEachIndexed { index, textView ->
            textView.text = if (index < pin.length) "●" else ""
        }
    }

    private fun verifyPassword(enteredPassword: String) {
        lifecycleScope.launch {
            val storedPasswordHash = withContext(Dispatchers.IO) {
                database.preferenceDao().getPreference("hidden_gallery_security_value")?.value
            }

            if (storedPasswordHash != null) {
                val enteredPasswordHash = android.util.Base64.encodeToString(enteredPassword.toByteArray(), android.util.Base64.DEFAULT)
                if (enteredPasswordHash.trim() == storedPasswordHash.trim()) {
                    hideLockScreen()
                    proceedToGallery()
                } else {
                    Toast.makeText(this@MediaGalleryActivity, "Incorrect password", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this@MediaGalleryActivity, "Authentication error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verifyPin(enteredPin: String, pinDigits: Array<android.widget.TextView>, onFailure: () -> Unit) {
        lifecycleScope.launch {
            val storedPinHash = withContext(Dispatchers.IO) {
                database.preferenceDao().getPreference("hidden_gallery_security_value")?.value
            }

            if (storedPinHash != null) {
                val enteredPinHash = android.util.Base64.encodeToString(enteredPin.toByteArray(), android.util.Base64.DEFAULT)
                if (enteredPinHash.trim() == storedPinHash.trim()) {
                    hideLockScreen()
                    proceedToGallery()
                } else {
                    showPinError(pinDigits)
                    Toast.makeText(this@MediaGalleryActivity, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                    onFailure()
                }
            } else {
                Toast.makeText(this@MediaGalleryActivity, "Authentication error", Toast.LENGTH_SHORT).show()
                onFailure()
            }
        }
    }

    private fun showPinError(pinDigits: Array<android.widget.TextView>) {
        // Animate PIN digits to show error
        pinDigits.forEach { digit ->
            digit.setBackgroundColor(android.graphics.Color.parseColor("#FFFF4444"))
            digit.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(100)
                .withEndAction {
                    digit.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .withEndAction {
                            // Reset background after animation
                            digit.setBackgroundResource(R.drawable.pin_digit_background)
                        }
                }
        }
    }

    private fun setupFingerprintAuthentication(fingerprintButton: android.widget.ImageButton) {
        fingerprintButton.setOnClickListener {
            android.util.Log.d("MediaGalleryActivity", "Fingerprint button clicked")

            // Add visual feedback
            fingerprintButton.isEnabled = false
            fingerprintButton.alpha = 0.5f

            val biometricManager = BiometricManager.from(this)
            when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
                BiometricManager.BIOMETRIC_SUCCESS -> {
                    android.util.Log.d("MediaGalleryActivity", "Biometric authentication available, showing prompt")
                    showBiometricPrompt {
                        // Reset button state on completion
                        fingerprintButton.isEnabled = true
                        fingerprintButton.alpha = 1.0f
                    }
                }
                else -> {
                    android.util.Log.e("MediaGalleryActivity", "Biometric authentication not available")
                    Toast.makeText(this, "Biometric authentication not available", Toast.LENGTH_SHORT).show()
                    fingerprintButton.isEnabled = true
                    fingerprintButton.alpha = 1.0f
                }
            }
        }
    }

    private fun showBiometricPrompt(onComplete: () -> Unit) {
        val executor: Executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this as FragmentActivity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    android.util.Log.e("MediaGalleryActivity", "Biometric authentication error: $errString")
                    Toast.makeText(this@MediaGalleryActivity, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                    onComplete()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    android.util.Log.d("MediaGalleryActivity", "Biometric authentication succeeded")
                    hideLockScreen()
                    proceedToGallery()
                    onComplete()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    android.util.Log.d("MediaGalleryActivity", "Biometric authentication failed")
                    Toast.makeText(this@MediaGalleryActivity, "Authentication failed", Toast.LENGTH_SHORT).show()
                    onComplete()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Hidden Gallery")
            .setSubtitle("Use your fingerprint to access the hidden gallery")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun proceedToGallery() {
        checkPermissions()
        setupClickListeners()
    }

    private fun updateFingerprintSectionVisibility(enabled: Boolean) {
        android.util.Log.d("MediaGalleryActivity", "Updating fingerprint button visibility: $enabled")

        runOnUiThread {
            if (enabled) {
                // Check if biometric authentication is actually available
                val biometricManager = BiometricManager.from(this)
                when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
                    BiometricManager.BIOMETRIC_SUCCESS -> {
                        // Enable fingerprint button in keypad (LEFT side)
                        binding.lockScreenOverlay.fingerprintButton.visibility = View.VISIBLE
                        binding.lockScreenOverlay.fingerprintButton.isEnabled = true
                        binding.lockScreenOverlay.fingerprintButton.alpha = 1.0f
                        setupFingerprintAuthentication(binding.lockScreenOverlay.fingerprintButton)
                        android.util.Log.d("MediaGalleryActivity", "Fingerprint button enabled on LEFT side")
                    }
                    else -> {
                        // Keep button visible but disabled for layout consistency
                        binding.lockScreenOverlay.fingerprintButton.visibility = View.VISIBLE
                        binding.lockScreenOverlay.fingerprintButton.isEnabled = false
                        binding.lockScreenOverlay.fingerprintButton.alpha = 0.3f
                        // Set click listener to show toast when disabled
                        binding.lockScreenOverlay.fingerprintButton.setOnClickListener {
                            Toast.makeText(this@MediaGalleryActivity, "Biometric authentication not available on this device", Toast.LENGTH_SHORT).show()
                        }
                        android.util.Log.d("MediaGalleryActivity", "Biometric not available, fingerprint button disabled")
                    }
                }
            } else {
                // Keep button visible but disabled for layout consistency
                binding.lockScreenOverlay.fingerprintButton.visibility = View.VISIBLE
                binding.lockScreenOverlay.fingerprintButton.isEnabled = false
                binding.lockScreenOverlay.fingerprintButton.alpha = 0.3f
                // Set click listener to show toast when disabled
                binding.lockScreenOverlay.fingerprintButton.setOnClickListener {
                    Toast.makeText(this@MediaGalleryActivity, "Fingerprint authentication is disabled", Toast.LENGTH_SHORT).show()
                }
                android.util.Log.d("MediaGalleryActivity", "Fingerprint button disabled")
            }
        }
    }

    private fun clearDialogReferences() {
        // No longer needed since fingerprint button is now in keypad
        android.util.Log.d("MediaGalleryActivity", "Dialog references cleared")
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.foldersRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private suspend fun createDefaultFolders() {
        try {
            android.util.Log.d("MediaGalleryActivity", "Creating default folders...")

            val defaultFolders = listOf(
                DefaultFolderInfo("Photos", "ic_folder", "Store your private photos", "icon_bg_1"),
                DefaultFolderInfo("Videos", "ic_folder", "Store your private videos", "icon_bg_2"),
                DefaultFolderInfo("Audios", "ic_folder", "Store your private audio files", "icon_bg_3")
            )

            val defaultPassword = "0000"

            for (folderInfo in defaultFolders) {
                // Check if folder already exists
                val existingFolder = encryptedFolderDao.getFolderByName(folderInfo.name)
                if (existingFolder == null) {
                    // Generate salt and hash password
                    val salt = EncryptionUtils.generateSalt()
                    val hashedPassword = EncryptionUtils.hashPassword(defaultPassword, salt)

                    // Create folder directory
                    val folderPath = createFolderDirectory(folderInfo.name)

                    // Create folder entity
                    val folder = EncryptedFolderEntity(
                        name = folderInfo.name,
                        passwordHash = hashedPassword,
                        salt = salt,
                        folderPath = folderPath
                    )

                    // Save to database
                    encryptedFolderDao.insertFolder(folder)
                    android.util.Log.d("MediaGalleryActivity", "Created default folder: ${folderInfo.name}")
                }
            }

            android.util.Log.d("MediaGalleryActivity", "Default folders creation completed")

            // Show welcome message on main thread
            runOnUiThread {
                showWelcomeMessage()
            }

        } catch (e: Exception) {
            android.util.Log.e("MediaGalleryActivity", "Error creating default folders", e)
            runOnUiThread {
                Toast.makeText(this@MediaGalleryActivity, "Error setting up default folders", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private data class DefaultFolderInfo(
        val name: String,
        val icon: String,
        val description: String,
        val iconBackground: String
    )

    private fun showWelcomeMessage() {
        val message = buildString {
            append("🎉 Welcome to Hidden Gallery!\n\n")
            append("We've created 3 default folders for you:\n\n")
            append("📷 Photos - For your private photos\n")
            append("🎥 Videos - For your private videos\n")
            append("🎵 Audios - For your private audio files\n\n")
            append("➕ Use the '+' button to create more folders\n\n")
            append("🔐 Default PIN: 0000\n\n")
            append("You can change the PIN for each folder individually by long-pressing on them.\n\n")
            append("💡 Tip: Set up a recovery question in Settings for account security!")
        }

        androidx.appcompat.app.AlertDialog.Builder(this@MediaGalleryActivity)
            .setTitle("Hidden Gallery Setup Complete")
            .setMessage(message)
            .setPositiveButton("Got It!") { _, _ ->
                // User acknowledged the welcome message
            }
            .setNeutralButton("Setup Recovery") { _, _ ->
                // Open settings to setup recovery
                try {
                    val intent = Intent(this@MediaGalleryActivity, SettingsActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    android.util.Log.e("MediaGalleryActivity", "Error opening settings", e)
                    Toast.makeText(this@MediaGalleryActivity, "Unable to open settings", Toast.LENGTH_SHORT).show()
                }
            }
            .setCancelable(true)
            .show()
    }

    private fun showCreateFolderDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_folder, null)
        val folderNameInput = dialogView.findViewById<TextInputEditText>(R.id.folder_name_input)
        val folderPasswordInput = dialogView.findViewById<TextInputEditText>(R.id.folder_password_input)
        val folderConfirmPasswordInput = dialogView.findViewById<TextInputEditText>(R.id.folder_confirm_password_input)
        val folderNameLayout = dialogView.findViewById<TextInputLayout>(R.id.folder_name_input_layout)
        val folderPasswordLayout = dialogView.findViewById<TextInputLayout>(R.id.folder_password_input_layout)
        val folderConfirmPasswordLayout = dialogView.findViewById<TextInputLayout>(R.id.folder_confirm_password_input_layout)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<View>(R.id.cancel_button).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.create_button).setOnClickListener {
            val name = folderNameInput.text?.toString()?.trim() ?: ""
            val password = folderPasswordInput.text?.toString() ?: ""
            val confirmPassword = folderConfirmPasswordInput.text?.toString() ?: ""

            // Clear previous errors
            folderNameLayout.error = null
            folderPasswordLayout.error = null
            folderConfirmPasswordLayout.error = null

            var hasError = false

            // Validate input
            if (name.isEmpty()) {
                folderNameLayout.error = "Folder name is required"
                hasError = true
            } else if (name.length < 2) {
                folderNameLayout.error = "Folder name must be at least 2 characters"
                hasError = true
            }

            if (password.isEmpty()) {
                folderPasswordLayout.error = "Password is required"
                hasError = true
            } else if (password.length < 4) {
                folderPasswordLayout.error = "Password must be at least 4 characters"
                hasError = true
            }

            if (confirmPassword != password) {
                folderConfirmPasswordLayout.error = "Passwords do not match"
                hasError = true
            }

            if (!hasError) {
                createFolder(name, password)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun createFolder(name: String, password: String) {
        lifecycleScope.launch {
            try {
                // Check if folder name already exists
                val existingFolder = encryptedFolderDao.getFolderByName(name)
                if (existingFolder != null) {
                    Toast.makeText(this@MediaGalleryActivity, "Folder name already exists", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Generate salt and hash password
                val salt = EncryptionUtils.generateSalt()
                val hashedPassword = EncryptionUtils.hashPassword(password, salt)

                // Create folder directory
                val folderPath = createFolderDirectory(name)

                // Create folder entity
                val folder = EncryptedFolderEntity(
                    name = name,
                    passwordHash = hashedPassword,
                    salt = salt,
                    folderPath = folderPath
                )

                // Save to database
                encryptedFolderDao.insertFolder(folder)

                Toast.makeText(this@MediaGalleryActivity, "Folder '$name' created successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MediaGalleryActivity, "Failed to create folder: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createFolderDirectory(folderName: String): String {
        val appDir = File(filesDir, "encrypted_media")
        if (!appDir.exists()) {
            appDir.mkdirs()
        }

        val folderDir = File(appDir, EncryptionUtils.generateSecureFileName())
        if (!folderDir.exists()) {
            folderDir.mkdirs()
        }

        return folderDir.absolutePath
    }

    private fun showFolderPopupMenu(folder: EncryptedFolderEntity, anchorView: View) {
        val popup = androidx.appcompat.widget.PopupMenu(this, anchorView)
        popup.menuInflater.inflate(R.menu.menu_folder_options, popup.menu)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_open_folder -> {
                    // TODO: Open folder contents
                    Toast.makeText(this, "Opening ${folder.name}", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.action_rename_folder -> {
                    showRenameFolderDialog(folder)
                    true
                }
                R.id.action_change_pin -> {
                    showChangePinDialog(folder)
                    true
                }
                R.id.action_export_folder -> {
                    exportFolder(folder)
                    true
                }
                R.id.action_delete_folder -> {
                    showDeleteFolderDialog(folder)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showFolderOptionsDialog(folder: EncryptedFolderEntity) {
        val options = arrayOf("Rename", "Change PIN", "Export", "Delete")

        MaterialAlertDialogBuilder(this)
            .setTitle(folder.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenameFolderDialog(folder)
                    1 -> showChangePinDialog(folder)
                    2 -> exportFolder(folder)
                    3 -> showDeleteFolderDialog(folder)
                }
            }
            .show()
    }

    private fun showRenameFolderDialog(folder: EncryptedFolderEntity) {
        // TODO: Implement rename functionality
        Toast.makeText(this, "Rename functionality coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun showDeleteFolderDialog(folder: EncryptedFolderEntity) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Folder")
            .setMessage("Are you sure you want to delete '${folder.name}'? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteFolder(folder)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteFolder(folder: EncryptedFolderEntity) {
        lifecycleScope.launch {
            try {
                // Delete folder directory
                val folderDir = File(folder.folderPath)
                if (folderDir.exists()) {
                    folderDir.deleteRecursively()
                }

                // Delete from database
                encryptedFolderDao.deleteFolder(folder)

                Toast.makeText(this@MediaGalleryActivity, "Folder '${folder.name}' deleted", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MediaGalleryActivity, "Failed to delete folder: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Permissions Required")
            .setMessage("Media access permissions are required to use the Hidden Gallery feature.")
            .setPositiveButton("Grant Permissions") { _, _ ->
                checkPermissions()
            }
            .setNegativeButton("Cancel") { _, _ ->
                finish()
            }
            .show()
    }

    private fun showRecoveryDialog() {
        lifecycleScope.launch {
            try {
                // Check recovery attempt limits first
                if (!checkRecoveryAttemptLimits()) {
                    return@launch
                }

                val recoveryData = withContext(Dispatchers.IO) {
                    RecoveryData(
                        question = database.preferenceDao().getPreference("hidden_gallery_recovery_question")?.value,
                        pinHint = database.preferenceDao().getPreference("hidden_gallery_pin_hint")?.value,
                        setupTime = database.preferenceDao().getPreference("hidden_gallery_recovery_setup_time")?.value?.toLongOrNull()
                    )
                }

                android.util.Log.d("MediaGalleryActivity", "Recovery data loaded: ${recoveryData.isValid()}")

                proceedWithRecovery(recoveryData)
            } catch (e: Exception) {
                android.util.Log.e("MediaGalleryActivity", "Error loading recovery data", e)
                showRecoveryError("Unable to load recovery options. Please try again.")
            }
        }
    }

    private data class RecoveryData(
        val question: String?,
        val pinHint: String?,
        val setupTime: Long?
    ) {
        fun isValid(): Boolean = !question.isNullOrEmpty() || !pinHint.isNullOrEmpty()
        fun hasQuestion(): Boolean = !question.isNullOrEmpty()
        fun hasHint(): Boolean = !pinHint.isNullOrEmpty()
        fun hasBoth(): Boolean = hasQuestion() && hasHint()
    }

    private fun proceedWithRecovery(recoveryData: RecoveryData) {
        when {
            !recoveryData.isValid() -> showNoRecoverySetupDialog()
            recoveryData.hasBoth() -> showRecoveryOptionsDialog(recoveryData)
            recoveryData.hasQuestion() -> showRecoveryQuestionDialog(recoveryData)
            recoveryData.hasHint() -> showPinHintDialog(recoveryData.pinHint!!)
        }
    }

    private fun showNoRecoverySetupDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this@MediaGalleryActivity)
            .setTitle("No Recovery Options Available")
            .setMessage("No recovery question or PIN hint has been configured for your account.\n\nTo set up account recovery:\n1. Go to Settings\n2. Select Hidden Gallery\n3. Choose 'Setup Recovery Question'\n\nThis will help you regain access if you forget your PIN/password.")
            .setPositiveButton("Open Settings") { _, _ ->
                try {
                    val intent = Intent(this@MediaGalleryActivity, SettingsActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    android.util.Log.e("MediaGalleryActivity", "Error opening settings", e)
                    Toast.makeText(this@MediaGalleryActivity, "Unable to open settings", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(true)
            .show()
    }

    private fun checkRecoveryAttemptLimits(): Boolean {
        val currentTime = System.currentTimeMillis()
        val lastAttemptTime = getSharedPreferences("recovery_attempts", MODE_PRIVATE)
            .getLong("last_attempt_time", 0)
        val attemptCount = getSharedPreferences("recovery_attempts", MODE_PRIVATE)
            .getInt("attempt_count", 0)

        // Reset attempts after 24 hours
        if (currentTime - lastAttemptTime > 24 * 60 * 60 * 1000) {
            getSharedPreferences("recovery_attempts", MODE_PRIVATE)
                .edit()
                .putInt("attempt_count", 0)
                .putLong("last_attempt_time", currentTime)
                .apply()
            return true
        }

        // Limit to 5 recovery attempts per 24 hours
        if (attemptCount >= 5) {
            val hoursRemaining = 24 - ((currentTime - lastAttemptTime) / (60 * 60 * 1000))
            androidx.appcompat.app.AlertDialog.Builder(this@MediaGalleryActivity)
                .setTitle("Recovery Limit Reached")
                .setMessage("You have exceeded the maximum number of recovery attempts (5) for today.\n\nPlease try again in $hoursRemaining hours.\n\nThis security measure protects your account from unauthorized access attempts.")
                .setPositiveButton("OK", null)
                .setCancelable(false)
                .show()
            return false
        }

        return true
    }

    private fun incrementRecoveryAttempt() {
        val prefs = getSharedPreferences("recovery_attempts", MODE_PRIVATE)
        val currentCount = prefs.getInt("attempt_count", 0)
        prefs.edit()
            .putInt("attempt_count", currentCount + 1)
            .putLong("last_attempt_time", System.currentTimeMillis())
            .apply()
    }

    private fun showRecoveryError(message: String) {
        androidx.appcompat.app.AlertDialog.Builder(this@MediaGalleryActivity)
            .setTitle("Recovery Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private data class CredentialInfo(
        val value: String,
        val type: String,
        val isDefault: Boolean
    )

    private suspend fun getCurrentCredentials(): CredentialInfo? {
        return try {
            val securityMethod = database.preferenceDao().getPreference("hidden_gallery_security_method")?.value
            val securityValueHash = database.preferenceDao().getPreference("hidden_gallery_security_value")?.value

            if (securityValueHash != null) {
                val credentials = String(android.util.Base64.decode(securityValueHash, android.util.Base64.DEFAULT))
                val credentialType = if (securityMethod == "pin") "PIN" else "Password"
                val isDefault = credentials == "0000" && securityMethod == "pin"

                CredentialInfo(credentials, credentialType, isDefault)
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("MediaGalleryActivity", "Error retrieving credentials", e)
            null
        }
    }

    private fun showCurrentCredentials(credentials: CredentialInfo) {
        val message = buildString {
            append("✅ Recovery Successful!\n\n")
            append("Your current ${credentials.type} is: ")
            append("${credentials.value}\n\n")

            if (credentials.isDefault) {
                append("⚠️ You are using the default PIN. ")
                append("Consider changing it in Settings for better security.")
            } else {
                append("💡 Make sure to remember this ${credentials.type.lowercase()} for future access.")
            }
        }

        androidx.appcompat.app.AlertDialog.Builder(this@MediaGalleryActivity)
            .setTitle("Account Recovery")
            .setMessage(message)
            .setPositiveButton("Auto-Fill & Continue") { _, _ ->
                if (credentials.type == "PIN") {
                    autoFillPin(credentials.value)
                } else {
                    autoFillPassword(credentials.value)
                }
            }
            .setNeutralButton("Copy to Clipboard") { _, _ ->
                copyToClipboard(credentials.value, credentials.type)
            }
            .setNegativeButton("Close", null)
            .setCancelable(false)
            .show()
    }

    private fun copyToClipboard(value: String, type: String) {
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Hidden Gallery $type", value)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(this, "$type copied to clipboard", Toast.LENGTH_SHORT).show()

            // Clear clipboard after 30 seconds for security
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    val emptyClip = android.content.ClipData.newPlainText("", "")
                    clipboard.setPrimaryClip(emptyClip)
                } catch (e: Exception) {
                    // Ignore clipboard clear errors
                }
            }, 30000)
        } catch (e: Exception) {
            android.util.Log.e("MediaGalleryActivity", "Error copying to clipboard", e)
            Toast.makeText(this, "Failed to copy to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    private fun autoFillPin(pin: String) {
        val overlay = binding.lockScreenOverlay
        val pinDigits = arrayOf(
            overlay.pinDigit1,
            overlay.pinDigit2,
            overlay.pinDigit3,
            overlay.pinDigit4
        )

        // Clear existing PIN display
        pinDigits.forEach { it.text = "" }

        // Fill PIN digits
        pin.forEachIndexed { index, digit ->
            if (index < pinDigits.size) {
                pinDigits[index].text = digit.toString()
            }
        }

        // Auto-verify after a short delay
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            verifyPin(pin, pinDigits) {
                // Clear on failure
                pinDigits.forEach { it.text = "" }
            }
        }, 1000)
    }

    private fun autoFillPassword(password: String) {
        val overlay = binding.lockScreenOverlay
        overlay.passwordInput.setText(password)

        // Auto-verify after a short delay
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            verifyPassword(password)
        }, 1000)
    }

    private fun showPinHintDialog(pinHint: String) {
        incrementRecoveryAttempt()

        val message = buildString {
            append("💡 Here's your PIN hint:\n\n")
            append("\"$pinHint\"\n\n")
            append("Take your time to think about what this hint means to you. ")
            append("Your PIN is likely related to this hint in some way.")
        }

        androidx.appcompat.app.AlertDialog.Builder(this@MediaGalleryActivity)
            .setTitle("PIN Recovery Hint")
            .setMessage(message)
            .setPositiveButton("Got It") { _, _ ->
                // User understands the hint
            }
            .setNeutralButton("Need More Help?") { _, _ ->
                showAdditionalHelpDialog()
            }
            .setNegativeButton("Copy Hint") { _, _ ->
                copyToClipboard(pinHint, "PIN Hint")
            }
            .setCancelable(true)
            .show()
    }

    private fun showAdditionalHelpDialog() {
        val helpMessage = buildString {
            append("If you still can't remember your PIN after reviewing the hint:\n\n")
            append("🔹 Try variations of numbers related to the hint\n")
            append("🔹 Think about significant dates or numbers in your life\n")
            append("🔹 Consider common PIN patterns you typically use\n\n")
            append("Security Options:\n")
            append("• Set up a recovery question in Settings\n")
            append("• Update your PIN to something more memorable\n")
            append("• Add a more detailed PIN hint")
        }

        androidx.appcompat.app.AlertDialog.Builder(this@MediaGalleryActivity)
            .setTitle("Additional Recovery Help")
            .setMessage(helpMessage)
            .setPositiveButton("Open Settings") { _, _ ->
                try {
                    val intent = Intent(this@MediaGalleryActivity, SettingsActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    android.util.Log.e("MediaGalleryActivity", "Error opening settings", e)
                    Toast.makeText(this@MediaGalleryActivity, "Unable to open settings", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Close", null)
            .setCancelable(true)
            .show()
    }

    private fun showRecoveryOptionsDialog(recoveryData: RecoveryData) {
        val options = mutableListOf<String>()
        val actions = mutableListOf<() -> Unit>()

        if (recoveryData.hasQuestion()) {
            options.add("🔐 Answer Security Question")
            actions.add { showRecoveryQuestionDialog(recoveryData) }
        }

        if (recoveryData.hasHint()) {
            options.add("💡 View PIN Hint")
            actions.add { showPinHintDialog(recoveryData.pinHint!!) }
        }

        options.add("❌ Cancel")
        actions.add { /* Cancel - do nothing */ }

        androidx.appcompat.app.AlertDialog.Builder(this@MediaGalleryActivity)
            .setTitle("Account Recovery Options")
            .setMessage("Choose your preferred recovery method:")
            .setItems(options.toTypedArray()) { _, which ->
                if (which < actions.size) {
                    actions[which].invoke()
                }
            }
            .setCancelable(true)
            .show()
    }

    private fun showRecoveryQuestionDialog(recoveryData: RecoveryData) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_recovery_process, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this@MediaGalleryActivity)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val questionText = dialogView.findViewById<TextView>(R.id.security_question_text)
        val answerInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.recovery_answer_input)
        val pinHintSection = dialogView.findViewById<LinearLayout>(R.id.pin_hint_section)
        val pinHintText = dialogView.findViewById<TextView>(R.id.pin_hint_text)
        val errorMessage = dialogView.findViewById<TextView>(R.id.error_message)
        val cancelButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.cancel_recovery_button)
        val verifyButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.verify_answer_button)

        questionText.text = recoveryData.question

        // Show PIN hint if available
        if (recoveryData.hasHint()) {
            pinHintSection.visibility = View.VISIBLE
            pinHintText.text = recoveryData.pinHint
        }

        var attemptCount = 0
        val maxAttempts = 3

        cancelButton.setOnClickListener {
            incrementRecoveryAttempt()
            dialog.dismiss()
        }

        verifyButton.setOnClickListener {
            val userAnswer = answerInput.text.toString().trim()

            // Validate input
            if (userAnswer.isEmpty()) {
                showAnswerError(errorMessage, "Please enter your answer")
                return@setOnClickListener
            }

            if (userAnswer.length < 2) {
                showAnswerError(errorMessage, "Answer must be at least 2 characters")
                return@setOnClickListener
            }

            attemptCount++
            verifyButton.isEnabled = false
            verifyButton.text = "Verifying..."

            lifecycleScope.launch {
                try {
                    val isCorrect = withContext(Dispatchers.IO) {
                        verifyRecoveryAnswer(userAnswer)
                    }

                    if (isCorrect) {
                        dialog.dismiss()
                        showSuccessfulRecovery()
                    } else {
                        incrementRecoveryAttempt()

                        if (attemptCount >= maxAttempts) {
                            dialog.dismiss()
                            showMaxAttemptsReached()
                        } else {
                            val remainingAttempts = maxAttempts - attemptCount
                            showAnswerError(errorMessage, "Incorrect answer. $remainingAttempts attempts remaining.")
                            answerInput.text?.clear()

                            // Add delay before allowing next attempt
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                verifyButton.isEnabled = true
                                verifyButton.text = "Verify Answer"
                            }, 2000)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MediaGalleryActivity", "Error verifying answer", e)
                    showAnswerError(errorMessage, "Verification failed. Please try again.")
                    verifyButton.isEnabled = true
                    verifyButton.text = "Verify Answer"
                }
            }
        }

        dialog.show()
    }

    private suspend fun verifyRecoveryAnswer(userAnswer: String): Boolean {
        return try {
            val storedAnswerHash = database.preferenceDao().getPreference("hidden_gallery_recovery_answer")?.value
            val storedSalt = database.preferenceDao().getPreference("hidden_gallery_recovery_salt")?.value

            if (storedAnswerHash == null) {
                android.util.Log.e("MediaGalleryActivity", "No stored answer hash found")
                return false
            }

            if (storedSalt != null) {
                // Use salt-based verification (new method)
                val userAnswerHash = hashAnswerWithSalt(userAnswer.lowercase().trim(), storedSalt)
                userAnswerHash.trim() == storedAnswerHash.trim()
            } else {
                // Fallback to old method for backward compatibility
                val userAnswerHash = android.util.Base64.encodeToString(userAnswer.lowercase().toByteArray(), android.util.Base64.DEFAULT)
                userAnswerHash.trim() == storedAnswerHash.trim()
            }
        } catch (e: Exception) {
            android.util.Log.e("MediaGalleryActivity", "Error in answer verification", e)
            false
        }
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
            android.util.Log.e("MediaGalleryActivity", "Error hashing answer", e)
            android.util.Base64.encodeToString(answer.toByteArray(), android.util.Base64.DEFAULT)
        }
    }

    private fun showAnswerError(errorMessage: TextView, message: String) {
        errorMessage.text = message
        errorMessage.visibility = View.VISIBLE

        // Auto-hide error after 5 seconds
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            errorMessage.visibility = View.GONE
        }, 5000)
    }

    private fun showMaxAttemptsReached() {
        androidx.appcompat.app.AlertDialog.Builder(this@MediaGalleryActivity)
            .setTitle("Maximum Attempts Reached")
            .setMessage("You have exceeded the maximum number of answer attempts for this session.\n\nFor security reasons, please wait before trying again or contact support if you continue to have issues.")
            .setPositiveButton("OK", null)
            .setCancelable(false)
            .show()
    }

    private fun showSuccessfulRecovery() {
        lifecycleScope.launch {
            val credentials = withContext(Dispatchers.IO) {
                getCurrentCredentials()
            }

            if (credentials != null) {
                showCurrentCredentials(credentials)
            } else {
                showRecoveryError("Unable to retrieve credentials. Please contact support.")
            }
        }
    }

    // Toolbar menu actions
    private fun selectAllFolders() {
        // TODO: Implement multi-select functionality
        Toast.makeText(this, "Select All functionality coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun deleteSelectedFolders() {
        // TODO: Implement bulk delete functionality
        Toast.makeText(this, "Delete Selected functionality coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun moveSelectedFolders() {
        // TODO: Implement bulk move functionality
        Toast.makeText(this, "Move Selected functionality coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun exportSelectedFolders() {
        // TODO: Implement bulk export functionality
        Toast.makeText(this, "Export Selected functionality coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun openSettings() {
        try {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("MediaGalleryActivity", "Error opening settings", e)
            Toast.makeText(this, "Unable to open settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showHelpDialog() {
        val helpMessage = buildString {
            append("🔐 Hidden Gallery Help\n\n")
            append("📁 Folder Management:\n")
            append("• Tap folder to open\n")
            append("• Long press for options\n")
            append("• Use ⋮ menu for quick actions\n\n")
            append("➕ Creating Folders:\n")
            append("• Tap '+' button to create new folder\n")
            append("• Set custom PIN for each folder\n\n")
            append("🔒 Security Features:\n")
            append("• Each folder has its own PIN\n")
            append("• Set up recovery questions in Settings\n")
            append("• Default PIN is 0000 (change recommended)\n\n")
            append("💡 Tips:\n")
            append("• Use ⋮ menu in header for bulk operations\n")
            append("• Export folders to backup your data\n")
            append("• Change PINs regularly for better security")
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Help & Tips")
            .setMessage(helpMessage)
            .setPositiveButton("Got It", null)
            .setNeutralButton("Settings") { _, _ ->
                openSettings()
            }
            .show()
    }

    // Folder-specific actions
    private fun showChangePinDialog(folder: EncryptedFolderEntity) {
        // TODO: Implement change PIN functionality
        Toast.makeText(this, "Change PIN for ${folder.name} coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun exportFolder(folder: EncryptedFolderEntity) {
        // TODO: Implement export functionality
        Toast.makeText(this, "Export ${folder.name} coming soon", Toast.LENGTH_SHORT).show()
    }
}