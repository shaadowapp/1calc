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

    // Current lock dialog reference for real-time updates
    private var currentLockDialog: androidx.appcompat.app.AlertDialog? = null
    private var currentFingerprintSection: android.widget.LinearLayout? = null

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

        // Dismiss any open dialogs
        currentLockDialog?.dismiss()
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
                // No security set up, proceed normally
                proceedToGallery()
            }
        }
    }

    private fun showLockScreen(securityMethod: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_gallery_lock_screen, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this, R.style.DialogStyle_Todo)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Store dialog reference for real-time updates
        currentLockDialog = dialog
        currentFingerprintSection = dialogView.findViewById(R.id.fingerprint_section)

        // Configure UI based on security method
        when (securityMethod) {
            "password" -> setupPasswordLockScreen(dialogView, dialog)
            "pin" -> setupPinLockScreen(dialogView, dialog)
            else -> proceedToGallery() // Fallback
        }

        dialog.show()
    }

    private fun setupPasswordLockScreen(dialogView: View, dialog: androidx.appcompat.app.AlertDialog) {
        val passwordInputLayout = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.password_input_layout)
        val passwordInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.password_input)
        val pinDisplayLayout = dialogView.findViewById<android.widget.LinearLayout>(R.id.pin_display_layout)
        val pinKeypadLayout = dialogView.findViewById<android.widget.LinearLayout>(R.id.pin_keypad_layout)
        val submitButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.submit_button)
        val fingerprintButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.fingerprint_button)
        val cancelButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.cancel_button)
        val subtitle = dialogView.findViewById<android.widget.TextView>(R.id.lock_subtitle)

        // Show password UI, hide PIN UI
        passwordInputLayout.visibility = View.VISIBLE
        pinDisplayLayout.visibility = View.GONE
        pinKeypadLayout.visibility = View.GONE
        submitButton.visibility = View.VISIBLE
        subtitle.text = "Enter your password to continue"

        // Check if fingerprint is enabled
        lifecycleScope.launch {
            val fingerprintEnabled = withContext(Dispatchers.IO) {
                database.preferenceDao().getPreference("hidden_gallery_fingerprint_enabled")?.value?.toBooleanStrictOrNull() ?: false
            }
            if (fingerprintEnabled) {
                val fingerprintSection = dialogView.findViewById<android.widget.LinearLayout>(R.id.fingerprint_section)
                fingerprintSection.visibility = View.VISIBLE
                setupFingerprintAuthentication(fingerprintButton, dialog)
            }
        }

        submitButton.setOnClickListener {
            val enteredPassword = passwordInput.text.toString()
            if (enteredPassword.isNotEmpty()) {
                verifyPassword(enteredPassword, dialog)
            } else {
                Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
            clearDialogReferences()
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

    private fun setupPinLockScreen(dialogView: View, dialog: androidx.appcompat.app.AlertDialog) {
        val passwordInputLayout = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.password_input_layout)
        val pinDisplayLayout = dialogView.findViewById<android.widget.LinearLayout>(R.id.pin_display_layout)
        val pinKeypadLayout = dialogView.findViewById<android.widget.LinearLayout>(R.id.pin_keypad_layout)
        val submitButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.submit_button)
        val fingerprintButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.fingerprint_button)
        val cancelButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.cancel_button)
        val subtitle = dialogView.findViewById<android.widget.TextView>(R.id.lock_subtitle)

        // Show PIN UI, hide password UI
        passwordInputLayout.visibility = View.GONE
        pinDisplayLayout.visibility = View.VISIBLE
        pinKeypadLayout.visibility = View.VISIBLE
        submitButton.visibility = View.GONE
        subtitle.text = "Enter your PIN to continue"

        // Check if fingerprint is enabled
        lifecycleScope.launch {
            val fingerprintEnabled = withContext(Dispatchers.IO) {
                database.preferenceDao().getPreference("hidden_gallery_fingerprint_enabled")?.value?.toBooleanStrictOrNull() ?: false
            }
            if (fingerprintEnabled) {
                val fingerprintSection = dialogView.findViewById<android.widget.LinearLayout>(R.id.fingerprint_section)
                fingerprintSection.visibility = View.VISIBLE
                setupFingerprintAuthentication(fingerprintButton, dialog)
            }
        }

        // Setup PIN input
        var currentPin = ""
        val pinDigits = arrayOf(
            dialogView.findViewById<android.widget.TextView>(R.id.pin_digit_1),
            dialogView.findViewById<android.widget.TextView>(R.id.pin_digit_2),
            dialogView.findViewById<android.widget.TextView>(R.id.pin_digit_3),
            dialogView.findViewById<android.widget.TextView>(R.id.pin_digit_4)
        )

        val numberButtons = arrayOf(
            dialogView.findViewById<android.widget.Button>(R.id.pin_btn_0),
            dialogView.findViewById<android.widget.Button>(R.id.pin_btn_1),
            dialogView.findViewById<android.widget.Button>(R.id.pin_btn_2),
            dialogView.findViewById<android.widget.Button>(R.id.pin_btn_3),
            dialogView.findViewById<android.widget.Button>(R.id.pin_btn_4),
            dialogView.findViewById<android.widget.Button>(R.id.pin_btn_5),
            dialogView.findViewById<android.widget.Button>(R.id.pin_btn_6),
            dialogView.findViewById<android.widget.Button>(R.id.pin_btn_7),
            dialogView.findViewById<android.widget.Button>(R.id.pin_btn_8),
            dialogView.findViewById<android.widget.Button>(R.id.pin_btn_9)
        )

        numberButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                if (currentPin.length < 4) {
                    currentPin += index.toString()
                    updatePinDisplay(pinDigits, currentPin)

                    // Auto-verify when 4 digits are entered
                    if (currentPin.length == 4) {
                        verifyPin(currentPin, dialog, pinDigits) {
                            // Clear PIN on failure
                            currentPin = ""
                            updatePinDisplay(pinDigits, currentPin)
                        }
                    }
                }
            }
        }

        dialogView.findViewById<android.widget.ImageButton>(R.id.pin_btn_backspace).setOnClickListener {
            if (currentPin.isNotEmpty()) {
                currentPin = currentPin.dropLast(1)
                updatePinDisplay(pinDigits, currentPin)
            }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
            clearDialogReferences()
            finish()
        }
    }

    private fun updatePinDisplay(pinDigits: Array<android.widget.TextView>, pin: String) {
        pinDigits.forEachIndexed { index, textView ->
            textView.text = if (index < pin.length) "●" else ""
        }
    }

    private fun verifyPassword(enteredPassword: String, dialog: androidx.appcompat.app.AlertDialog) {
        lifecycleScope.launch {
            val storedPasswordHash = withContext(Dispatchers.IO) {
                database.preferenceDao().getPreference("hidden_gallery_security_value")?.value
            }

            if (storedPasswordHash != null) {
                val enteredPasswordHash = android.util.Base64.encodeToString(enteredPassword.toByteArray(), android.util.Base64.DEFAULT)
                if (enteredPasswordHash.trim() == storedPasswordHash.trim()) {
                    dialog.dismiss()
                    clearDialogReferences()
                    proceedToGallery()
                } else {
                    Toast.makeText(this@MediaGalleryActivity, "Incorrect password", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this@MediaGalleryActivity, "Authentication error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verifyPin(enteredPin: String, dialog: androidx.appcompat.app.AlertDialog, pinDigits: Array<android.widget.TextView>, onFailure: () -> Unit) {
        lifecycleScope.launch {
            val storedPinHash = withContext(Dispatchers.IO) {
                database.preferenceDao().getPreference("hidden_gallery_security_value")?.value
            }

            if (storedPinHash != null) {
                val enteredPinHash = android.util.Base64.encodeToString(enteredPin.toByteArray(), android.util.Base64.DEFAULT)
                if (enteredPinHash.trim() == storedPinHash.trim()) {
                    dialog.dismiss()
                    clearDialogReferences()
                    proceedToGallery()
                } else {
                    Toast.makeText(this@MediaGalleryActivity, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                    onFailure()
                }
            } else {
                Toast.makeText(this@MediaGalleryActivity, "Authentication error", Toast.LENGTH_SHORT).show()
                onFailure()
            }
        }
    }

    private fun setupFingerprintAuthentication(fingerprintButton: com.google.android.material.button.MaterialButton, dialog: androidx.appcompat.app.AlertDialog) {
        fingerprintButton.setOnClickListener {
            android.util.Log.d("MediaGalleryActivity", "Fingerprint button clicked")

            // Add visual feedback
            fingerprintButton.isEnabled = false
            fingerprintButton.text = "Authenticating..."

            val biometricManager = BiometricManager.from(this)
            when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
                BiometricManager.BIOMETRIC_SUCCESS -> {
                    android.util.Log.d("MediaGalleryActivity", "Biometric authentication available, showing prompt")
                    showBiometricPrompt(dialog) {
                        // Reset button state on completion
                        fingerprintButton.isEnabled = true
                        fingerprintButton.text = "Use Fingerprint"
                    }
                }
                else -> {
                    android.util.Log.e("MediaGalleryActivity", "Biometric authentication not available")
                    Toast.makeText(this, "Biometric authentication not available", Toast.LENGTH_SHORT).show()
                    fingerprintButton.isEnabled = true
                    fingerprintButton.text = "Use Fingerprint"
                }
            }
        }
    }

    private fun showBiometricPrompt(dialog: androidx.appcompat.app.AlertDialog, onComplete: () -> Unit) {
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
                    dialog.dismiss()
                    clearDialogReferences()
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
        android.util.Log.d("MediaGalleryActivity", "Updating fingerprint section visibility: $enabled")

        currentFingerprintSection?.let { fingerprintSection ->
            runOnUiThread {
                if (enabled) {
                    // Check if biometric authentication is actually available
                    val biometricManager = BiometricManager.from(this)
                    when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
                        BiometricManager.BIOMETRIC_SUCCESS -> {
                            fingerprintSection.visibility = View.VISIBLE

                            // Setup fingerprint authentication if not already set up
                            val fingerprintButton = fingerprintSection.findViewById<com.google.android.material.button.MaterialButton>(R.id.fingerprint_button)
                            currentLockDialog?.let { dialog ->
                                setupFingerprintAuthentication(fingerprintButton, dialog)
                            }

                            android.util.Log.d("MediaGalleryActivity", "Fingerprint section shown")
                        }
                        else -> {
                            fingerprintSection.visibility = View.GONE
                            android.util.Log.d("MediaGalleryActivity", "Biometric not available, hiding fingerprint section")
                        }
                    }
                } else {
                    fingerprintSection.visibility = View.GONE
                    android.util.Log.d("MediaGalleryActivity", "Fingerprint section hidden")
                }
            }
        } ?: run {
            android.util.Log.w("MediaGalleryActivity", "No current fingerprint section to update")
        }
    }

    private fun clearDialogReferences() {
        currentLockDialog = null
        currentFingerprintSection = null
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