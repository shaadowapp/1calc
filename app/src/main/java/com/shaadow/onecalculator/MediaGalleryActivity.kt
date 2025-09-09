package com.shaadow.onecalculator

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.shaadow.onecalculator.databinding.ActivityMediaGalleryBinding
import com.shaadow.onecalculator.utils.EncryptionUtils
import com.shaadow.onecalculator.utils.ExternalStorageManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.EncryptionMethod
import net.lingala.zip4j.model.enums.AesKeyStrength
import java.io.File
import java.util.concurrent.Executor
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.text.Charsets

class MediaGalleryActivity : BaseActivity() {

    private lateinit var binding: ActivityMediaGalleryBinding
    private lateinit var folderAdapter: EncryptedFolderAdapter
    private lateinit var database: HistoryDatabase
    private lateinit var encryptedFolderDao: EncryptedFolderDao
    private var currentMasterPassword: String = ""
    private var isAllSelected: Boolean = false
    private var isAutoLocking: Boolean = false
    private var isAuthenticated: Boolean = false
    private var isFromBackground: Boolean = false
    private var sessionAuthenticated: Boolean = false
    private var isExternalStorageInitialized: Boolean = false

    // SharedPreferences for persistent authentication state
    private lateinit var authPrefs: android.content.SharedPreferences
    private val AUTH_PREFS_NAME = "gallery_auth_prefs"
    private val KEY_SESSION_AUTHENTICATED = "session_authenticated"
    private val KEY_AUTH_TIMESTAMP = "auth_timestamp"
    private val AUTH_TIMEOUT_MS = 30 * 60 * 1000L // 30 minutes timeout

    // Shortcut-related variables
    private var isShortcutAccess: Boolean = false
    private var shortcutFolderId: Long = -1
    private var shortcutPosition: Int = -1

    companion object {
        const val ACTION_FINGERPRINT_SETTING_CHANGED = "com.shaadow.onecalculator.FINGERPRINT_SETTING_CHANGED"
        const val ACTION_FILES_CHANGED = "com.shaadow.onecalculator.FILES_CHANGED"
        const val ACTION_FOLDER_UPDATED = "com.shaadow.onecalculator.FOLDER_UPDATED"
        const val EXTRA_FOLDER_ID = "folder_id"
    }

