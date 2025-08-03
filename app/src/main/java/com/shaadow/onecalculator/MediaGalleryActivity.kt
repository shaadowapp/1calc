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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
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
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import java.util.concurrent.Executor

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
            }
        )

        binding.foldersRecyclerView.apply {
            layoutManager = GridLayoutManager(this@MediaGalleryActivity, 2)
            adapter = folderAdapter
        }

        // Observe folders from database
        lifecycleScope.launch {
            encryptedFolderDao.getAllFolders().collect { folders ->
                folderAdapter.submitList(folders)
                updateEmptyState(folders.isEmpty())
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

        // Configure UI based on security method
        when (securityMethod) {
            "password" -> setupPasswordLockScreen()
            "pin" -> setupPinLockScreen()
            else -> proceedToGallery() // Fallback
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

        // Check if fingerprint is enabled
        lifecycleScope.launch {
            val fingerprintEnabled = withContext(Dispatchers.IO) {
                database.preferenceDao().getPreference("hidden_gallery_fingerprint_enabled")?.value?.toBooleanStrictOrNull() ?: false
            }
            if (fingerprintEnabled) {
                // Fingerprint button is now in keypad, no separate section
                setupFingerprintAuthentication(overlay.fingerprintButton)
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
    }

    private fun setupClickListeners() {
        binding.fabCreateFolder.setOnClickListener {
            showCreateFolderDialog()
        }

        binding.createFirstFolderBtn.setOnClickListener {
            showCreateFolderDialog()
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

        // Check if this is the default PIN
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

        // Check if fingerprint is enabled and setup fingerprint button in keypad
        lifecycleScope.launch {
            val fingerprintEnabled = withContext(Dispatchers.IO) {
                database.preferenceDao().getPreference("hidden_gallery_fingerprint_enabled")?.value?.toBooleanStrictOrNull() ?: false
            }
            if (fingerprintEnabled) {
                // Show fingerprint button in keypad (fingerprint_button - LEFT side)
                overlay.fingerprintButton.visibility = View.VISIBLE
                setupFingerprintAuthentication(overlay.fingerprintButton)
            } else {
                // Hide fingerprint button if not enabled
                overlay.fingerprintButton.visibility = View.GONE
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
                        // Show fingerprint button in keypad (LEFT side)
                        binding.lockScreenOverlay.fingerprintButton.visibility = View.VISIBLE
                        setupFingerprintAuthentication(binding.lockScreenOverlay.fingerprintButton)
                        android.util.Log.d("MediaGalleryActivity", "Fingerprint button shown on LEFT side")
                    }
                    else -> {
                        binding.lockScreenOverlay.fingerprintButton.visibility = View.GONE
                        android.util.Log.d("MediaGalleryActivity", "Biometric not available, hiding fingerprint button")
                    }
                }
            } else {
                binding.lockScreenOverlay.fingerprintButton.visibility = View.GONE
                android.util.Log.d("MediaGalleryActivity", "Fingerprint button hidden")
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

    private fun showFolderOptionsDialog(folder: EncryptedFolderEntity) {
        val options = arrayOf("Rename", "Delete")
        
        MaterialAlertDialogBuilder(this)
            .setTitle(folder.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenameFolderDialog(folder)
                    1 -> showDeleteFolderDialog(folder)
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
}