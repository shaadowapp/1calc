package com.shaadow.onecalculator

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.shaadow.onecalculator.services.FileEncryptionService
import com.shaadow.onecalculator.utils.EncryptionUtils
import com.shaadow.onecalculator.utils.ExternalStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NewFolderContentsActivity : AppCompatActivity() {

    private lateinit var database: HistoryDatabase
    private lateinit var fileEncryptionService: FileEncryptionService
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateView: View
    private lateinit var btnAddFiles: MaterialButton
    private lateinit var fileAdapter: EncryptedFileAdapter
    
    private var folderId: Long = -1
    private var folderEntity: EncryptedFolderEntity? = null
    private var masterPassword: String = ""
    private var salt: String = ""

    companion object {
        const val EXTRA_FOLDER_ID = "folder_id"
        const val EXTRA_MASTER_PASSWORD = "master_password"
        private const val STORAGE_PERMISSION_REQUEST_CODE = 1001
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                handleSelectedFiles(data)
            }
        }
    }

    private val manageStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                android.util.Log.d("NewFolderContentsActivity", "MANAGE_EXTERNAL_STORAGE permission granted")
                initializeStorageAfterPermission()
            } else {
                android.util.Log.w("NewFolderContentsActivity", "MANAGE_EXTERNAL_STORAGE permission denied")
                // Permission denied handled silently (removed toast message)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_folder_contents)

        // Get folder info from intent
        folderId = intent.getLongExtra(EXTRA_FOLDER_ID, -1)
        masterPassword = intent.getStringExtra(EXTRA_MASTER_PASSWORD) ?: ""

        if (folderId == -1L || masterPassword.isEmpty()) {
            Toast.makeText(this, "Invalid folder access", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeComponents()
        setupUI()
        loadFolderData()
    }

    private fun initializeComponents() {
        database = HistoryDatabase.getInstance(this)
        fileEncryptionService = FileEncryptionService(this)

        recyclerView = findViewById(R.id.contents_recycler_view)
        emptyStateView = findViewById(R.id.empty_state_layout)
        btnAddFiles = findViewById(R.id.btn_add_files)

        fileAdapter = EncryptedFileAdapter { fileEntity ->
            openFile(fileEntity)
        }

        recyclerView.apply {
            layoutManager = GridLayoutManager(this@NewFolderContentsActivity, 2)
            adapter = fileAdapter
        }
    }

    private fun setupUI() {
        btnAddFiles.setOnClickListener {
            openFilePicker()
        }

        // Setup toolbar back button
        findViewById<android.widget.ImageButton>(R.id.btn_back)?.setOnClickListener {
            finish()
        }

        // Setup FAB as alternative add button
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_add_content)?.setOnClickListener {
            openFilePicker()
        }
    }

    private fun loadFolderData() {
        lifecycleScope.launch {
            try {
                // Load folder entity
                folderEntity = withContext(Dispatchers.IO) {
                    database.encryptedFolderDao().getFolderById(folderId)
                }

                folderEntity?.let { folder ->
                    salt = folder.salt

                    android.util.Log.d("NewFolderContentsActivity", "Folder loaded: ${folder.name}")
                    android.util.Log.d("NewFolderContentsActivity", "Folder salt: ${folder.salt}")
                    android.util.Log.d("NewFolderContentsActivity", "Master password available: ${masterPassword.isNotEmpty()}")

                    // Test encryption/decryption with the current password and salt
                    testEncryptionDecryption()

                    // Set folder name in toolbar
                    findViewById<android.widget.TextView>(R.id.folder_name_title)?.text = folder.name

                    // Load files in folder
                    database.encryptedFileDao().getFilesInFolder(folderId).collect { files ->
                        android.util.Log.d("NewFolderContentsActivity", "Loaded ${files.size} files from database")
                        fileAdapter.submitList(files)
                        updateEmptyState(files.isEmpty())
                    }
                } ?: run {
                    Toast.makeText(this@NewFolderContentsActivity, "Folder not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                android.util.Log.e("NewFolderContentsActivity", "Error loading folder data", e)
                Toast.makeText(this@NewFolderContentsActivity, "Error loading folder", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun openFilePicker() {
        // Check permissions first
        if (!checkStoragePermissions()) {
            android.util.Log.w("NewFolderContentsActivity", "Storage permissions not granted, requesting...")
            requestStoragePermissions()
            return
        }

        // Check storage before opening picker
        lifecycleScope.launch {
            val storageReady = withContext(Dispatchers.IO) {
                ExternalStorageManager.isHiddenDirectoryReady(this@NewFolderContentsActivity)
            }

            if (!storageReady) {
                android.util.Log.e("NewFolderContentsActivity", "Storage not ready, attempting to initialize...")
                val initialized = withContext(Dispatchers.IO) {
                    ExternalStorageManager.initializeHiddenDirectory(this@NewFolderContentsActivity)
                }
                if (!initialized) {
                    android.util.Log.e("NewFolderContentsActivity", "Failed to initialize storage")
                    return@launch
                }
            }

            android.util.Log.d("NewFolderContentsActivity", "Opening file picker...")
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            filePickerLauncher.launch(Intent.createChooser(intent, "Select files"))
        }
    }

    private fun handleSelectedFiles(data: Intent) {
        lifecycleScope.launch {
            try {
                // Check if external storage is ready
                if (!ExternalStorageManager.isHiddenDirectoryReady(this@NewFolderContentsActivity)) {
                    android.util.Log.e("NewFolderContentsActivity", "Hidden directory not ready, attempting to initialize...")
                    val initialized = withContext(Dispatchers.IO) {
                        ExternalStorageManager.initializeHiddenDirectory(this@NewFolderContentsActivity)
                    }
                    if (!initialized) {
                        Toast.makeText(this@NewFolderContentsActivity, "Error: Storage not available", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                }

                val uris = mutableListOf<Uri>()

                // Handle multiple files
                data.clipData?.let { clipData ->
                    for (i in 0 until clipData.itemCount) {
                        clipData.getItemAt(i).uri?.let { uri ->
                            uris.add(uri)
                        }
                    }
                } ?: data.data?.let { uri ->
                    // Handle single file
                    uris.add(uri)
                }

                if (uris.isEmpty()) {
                    Toast.makeText(this@NewFolderContentsActivity, "No files selected", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Show progress (removed toast message)

                var successCount = 0
                val fileDao = database.encryptedFileDao()

                withContext(Dispatchers.IO) {
                    uris.forEach { uri ->
                        try {
                            android.util.Log.d("NewFolderContentsActivity", "Processing file: $uri")

                            val encryptedFile = fileEncryptionService.encryptAndStoreFile(
                                uri = uri,
                                folderId = folderId,
                                masterPassword = masterPassword,
                                salt = salt
                            )

                            if (encryptedFile != null) {
                                fileDao.insertFile(encryptedFile)
                                successCount++
                                android.util.Log.d("NewFolderContentsActivity", "Successfully encrypted and stored: ${encryptedFile.originalFileName}")
                            } else {
                                android.util.Log.e("NewFolderContentsActivity", "Encryption service returned null for: $uri")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("NewFolderContentsActivity", "Error encrypting file: $uri", e)
                            android.util.Log.e("NewFolderContentsActivity", "Error details: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }

                // Files added silently (removed toast messages)

            } catch (e: Exception) {
                android.util.Log.e("NewFolderContentsActivity", "Error handling selected files", e)
                // Error handled silently (removed toast message)
            }
        }
    }

    private fun openFile(fileEntity: EncryptedFileEntity) {
        lifecycleScope.launch {
            try {
                android.util.Log.d("NewFolderContentsActivity", "Opening file: ${fileEntity.originalFileName}")
                android.util.Log.d("NewFolderContentsActivity", "Master password length: ${masterPassword.length}")
                android.util.Log.d("NewFolderContentsActivity", "Salt: $salt")
                android.util.Log.d("NewFolderContentsActivity", "Encrypted filename: ${fileEntity.encryptedFileName}")

                // Decrypting silently (removed toast message)

                val tempFile = withContext(Dispatchers.IO) {
                    fileEncryptionService.decryptFileForViewing(fileEntity, masterPassword, salt)
                }

                if (tempFile != null && tempFile.exists()) {
                    android.util.Log.d("NewFolderContentsActivity", "Decryption successful, temp file: ${tempFile.absolutePath}")
                    val intent = Intent(this@NewFolderContentsActivity, ImageViewerActivity::class.java).apply {
                        putExtra(ImageViewerActivity.EXTRA_FILE_PATH, tempFile.absolutePath)
                        putExtra(ImageViewerActivity.EXTRA_FILE_NAME, fileEntity.originalFileName)
                        putExtra(ImageViewerActivity.EXTRA_IS_TEMPORARY, true)
                    }
                    startActivity(intent)
                } else {
                    android.util.Log.e("NewFolderContentsActivity", "Decryption failed - temp file is null or doesn't exist")
                    // Failed silently (removed toast message)
                }

            } catch (e: Exception) {
                android.util.Log.e("NewFolderContentsActivity", "Error opening file: ${fileEntity.originalFileName}", e)
                android.util.Log.e("NewFolderContentsActivity", "Error details: ${e.message}")
                e.printStackTrace()
                // Error handled silently (removed toast message)
            }
        }
    }

    private fun testEncryptionDecryption() {
        try {
            android.util.Log.d("NewFolderContentsActivity", "Testing encryption/decryption...")

            // Test data
            val testData = "Hello, this is a test file content!".toByteArray()

            // Generate a test file key
            val testFileKey = EncryptionUtils.generateFileEncryptionKey()
            android.util.Log.d("NewFolderContentsActivity", "Generated test file key")

            // Encrypt the file key with master password and salt
            val encryptedFileKey = EncryptionUtils.encryptFileKey(testFileKey, masterPassword, salt)
            android.util.Log.d("NewFolderContentsActivity", "Encrypted file key with master password")

            // Encrypt the test data
            val encryptedData = EncryptionUtils.encryptFileWithKey(testData, testFileKey)
            android.util.Log.d("NewFolderContentsActivity", "Encrypted test data")

            // Now try to decrypt
            val decryptedFileKey = EncryptionUtils.decryptFileKey(encryptedFileKey, masterPassword, salt)
            android.util.Log.d("NewFolderContentsActivity", "Decrypted file key")

            val decryptedData = EncryptionUtils.decryptFileWithKey(encryptedData, decryptedFileKey)
            android.util.Log.d("NewFolderContentsActivity", "Decrypted test data")

            val decryptedString = String(decryptedData)
            android.util.Log.d("NewFolderContentsActivity", "Test result: $decryptedString")

            if (decryptedString == "Hello, this is a test file content!") {
                android.util.Log.d("NewFolderContentsActivity", "✅ Encryption/decryption test PASSED")
            } else {
                android.util.Log.e("NewFolderContentsActivity", "❌ Encryption/decryption test FAILED")
            }

        } catch (e: Exception) {
            android.util.Log.e("NewFolderContentsActivity", "❌ Encryption/decryption test ERROR", e)
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            recyclerView.visibility = View.GONE
            emptyStateView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyStateView.visibility = View.GONE
        }
    }

    private fun checkStoragePermissions(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ - For creating folder in external storage root, we need MANAGE_EXTERNAL_STORAGE
                Environment.isExternalStorageManager()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11+ - Need MANAGE_EXTERNAL_STORAGE to create folders in external storage root
                Environment.isExternalStorageManager()
            }
            else -> {
                // Below Android 11 - Check for general storage permission
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    private fun requestStoragePermissions() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11+ - Request MANAGE_EXTERNAL_STORAGE
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    manageStoragePermissionLauncher.launch(intent)
                } catch (e: Exception) {
                    android.util.Log.e("NewFolderContentsActivity", "Error requesting MANAGE_EXTERNAL_STORAGE", e)
                    Toast.makeText(this, "Please grant storage permission in settings", Toast.LENGTH_LONG).show()
                }
            }
            else -> {
                // Below Android 11 - Request traditional storage permissions
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    STORAGE_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun initializeStorageAfterPermission() {
        lifecycleScope.launch {
            val initialized = withContext(Dispatchers.IO) {
                ExternalStorageManager.initializeHiddenDirectory(this@NewFolderContentsActivity)
            }
            // Storage initialization handled silently (removed toast messages)
            android.util.Log.d("NewFolderContentsActivity", "Storage initialization result: $initialized")
        }
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                android.util.Log.d("NewFolderContentsActivity", "Storage permissions granted")
                initializeStorageAfterPermission()
            } else {
                android.util.Log.w("NewFolderContentsActivity", "Storage permissions denied")
                // Permission denied handled silently (removed toast message)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up temporary files
        fileEncryptionService.cleanupTempFiles()
    }
}