    private val fingerprintSettingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Handle fingerprint setting changes if needed
        }
    }

    private val fileChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_FILES_CHANGED -> {
                    android.util.Log.d("MediaGalleryActivity", "Received FILES_CHANGED broadcast, refreshing folder list")
                    // Refresh folder list to update item counts
                    setupFolderList()
                }
                ACTION_FOLDER_UPDATED -> {
                    val folderId = intent.getLongExtra(EXTRA_FOLDER_ID, -1)
                    if (folderId != -1L) {
                        android.util.Log.d("MediaGalleryActivity", "Received FOLDER_UPDATED broadcast for folder $folderId, refreshing folder list")
                        // Refresh folder list to update specific folder's item count
                        setupFolderList()
                    }
                }
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

    // Activity result launcher for importing files
    private val importFilesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { intent ->
                val uris = mutableListOf<Uri>()

                // Handle multiple files
                intent.clipData?.let { clipData ->
                    for (i in 0 until clipData.itemCount) {
                        uris.add(clipData.getItemAt(i).uri)
                    }
                } ?: intent.data?.let { uri ->
                    // Handle single file
                    uris.add(uri)
                }

                if (uris.isNotEmpty()) {
                    // Process the selected files
                    processImportedFiles(uris)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize SharedPreferences for authentication state
        authPrefs = getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)

        // Prevent screenshots and screen recording for security
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        setupToolbar()
        initializeDatabase()
        initializeExternalStorage()

        // Check for shortcut access
        checkShortcutIntent()

        // Register broadcast receiver for fingerprint setting changes
        val filter = IntentFilter(ACTION_FINGERPRINT_SETTING_CHANGED)
        @Suppress("UnspecifiedRegisterReceiverFlag")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(fingerprintSettingReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(fingerprintSettingReceiver, filter)
        }

        // Register broadcast receiver for file changes
        val fileChangeFilter = IntentFilter().apply {
            addAction(ACTION_FILES_CHANGED)
            addAction(ACTION_FOLDER_UPDATED)
        }
        @Suppress("UnspecifiedRegisterReceiverFlag")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(fileChangeReceiver, fileChangeFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(fileChangeReceiver, fileChangeFilter)
        }

        // Load persistent authentication state
        sessionAuthenticated = isSessionAuthenticated()

        // Check if this is the first time opening the gallery (not from navigation)
        val isFirstLaunch = intent.getBooleanExtra("is_first_launch", true)
        val authenticationDone = intent.getBooleanExtra("authentication_done", false)
        val sessionAuthFromIntent = intent.getBooleanExtra("session_authenticated", false)

        // If session authentication is passed from intent, set it
        if (sessionAuthFromIntent && !sessionAuthenticated) {
            setSessionAuthenticated(true)
        }

        // Only require authentication if it's first launch AND session not authenticated AND authentication not already done
        if (isFirstLaunch && !sessionAuthenticated && !authenticationDone) {
            // Check authentication only on first launch
            checkAuthentication()
        } else {
            // Skip authentication for navigation within app, if already authenticated in session, or if authentication was already done
            proceedToGallery()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister broadcast receivers
        try {
            unregisterReceiver(fingerprintSettingReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver was not registered
            android.util.Log.w("MediaGalleryActivity", "Fingerprint broadcast receiver was not registered")
        }

        try {
            unregisterReceiver(fileChangeReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver was not registered
            android.util.Log.w("MediaGalleryActivity", "File change broadcast receiver was not registered")
        }

        // Reset per-activity authentication state, but keep session state
        isAuthenticated = false
        // Note: sessionAuthenticated persists until app is killed or timeout
    }

    override fun onResume() {
        super.onResume()
        @Suppress("SuspiciousEqualsCombination")
        android.util.Log.d("MediaGalleryActivity", "onResume called - isFromBackground: $isFromBackground, sessionAuthenticated: $sessionAuthenticated")

        // Only re-check authentication if session has timed out (not just from background)
        if (!isSessionAuthenticated()) {
            android.util.Log.d("MediaGalleryActivity", "Session authentication expired, re-checking authentication")
            checkAuthentication()
        } else {
            android.util.Log.d("MediaGalleryActivity", "Session authentication still valid")
        }

        // Reset the background flag after processing
        isFromBackground = false
    }

    override fun onPause() {
        super.onPause()
        // Set flag to indicate we're going to background (kept for other logic)
        isFromBackground = true
        // Note: Auto-lock is now only triggered when activity finishes
    }

    override fun onStop() {
        super.onStop()
        // Only auto-lock gallery when activity is finishing (user pressed back)
        // Do not auto-lock when app goes to background or during app switching
        if (isFinishing) {
            android.util.Log.d("MediaGalleryActivity", "Activity is finishing, auto-locking gallery")
            autoLockGallery()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Removed auto-lock on user leave hint to prevent locking when app goes to background
        // Gallery will only lock when user explicitly closes it (presses back)
        android.util.Log.d("MediaGalleryActivity", "User leaving activity - not auto-locking")
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

    private fun initializeExternalStorage() {
        // Prevent multiple initialization calls
        if (isExternalStorageInitialized) {
            android.util.Log.d("MediaGalleryActivity", "External storage already initialized, skipping")
            return
        }

        lifecycleScope.launch {
            try {
                android.util.Log.d("MediaGalleryActivity", "Initializing external storage...")

                val success = withContext(Dispatchers.IO) {
                    ExternalStorageManager.ensureHiddenDirectoryExists(this@MediaGalleryActivity)
                }
                if (success) {
                    android.util.Log.d("MediaGalleryActivity", "External storage initialized successfully")
                    val hiddenDir = ExternalStorageManager.getHiddenCalculatorDir(this@MediaGalleryActivity)
                    android.util.Log.d("MediaGalleryActivity", "Hidden folder location: ${hiddenDir?.absolutePath}")
                    isExternalStorageInitialized = true
                } else {
                    android.util.Log.w("MediaGalleryActivity", "Failed to initialize external storage")
                    // Show info to user about the hidden folder location
                    val hiddenDir = ExternalStorageManager.getHiddenCalculatorDir(this@MediaGalleryActivity)
                    if (hiddenDir != null) {
                        android.util.Log.i("MediaGalleryActivity", "Hidden folder will be created at: ${hiddenDir.absolutePath}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MediaGalleryActivity", "Error initializing external storage", e)
            }
        }
    }
    private fun checkShortcutIntent() {
        isShortcutAccess = intent.getBooleanExtra("is_shortcut_access", false)
        shortcutFolderId = intent.getLongExtra("shortcut_folder_id", -1)
        shortcutPosition = intent.getIntExtra("shortcut_position", -1)

        if (isShortcutAccess) {
            android.util.Log.d("MediaGalleryActivity", "Shortcut access detected - Position: $shortcutPosition, Folder ID: $shortcutFolderId")
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        // Check for MANAGE_EXTERNAL_STORAGE permission on Android 11+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                android.util.Log.d("MediaGalleryActivity", "Requesting MANAGE_EXTERNAL_STORAGE permission")
                requestManageExternalStoragePermission()
                return
            }
        }

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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        } else {
            setupFolderList()
        }
    }

    private fun requestManageExternalStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = android.net.Uri.parse("package:$packageName")
                }
                manageStorageLauncher.launch(intent)
            } catch (e: Exception) {
                android.util.Log.e("MediaGalleryActivity", "Error requesting MANAGE_EXTERNAL_STORAGE permission", e)
                // Fallback to regular permissions
                checkPermissions()
            }
        }
    }

    // Launcher for MANAGE_EXTERNAL_STORAGE permission
    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (android.os.Environment.isExternalStorageManager()) {
                android.util.Log.d("MediaGalleryActivity", "MANAGE_EXTERNAL_STORAGE permission granted")
                checkPermissions() // Continue with other permissions
            } else {
                android.util.Log.w("MediaGalleryActivity", "MANAGE_EXTERNAL_STORAGE permission denied")
                showManageStoragePermissionDialog()
            }
        }
    }

    private fun showManageStoragePermissionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Storage Permission Required")
            .setMessage("To create the hidden gallery folder (.1Calculator), the app needs permission to manage all files. Please grant this permission in the next screen.")
            .setPositiveButton("Grant Permission") { _, _ ->
                requestManageExternalStoragePermission()
            }
            .setNegativeButton("Continue Without") { _, _ ->
                // Continue with fallback storage
                checkPermissions()
            }
            .setCancelable(false)
            .show()
    }

    private fun setupFolderList() {
        folderAdapter = EncryptedFolderAdapter(
            onFolderClick = { folderWithCount ->
                showFolderPinDialog(folderWithCount.folder)
            },
            onFolderLongClick = { folderWithCount ->
                showFolderOptionsDialog(folderWithCount.folder)
            },
            onFolderMenuClick = { folderWithCount, view ->
                showFolderPopupMenu(folderWithCount.folder, view)
            },
            onAddFolderClick = {
                showCreateFolderDialog()
            },
            onSelectionChanged = { allSelected ->
                updateBulkActionVisibility(allSelected)
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
                // Only create default folders if this is the very first time (no folders ever existed)
                // and user hasn't explicitly deleted them all
                val prefs = getSharedPreferences("gallery_prefs", Context.MODE_PRIVATE)
                val hasCreatedDefaultFolders = prefs.getBoolean("has_created_default_folders", false)

                if (folders.isEmpty() && !hasCreatedDefaultFolders) {
                    // Create default folders on first access only
                    withContext(Dispatchers.IO) {
                        createDefaultFolders()
                    }
                    // Mark that we've created default folders
                    prefs.edit().putBoolean("has_created_default_folders", true).apply()
                } else {
                    // Convert to FolderWithCount - do this in background thread
                    val foldersWithCount = withContext(Dispatchers.IO) {
                        folders.map { folder ->
                            val fileCount = database.encryptedFileDao().getFileCountInFolderSync(folder.id)
                            android.util.Log.d("MediaGalleryActivity", "Folder ${folder.name} has $fileCount files")
                            FolderWithCount(folder, fileCount)
                        }
                    }
                    android.util.Log.d("MediaGalleryActivity", "Submitting ${foldersWithCount.size} folders to adapter")
                    folderAdapter.submitFoldersWithAddButton(foldersWithCount)
                    updateEmptyState(folders.isEmpty())
                    android.util.Log.d("MediaGalleryActivity", "Folder list setup completed")
                }
            }
        }
    }

    private fun checkAuthentication() {
        android.util.Log.d("MediaGalleryActivity", "checkAuthentication called - isAuthenticated: $isAuthenticated, isFromBackground: $isFromBackground, sessionAuthenticated: $sessionAuthenticated")

        // Skip authentication if user is already authenticated in this session
        if (sessionAuthenticated) {
            android.util.Log.d("MediaGalleryActivity", "User already authenticated in session, skipping fingerprint prompt")
            // Ensure master password is set for folder operations
            if (currentMasterPassword.isEmpty()) {
                currentMasterPassword = "0000"
                android.util.Log.d("MediaGalleryActivity", "Master password set for existing session")
            }
            proceedToGallery()
            return
        }

        // Skip authentication if already authenticated in this activity
        if (isAuthenticated) {
            android.util.Log.d("MediaGalleryActivity", "User already authenticated in this activity session, skipping fingerprint prompt")
            // Ensure master password is set for folder operations
            if (currentMasterPassword.isEmpty()) {
                currentMasterPassword = "0000"
                android.util.Log.d("MediaGalleryActivity", "Master password set for existing activity session")
            }
            proceedToGallery()
            return
        }

        // Skip authentication if this is a configuration change (screen rotation, etc.)
        if (isChangingConfigurations) {
            android.util.Log.d("MediaGalleryActivity", "Configuration change detected, skipping authentication")
            // Ensure master password is set for folder operations
            if (currentMasterPassword.isEmpty()) {
                currentMasterPassword = "0000"
                android.util.Log.d("MediaGalleryActivity", "Master password set for configuration change")
            }
            proceedToGallery()
            return
        }

        // Check if biometric authentication is available
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                android.util.Log.d("MediaGalleryActivity", "Biometric authentication is available")
                showBiometricPrompt()
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                android.util.Log.e("MediaGalleryActivity", "No biometric hardware available")
                showErrorAndExit("Biometric authentication not available on this device")
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                android.util.Log.e("MediaGalleryActivity", "Biometric hardware unavailable")
                showErrorAndExit("Biometric hardware is currently unavailable")
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                android.util.Log.e("MediaGalleryActivity", "No biometric credentials enrolled")
                showErrorAndExit("No fingerprints enrolled. Please set up fingerprint authentication in device settings")
            }
            else -> {
                android.util.Log.e("MediaGalleryActivity", "Biometric authentication not available")
                showErrorAndExit("Biometric authentication is not available")
            }
        }
    }

    // Helper methods for persistent authentication state
    private fun setSessionAuthenticated(authenticated: Boolean) {
        sessionAuthenticated = authenticated
        val editor = authPrefs.edit()
        if (authenticated) {
            editor.putBoolean(KEY_SESSION_AUTHENTICATED, true)
            editor.putLong(KEY_AUTH_TIMESTAMP, System.currentTimeMillis())
        } else {
            editor.remove(KEY_SESSION_AUTHENTICATED)
            editor.remove(KEY_AUTH_TIMESTAMP)
        }
        editor.commit() // Use commit() for synchronous write to ensure immediate persistence
        android.util.Log.d("MediaGalleryActivity", "Session authentication state saved: $authenticated")
    }

    private fun isSessionAuthenticated(): Boolean {
        // First check the in-memory flag for immediate response
        if (sessionAuthenticated) {
            // Double-check with SharedPreferences to ensure consistency
            val prefsAuthenticated = authPrefs.getBoolean(KEY_SESSION_AUTHENTICATED, false)
            if (!prefsAuthenticated) {
                // In-memory state is stale, reset it
                sessionAuthenticated = false
                return false
            }

            // Check if authentication has timed out
            val authTimestamp = authPrefs.getLong(KEY_AUTH_TIMESTAMP, 0)
            val currentTime = System.currentTimeMillis()
            val timeDiff = currentTime - authTimestamp

            if (timeDiff > AUTH_TIMEOUT_MS) {
                android.util.Log.d("MediaGalleryActivity", "Authentication timed out after ${timeDiff / 1000} seconds")
                // Clear expired authentication
                setSessionAuthenticated(false)
                return false
            }

            android.util.Log.d("MediaGalleryActivity", "Valid persistent authentication found, time remaining: ${(AUTH_TIMEOUT_MS - timeDiff) / 1000} seconds")
            return true
        }

        // Check SharedPreferences if in-memory flag is false
        val authenticated = authPrefs.getBoolean(KEY_SESSION_AUTHENTICATED, false)
        if (!authenticated) {
            return false
        }

        // Check if authentication has timed out
        val authTimestamp = authPrefs.getLong(KEY_AUTH_TIMESTAMP, 0)
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - authTimestamp

        if (timeDiff > AUTH_TIMEOUT_MS) {
            android.util.Log.d("MediaGalleryActivity", "Authentication timed out after ${timeDiff / 1000} seconds")
            // Clear expired authentication
            setSessionAuthenticated(false)
            return false
        }

        // Update in-memory flag to match SharedPreferences
        sessionAuthenticated = true
        android.util.Log.d("MediaGalleryActivity", "Valid persistent authentication found, time remaining: ${(AUTH_TIMEOUT_MS - timeDiff) / 1000} seconds")
        return true
    }

    private fun clearAuthenticationState() {
        setSessionAuthenticated(false)
        isAuthenticated = false
        android.util.Log.d("MediaGalleryActivity", "Authentication state cleared")
    }




    // Removed: setupPasswordLockScreen() - no longer needed with fingerprint-only authentication

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
                    toggleSelectAllFolders(menuItem)
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
                    showHelp()
                    true
                }
                R.id.action_storage_info -> {
                    showStorageInfo()
                    true
                }
                R.id.action_export_gallery -> {
                    exportEntireGallery()
                    true
                }
                R.id.action_import_gallery -> {
                    importGalleryBackup()
                    true
                }
                R.id.action_report_bug -> {
                    showBugReportDialog()
                    true
                }
                else -> false
            }
        }
    }


    private fun autoLockGallery() {
        // Auto-lock gallery by finishing the activity
        // This is called when the activity is finishing (user pressed back)
        if (!isAutoLocking) {
            android.util.Log.d("MediaGalleryActivity", "Auto-locking gallery - activity finishing")

            // Clear authentication state when auto-locking
            clearAuthenticationState()

            // Set flag to prevent multiple auto-lock calls
            isAutoLocking = true

            // Finish the activity to "lock" the gallery
            android.util.Log.d("MediaGalleryActivity", "Finishing activity to lock gallery")
            finish()

            // Reset the flag after a delay
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                isAutoLocking = false
            }, 1000)
        } else {
            android.util.Log.d("MediaGalleryActivity", "Skipping auto-lock - already auto-locking")
        }
    }

    private fun showSystemUI() {
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
    }

    // Removed: setupPinLockScreen() - no longer needed with fingerprint-only authentication

    private fun updatePinDisplay(pinDigits: Array<android.widget.TextView>, pin: String) {
        pinDigits.forEachIndexed { index, textView ->
            textView.text = if (index < pin.length) "●" else ""
        }
    }

    // Removed: verifyPassword() - no longer needed with fingerprint-only authentication

    // Removed: verifyPin() - no longer needed with fingerprint-only authentication

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

    // Removed: setupFingerprintAuthentication() - no longer needed with fingerprint-only lock screen

    private fun showBiometricPrompt() {
        val executor: Executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this as FragmentActivity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    android.util.Log.e("MediaGalleryActivity", "Biometric authentication error: $errorCode - $errString")

                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                            // User cancelled, exit the activity
                            finish()
                        }
                        BiometricPrompt.ERROR_LOCKOUT,
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                            showErrorAndExit("Too many failed attempts. Please try again later.")
                        }
                        else -> {
                            Toast.makeText(this@MediaGalleryActivity, "Authentication error: $errString", Toast.LENGTH_LONG).show()
                            // Allow retry by showing the prompt again after a delay
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                showBiometricPrompt()
                            }, 2000)
                        }
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    android.util.Log.d("MediaGalleryActivity", "Biometric authentication succeeded")

                    // Set default master password for file encryption since we only use fingerprint
                    currentMasterPassword = "0000" // Default password for file encryption

                    // Mark user as authenticated to prevent repeated prompts
                    isAuthenticated = true
                    setSessionAuthenticated(true) // Use persistent state

                    // Authentication success is indicated by gallery opening - no toast needed

                    // Reset auto-lock flag when user successfully unlocks
                    isAutoLocking = false

                    if (isShortcutAccess && shortcutPosition >= 0) {
                        // Handle shortcut access - directly open target folder
                        handleShortcutAccess()
                    } else {
                        // Normal gallery access
                        proceedToGallery()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    android.util.Log.d("MediaGalleryActivity", "Biometric authentication failed - fingerprint not recognized")
                    // Biometric prompt already shows feedback - no additional toast needed
                    // Don't exit on failed recognition, let user try again
                }
            })

        val promptInfo = if (isShortcutAccess && shortcutPosition >= 0) {
            // For shortcut access, get the specific folder name for better UX
            getBiometricPromptInfoForShortcut()
        } else {
            // Normal gallery access
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Hidden Gallery")
                .setSubtitle("Use your fingerprint to access the hidden gallery")
                .setDescription("Place your finger on the fingerprint sensor")
                .setNegativeButtonText("Cancel")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build()
        }

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            android.util.Log.e("MediaGalleryActivity", "Error showing biometric prompt", e)
            showErrorAndExit("Failed to show fingerprint authentication: ${e.message}")
        }
    }

    private fun getBiometricPromptInfoForShortcut(): BiometricPrompt.PromptInfo {
        return if (shortcutPosition == 0) {
            // Special case: position 0 opens the main gallery screen
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Hidden Gallery")
                .setSubtitle("Use fingerprint to open Hidden Gallery")
                .setDescription("Place your finger on the fingerprint sensor")
                .setNegativeButtonText("Cancel")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build()
        } else {
            // For positions 1-9, try to get the folder name
            val folderName = getFolderNameForPosition(shortcutPosition)
            if (folderName != null) {
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock Folder")
                    .setSubtitle("Use fingerprint to open $folderName")
                    .setDescription("Place your finger on the fingerprint sensor")
                    .setNegativeButtonText("Cancel")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                    .build()
            } else {
                // Fallback if folder name can't be retrieved
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock Folder")
                    .setSubtitle("Calculator shortcut to position $shortcutPosition")
                    .setDescription("Place your finger on the fingerprint sensor")
                    .setNegativeButtonText("Cancel")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                    .build()
            }
        }
    }

    private fun getFolderNameForPosition(position: Int): String? {
        return try {
            // Adjust position for 1-based indexing (position 1 = folders[0], position 2 = folders[1], etc.)
            val folderIndex = position - 1

            // Get all folders synchronously (this should be fast since it's just a database query)
            val folders = runBlocking(Dispatchers.IO) {
                encryptedFolderDao.getAllFoldersSync()
            }

            if (folderIndex >= 0 && folderIndex < folders.size) {
                folders[folderIndex].name
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("MediaGalleryActivity", "Error getting folder name for position $position", e)
            null
        }
    }

    private fun proceedToGallery() {
        // Ensure hidden directory exists before proceeding with gallery operations (only if not already initialized)
        if (!isExternalStorageInitialized) {
            ExternalStorageManager.ensureHiddenDirectoryExists(this)
        }

        checkPermissions()
        setupClickListeners()

        // Perform post-authentication file validation to remove any non-media files
        validateAndCleanNonMediaFiles()
    }

    private fun handleShortcutAccess() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("MediaGalleryActivity", "Handling shortcut access for position: $shortcutPosition")

                // Special case: position 0 opens the main gallery screen
                if (shortcutPosition == 0) {
                    android.util.Log.d("MediaGalleryActivity", "Shortcut position 0: showing main gallery screen")
                    // Just proceed to gallery without opening any folder
                    proceedToGallery()
                    return@launch
                }

                // For positions 1-9, open specific folders
                val folders = withContext(Dispatchers.IO) {
                    encryptedFolderDao.getAllFoldersSync()
                }

                // Adjust position for 1-based indexing (position 1 = folders[0], position 2 = folders[1], etc.)
                val folderIndex = shortcutPosition - 1

                if (folderIndex >= 0 && folderIndex < folders.size) {
                    val targetFolder = folders[folderIndex]
                    android.util.Log.d("MediaGalleryActivity", "Opening shortcut folder: ${targetFolder.name} (ID: ${targetFolder.id}) at position $shortcutPosition")

                    // For shortcuts, finish this activity before opening folder to prevent it from staying in back stack
                    openFolderContentsForShortcut(targetFolder)
                    finish() // Close MediaGalleryActivity so back from folder goes directly to calculator
                } else {
                    android.util.Log.e("MediaGalleryActivity", "Invalid shortcut position: $shortcutPosition, available folders: ${folders.size}")
                    Toast.makeText(this@MediaGalleryActivity, "No folder at position $shortcutPosition", Toast.LENGTH_SHORT).show()
                    // Fall back to normal gallery
                    proceedToGallery()
                }
            } catch (e: Exception) {
                android.util.Log.e("MediaGalleryActivity", "Error handling shortcut access", e)
                Toast.makeText(this@MediaGalleryActivity, "Error accessing folder", Toast.LENGTH_SHORT).show()
                // Fall back to normal gallery
                proceedToGallery()
            }
        }
    }

    private fun openFolderContentsForShortcut(folder: EncryptedFolderEntity) {
        try {
            android.util.Log.d("MediaGalleryActivity", "Opening folder contents for shortcut: ${folder.name}, ID: ${folder.id}")

            // Ensure master password is available for shortcut access
            if (currentMasterPassword.isEmpty()) {
                android.util.Log.w("MediaGalleryActivity", "Master password is empty for shortcut, setting default")
                currentMasterPassword = "0000"
            }

            val intent = Intent(this, NewFolderContentsActivity::class.java)
            intent.putExtra("folder_id", folder.id)
            intent.putExtra("master_password", currentMasterPassword)
            intent.putExtra("is_shortcut_access", true) // Flag for shortcut navigation

            android.util.Log.d("MediaGalleryActivity", "Starting NewFolderContentsActivity with folder ID: ${folder.id}")
            startActivity(intent)
            android.util.Log.d("MediaGalleryActivity", "Intent started successfully")
        } catch (e: Exception) {
            android.util.Log.e("MediaGalleryActivity", "Error opening folder contents for shortcut", e)
            Toast.makeText(this, "Error opening folder: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showErrorAndExit(message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Authentication Error")
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun hideSystemUI() {
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

    private fun updateFingerprintSectionVisibility(enabled: Boolean) {
        // With fingerprint-only authentication, we don't need to update visibility based on settings
        // The fingerprint lock screen handles its own UI setup
        android.util.Log.d("MediaGalleryActivity", "Fingerprint-only mode: ignoring visibility update")
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
            android.util.Log.d("MediaGalleryActivity", "Creating default folders for new user...")

            val defaultFolders = listOf(
                DefaultFolderInfo("Photos", "ic_folder", "Store your private photos", "icon_bg_1", 1),
                DefaultFolderInfo("Videos", "ic_folder", "Store your private videos", "icon_bg_2", 2),
                DefaultFolderInfo("Audios", "ic_folder", "Store your private audio files", "icon_bg_3", 3),
                DefaultFolderInfo("Others", "ic_folder", "Store other private files", "icon_bg_4", 4)
            )

            for (folderInfo in defaultFolders) {
                // Check if folder already exists
                val existingFolder = encryptedFolderDao.getFolderByName(folderInfo.name)
                if (existingFolder == null) {
                    // Create unlocked folder by default (no password needed)
                    val folder = EncryptedFolderEntity(
                        name = folderInfo.name,
                        passwordHash = "",
                        salt = "",
                        isLocked = false
                    )

                    // Save to database
                    encryptedFolderDao.insertFolder(folder)
                    android.util.Log.d("MediaGalleryActivity", "Created default unlocked folder: ${folderInfo.name}")
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
        val iconBackground: String,
        val order: Int
    )

    private fun showWelcomeMessage() {
        val hiddenDir = ExternalStorageManager.getHiddenCalculatorDir(this)
        val storageInfo = if (hiddenDir != null) {
            "📂 Storage Location: ${hiddenDir.absolutePath}\n\n"
        } else {
            "📂 Storage Location: Device external storage\n\n"
        }

        val message = buildString {
            append("🎉 Welcome to Hidden Gallery!\n\n")
            append("Your encrypted files are stored securely in:\n")
            append("$storageInfo")
            append("We've created 4 default folders for you:\n\n")
            append("📷 Photos - For your private photos\n")
            append("🎥 Videos - For your private videos\n")
            append("🎵 Audios - For your private audio files\n")
            append("📁 Others - For other private files\n\n")
            append("🔓 These folders are unlocked by default for easy access.\n\n")
            append("🔒 You can lock any folder with a PIN using the ⋮ menu.\n\n")
            append("➕ Use the '+' button to create more folders\n\n")
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
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_folder_pin, null)
        val folderNameInput = dialogView.findViewById<TextInputEditText>(R.id.folder_name_input)
        val folderPinInput = dialogView.findViewById<TextInputEditText>(R.id.folder_pin_input)
        val folderConfirmPinInput = dialogView.findViewById<TextInputEditText>(R.id.folder_confirm_pin_input)
        val lockFolderSwitch = dialogView.findViewById<SwitchCompat>(R.id.lock_folder_switch)
        val folderNameLayout = dialogView.findViewById<TextInputLayout>(R.id.folder_name_input_layout)
        val folderPinLayout = dialogView.findViewById<TextInputLayout>(R.id.folder_pin_input_layout)
        val folderConfirmPinLayout = dialogView.findViewById<TextInputLayout>(R.id.folder_confirm_pin_input_layout)

        // Initially hide PIN fields since locking is disabled by default
        updatePinFieldsVisibility(dialogView, false)

        // Handle switch toggle
        lockFolderSwitch.setOnCheckedChangeListener { _, isChecked ->
            updatePinFieldsVisibility(dialogView, isChecked)
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<View>(R.id.cancel_button).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.create_button).setOnClickListener {
            val name = folderNameInput.text?.toString()?.trim() ?: ""
            val isLocked = lockFolderSwitch.isChecked
            val pin = if (isLocked) folderPinInput.text?.toString() ?: "" else ""
            val confirmPin = if (isLocked) folderConfirmPinInput.text?.toString() ?: "" else ""

            // Clear previous errors
            folderNameLayout.error = null
            folderPinLayout.error = null
            folderConfirmPinLayout.error = null

            var hasError = false

            // Validate folder name
            if (name.isEmpty()) {
                folderNameLayout.error = "Folder name is required"
                hasError = true
            } else if (name.length < 2) {
                folderNameLayout.error = "Folder name must be at least 2 characters"
                hasError = true
            }

            // Validate PIN only if locking is enabled
            if (isLocked) {
                if (pin.length != 4) {
                    folderPinLayout.error = "PIN must be exactly 4 digits"
                    hasError = true
                } else if (!pin.all { it.isDigit() }) {
                    folderPinLayout.error = "PIN must contain only numbers"
                    hasError = true
                } else if (isWeakPin(pin)) {
                    folderPinLayout.error = "PIN is too weak. Avoid repeating digits or sequential patterns"
                    hasError = true
                }

                if (confirmPin != pin) {
                    folderConfirmPinLayout.error = "PINs do not match"
                    hasError = true
                }

                // Check PIN reuse
                if (pin.isNotEmpty() && runBlocking { isPinAlreadyUsed(pin) }) {
                    folderPinLayout.error = "This PIN is already used by another folder"
                    hasError = true
                }
            }

            if (!hasError) {
                createFolder(name, if (isLocked) pin else "", isLocked)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun updatePinFieldsVisibility(dialogView: View, isVisible: Boolean) {
        val pinLayout = dialogView.findViewById<TextInputLayout>(R.id.folder_pin_input_layout)
        val confirmPinLayout = dialogView.findViewById<TextInputLayout>(R.id.folder_confirm_pin_input_layout)

        pinLayout?.visibility = if (isVisible) View.VISIBLE else View.GONE
        confirmPinLayout?.visibility = if (isVisible) View.VISIBLE else View.GONE

        // Clear PIN fields when hiding
        if (!isVisible) {
            dialogView.findViewById<TextInputEditText>(R.id.folder_pin_input)?.setText("")
            dialogView.findViewById<TextInputEditText>(R.id.folder_confirm_pin_input)?.setText("")
            pinLayout?.error = null
            confirmPinLayout?.error = null
        }
    }

    private suspend fun isPinAlreadyUsed(pin: String, excludeFolderId: Long? = null): Boolean {
        return try {
            val folders = withContext(Dispatchers.IO) {
                encryptedFolderDao.getAllFoldersSync()
            }
            folders.any { folder ->
                folder.id != excludeFolderId &&
                folder.isLocked &&
                EncryptionUtils.hashPassword(pin, folder.salt) == folder.passwordHash
            }
        } catch (e: Exception) {
            android.util.Log.e("MediaGalleryActivity", "Error checking PIN reuse", e)
            false
        }
    }

    private fun isWeakPin(pin: String): Boolean {
        // Check for repeating digits (000, 111, 222, etc.)
        if (pin.all { it == pin[0] }) {
            return true
        }

        // Check for sequential digits (012, 123, 234, etc.)
        val sequentialPatterns = listOf("012", "123", "234", "345", "456", "567", "678", "789", "890", "901")
        if (sequentialPatterns.contains(pin)) {
            return true
        }

        // Check for reverse sequential (210, 321, 432, etc.)
        val reverseSequentialPatterns = listOf("210", "321", "432", "543", "654", "765", "876", "987", "098", "109")
        if (reverseSequentialPatterns.contains(pin)) {
            return true
        }

        return false
    }


    private fun createFolder(name: String, password: String, isLocked: Boolean = false) {
        lifecycleScope.launch {
            try {
                // Check if folder name already exists
                val existingFolder = encryptedFolderDao.getFolderByName(name)
                if (existingFolder != null) {
                    Toast.makeText(this@MediaGalleryActivity, "Folder name already exists", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Generate salt and hash password (only if locked)
                val salt = if (isLocked) EncryptionUtils.generateSalt() else ""
                val hashedPassword = if (isLocked) EncryptionUtils.hashPassword(password, salt) else ""

                // Create virtual folder entity
                val folder = EncryptedFolderEntity(
                    name = name,
                    passwordHash = hashedPassword,
                    salt = salt,
                    isLocked = isLocked
                )

                // Save to database
                val folderId = encryptedFolderDao.insertFolder(folder)
                android.util.Log.d("MediaGalleryActivity", "Folder '$name' created with ID: $folderId, locked: $isLocked")

                // Show appropriate success message
                val message = if (isLocked) {
                    "Folder '$name' created with PIN protection"
                } else {
                    "Folder '$name' created successfully"
                }
                Toast.makeText(this@MediaGalleryActivity, message, Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                android.util.Log.e("MediaGalleryActivity", "Error creating folder", e)
                Toast.makeText(this@MediaGalleryActivity, "Failed to create folder: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Removed: No longer need physical folder creation
    // Virtual folders are managed in database only

    private fun showFolderPopupMenu(folder: EncryptedFolderEntity, anchorView: View) {
        val popup = androidx.appcompat.widget.PopupMenu(this, anchorView)
        popup.menuInflater.inflate(R.menu.menu_folder_options, popup.menu)

        // Add import option to the menu
        val importId = 1001 // Use a unique ID that won't conflict with existing menu items
        popup.menu.add(0, importId, 4, "Import Files")

        // Add lock/unlock option based on current state
        val lockUnlockId = 1002
        val lockUnlockText = if (folder.isLocked) "Unlock Folder" else "Lock Folder"
        popup.menu.add(0, lockUnlockId, 2, lockUnlockText)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_open_folder -> {
                    showFolderPinDialog(folder)
                    true
                }
                R.id.action_rename_folder -> {
                    showRenameFolderDialog(folder)
                    true
                }
                R.id.action_change_pin -> {
                    if (folder.isLocked) {
                        showChangePinDialog(folder)
                    } else {
                        Toast.makeText(this, "Folder is not locked. Lock it first to set a PIN.", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                R.id.action_export_folder -> {
                    exportFolder(folder)
                    true
                }
                importId -> {
                    importFilesToFolder(folder)
                    true
                }
                lockUnlockId -> {
                    if (folder.isLocked) {
                        showUnlockFolderDialog(folder)
                    } else {
                        showLockFolderDialog(folder)
                    }
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
        val dialogView = layoutInflater.inflate(R.layout.dialog_rename_file, null)
        val nameInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.file_name_input)
        val nameLayout = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.file_name_input_layout)

        nameInput.setText(folder.name)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setTitle("Rename Folder")
            .setPositiveButton("Rename") { _, _ ->
                val newName = nameInput.text?.toString()?.trim()
                if (newName.isNullOrEmpty()) {
                    Toast.makeText(this, "Folder name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (newName == folder.name) {
                    Toast.makeText(this, "Please enter a different name", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                renameFolder(folder, newName)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun renameFolder(folder: EncryptedFolderEntity, newName: String) {
        lifecycleScope.launch {
            try {
                // Check if name already exists
                val existingFolder = database.encryptedFolderDao().getFolderByName(newName)
                if (existingFolder != null && existingFolder.id != folder.id) {
                    Toast.makeText(this@MediaGalleryActivity, "Folder name already exists", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Update folder name
                val updatedFolder = folder.copy(name = newName)
                database.encryptedFolderDao().updateFolder(updatedFolder)

                Toast.makeText(this@MediaGalleryActivity, "Folder renamed successfully", Toast.LENGTH_SHORT).show()

                // Refresh the folder list with a small delay to ensure database consistency
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    setupFolderList()
                }, 100)

            } catch (e: Exception) {
                android.util.Log.e("MediaGalleryActivity", "Error renaming folder", e)
                Toast.makeText(this@MediaGalleryActivity, "Error renaming folder: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLockFolderDialog(folder: EncryptedFolderEntity) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_folder_pin, null)

        // Hide folder name input since we're locking existing folder
        val folderNameLayout = dialogView.findViewById<TextInputLayout>(R.id.folder_name_input_layout)
        folderNameLayout?.visibility = View.GONE

        // Update title
        val titleText = dialogView.findViewById<TextView>(android.R.id.text1)
        if (titleText != null) {
            titleText.text = "Lock Folder: ${folder.name}"
        }

        // Update button text
        val createButton = dialogView.findViewById<MaterialButton>(R.id.create_button)
        createButton?.text = "Lock Folder"

        // Get PIN fields
        val pinInput = dialogView.findViewById<TextInputEditText>(R.id.folder_pin_input)
        val confirmPinInput = dialogView.findViewById<TextInputEditText>(R.id.folder_confirm_pin_input)
        val pinLayout = dialogView.findViewById<TextInputLayout>(R.id.folder_pin_input_layout)
        val confirmPinLayout = dialogView.findViewById<TextInputLayout>(R.id.folder_confirm_pin_input_layout)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        createButton?.setOnClickListener {
            val pin = pinInput?.text?.toString() ?: ""
            val confirmPin = confirmPinInput?.text?.toString() ?: ""

            // Clear previous errors
            pinLayout?.error = null
            confirmPinLayout?.error = null

            var hasError = false

            // Validate PIN
            if (pin.length != 4) {
                pinLayout?.error = "PIN must be exactly 4 digits"
                hasError = true
            } else if (!pin.all { it.isDigit() }) {
                pinLayout?.error = "PIN must contain only numbers"
                hasError = true
            } else if (isWeakPin(pin)) {
                pinLayout?.error = "PIN is too weak. Avoid repeating digits or sequential patterns"
                hasError = true
            }

            if (confirmPin != pin) {
                confirmPinLayout?.error = "PINs do not match"
                hasError = true
            }

            // Check PIN reuse
            if (pin.isNotEmpty() && runBlocking { isPinAlreadyUsed(pin) }) {
                pinLayout?.error = "This PIN is already used by another folder"
                hasError = true
            }

            if (!hasError) {
                lockFolder(folder, pin)
                dialog.dismiss()
            }
        }

        val cancelButton = dialogView.findViewById<MaterialButton>(R.id.cancel_button)
        cancelButton?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showUnlockFolderDialog(folder: EncryptedFolderEntity) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Unlock Folder")
            .setMessage("Are you sure you want to unlock '${folder.name}'? Anyone will be able to access this folder without a PIN.")
            .setPositiveButton("Unlock") { _, _ ->
                unlockFolder(folder)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun lockFolder(folder: EncryptedFolderEntity, pin: String) {
        lifecycleScope.launch {
            try {
                // Generate salt and hash password
                val salt = EncryptionUtils.generateSalt()
                val hashedPassword = EncryptionUtils.hashPassword(pin, salt)

                // Update folder with PIN
                val updatedFolder = folder.copy(
                    passwordHash = hashedPassword,
                    salt = salt,
                    isLocked = true
                )

                withContext(Dispatchers.IO) {
                    database.encryptedFolderDao().updateFolder(updatedFolder)
                }

                Toast.makeText(this@MediaGalleryActivity, "Folder '${folder.name}' locked successfully", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                android.util.Log.e("MediaGalleryActivity", "Error locking folder", e)
                Toast.makeText(this@MediaGalleryActivity, "Error locking folder: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun unlockFolder(folder: EncryptedFolderEntity) {
        lifecycleScope.launch {
            try {
                // Update folder to remove PIN
                val updatedFolder = folder.copy(
                    passwordHash = "",
                    salt = "",
                    isLocked = false
                )

                withContext(Dispatchers.IO) {
                    database.encryptedFolderDao().updateFolder(updatedFolder)
                }

                Toast.makeText(this@MediaGalleryActivity, "Folder '${folder.name}' unlocked successfully", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                android.util.Log.e("MediaGalleryActivity", "Error unlocking folder", e)
                Toast.makeText(this@MediaGalleryActivity, "Error unlocking folder: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
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

    // Removed: showRecoveryDialog() - no longer needed with fingerprint-only authentication

    // Removed: RecoveryData class and proceedWithRecovery() - no longer needed with fingerprint-only authentication

    // Removed: showNoRecoverySetupDialog() - no longer needed with fingerprint-only authentication

    // Removed: checkRecoveryAttemptLimits(), incrementRecoveryAttempt(), showRecoveryError() - no longer needed with fingerprint-only authentication

    // Removed: CredentialInfo class and getCurrentCredentials() - no longer needed with fingerprint-only authentication

    // Removed: showCurrentCredentials() - no longer needed with fingerprint-only authentication

    // Removed: copyToClipboard() - no longer needed with fingerprint-only authentication

    // Removed: autoFillPin() and autoFillPassword() - no longer needed with fingerprint-only authentication

    // Removed: showPinHintDialog() and showAdditionalHelpDialog() - no longer needed with fingerprint-only authentication

    // Removed: showRecoveryOptionsDialog() - no longer needed with fingerprint-only authentication

    // Removed: showRecoveryQuestionDialog() - no longer needed with fingerprint-only authentication

    // Removed: verifyRecoveryAnswer() and hashAnswerWithSalt() - no longer needed with fingerprint-only authentication

    // Removed: showAnswerError(), showMaxAttemptsReached(), showSuccessfulRecovery() - no longer needed with fingerprint-only authentication

    // Removed duplicate toolbar menu actions - keeping the implementations below

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
            append("• Choose your own PIN when locking folders\n\n")
            append("⚡ Calculator Shortcuts:\n")
            append("• Enter digit 0-9 in calculator\n")
            append("• Long-press = button for 4 seconds\n")
            append("• '0' opens main gallery screen\n")
            append("• '1' opens 1st folder, '2' opens 2nd folder, etc.\n")
            append("• Example: Enter '0' → long-press = → opens gallery\n")
            append("• Example: Enter '1' → long-press = → opens Photos folder\n")
            append("• Example: Enter '2' → long-press = → opens Videos folder\n\n")
            append("💡 Tips:\n")
            append("• Use ⋮ menu in header for bulk operations\n")
            append("• Export folders to backup your data\n")
            append("• Change PINs regularly for better security\n")
            append("• Check folder info to see shortcut position")
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
        if (!folder.isLocked) {
            Toast.makeText(this, "Folder is not locked. Lock it first to change PIN.", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_create_folder_pin, null)

        // Hide the folder name input since we're changing PIN for existing folder
        val folderNameLayout = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.folder_name_input_layout)
        folderNameLayout?.visibility = View.GONE

        // Update title
        val titleText = dialogView.findViewById<android.widget.TextView>(android.R.id.text1)
        if (titleText != null) {
            titleText.text = "Change PIN for ${folder.name}"
        }

        // Update button text
        val createButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.create_button)
        createButton?.text = "Change PIN"

        // Use existing PIN fields
        val currentPinInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.folder_pin_input)
        val newPinInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.folder_confirm_pin_input)
        val currentPinLayout = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.folder_pin_input_layout)
        val newPinLayout = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.folder_confirm_pin_input_layout)

        // Update hints
        currentPinLayout?.hint = "Current PIN"
        newPinLayout?.hint = "New PIN"

        // Add a third field for confirming new PIN
        val confirmPinLayout = com.google.android.material.textfield.TextInputLayout(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 20, 0, 0)
            }
            boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
            boxBackgroundColor = resources.getColor(R.color.card_bg_2a2833, null)
            boxStrokeColor = resources.getColor(R.color.accent_purple_bb86fc, null)
            boxStrokeWidth = 2
            setBoxCornerRadii(12f, 12f, 12f, 12f)
            hintTextColor = resources.getColorStateList(R.color.accent_purple_bb86fc, null)
            setStartIconDrawable(R.drawable.ic_lock)
            setStartIconTintList(resources.getColorStateList(R.color.accent_purple_bb86fc, null))
            isPasswordVisibilityToggleEnabled = true
        }

        val confirmPinInput = com.google.android.material.textfield.TextInputEditText(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            hint = "Confirm New PIN"
            setTextColor(resources.getColor(R.color.white_ffffff, null))
            setHintTextColor(resources.getColor(R.color.text_muted_b0aec0, null))
            textSize = 16f
            maxLines = 1
            filters = arrayOf(android.text.InputFilter.LengthFilter(3))
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setPadding(resources.getDimensionPixelSize(R.dimen.padding_large), resources.getDimensionPixelSize(R.dimen.padding_large), resources.getDimensionPixelSize(R.dimen.padding_large), resources.getDimensionPixelSize(R.dimen.padding_large))
        }

        confirmPinLayout.addView(confirmPinInput)

        // Add the confirm PIN field to the layout
        // Find the input fields container and add the confirm PIN field
        val rootView = dialogView as? android.widget.LinearLayout
        if (rootView != null) {
            // The input fields are in the second child (index 1)
            val inputContainer = rootView.getChildAt(1) as? android.widget.LinearLayout
            if (inputContainer != null) {
                inputContainer.addView(confirmPinLayout, 2) // Add after the existing PIN fields
            }
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        createButton?.setOnClickListener {
            val currentPin = currentPinInput?.text?.toString() ?: ""
            val newPin = newPinInput?.text?.toString() ?: ""
            val confirmPin = confirmPinInput?.text?.toString() ?: ""

            // Clear previous errors
            currentPinLayout?.error = null
            newPinLayout?.error = null
            confirmPinLayout?.error = null

            var hasError = false

            // Validate current PIN
            if (currentPin.isEmpty()) {
                currentPinLayout?.error = "Current PIN is required"
                hasError = true
            } else if (currentPin.length != 4 || !currentPin.all { it.isDigit() }) {
                currentPinLayout?.error = "Current PIN must be 4 digits"
                hasError = true
            }

            // Validate new PIN
            if (newPin.isEmpty()) {
                newPinLayout?.error = "New PIN is required"
                hasError = true
            } else if (newPin.length != 4 || !newPin.all { it.isDigit() }) {
                newPinLayout?.error = "New PIN must be 4 digits"
                hasError = true
            } else if (isWeakPin(newPin)) {
                newPinLayout?.error = "PIN is too weak. Avoid repeating digits or sequential patterns"
                hasError = true
            }

            // Validate confirm PIN
            if (confirmPin != newPin) {
                confirmPinLayout?.error = "PINs do not match"
                hasError = true
            }

            // Check PIN reuse for new PIN
            if (newPin.isNotEmpty() && runBlocking { isPinAlreadyUsed(newPin, folder.id) } && newPin != currentPin) {
                newPinLayout?.error = "This PIN is already used by another folder"
                hasError = true
            }

            if (!hasError) {
                changeFolderPin(folder, currentPin, newPin)
                dialog.dismiss()
            }
        }

        val cancelButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.cancel_button)
        cancelButton?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun changeFolderPin(folder: EncryptedFolderEntity, currentPin: String, newPin: String) {
        lifecycleScope.launch {
            try {
                // Verify current PIN
                val isCurrentPinValid = withContext(Dispatchers.IO) {
                    val storedHash = folder.passwordHash
                    val salt = folder.salt
                    val currentHash = EncryptionUtils.hashPassword(currentPin, salt)
                    storedHash == currentHash
                }

                if (!isCurrentPinValid) {
                    Toast.makeText(this@MediaGalleryActivity, "Current PIN is incorrect", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Generate new salt and hash for new PIN
                val newSalt = EncryptionUtils.generateSalt()
                val newHash = EncryptionUtils.hashPassword(newPin, newSalt)

                // Update folder with new PIN
                val updatedFolder = folder.copy(
                    passwordHash = newHash,
                    salt = newSalt
                )

                withContext(Dispatchers.IO) {
                    database.encryptedFolderDao().updateFolder(updatedFolder)
                }

                Toast.makeText(this@MediaGalleryActivity, "PIN changed successfully for ${folder.name}", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                android.util.Log.e("MediaGalleryActivity", "Error changing folder PIN", e)
                Toast.makeText(this@MediaGalleryActivity, "Error changing PIN: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun exportFolder(folder: EncryptedFolderEntity) {
        lifecycleScope.launch {
            try {
                // Get all files in the folder
                val files = withContext(Dispatchers.IO) {
                    database.encryptedFileDao().getFilesInFolderSync(folder.id)
                }

                if (files.isEmpty()) {
                    Toast.makeText(this@MediaGalleryActivity, "Folder is empty, nothing to export", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Create export directory
                val exportDir = File(getExternalFilesDir(null), "Exported_${folder.name}_${System.currentTimeMillis()}")
                if (!exportDir.exists()) {
                    exportDir.mkdirs()
                }

                var successCount = 0
                var errorCount = 0

                // Export each file
                for (file in files) {
                    try {
                        val fileEncryptionService = com.shaadow.onecalculator.services.FileEncryptionService(this@MediaGalleryActivity)

                        // Decrypt file to export directory
                        val exportedFile = withContext(Dispatchers.IO) {
                            fileEncryptionService.decryptFileForViewing(file, "0000", folder.salt)
                        }

                        if (exportedFile != null) {
                            // Copy to export directory with original name
                            val targetFile = File(exportDir, file.originalFileName)
                            exportedFile.copyTo(targetFile, overwrite = true)

                            // Clean up temp file
                            exportedFile.delete()
                            successCount++
                        } else {
                            errorCount++
                        }

                    } catch (e: Exception) {
                        android.util.Log.e("MediaGalleryActivity", "Error exporting file: ${file.originalFileName}", e)
                        errorCount++
                    }
                }

                // Show result
                val message = when {
                    successCount > 0 && errorCount == 0 -> "Successfully exported $successCount files to ${exportDir.absolutePath}"
                    successCount > 0 && errorCount > 0 -> "Exported $successCount files, $errorCount failed. Location: ${exportDir.absolutePath}"
                    else -> "Failed to export files"
                }

                Toast.makeText(this@MediaGalleryActivity, message, Toast.LENGTH_LONG).show()

                // Open export directory in file manager
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(android.net.Uri.parse(exportDir.parent), "resource/folder")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    android.util.Log.w("MediaGalleryActivity", "Could not open export directory in file manager", e)
                }

            } catch (e: Exception) {
                android.util.Log.e("MediaGalleryActivity", "Error exporting folder", e)
                Toast.makeText(this@MediaGalleryActivity, "Error exporting folder: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verifyFolderPin(folder: EncryptedFolderEntity, enteredPin: String, callback: (Boolean) -> Unit) {
        lifecycleScope.launch {
            try {
                android.util.Log.d("MediaGalleryActivity", "Verifying PIN '$enteredPin' for folder: ${folder.name}")

                // If folder is not locked, always return true
                if (!folder.isLocked) {
                    android.util.Log.d("MediaGalleryActivity", "Folder is not locked, allowing access")
                    callback(true)
                    return@launch
                }

                val isValid = withContext(Dispatchers.IO) {
                    val storedHash = folder.passwordHash
                    val salt = folder.salt
                    val enteredHash = EncryptionUtils.hashPassword(enteredPin, salt)
                    android.util.Log.d("MediaGalleryActivity", "Stored hash: $storedHash")
                    android.util.Log.d("MediaGalleryActivity", "Entered hash: $enteredHash")
                    android.util.Log.d("MediaGalleryActivity", "Salt: $salt")
                    val result = storedHash == enteredHash
                    android.util.Log.d("MediaGalleryActivity", "PIN verification result: $result")
                    result
                }
                callback(isValid)
            } catch (e: Exception) {
                android.util.Log.e("MediaGalleryActivity", "Error verifying folder PIN", e)
                callback(false)
            }
        }
    }

    private fun openFolderContents(folder: EncryptedFolderEntity) {
        try {
            android.util.Log.d("MediaGalleryActivity", "Opening folder contents for: ${folder.name}, ID: ${folder.id}")
            android.util.Log.d("MediaGalleryActivity", "Using master password: ${currentMasterPassword.isNotEmpty()}")

            // Ensure master password is available
            if (currentMasterPassword.isEmpty()) {
                android.util.Log.w("MediaGalleryActivity", "Master password is empty, setting default")
                currentMasterPassword = "0000"
            }

            val intent = Intent(this, NewFolderContentsActivity::class.java)
            intent.putExtra("folder_id", folder.id)
            intent.putExtra("master_password", currentMasterPassword)

            android.util.Log.d("MediaGalleryActivity", "Starting NewFolderContentsActivity with folder ID: ${folder.id}")
            startActivity(intent)
            android.util.Log.d("MediaGalleryActivity", "Intent started successfully")
        } catch (e: Exception) {
            android.util.Log.e("MediaGalleryActivity", "Error opening folder contents", e)
            Toast.makeText(this, "Error opening folder: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
private fun toggleSelectAllFolders(menuItem: android.view.MenuItem) {
    if (isAllSelected) {
        // Deselect all
        folderAdapter.deselectAll()
        updateMenuVisibility(false)
        menuItem.title = "Select All"
        Toast.makeText(this, "All folders deselected", Toast.LENGTH_SHORT).show()
    } else {
        // Select all
        folderAdapter.selectAll()
        updateMenuVisibility(true)
        menuItem.title = "Deselect All"
        Toast.makeText(this, "All folders selected", Toast.LENGTH_SHORT).show()
    }
    isAllSelected = !isAllSelected
}

    
    private fun deleteSelectedFolders() {
        val selectedFolders = folderAdapter.getSelectedFolders()
        if (selectedFolders.isEmpty()) {
            Toast.makeText(this, "No folders selected", Toast.LENGTH_SHORT).show()
            return
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Folders")
            .setMessage("Are you sure you want to delete ${selectedFolders.size} folder(s)? This will permanently delete all files in these folders.")
            .setPositiveButton("Delete") { _, _ ->
                performFolderDeletion(selectedFolders)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun performFolderDeletion(folders: List<EncryptedFolderEntity>) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    folders.forEach { folder ->
                        // Delete all files in the folder first
                        val files = database.encryptedFileDao().getFilesInFolderSync(folder.id)
                        files.forEach { file ->
                            // Delete encrypted file from storage
                            val hiddenDir = ExternalStorageManager.getHiddenCalculatorDir(this@MediaGalleryActivity)
                            if (hiddenDir != null) {
                                val encryptedFile = File(hiddenDir, file.encryptedFileName)
                                if (encryptedFile.exists()) {
                                    encryptedFile.delete()
                                }
                            }
                            // Delete from database
                            database.encryptedFileDao().deleteFile(file)
                        }
                        
                        // Delete the folder from database
                        database.encryptedFolderDao().deleteFolder(folder)
                    }
                }
                
                folderAdapter.clearSelection()
                updateMenuVisibility(false)

                // Notify about file changes for instant UI update
                notifyFilesChanged()

                Toast.makeText(this@MediaGalleryActivity, "${folders.size} folder(s) deleted", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.util.Log.e("MediaGalleryActivity", "Error deleting folders", e)
                Toast.makeText(this@MediaGalleryActivity, "Error deleting folders", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun moveSelectedFolders() {
        val selectedFolders = folderAdapter.getSelectedFolders()
        if (selectedFolders.isEmpty()) {
            Toast.makeText(this, "No folders selected", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Create a dialog to get the category name
        val dialogView = layoutInflater.inflate(R.layout.dialog_rename_file, null)
        val nameInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.file_name_input)
        val nameLayout = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.file_name_input_layout)
        
        nameLayout.hint = "Enter category name"
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Move to Category")
            .setMessage("Enter a category name to prefix selected folders (e.g., 'Work: Folder1')")
            .setView(dialogView)
            .setPositiveButton("Move") { _, _ ->
                val category = nameInput.text?.toString()?.trim() ?: ""
                if (category.isEmpty()) {
                    Toast.makeText(this, "Category name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                performFolderMove(selectedFolders, category)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun performFolderMove(folders: List<EncryptedFolderEntity>, category: String) {
        lifecycleScope.launch {
            try {
                var successCount = 0
                
                withContext(Dispatchers.IO) {
                    folders.forEach { folder ->
                        // Create new name with category prefix
                        val newName = "$category: ${folder.name}"
                        
                        // Check if the new name already exists
                        val existingFolder = database.encryptedFolderDao().getFolderByName(newName)
                        if (existingFolder != null) {
                            // Skip this folder if name already exists
                            return@forEach
                        }
                        
                        // Update folder name
                        val updatedFolder = folder.copy(name = newName)
                        database.encryptedFolderDao().updateFolder(updatedFolder)
                        successCount++
                    }
                }
                
                // Clear selection and refresh the list
                folderAdapter.clearSelection()
                updateMenuVisibility(false)
                isAllSelected = false
                
                // Refresh the folder list with a small delay to ensure database consistency
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    setupFolderList()
                }, 100)
                
                Toast.makeText(this@MediaGalleryActivity, "Moved $successCount folder(s) to '$category' category", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.util.Log.e("MediaGalleryActivity", "Error moving folders", e)
                Toast.makeText(this@MediaGalleryActivity, "Error moving folders: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun exportSelectedFolders() {
        val selectedFolders = folderAdapter.getSelectedFolders()
        if (selectedFolders.isEmpty()) {
            Toast.makeText(this, "No folders selected", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Create a confirmation dialog
        MaterialAlertDialogBuilder(this)
            .setTitle("Export Folders")
            .setMessage("Are you sure you want to export ${selectedFolders.size} folder(s)? This will decrypt and save all files to your device.")
            .setPositiveButton("Export") { _, _ ->
                performFolderExport(selectedFolders)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun performFolderExport(folders: List<EncryptedFolderEntity>) {
        lifecycleScope.launch {
            try {
                // Create a parent export directory with timestamp
                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                val exportParentDir = File(getExternalFilesDir(null), "Exported_Folders_$timestamp")
                if (!exportParentDir.exists()) {
                    exportParentDir.mkdirs()
                }
                
                var totalSuccessCount = 0
                var totalErrorCount = 0
                
                // Export each folder
                for (folder in folders) {
                    try {
                        // Create subfolder for this folder
                        val folderExportDir = File(exportParentDir, folder.name)
                        if (!folderExportDir.exists()) {
                            folderExportDir.mkdirs()
                        }
                        
                        // Get all files in the folder
                        val files = withContext(Dispatchers.IO) {
                            database.encryptedFileDao().getFilesInFolderSync(folder.id)
                        }
                        
                        if (files.isEmpty()) {
                            continue // Skip empty folders
                        }
                        
                        var folderSuccessCount = 0
                        var folderErrorCount = 0
                        
                        // Export each file
                        for (file in files) {
                            try {
                                val fileEncryptionService = com.shaadow.onecalculator.services.FileEncryptionService(this@MediaGalleryActivity)
                                
                                // Decrypt file
                                val exportedFile = withContext(Dispatchers.IO) {
                                    fileEncryptionService.decryptFileForViewing(file, currentMasterPassword, folder.salt)
                                }
                                
                                if (exportedFile != null) {
                                    // Copy to export directory with original name
                                    val targetFile = File(folderExportDir, file.originalFileName)
                                    exportedFile.copyTo(targetFile, overwrite = true)
                                    
                                    // Clean up temp file
                                    exportedFile.delete()
                                    folderSuccessCount++
                                    totalSuccessCount++
                                } else {
                                    folderErrorCount++
                                    totalErrorCount++
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("MediaGalleryActivity", "Error exporting file: ${file.originalFileName}", e)
                                folderErrorCount++
                                totalErrorCount++
                            }
                        }
                        
                        android.util.Log.d("MediaGalleryActivity", "Exported folder ${folder.name}: $folderSuccessCount success, $folderErrorCount errors")
                    } catch (e: Exception) {
                        android.util.Log.e("MediaGalleryActivity", "Error exporting folder: ${folder.name}", e)
                        totalErrorCount++
                    }
                }
                
                // Clear selection
                folderAdapter.clearSelection()
                updateMenuVisibility(false)
                isAllSelected = false
                
                // Show result
                val locationMessage = "📁 Files saved in app storage"

                val message = when {
                    totalSuccessCount > 0 && totalErrorCount == 0 ->
                        "✅ Successfully exported $totalSuccessCount files from ${folders.size} folder(s)!\n$locationMessage\n📂 Location: ${exportParentDir.absolutePath}"
                    totalSuccessCount > 0 && totalErrorCount > 0 ->
                        "⚠️ Exported $totalSuccessCount files, $totalErrorCount failed.\n$locationMessage\n📂 Location: ${exportParentDir.absolutePath}"
                    else ->
                        "❌ Failed to export files"
                }
                
                Toast.makeText(this@MediaGalleryActivity, message, Toast.LENGTH_LONG).show()
                
                // Open export directory in file manager
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(android.net.Uri.fromFile(exportParentDir), "resource/folder")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    android.util.Log.w("MediaGalleryActivity", "Could not open export directory in file manager", e)
                }
                
            } catch (e: Exception) {
                android.util.Log.e("MediaGalleryActivity", "Error exporting folders", e)
                Toast.makeText(this@MediaGalleryActivity, "Error exporting folders: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }
    
    private fun showHelp() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Hidden Gallery Help")
            .setMessage("• Create folders to organize your private media\n• Use fingerprint to unlock the gallery\n• Files are encrypted and stored securely\n• Long press folders for more options")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showStorageInfo() {
        val hiddenDir = ExternalStorageManager.getHiddenCalculatorDir(this)
        val encryptedDir = ExternalStorageManager.getEncryptedFilesDir(this)

        val message = buildString {
            append("📂 Storage Information\n\n")
            append("🔒 Hidden Directory:\n")
            append("${hiddenDir?.absolutePath ?: "Not available"}\n\n")
            append("🔐 Encrypted Files:\n")
            append("${encryptedDir?.absolutePath ?: "Not available"}\n\n")
            append("💡 This folder is visible in your file manager.\n")
            append("All files are encrypted and can only be accessed through this app.\n\n")
            append("⚠️ Do not modify or delete files directly from the file manager!")
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Storage Location")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNeutralButton("Open in File Manager") { _, _ ->
                openInFileManager()
            }
            .show()
    }

    private fun openInFileManager() {
        try {
            val hiddenDir = ExternalStorageManager.getHiddenCalculatorDir(this)
            if (hiddenDir != null && hiddenDir.exists()) {
                // Try to open the specific directory
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(android.net.Uri.fromFile(hiddenDir), "resource/folder")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                // If direct file URI doesn't work, try opening the parent directory
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    android.util.Log.w("MediaGalleryActivity", "Direct directory opening failed, trying parent", e)
                    val parentIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(android.net.Uri.fromFile(hiddenDir.parentFile), "resource/folder")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(parentIntent)
                }

                // File manager opened silently
            } else {
                // Storage directory not found - handled silently
            }
        } catch (e: Exception) {
            android.util.Log.e("MediaGalleryActivity", "Error opening file manager", e)
            Toast.makeText(this, "Cannot open file manager: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateMenuVisibility(hasSelection: Boolean) {
        val menu = binding.toolbar.menu
        menu.findItem(R.id.action_delete_selected)?.isVisible = hasSelection
        menu.findItem(R.id.action_move_selected)?.isVisible = hasSelection
        menu.findItem(R.id.action_export_selected)?.isVisible = hasSelection
    }

    private fun updateBulkActionVisibility(allSelected: Boolean) {
        val menu = binding.toolbar.menu
        // Show bulk actions when all folders are selected
        if (allSelected) {
            menu.findItem(R.id.action_delete_selected)?.isVisible = true
            menu.findItem(R.id.action_move_selected)?.isVisible = true
            menu.findItem(R.id.action_export_selected)?.isVisible = true
            // You could also add a special "Select All" action or modify existing ones
            Toast.makeText(this, "All folders selected! Bulk actions available.", Toast.LENGTH_SHORT).show()
        } else {
            // Hide bulk actions when not all folders are selected
            updateMenuVisibility(folderAdapter.getSelectedFolders().isNotEmpty())
        }
    }

    private fun validateAndCleanNonMediaFiles() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("MediaGalleryActivity", "Starting post-authentication file validation...")

                val folders = withContext(Dispatchers.IO) {
                    database.encryptedFolderDao().getAllFoldersSync()
                }

                var totalRemovedFiles = 0
                var totalScannedFiles = 0

                for (folder in folders) {
                    val files = withContext(Dispatchers.IO) {
                        database.encryptedFileDao().getFilesInFolderSync(folder.id)
                    }

                    totalScannedFiles += files.size

                    val filesToRemove = mutableListOf<EncryptedFileEntity>()

                    for (file in files) {
                        // Check if file is a supported media type
                        val isMediaFile = isSupportedMediaFile(file.originalFileName, file.mimeType)
                        if (!isMediaFile) {
                            android.util.Log.w("MediaGalleryActivity", "Found non-media file: ${file.originalFileName} (MIME: ${file.mimeType})")
                            filesToRemove.add(file)
                        }
                    }

                    // Remove non-media files
                    if (filesToRemove.isNotEmpty()) {
                        withContext(Dispatchers.IO) {
                            filesToRemove.forEach { file ->
                                // Delete encrypted file from storage
                                val hiddenDir = ExternalStorageManager.getHiddenCalculatorDir(this@MediaGalleryActivity)
                                if (hiddenDir != null) {
                                    val encryptedFile = File(hiddenDir, file.encryptedFileName)
                                    if (encryptedFile.exists()) {
                                        val deleted = encryptedFile.delete()
                                        android.util.Log.d("MediaGalleryActivity", "Deleted encrypted file: ${file.encryptedFileName}, success: $deleted")
                                    }
                                }

                                // Delete from database
                                database.encryptedFileDao().deleteFile(file)
                                android.util.Log.d("MediaGalleryActivity", "Removed non-media file from database: ${file.originalFileName}")
                            }
                        }
                        totalRemovedFiles += filesToRemove.size
                    }
                }

                if (totalRemovedFiles > 0) {
                    android.util.Log.i("MediaGalleryActivity", "File validation complete: $totalScannedFiles scanned, $totalRemovedFiles removed")
                    Toast.makeText(
                        this@MediaGalleryActivity,
                        "Cleaned up $totalRemovedFiles unsupported files from your gallery",
                        Toast.LENGTH_LONG
                    ).show()

                    // Notify about file changes for instant UI update
                    if (totalRemovedFiles > 0) {
                        notifyFilesChanged()
                    }

                    // Refresh the folder list to update counts with a small delay to ensure database consistency
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        setupFolderList()
                    }, 100)
                } else {
                    android.util.Log.d("MediaGalleryActivity", "File validation complete: $totalScannedFiles scanned, no cleanup needed")
                }

            } catch (e: Exception) {
                android.util.Log.e("MediaGalleryActivity", "Error during file validation", e)
                Toast.makeText(
                    this@MediaGalleryActivity,
                    "Error during file cleanup: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun isSupportedMediaFile(fileName: String, mimeType: String): Boolean {
        // Check MIME type first
        if (mimeType.startsWith("image/") ||
            mimeType.startsWith("video/") ||
            mimeType.startsWith("audio/")) {
            return true
        }

        // Check file extension for additional support
        val extension = fileName.substringAfterLast('.', "").lowercase()

        // Comprehensive list of supported file extensions
        val supportedExtensions = setOf(
            // Images
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff", "tif", "ico", "svg",
            "heic", "heif", "raw", "cr2", "nef", "arw", "dng", "orf", "rw2", "pef",

            // Videos
            "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "3gp", "mpg",
            "mpeg", "m2ts", "mts", "vob", "asf", "rm", "rmvb", "divx", "xvid",

            // Audio
            "mp3", "wav", "aac", "ogg", "wma", "flac", "m4a", "opus", "aiff",
            "au", "ra", "ape", "ac3", "dts", "pcm", "amr", "mid", "midi"
        )

        return extension in supportedExtensions
    }

    private fun importFilesToFolder(folder: EncryptedFolderEntity) {
        // Verify PIN before importing
        showFolderPinDialog(folder, onSuccess = {
            // Launch file picker for importing files
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                addCategory(Intent.CATEGORY_OPENABLE)
            }

            try {
                importFilesLauncher.launch(Intent.createChooser(intent, "Select Files to Import"))
            } catch (e: Exception) {
                Toast.makeText(this, "No file manager found", Toast.LENGTH_SHORT).show()
                android.util.Log.e("MediaGalleryActivity", "Error launching file picker", e)
            }
        })
    }

    private fun processImportedFiles(uris: List<Uri>) {
        // Show folder selection dialog
        lifecycleScope.launch {
            try {
                val folders: List<EncryptedFolderEntity> = withContext(Dispatchers.IO) {
                    database.encryptedFolderDao().getAllFoldersSync()
                }

                if (folders.isEmpty()) {
                    Toast.makeText(this@MediaGalleryActivity, "No folders available", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val folderNames = folders.map { folder: EncryptedFolderEntity -> folder.name }.toTypedArray()

                MaterialAlertDialogBuilder(this@MediaGalleryActivity)
                    .setTitle("Select Folder")
                    .setItems(folderNames) { dialog: android.content.DialogInterface, which: Int ->
                        val selectedFolder = folders[which]
                        importFilesToExistingFolder(selectedFolder, uris)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } catch (e: Exception) {
                android.util.Log.e("MediaGalleryActivity", "Error loading folders", e)
                Toast.makeText(this@MediaGalleryActivity, "Error loading folders", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun importFilesToExistingFolder(folder: EncryptedFolderEntity, uris: List<Uri>) {
        lifecycleScope.launch {
            try {
                android.util.Log.d("MediaGalleryActivity", "Importing ${uris.size} files to folder: ${folder.name}")

                // Ensure storage is ready
                if (!ExternalStorageManager.isHiddenDirectoryReady(this@MediaGalleryActivity)) {
                    val initialized = ExternalStorageManager.initializeHiddenDirectory(this@MediaGalleryActivity)
                    if (!initialized) {
                        Toast.makeText(this@MediaGalleryActivity, "Error: Cannot access storage", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                }

                val fileEncryptionService = com.shaadow.onecalculator.services.FileEncryptionService(this@MediaGalleryActivity)
                var successCount = 0
                var errorCount = 0

                for (uri in uris) {
                    try {
                        // Get file name for better logging
                        val fileName = getFileName(uri)
                        android.util.Log.d("MediaGalleryActivity", "Importing file: $fileName")

                        val fileEntity = withContext(Dispatchers.IO) {
                            fileEncryptionService.encryptAndStoreFile(uri, folder.id, currentMasterPassword, folder.salt)
                        }

                        if (fileEntity != null) {
                            val insertedId = withContext(Dispatchers.IO) {
                                database.encryptedFileDao().insertFile(fileEntity)
                            }

                            android.util.Log.d("MediaGalleryActivity", "File imported with ID: $insertedId")
                            successCount++
                        } else {
                            errorCount++
                            android.util.Log.e("MediaGalleryActivity", "Failed to import file: $uri")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MediaGalleryActivity", "Error importing file", e)
                        errorCount++
                    }
                }

                // Show result
                val message = when {
                    successCount > 0 && errorCount == 0 -> "$successCount file(s) imported successfully"
                    successCount > 0 && errorCount > 0 -> "$successCount file(s) imported, $errorCount failed"
                    else -> "Failed to import files"
                }

                Toast.makeText(this@MediaGalleryActivity, message, Toast.LENGTH_SHORT).show()

                // Notify about file changes for instant UI update
                if (successCount > 0) {
                    notifyFolderUpdated(folder.id)
                }

                // Refresh folder list to update counts with a small delay to ensure database consistency
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    setupFolderList()
                }, 100)

            } catch (e: Exception) {
                android.util.Log.e("MediaGalleryActivity", "Error importing files", e)
                Toast.makeText(this@MediaGalleryActivity, "Error importing files", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var fileName = "unknown_file"

        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex) ?: fileName
                }
            }
        }

        return fileName
    }

    // Modified version of showFolderPinDialog that takes a success callback
    private fun showFolderPinDialog(folder: EncryptedFolderEntity, onSuccess: () -> Unit = {
        openFolderContents(folder)
    }) {
        // If folder is not locked, skip PIN dialog and open directly
        if (!folder.isLocked) {
            onSuccess()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_folder_pin, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this, R.style.DialogStyle_Todo)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Set folder name
        dialogView.findViewById<TextView>(R.id.folder_name_text).text = folder.name

        val pinDigits = arrayOf(
            dialogView.findViewById<TextView>(R.id.pin_digit_1),
            dialogView.findViewById<TextView>(R.id.pin_digit_2),
            dialogView.findViewById<TextView>(R.id.pin_digit_3),
            dialogView.findViewById<TextView>(R.id.pin_digit_4)
        )

        var currentPin = ""
        val errorMessage = dialogView.findViewById<TextView>(R.id.error_message)

        // Number buttons with null checks
        val numberButtons = arrayOf(
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_1),
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_2),
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_3),
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_4),
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_5),
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_6),
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_7),
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_8),
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_9),
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_0)
        )

        numberButtons.forEachIndexed { index, button ->
            button?.setOnClickListener {
                if (currentPin.length < 3) {
                    currentPin += if (index == 9) "0" else (index + 1).toString()
                    updatePinDisplay(pinDigits, currentPin)
                    errorMessage?.visibility = View.GONE

                    if (currentPin.length == 4) {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            verifyFolderPin(folder, currentPin) { success ->
                                if (success) {
                                    dialog.dismiss()
                                    onSuccess()
                                } else {
                                    showPinError(pinDigits)
                                    errorMessage?.visibility = View.VISIBLE
                                    currentPin = ""
                                    updatePinDisplay(pinDigits, currentPin)
                                }
                            }
                        }, 300)
                    }
                }
            }
        }

        // Backspace button with null check
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_backspace)?.setOnClickListener {
            if (currentPin.isNotEmpty()) {
                currentPin = currentPin.dropLast(1)
                updatePinDisplay(pinDigits, currentPin)
                errorMessage?.visibility = View.GONE
            }
        }

        // Cancel button with null check
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.cancel_button)?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun exportEntireGallery() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("MediaGalleryActivity", "Starting complete gallery export...")

                // Check if there are any folders/files to export
                val folders = withContext(Dispatchers.IO) {
                    database.encryptedFolderDao().getAllFoldersSync()
                }

                if (folders.isEmpty()) {
                    Toast.makeText(this@MediaGalleryActivity, "No folders to export", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Show ZIP password dialog first
                showZipPasswordDialog { zipPassword ->
                    // Start the actual export process
                    performZipExport(folders, zipPassword)
                }

            } catch (e: Exception) {
                android.util.Log.e("MediaGalleryActivity", "Error starting gallery export", e)
                Toast.makeText(this@MediaGalleryActivity, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showZipPasswordDialog(onPasswordSet: (String) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_zip_password, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val passwordInput = dialogView.findViewById<TextInputEditText>(R.id.zip_password_input)
        val confirmPasswordInput = dialogView.findViewById<TextInputEditText>(R.id.confirm_zip_password_input)
        val passwordLayout = dialogView.findViewById<TextInputLayout>(R.id.zip_password_layout)
        val confirmPasswordLayout = dialogView.findViewById<TextInputLayout>(R.id.confirm_zip_password_layout)

        dialogView.findViewById<View>(R.id.cancel_button).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.create_zip_button).setOnClickListener {
            val password = passwordInput.text?.toString()?.trim() ?: ""
            val confirmPassword = confirmPasswordInput.text?.toString()?.trim() ?: ""

            // Clear previous errors
            passwordLayout.error = null
            confirmPasswordLayout.error = null

            var hasError = false

            // Validate password
            if (password.isEmpty()) {
                passwordLayout.error = "Password is required"
                hasError = true
            } else if (password.length < 4) {
                passwordLayout.error = "Password must be at least 4 characters"
                hasError = true
            }

            // Validate confirm password
            if (confirmPassword != password) {
                confirmPasswordLayout.error = "Passwords do not match"
                hasError = true
            }

            if (!hasError) {
                dialog.dismiss()
                onPasswordSet(password)
            }
        }

        dialog.show()
    }

    private fun performZipExport(folders: List<EncryptedFolderEntity>, zipPassword: String) {
        val loadingDialog = showLoadingDialog()
        var totalFiles = 0
        var successCount = 0

        lifecycleScope.launch {
            try {
                android.util.Log.d("MediaGalleryActivity", "Starting ZIP export process...")

                // Create ZIP file in Downloads folder
                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                val downloadsDir = com.shaadow.onecalculator.utils.ExternalStorageManager.getDownloadsDirectory(this@MediaGalleryActivity)
                val zipFileName = "export_hg_1calc_$timestamp.zip"
                val zipFile = if (downloadsDir != null) {
                    File(downloadsDir, zipFileName)
                } else {
                    // Fallback to app's external files directory
                    File(getExternalFilesDir(null), zipFileName)
                }

                android.util.Log.d("MediaGalleryActivity", "ZIP file: ${zipFile.absolutePath}")

                // Create temporary directory for decrypted files
                val tempDir = File(cacheDir, "export_temp_$timestamp")
                if (!tempDir.exists()) {
                    tempDir.mkdirs()
                }

                // Update loading message
                runOnUiThread {
                    loadingDialog.findViewById<TextView>(R.id.loading_message)?.text = "Decrypting files..."
                    loadingDialog.findViewById<TextView>(R.id.progress_text)?.text = "Preparing files..."
                }

                // Process each folder
                for ((folderIndex, folder) in folders.withIndex()) {
                    val files = withContext(Dispatchers.IO) {
                        database.encryptedFileDao().getFilesInFolderSync(folder.id)
                    }

                    totalFiles += files.size

                    // Update progress
                    runOnUiThread {
                        loadingDialog.findViewById<TextView>(R.id.progress_text)?.text =
                            "Processing folder ${folderIndex + 1}/${folders.size}: ${folder.name}"
                    }

                    for (file in files) {
                        try {
                            val fileEncryptionService = com.shaadow.onecalculator.services.FileEncryptionService(this@MediaGalleryActivity)

                            // Decrypt file
                            val decryptedFile = withContext(Dispatchers.IO) {
                                fileEncryptionService.decryptFileForViewing(file, currentMasterPassword, folder.salt)
                            }

                            if (decryptedFile != null) {
                                // Copy to temp directory with original name (flat structure)
                                val targetFile = File(tempDir, file.originalFileName)
                                decryptedFile.copyTo(targetFile, overwrite = true)

                                // Clean up temp decrypted file
                                decryptedFile.delete()
                                successCount++
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MediaGalleryActivity", "Error processing file: ${file.originalFileName}", e)
                        }
                    }
                }

                if (successCount == 0) {
                    runOnUiThread {
                        Toast.makeText(this@MediaGalleryActivity, "No files were processed for export", Toast.LENGTH_SHORT).show()
                    }
                    loadingDialog.dismiss()
                    return@launch
                }

                // Update loading message
                runOnUiThread {
                    loadingDialog.findViewById<TextView>(R.id.loading_message)?.text = "Creating ZIP file..."
                    loadingDialog.findViewById<TextView>(R.id.progress_text)?.text = "Compressing files..."
                }

                // Create metadata.json
                val metadata = withContext(Dispatchers.IO) {
                    createGalleryMetadata(folders)
                }
                val metadataFile = File(tempDir, "metadata.json")
                metadataFile.writeText(metadata)

                // Verify metadata file was created
                if (!metadataFile.exists()) {
                    throw Exception("Failed to create metadata.json file")
                }

                android.util.Log.d("MediaGalleryActivity", "Metadata file created at: ${metadataFile.absolutePath}")
                android.util.Log.d("MediaGalleryActivity", "Metadata file size: ${metadataFile.length()} bytes")

                // Create password-protected ZIP file
                val zipFileObj = ZipFile(zipFile, zipPassword.toCharArray())
                val zipParameters = ZipParameters().apply {
                    isEncryptFiles = true
                    encryptionMethod = EncryptionMethod.AES
                    aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                }

                // Add metadata.json first
                val metadataParams = ZipParameters(zipParameters).apply {
                    fileNameInZip = "metadata.json"
                }
                zipFileObj.addFile(metadataFile, metadataParams)

                // Add all files from temp directory (flat structure)
                val files = tempDir.listFiles()?.filter { it.isFile && it.name != "metadata.json" } ?: emptyList()
                for (file in files) {
                    val fileParams = ZipParameters(zipParameters).apply {
                        fileNameInZip = file.name
                    }
                    zipFileObj.addFile(file, fileParams)
                }

                android.util.Log.d("MediaGalleryActivity", "ZIP file created successfully at: ${zipFile.absolutePath}")
                android.util.Log.d("MediaGalleryActivity", "ZIP file size: ${zipFile.length()} bytes")

                // Clean up temp directory
                tempDir.deleteRecursively()

                // Dismiss loading dialog
                loadingDialog.dismiss()

                // Show success dialog with PIN info
                showZipExportSuccessDialog(zipFile, successCount, zipPassword)

            } catch (e: Exception) {
                android.util.Log.e("MediaGalleryActivity", "Error exporting gallery to ZIP", e)
                loadingDialog.dismiss()
                runOnUiThread {
                    Toast.makeText(this@MediaGalleryActivity, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showLoadingDialog(): androidx.appcompat.app.AlertDialog {
        val dialogView = layoutInflater.inflate(R.layout.dialog_export_loading, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.show()
        return dialog
    }

    private fun showZipExportSuccessDialog(zipFile: File, fileCount: Int, zipPassword: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_export_success, null)

        // Set up the dialog content
        val titleText = dialogView.findViewById<TextView>(R.id.export_success_title)
        val messageText = dialogView.findViewById<TextView>(R.id.export_success_message)
        val fileNameText = dialogView.findViewById<TextView>(R.id.export_file_name)
        val fileSizeText = dialogView.findViewById<TextView>(R.id.export_file_size)
        val zipPasswordText = dialogView.findViewById<TextView>(R.id.zip_password_display)
        val viewButton = dialogView.findViewById<MaterialButton>(R.id.view_file_button)
        val closeButton = dialogView.findViewById<MaterialButton>(R.id.close_button)

        // Set dialog content
        titleText?.text = "✅ ZIP Export Complete"
        messageText?.text = "Your hidden gallery has been successfully exported to a password-protected ZIP file."
        fileNameText?.text = "📄 ${zipFile.name}"
        fileSizeText?.text = "📊 $fileCount files • ${(zipFile.length() / 1024)} KB"
        zipPasswordText?.text = "Password: $zipPassword"

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // View file button action
        viewButton?.setOnClickListener {
            openExportedFileLocation(zipFile)
            dialog.dismiss()
        }

        // Close button action
        closeButton?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun createEncryptedArchive(dataParts: List<ByteArray>, masterPassword: String): Pair<ByteArray, ByteArray> {
        try {
            // Create a simple archive format
            val archiveData = mutableListOf<Byte>()

            // Archive header (will be stored unencrypted for validation)
            val header = "HGB_ARCHIVE_V1".toByteArray(Charsets.UTF_8)

            // Number of parts
            val partCount = dataParts.size
            val partCountBytes = ByteArray(4)
            for (i in 0..3) {
                partCountBytes[i] = (partCount shr (i * 8)).toByte()
            }
            archiveData.addAll(partCountBytes.toList())

            // Add each data part with its size
            for (data in dataParts) {
                // Part size
                val sizeBytes = ByteArray(4)
                val size = data.size
                for (i in 0..3) {
                    sizeBytes[i] = (size shr (i * 8)).toByte()
                }
                archiveData.addAll(sizeBytes.toList())

                // Part data
                archiveData.addAll(data.toList())
            }

            // Encrypt the archive data (without header) using deterministic key from master password
            val archiveArray = archiveData.toByteArray()
            val archiveSalt = "HGB_ARCHIVE_SALT_2024" // Fixed salt for archive encryption
            val encryptedArchive = EncryptionUtils.encrypt(archiveArray, masterPassword, archiveSalt)

            return Pair(header, encryptedArchive)

        } catch (e: Exception) {
            android.util.Log.e("MediaGalleryActivity", "Error creating encrypted archive", e)
            throw e
        }
    }

    private suspend fun createGalleryMetadata(folders: List<EncryptedFolderEntity>): String {
        val metadata = mutableMapOf<String, Any>()
        metadata["version"] = "1.0"
        metadata["exportTimestamp"] = java.time.Instant.now().toString()

        val galleryData = mutableListOf<Map<String, Any>>()

        for (folder in folders) {
            // Get file information
            val files = withContext(Dispatchers.IO) {
                database.encryptedFileDao().getFilesInFolderSync(folder.id)
            }

            for (file in files) {
                val fileInfo = mutableMapOf<String, Any>()
                fileInfo["originalName"] = file.originalFileName
                fileInfo["encryptedName"] = file.encryptedFileName
                fileInfo["sizeBytes"] = file.fileSize
                fileInfo["contentType"] = file.mimeType
                fileInfo["location"] = folder.name // Use folder name as location
                galleryData.add(fileInfo)
            }
        }

        metadata["galleryData"] = galleryData

        return com.google.gson.Gson().toJson(metadata)
    }


    private fun importGalleryBackup() {
        // Launch file picker for backup file with ZIP filter
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/zip", "application/octet-stream"))
            addCategory(Intent.CATEGORY_OPENABLE)
        }

        try {
            importBackupLauncher.launch(Intent.createChooser(intent, "Select Hidden Gallery Backup (.zip) File"))
        } catch (e: Exception) {
            Toast.makeText(this, "No file manager found", Toast.LENGTH_SHORT).show()
            android.util.Log.e("MediaGalleryActivity", "Error launching backup file picker", e)
        }
    }

    // Activity result launcher for importing backup files
    private val importBackupLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                restoreGalleryFromBackup(uri)
            }
        }
    }

    private fun restoreGalleryFromBackup(backupUri: Uri) {
        // Show password input dialog first
        showZipImportPasswordDialog(backupUri)
    }

    private fun showZipImportPasswordDialog(zipUri: Uri) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_zip_import_password, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val passwordInput = dialogView.findViewById<TextInputEditText>(R.id.zip_import_password_input)
        val passwordLayout = dialogView.findViewById<TextInputLayout>(R.id.zip_import_password_layout)

        dialogView.findViewById<View>(R.id.cancel_import_button).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.unlock_zip_button).setOnClickListener {
            val password = passwordInput.text?.toString()?.trim() ?: ""

            // Clear previous errors
            passwordLayout.error = null

            // Validate password
            if (password.isEmpty()) {
                passwordLayout.error = "Password is required"
                return@setOnClickListener
            }

            dialog.dismiss()
            performZipImport(zipUri, password)
        }

        dialog.show()
    }

    private fun performZipImport(zipUri: Uri, password: String) {
        val loadingDialog = showImportLoadingDialog()
        var importedFolders = 0
        var importedFiles = 0

        lifecycleScope.launch {
            try {
                android.util.Log.d("MediaGalleryActivity", "Starting ZIP import process...")

                // Update loading message
                runOnUiThread {
                    loadingDialog.findViewById<TextView>(R.id.import_loading_message)?.text = "Extracting ZIP file..."
                    loadingDialog.findViewById<TextView>(R.id.import_progress_text)?.text = "Reading archive..."
                }

                // Create temporary directory for extraction
                val tempDir = File(cacheDir, "zip_import_temp_${System.currentTimeMillis()}")
                if (!tempDir.exists()) {
                    tempDir.mkdirs()
                }

                // Create temporary ZIP file from URI
                val tempZipFile = File(cacheDir, "temp_import_${System.currentTimeMillis()}.zip")
                contentResolver.openInputStream(zipUri)?.use { inputStream ->
                    tempZipFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: throw Exception("Cannot read ZIP file")

                // Extract ZIP file to temp directory
                val zipFile = ZipFile(tempZipFile, password.toCharArray())

                try {
                    zipFile.extractAll(tempDir.absolutePath)
                    android.util.Log.d("MediaGalleryActivity", "ZIP extraction successful")

                    // Debug: List all files in temp directory
                    val extractedFiles = tempDir.listFiles()
                    if (extractedFiles != null) {
                        android.util.Log.d("MediaGalleryActivity", "Extracted files count: ${extractedFiles.size}")
                        for (file in extractedFiles) {
                            android.util.Log.d("MediaGalleryActivity", "Extracted file: ${file.name} (${file.length()} bytes)")
                            if (file.isDirectory) {
                                val subFiles = file.listFiles()
                                if (subFiles != null) {
                                    android.util.Log.d("MediaGalleryActivity", "  Directory ${file.name} contains ${subFiles.size} files")
                                    for (subFile in subFiles) {
                                        android.util.Log.d("MediaGalleryActivity", "    ${subFile.name} (${subFile.length()} bytes)")
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MediaGalleryActivity", "ZIP extraction failed", e)
                    throw Exception("Failed to extract ZIP file. Please check the password and ensure the file is not corrupted.")
                } finally {
                    // Clean up temp ZIP file
                    tempZipFile.delete()
                }

                // Update loading message
                runOnUiThread {
                    loadingDialog.findViewById<TextView>(R.id.import_progress_text)?.text = "Reading metadata..."
                }

                // Read and parse metadata.json
                val metadataFile = File(tempDir, "metadata.json")
                if (!metadataFile.exists()) {
                    android.util.Log.e("MediaGalleryActivity", "metadata.json not found in: ${tempDir.absolutePath}")
                    // List all files in temp directory to debug
                    val allFiles = tempDir.listFiles()
                    if (allFiles != null) {
                        android.util.Log.e("MediaGalleryActivity", "Files in temp directory:")
                        for (file in allFiles) {
                            android.util.Log.e("MediaGalleryActivity", "  ${file.name}")
                        }
                    }
                    throw Exception("Invalid ZIP file: metadata.json not found")
                }

                val metadataJson = metadataFile.readText()
                android.util.Log.d("MediaGalleryActivity", "Metadata JSON content: $metadataJson")

                val metadata = com.google.gson.Gson().fromJson(metadataJson, Map::class.java)
                android.util.Log.d("MediaGalleryActivity", "Parsed metadata keys: ${metadata.keys}")

                val galleryData = metadata["galleryData"] as? List<Map<String, Any>>
                    ?: throw Exception("Invalid metadata format: galleryData not found")

                android.util.Log.d("MediaGalleryActivity", "Found ${galleryData.size} files in metadata")

                // Group files by location (folder name)
                val filesByLocation = galleryData.groupBy { it["location"] as String }

                // Process each location/folder
                for ((locationIndex, locationEntry) in filesByLocation.entries.withIndex()) {
                    val locationName = locationEntry.key
                    val filesInLocation = locationEntry.value

                    // Update progress
                    runOnUiThread {
                        loadingDialog.findViewById<TextView>(R.id.import_progress_text)?.text =
                            "Processing location ${locationIndex + 1}/${filesByLocation.size}: $locationName"
                    }

                    // Check if folder already exists, create if not
                    var targetFolder = withContext(Dispatchers.IO) {
                        database.encryptedFolderDao().getFolderByName(locationName)
                    }

                    if (targetFolder == null) {
                        // Create new folder with default salt
                        val defaultSalt = EncryptionUtils.generateSalt()
                        val newFolder = EncryptedFolderEntity(
                            name = locationName,
                            passwordHash = EncryptionUtils.hashPassword(currentMasterPassword, defaultSalt),
                            salt = defaultSalt
                        )

                        val folderId = withContext(Dispatchers.IO) {
                            database.encryptedFolderDao().insertFolder(newFolder)
                        }

                        targetFolder = withContext(Dispatchers.IO) {
                            database.encryptedFolderDao().getFolderById(folderId)
                        }

                        android.util.Log.d("MediaGalleryActivity", "Created new folder: $locationName with ID: ${targetFolder?.id}")
                        importedFolders++
                    }

                    if (targetFolder != null) {
                        val fileEncryptionService = com.shaadow.onecalculator.services.FileEncryptionService(this@MediaGalleryActivity)

                        for (fileData in filesInLocation) {
                            try {
                                val originalName = fileData["originalName"] as String
                                val tempFile = File(tempDir, originalName)

                                if (tempFile.exists()) {
                                    // Create URI for the temp file
                                    val tempUri = androidx.core.content.FileProvider.getUriForFile(
                                        this@MediaGalleryActivity,
                                        "${packageName}.fileprovider",
                                        tempFile
                                    )

                                    // Encrypt and store the file
                                    val fileEntity = withContext(Dispatchers.IO) {
                                        fileEncryptionService.encryptAndStoreFile(tempUri, targetFolder.id, currentMasterPassword, targetFolder.salt)
                                    }

                                    if (fileEntity != null) {
                                        val insertedId = withContext(Dispatchers.IO) {
                                            database.encryptedFileDao().insertFile(fileEntity)
                                        }
                                        android.util.Log.d("MediaGalleryActivity", "Imported file: $originalName to $locationName with ID: $insertedId")
                                        importedFiles++
                                    } else {
                                        android.util.Log.e("MediaGalleryActivity", "Failed to create file entity for: $originalName")
                                    }
                                } else {
                                    android.util.Log.w("MediaGalleryActivity", "File not found in ZIP: $originalName")
                                }

                            } catch (e: Exception) {
                                android.util.Log.e("MediaGalleryActivity", "Error importing file: ${fileData["originalName"]}", e)
                            }
                        }
                    }
                }

                // Clean up temp directory
                tempDir.deleteRecursively()

                // Dismiss loading dialog
                loadingDialog.dismiss()

                // Debug: Check database state before refresh
                android.util.Log.d("MediaGalleryActivity", "Checking database state after import...")
                val allFoldersAfterImport = withContext(Dispatchers.IO) {
                    database.encryptedFolderDao().getAllFoldersSync()
                }
                android.util.Log.d("MediaGalleryActivity", "Total folders in database: ${allFoldersAfterImport.size}")
                for (folder in allFoldersAfterImport) {
                    val fileCount = withContext(Dispatchers.IO) {
                        database.encryptedFileDao().getFileCountInFolderSync(folder.id)
                    }
                    android.util.Log.d("MediaGalleryActivity", "Folder '${folder.name}' has $fileCount files")
                }

                // Notify about file changes for instant UI update
                if (importedFiles > 0) {
                    notifyFilesChanged()
                }

                // Refresh folder list with a small delay to ensure database consistency
                android.util.Log.d("MediaGalleryActivity", "Refreshing folder list...")
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    setupFolderList()
                }, 100)

                // Show success message
                val message = "Gallery import completed!\n" +
                              "📁 Folders imported: $importedFolders\n" +
                              "📄 Files imported: $importedFiles"
                android.util.Log.d("MediaGalleryActivity", "Import completed with message: $message")
                Toast.makeText(this@MediaGalleryActivity, message, Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                android.util.Log.e("MediaGalleryActivity", "Error importing gallery from ZIP", e)
                loadingDialog.dismiss()
                runOnUiThread {
                    Toast.makeText(this@MediaGalleryActivity, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showImportLoadingDialog(): androidx.appcompat.app.AlertDialog {
        val dialogView = layoutInflater.inflate(R.layout.dialog_import_loading, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.show()
        return dialog
    }

    private fun parseEncryptedArchive(data: ByteArray): List<ByteArray> {
        try {
            val parts = mutableListOf<ByteArray>()
            var offset = 0

            // Read number of parts
            if (offset + 4 > data.size) throw Exception("Invalid archive format")
            var partCount = 0
            for (i in 0..3) {
                partCount = partCount or ((data[offset + i].toInt() and 0xFF) shl (i * 8))
            }
            offset += 4

            // Read each part
            for (i in 0 until partCount) {
                if (offset + 4 > data.size) throw Exception("Invalid part size")
                var partSize = 0
                for (j in 0..3) {
                    partSize = partSize or ((data[offset + j].toInt() and 0xFF) shl (j * 8))
                }
                offset += 4

                if (offset + partSize > data.size) throw Exception("Invalid part data")
                val partData = data.copyOfRange(offset, offset + partSize)
                parts.add(partData)
                offset += partSize
            }

            return parts

        } catch (e: Exception) {
            android.util.Log.e("MediaGalleryActivity", "Error parsing encrypted archive", e)
            throw e
        }
    }

    private fun showExportSuccessDialog(backupFile: File, fileCount: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_export_success, null)

        // Set up the dialog content
        val titleText = dialogView.findViewById<android.widget.TextView>(R.id.export_success_title)
        val messageText = dialogView.findViewById<android.widget.TextView>(R.id.export_success_message)
        val fileNameText = dialogView.findViewById<android.widget.TextView>(R.id.export_file_name)
        val fileSizeText = dialogView.findViewById<android.widget.TextView>(R.id.export_file_size)
        val viewButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.view_file_button)
        val closeButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.close_button)

        // Set dialog content
        titleText?.text = "✅ Export Complete"
        messageText?.text = "Your hidden gallery has been successfully exported to a secure backup file."
        fileNameText?.text = "📄 ${backupFile.name}"
        fileSizeText?.text = "📊 ${fileCount} files • ${(backupFile.length() / 1024)} KB"

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this, R.style.DialogStyle_Todo)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // View file button action
        viewButton?.setOnClickListener {
            openExportedFileLocation(backupFile)
            dialog.dismiss()
        }

        // Close button action
        closeButton?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun openExportedFileLocation(backupFile: File) {
        try {
            // Try to open the folder containing the exported file
            val folderUri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                backupFile.parentFile!!
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(folderUri, "resource/folder")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Try to start the activity
            startActivity(intent)

        } catch (e: Exception) {
            android.util.Log.e("MediaGalleryActivity", "Error opening exported file location", e)

            // Fallback: Show a toast with the file path
            Toast.makeText(
                this,
                "File saved to: ${backupFile.parentFile?.absolutePath}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showBugReportDialog() {
        val bugReportSheet = BugReportBottomSheet.newInstance()
        bugReportSheet.show(supportFragmentManager, BugReportBottomSheet.TAG)
    }

    // Helper methods to send broadcasts for file changes
    @Suppress("UnsafeIntentLaunch")
    private fun notifyFilesChanged() {
        val intent = Intent(ACTION_FILES_CHANGED).apply {
            setPackage(packageName)
        }
        sendBroadcast(intent)
        android.util.Log.d("MediaGalleryActivity", "Sent FILES_CHANGED broadcast")
    }

    @Suppress("UnsafeIntentLaunch")
    private fun notifyFolderUpdated(folderId: Long) {
        val intent = Intent(ACTION_FOLDER_UPDATED).apply {
            putExtra(EXTRA_FOLDER_ID, folderId)
            setPackage(packageName)
        }
        sendBroadcast(intent)
        android.util.Log.d("MediaGalleryActivity", "Sent FOLDER_UPDATED broadcast for folder $folderId")
    }

}