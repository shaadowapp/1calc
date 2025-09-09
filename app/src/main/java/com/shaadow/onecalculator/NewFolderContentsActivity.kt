
package com.shaadow.onecalculator

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

import com.shaadow.onecalculator.utils.EncryptionUtils
import com.shaadow.onecalculator.utils.ExternalStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.log10
import kotlin.math.pow

class NewFolderContentsActivity : AppCompatActivity() {

    private lateinit var database: HistoryDatabase
    private lateinit var encryptedFileDao: EncryptedFileDao
    private lateinit var filesRecyclerView: RecyclerView
    private lateinit var emptyStateLayout: View
    private lateinit var fabAddFile: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var fileAdapter: EncryptedFileAdapter

    private var folderId: Long = -1
    private var masterPassword: String = ""
    private var folderName: String = ""
    private var folderSalt: String = ""
    private var isShortcutAccess: Boolean = false

    companion object {
        const val REQUEST_CODE_PICK_FILES = 1001
    }

    // Activity result launcher for file picker
    private val filePickerLauncher = registerForActivityResult(
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
                    encryptAndStoreFiles(uris)
                }
            }
        }
    }

    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            android.util.Log.d("NewFolderContentsActivity", "All permissions granted")
        } else {
            Toast.makeText(this, "Media permissions are required to add files", Toast.LENGTH_LONG)
                .show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_folder_contents)

        android.util.Log.d("NewFolderContentsActivity", "onCreate started")

        // Get folder ID and master password from intent
        folderId = intent.getLongExtra("folder_id", -1L)
        masterPassword = intent.getStringExtra("master_password") ?: ""
        isShortcutAccess = intent.getBooleanExtra("is_shortcut_access", false)

        android.util.Log.d(
            "NewFolderContentsActivity",
            "Folder ID: $folderId, Password provided: ${masterPassword.isNotEmpty()}"
        )

        if (folderId == -1L) {
            Toast.makeText(this, "Error: Invalid folder ID", Toast.LENGTH_SHORT).show()
            android.util.Log.e("NewFolderContentsActivity", "Invalid folder ID received: $folderId")
            finish()
            return
        }

        if (masterPassword.isEmpty()) {
            Toast.makeText(this, "Error: No password provided", Toast.LENGTH_SHORT).show()
            android.util.Log.e("NewFolderContentsActivity", "No master password provided")
            finish()
            return
        }

        initializeDatabase()
        setupToolbar()
        setupRecyclerView()
        setupFab()
        loadFolderData()
    }

    private fun initializeDatabase() {
        database = HistoryDatabase.getInstance(this)
        encryptedFileDao = database.encryptedFileDao()

        // Initialize storage directory structure - CRITICAL for production
        android.util.Log.d("NewFolderContentsActivity", "Initializing storage directory...")

        // Check external storage state first
        val storageState = android.os.Environment.getExternalStorageState()
        android.util.Log.d("NewFolderContentsActivity", "External storage state: $storageState")

        if (storageState != android.os.Environment.MEDIA_MOUNTED) {
            Toast.makeText(
                this,
                "Error: External storage not available. State: $storageState",
                Toast.LENGTH_LONG
            ).show()
            android.util.Log.e(
                "NewFolderContentsActivity",
                "External storage not mounted: $storageState"
            )
        }

        // Check storage permissions
        val hasStoragePermission =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_MEDIA_IMAGES
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }

        android.util.Log.d(
            "NewFolderContentsActivity",
            "Storage permission granted: $hasStoragePermission"
        )

        if (!ExternalStorageManager.ensureHiddenDirectoryExists(this)) {
            android.util.Log.w(
                "NewFolderContentsActivity",
                "Hidden directory not ready and could not be created"
            )

            // Get the target directory path for debugging
            val targetDir = ExternalStorageManager.getHiddenCalculatorDir(this)
            android.util.Log.d(
                "NewFolderContentsActivity",
                "Target directory: ${targetDir?.absolutePath}"
            )

            Toast.makeText(
                this,
                "Error: Cannot access storage directory. Please check app permissions and storage space.",
                Toast.LENGTH_LONG
            ).show()
            android.util.Log.e(
                "NewFolderContentsActivity",
                "CRITICAL: Failed to ensure hidden directory exists"
            )

            // Show detailed error information
            val encryptedDir = ExternalStorageManager.getEncryptedFilesDir(this)
            android.util.Log.e(
                "NewFolderContentsActivity",
                "Encrypted dir path: ${encryptedDir?.absolutePath}"
            )
            android.util.Log.e(
                "NewFolderContentsActivity",
                "Encrypted dir exists: ${encryptedDir?.exists()}"
            )
            android.util.Log.e(
                "NewFolderContentsActivity",
                "Encrypted dir writable: ${encryptedDir?.canWrite()}"
            )

            finish()
            return
        } else {
            android.util.Log.d("NewFolderContentsActivity", "Storage directory is ready")
        }
    }

    private fun setupToolbar() {
        findViewById<android.widget.ImageButton>(R.id.btn_back).setOnClickListener {
            if (isShortcutAccess) {
                // For shortcut access, go directly to calculator
                val intent = Intent(this, BasicActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intent)
                finish()
            } else {
                // Normal navigation - go back to gallery
                finish()
            }
        }

        // Set up the menu button click listener
        findViewById<android.widget.ImageButton>(R.id.btn_menu).setOnClickListener {
            showFolderOptionsMenu(it)
        }
    }

    private fun showFolderOptionsMenu(anchorView: View) {
        val popup = androidx.appcompat.widget.PopupMenu(this, anchorView)
        popup.menuInflater.inflate(R.menu.menu_folder_header, popup.menu)
        
        // Add import option to the menu
        popup.menu.add(0, 100, 7, "Import Files")

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_select_all -> {
                    toggleSelectAll()
                    true
                }

                R.id.action_sort_by_name -> {
                    sortFilesByName()
                    true
                }

                R.id.action_sort_by_date -> {
                    sortFilesByDate()
                    true
                }

                R.id.action_sort_by_size -> {
                    sortFilesBySize()
                    true
                }

                R.id.action_view_grid -> {
                    setGridView()
                    true
                }

                R.id.action_view_list -> {
                    setListView()
                    true
                }

                R.id.action_folder_info -> {
                    showFolderInfo()
                    true
                }
                
                100 -> { // Import Files action
                    importFiles()
                    true
                }

                else -> false
            }
        }

        popup.show()
    }

    private fun setupRecyclerView() {
        // This will be called after loadFolderData, so folderSalt will be available
        // For now, we'll initialize with a placeholder and update later
        filesRecyclerView = findViewById(R.id.contents_recycler_view)
        filesRecyclerView.apply {
            layoutManager = GridLayoutManager(this@NewFolderContentsActivity, 2)
        }
    }

    private fun initializeAdapter() {
        try {
            android.util.Log.d(
                "NewFolderContentsActivity",
                "Initializing adapter with salt: ${folderSalt.take(10)}..."
            )

            val fileEncryptionService =
                com.shaadow.onecalculator.services.FileEncryptionService(this)

            fileAdapter = EncryptedFileAdapter(
                context = this,
                fileEncryptionService = fileEncryptionService,
                masterPassword = masterPassword,
                salt = folderSalt,
                onFileClick = { file -> openFile(file) },
                onFileLongClick = { file, view -> showFileOptions(file) },
                onFileDelete = { file -> deleteFile(file) },
                onFileRename = { file -> renameFile(file) },
                onFileShare = { file -> shareFile(file) }
            )

            filesRecyclerView.adapter = fileAdapter
            android.util.Log.d("NewFolderContentsActivity", "Adapter initialized successfully")

        } catch (e: Exception) {
            android.util.Log.e("NewFolderContentsActivity", "Error initializing adapter", e)
            Toast.makeText(this, "Error initializing file view: ${e.message}", Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun setupFab() {
        fabAddFile = findViewById(R.id.fab_add_content)
        fabAddFile.setOnClickListener {
            android.util.Log.d("NewFolderContentsActivity", "FAB clicked")
            showAddFileOptions()
        }

        // Also setup the empty state button
        val btnAddFiles =
            findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_add_files)
        btnAddFiles?.setOnClickListener {
            android.util.Log.d("NewFolderContentsActivity", "Empty state add button clicked")
            showAddFileOptions()
        }
    }

    private fun loadFolderData() {
        lifecycleScope.launch {
            try {
                val folder = withContext(Dispatchers.IO) {
                    database.encryptedFolderDao().getFolderById(folderId)
                }

                if (folder == null) {
                    Toast.makeText(
                        this@NewFolderContentsActivity,
                        "Folder not found",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                    return@launch
                }

                folderName = folder.name
                folderSalt = folder.salt
                findViewById<android.widget.TextView>(R.id.folder_name_title).text = folderName
                initializeAdapter()
                loadFiles()

            } catch (e: Exception) {
                android.util.Log.e("NewFolderContentsActivity", "Error loading folder data", e)
                Toast.makeText(
                    this@NewFolderContentsActivity,
                    "Error loading folder",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun loadFiles() {
        lifecycleScope.launch {
            try {
                android.util.Log.d(
                    "NewFolderContentsActivity",
                    "Loading files for folder ID: $folderId"
                )
                android.util.Log.d(
                    "NewFolderContentsActivity",
                    "Master password available: ${masterPassword.isNotEmpty()}"
                )
                android.util.Log.d(
                    "NewFolderContentsActivity",
                    "Folder salt available: ${folderSalt.isNotEmpty()}"
                )

                // Ensure adapter is initialized before loading files
                if (!::fileAdapter.isInitialized) {
                    android.util.Log.w(
                        "NewFolderContentsActivity",
                        "Adapter not initialized yet, waiting..."
                    )
                    // Wait a bit and try again
                    kotlinx.coroutines.delay(100)
                    if (!::fileAdapter.isInitialized) {
                        android.util.Log.e(
                            "NewFolderContentsActivity",
                            "Adapter still not initialized, skipping file load"
                        )
                        return@launch
                    }
                }

                val files = withContext(Dispatchers.IO) {
                    encryptedFileDao.getFilesInFolderSync(folderId)
                }

                android.util.Log.d(
                    "NewFolderContentsActivity",
                    "Loaded ${files.size} files from database"
                )

                // Log each file for debugging
                files.forEachIndexed { index, file ->
                    android.util.Log.d(
                        "NewFolderContentsActivity",
                        "File $index: ${file.originalFileName} (${file.encryptedFileName})"
                    )
                }

                // Verify that encrypted files still exist on disk, but be more lenient
                val validFiles = withContext(Dispatchers.IO) {
                    val encryptedDir =
                        ExternalStorageManager.getEncryptedFilesDir(this@NewFolderContentsActivity)
                    if (encryptedDir == null) {
                        android.util.Log.w(
                            "NewFolderContentsActivity",
                            "Cannot access encrypted files directory for verification"
                        )
                        // Return all files if we can't check - better to show all files than none
                        files
                    } else {
                        files.filter { file ->
                            val encryptedFile = File(encryptedDir, file.encryptedFileName)
                            val exists = encryptedFile.exists()
                            if (!exists) {
                                android.util.Log.w(
                                    "NewFolderContentsActivity",
                                    "Encrypted file missing: ${file.encryptedFileName}"
                                )
                            }
                            exists
                        }.also { filteredFiles ->
                            if (filteredFiles.size < files.size) {
                                val missingCount = files.size - filteredFiles.size
                                android.util.Log.w(
                                    "NewFolderContentsActivity",
                                    "$missingCount files are missing from storage"
                                )
                            }
                        }
                    }
                }

                android.util.Log.d(
                    "NewFolderContentsActivity",
                    "Files to display: ${validFiles.size}"
                )

                fileAdapter.submitList(validFiles)
                updateEmptyState(validFiles.isEmpty())

            } catch (e: Exception) {
                android.util.Log.e(
                    "NewFolderContentsActivity",
                    "Error loading files for folder $folderId",
                    e
                )
                Toast.makeText(
                    this@NewFolderContentsActivity,
                    "Error loading files: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                updateEmptyState(true)
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        emptyStateLayout = findViewById(R.id.empty_state_layout)
        emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        filesRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun showAddFileOptions() {
        // Check permissions before showing file picker options
        if (!hasMediaPermissions()) {
            requestMediaPermissions()
            return
        }

        val options = arrayOf("Add Photos", "Add Videos", "Add Audio")

        MaterialAlertDialogBuilder(this)
            .setTitle("Add Files to $folderName")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openFilePicker("image/*")
                    1 -> openFilePicker("video/*")
                    2 -> openFilePicker("audio/*")
                }
            }
            .show()
    }

    private fun openFilePicker(mimeType: String) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = mimeType
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addCategory(Intent.CATEGORY_OPENABLE)
        }

        try {
            filePickerLauncher.launch(Intent.createChooser(intent, "Select Files"))
        } catch (e: Exception) {
            Toast.makeText(this, "No file manager found", Toast.LENGTH_SHORT).show()
        }
    }


    private fun encryptAndStoreFiles(uris: List<Uri>) {
        lifecycleScope.launch {
            try {
                // Ensure storage is ready - CRITICAL check before file operations
                android.util.Log.d(
                    "NewFolderContentsActivity",
                    "Checking storage readiness before encryption..."
                )
                android.util.Log.d("NewFolderContentsActivity", "Processing ${uris.size} files")

                if (!ExternalStorageManager.ensureHiddenDirectoryExists(this@NewFolderContentsActivity)) {
                    android.util.Log.w(
                        "NewFolderContentsActivity",
                        "Storage not ready and could not be created"
                    )
                    Toast.makeText(
                        this@NewFolderContentsActivity,
                        "Error: Cannot access storage. Please check app permissions and storage space.",
                        Toast.LENGTH_LONG
                    ).show()
                    android.util.Log.e(
                        "NewFolderContentsActivity",
                        "CRITICAL: Cannot initialize storage for file encryption"
                    )
                    return@launch
                } else {
                    android.util.Log.d(
                        "NewFolderContentsActivity",
                        "Storage initialized successfully"
                    )

                    // Force media scanner to refresh
                    val hiddenDir =
                        ExternalStorageManager.getHiddenCalculatorDir(this@NewFolderContentsActivity)
                    if (hiddenDir != null) {
                        android.media.MediaScannerConnection.scanFile(
                            this@NewFolderContentsActivity,
                            arrayOf(hiddenDir.absolutePath),
                            null,
                            null
                        )
                        android.util.Log.d(
                            "NewFolderContentsActivity",
                            "Media scanner notified about new directory: ${hiddenDir.absolutePath}"
                        )
                    }
                }

                // Double-check that we have valid folder data
                if (folderSalt.isEmpty()) {
                    android.util.Log.e(
                        "NewFolderContentsActivity",
                        "CRITICAL: Folder salt is empty"
                    )
                    Toast.makeText(
                        this@NewFolderContentsActivity,
                        "Error: Folder data is invalid. Please try reopening the folder.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                if (masterPassword.isEmpty()) {
                    android.util.Log.e(
                        "NewFolderContentsActivity",
                        "CRITICAL: Master password is empty"
                    )
                    Toast.makeText(
                        this@NewFolderContentsActivity,
                        "Error: Authentication data is missing. Please try reopening the folder.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                val fileEncryptionService =
                    com.shaadow.onecalculator.services.FileEncryptionService(this@NewFolderContentsActivity)
                var successCount = 0
                var errorCount = 0

                for (uri in uris) {
                    try {
                        android.util.Log.d("NewFolderContentsActivity", "Processing file: $uri")

                        // Get file name and check if it's a supported media file
                        val fileName = getFileName(uri)
                        android.util.Log.d("NewFolderContentsActivity", "Processing file: $fileName")

                        // Filter out non-media files (only allow images, videos, and audio)
                        if (!isMediaFile(uri)) {
                            android.util.Log.w("NewFolderContentsActivity", "Skipping non-media file: $fileName")
                            errorCount++
                            continue
                        }

                        val fileEntity = withContext(Dispatchers.IO) {
                            fileEncryptionService.encryptAndStoreFile(
                                uri,
                                folderId,
                                masterPassword,
                                folderSalt
                            )
                        }

                        if (fileEntity != null) {
                            android.util.Log.d(
                                "NewFolderContentsActivity",
                                "File entity created: ${fileEntity.originalFileName}"
                            )
                            android.util.Log.d(
                                "NewFolderContentsActivity",
                                "Encrypted filename: ${fileEntity.encryptedFileName}"
                            )
                            android.util.Log.d(
                                "NewFolderContentsActivity",
                                "File size: ${fileEntity.fileSize} bytes"
                            )

                            val insertedId = withContext(Dispatchers.IO) {
                                encryptedFileDao.insertFile(fileEntity)
                            }

                            android.util.Log.d(
                                "NewFolderContentsActivity",
                                "File inserted with ID: $insertedId"
                            )
                            successCount++
                            android.util.Log.d(
                                "NewFolderContentsActivity",
                                "Successfully encrypted: ${fileEntity.originalFileName}"
                            )
                        } else {
                            errorCount++
                            android.util.Log.e(
                                "NewFolderContentsActivity",
                                "Failed to encrypt file: $uri (fileEntity is null)"
                            )
                        }

                    } catch (e: Exception) {
                        android.util.Log.e(
                            "NewFolderContentsActivity",
                            "Error encrypting file: $uri",
                            e
                        )
                        android.util.Log.e(
                            "NewFolderContentsActivity",
                            "Error details: ${e.message}"
                        )
                        e.printStackTrace()
                        errorCount++
                    }
                }

                // Show result
                val message = when {
                    successCount > 0 && errorCount == 0 -> "$successCount media file(s) added successfully"
                    successCount > 0 && errorCount > 0 -> "$successCount media file(s) added, $errorCount non-media files skipped"
                    errorCount > 0 && successCount == 0 -> "No media files found. Only images, videos, and audio files are supported."
                    else -> "Failed to add files"
                }
                Toast.makeText(this@NewFolderContentsActivity, message, Toast.LENGTH_SHORT).show()

                // Reload files
                loadFiles()

            } catch (e: Exception) {
                android.util.Log.e("NewFolderContentsActivity", "Error in encryptAndStoreFiles", e)
                Toast.makeText(
                    this@NewFolderContentsActivity,
                    "Error adding files",
                    Toast.LENGTH_SHORT
                ).show()
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

    private fun openFile(file: EncryptedFileEntity) {
        lifecycleScope.launch {
            try {
                android.util.Log.d(
                    "NewFolderContentsActivity",
                    "Opening file: ${file.originalFileName}, MIME: ${file.mimeType}"
                )
                val fileEncryptionService =
                    com.shaadow.onecalculator.services.FileEncryptionService(this@NewFolderContentsActivity)

                val tempFile = withContext(Dispatchers.IO) {
                    fileEncryptionService.decryptFileForViewing(file, masterPassword, folderSalt)
                }

                if (tempFile == null || !tempFile.exists()) {
                    android.util.Log.e(
                        "NewFolderContentsActivity",
                        "Failed to decrypt file: ${file.originalFileName}"
                    )
                    Toast.makeText(
                        this@NewFolderContentsActivity,
                        "Error: Cannot decrypt file",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                android.util.Log.d(
                    "NewFolderContentsActivity",
                    "File decrypted successfully: ${tempFile.absolutePath}"
                )

                // Get proper MIME type for the file
                val mimeType = getProperMimeType(file, tempFile)
                android.util.Log.d("NewFolderContentsActivity", "Using MIME type: $mimeType")

                // Open file with appropriate app or built-in viewer
                when {
                    file.isImage -> {
                        // Open images with built-in ImageViewerActivity
                        android.util.Log.d("NewFolderContentsActivity", "Opening image file with built-in viewer")
                        val intent = Intent(this@NewFolderContentsActivity, ImageViewerActivity::class.java).apply {
                            putExtra(ImageViewerActivity.EXTRA_FILE_PATH, tempFile.absolutePath)
                            putExtra(ImageViewerActivity.EXTRA_FILE_NAME, file.originalFileName)
                            putExtra(ImageViewerActivity.EXTRA_IS_TEMPORARY, true)
                        }
                        startActivity(intent)
                    }
                    file.isVideo || mimeType.startsWith("audio/") -> {
                        // Open videos and audio with built-in MediaPlayerActivity (ExoPlayer)
                        android.util.Log.d("NewFolderContentsActivity", "Opening media file with ExoPlayer")
                        val intent = Intent(this@NewFolderContentsActivity, MediaPlayerActivity::class.java).apply {
                            putExtra(MediaPlayerActivity.EXTRA_FILE_PATH, tempFile.absolutePath)
                            putExtra(MediaPlayerActivity.EXTRA_FILE_NAME, file.originalFileName)
                            putExtra(MediaPlayerActivity.EXTRA_IS_TEMPORARY, true)
                            putExtra(MediaPlayerActivity.EXTRA_MIME_TYPE, mimeType)
                        }
                        startActivity(intent)
                    }
                    else -> {
                        // Open other media files with external apps
                        android.util.Log.d("NewFolderContentsActivity", "Opening file with external app")
                        val uri = FileProvider.getUriForFile(
                            this@NewFolderContentsActivity,
                            "${packageName}.fileprovider",
                            tempFile
                        )
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, mimeType)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }

                        // Check if there's an app to handle this file type
                        if (intent.resolveActivity(packageManager) != null) {
                            startActivity(intent)
                        } else {
                            Toast.makeText(
                                this@NewFolderContentsActivity,
                                "No app found to open this file type",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e(
                    "NewFolderContentsActivity",
                    "Error opening file: ${file.originalFileName}",
                    e
                )
                Toast.makeText(
                    this@NewFolderContentsActivity,
                    "Error opening file: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun getProperMimeType(file: EncryptedFileEntity, tempFile: File): String {
        // First try to get MIME type from file extension
        val extension = tempFile.name.substringAfterLast('.', "").lowercase()
        val mimeFromExtension = when (extension) {
            "mp4" -> "video/mp4"
            "avi" -> "video/avi"
            "mkv" -> "video/x-matroska"
            "mov" -> "video/quicktime"
            "wmv" -> "video/x-ms-wmv"
            "flv" -> "video/x-flv"
            "webm" -> "video/webm"
            "m4v" -> "video/mp4"
            "3gp" -> "video/3gpp"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "webp" -> "image/webp"
            else -> null
        }

        // Return MIME type from extension if available, otherwise use stored MIME type
        return mimeFromExtension ?: file.mimeType
    }

    private fun showFileOptions(file: EncryptedFileEntity) {
        val options = arrayOf("Open", "Share", "Rename", "Delete")

        MaterialAlertDialogBuilder(this)
            .setTitle(file.originalFileName)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openFile(file)
                    1 -> shareFile(file)
                    2 -> renameFile(file)
                    3 -> deleteFile(file)
                }
            }
            .show()
    }

    private fun shareFile(file: EncryptedFileEntity) {
        lifecycleScope.launch {
            try {
                val fileEncryptionService =
                    com.shaadow.onecalculator.services.FileEncryptionService(this@NewFolderContentsActivity)

                val tempFile = withContext(Dispatchers.IO) {
                    fileEncryptionService.decryptFileForViewing(file, masterPassword, folderSalt)
                }

                if (tempFile == null || !tempFile.exists()) {
                    Toast.makeText(
                        this@NewFolderContentsActivity,
                        "Error: Cannot decrypt file for sharing",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                // Share file
                val uri = FileProvider.getUriForFile(
                    this@NewFolderContentsActivity,
                    "${packageName}.fileprovider",
                    tempFile
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = file.mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(Intent.createChooser(shareIntent, "Share ${file.originalFileName}"))

            } catch (e: Exception) {
                android.util.Log.e("NewFolderContentsActivity", "Error sharing file", e)
                Toast.makeText(
                    this@NewFolderContentsActivity,
                    "Error sharing file: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun renameFile(file: EncryptedFileEntity) {
        // Create a dialog to get the new file name
        val dialogView = layoutInflater.inflate(R.layout.dialog_rename_file, null)
        val nameInput =
            dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.file_name_input)
        val nameLayout =
            dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.file_name_input_layout)

        // Set the current file name
        nameInput.setText(file.originalFileName)

        MaterialAlertDialogBuilder(this)
            .setTitle("Rename File")
            .setView(dialogView)
            .setPositiveButton("Rename") { _, _ ->
                val newName = nameInput.text?.toString()?.trim() ?: ""
                if (newName.isEmpty()) {
                    Toast.makeText(this, "File name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newName == file.originalFileName) {
                    Toast.makeText(
                        this,
                        "New name is the same as the current name",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                performFileRename(file, newName)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performFileRename(file: EncryptedFileEntity, newName: String) {
        lifecycleScope.launch {
            try {
                android.util.Log.d(
                    "NewFolderContentsActivity",
                    "Renaming file: ${file.originalFileName} to $newName"
                )

                // Create a copy of the file entity with the new name
                val updatedFile = file.copy(originalFileName = newName)

                // Update the file in the database
                withContext(Dispatchers.IO) {
                    encryptedFileDao.updateFile(updatedFile)
                }

                android.util.Log.d(
                    "NewFolderContentsActivity",
                    "File renamed successfully in database"
                )
                Toast.makeText(
                    this@NewFolderContentsActivity,
                    "File renamed successfully",
                    Toast.LENGTH_SHORT
                ).show()

                // Reload the files to refresh the UI
                loadFiles()

            } catch (e: Exception) {
                android.util.Log.e("NewFolderContentsActivity", "Error renaming file", e)
                Toast.makeText(
                    this@NewFolderContentsActivity,
                    "Error renaming file: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun deleteFile(file: EncryptedFileEntity) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete File")
            .setMessage("Are you sure you want to delete '${file.originalFileName}'?")
            .setPositiveButton("Delete") { _, _ ->
                performFileDelete(file)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performFileDelete(file: EncryptedFileEntity) {
        lifecycleScope.launch {
            try {
                android.util.Log.d("NewFolderContentsActivity", "Deleting file: ${file.originalFileName}")

                // Delete encrypted file from storage using proper path
                val encryptedDir = ExternalStorageManager.getEncryptedFilesDir(this@NewFolderContentsActivity)
                if (encryptedDir != null) {
                    val encryptedFile = File(encryptedDir, file.encryptedFileName)
                    android.util.Log.d("NewFolderContentsActivity", "Looking for encrypted file at: ${encryptedFile.absolutePath}")

                    if (encryptedFile.exists()) {
                        val deleted = encryptedFile.delete()
                        android.util.Log.d("NewFolderContentsActivity", "File deleted from storage: $deleted")
                    } else {
                        android.util.Log.w("NewFolderContentsActivity", "Encrypted file not found in storage: ${encryptedFile.absolutePath}")
                    }
                } else {
                    android.util.Log.e("NewFolderContentsActivity", "Cannot access encrypted files directory")
                }

                // Delete from database
                withContext(Dispatchers.IO) {
                    encryptedFileDao.deleteFile(file)
                }

                Toast.makeText(this@NewFolderContentsActivity, "File deleted successfully", Toast.LENGTH_SHORT).show()
                loadFiles()

            } catch (e: Exception) {
                android.util.Log.e("NewFolderContentsActivity", "Error deleting file: ${file.originalFileName}", e)
                Toast.makeText(this@NewFolderContentsActivity, "Error deleting file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleSelectAll() {
        if (!::fileAdapter.isInitialized) return
        
        // Check if we're already in selection mode with all items selected
        val allSelected = fileAdapter.getSelectedCount() == fileAdapter.itemCount
        
        if (allSelected) {
            // Deselect all
            fileAdapter.setSelectionMode(false)
            Toast.makeText(this, "Selection cleared", Toast.LENGTH_SHORT).show()
        } else {
            // Select all
            fileAdapter.setSelectionMode(true)
            fileAdapter.selectAll()
            Toast.makeText(this, "All items selected", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun sortFilesByName() {
        if (!::fileAdapter.isInitialized) return
        
        lifecycleScope.launch {
            try {
                val files = withContext(Dispatchers.IO) {
                    encryptedFileDao.getFilesInFolderSync(folderId)
                }
                
                val sortedFiles = files.sortedBy { it.originalFileName.lowercase() }
                fileAdapter.submitList(sortedFiles)
                Toast.makeText(this@NewFolderContentsActivity, "Sorted by name", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.util.Log.e("NewFolderContentsActivity", "Error sorting files by name", e)
            }
        }
    }
    
    private fun sortFilesByDate() {
        if (!::fileAdapter.isInitialized) return
        
        lifecycleScope.launch {
            try {
                val files = withContext(Dispatchers.IO) {
                    encryptedFileDao.getFilesInFolderSync(folderId)
                }
                
                val sortedFiles = files.sortedByDescending { it.dateAdded }
                fileAdapter.submitList(sortedFiles)
                Toast.makeText(this@NewFolderContentsActivity, "Sorted by date", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.util.Log.e("NewFolderContentsActivity", "Error sorting files by date", e)
            }
        }
    }
    
    private fun sortFilesBySize() {
        if (!::fileAdapter.isInitialized) return
        
        lifecycleScope.launch {
            try {
                val files = withContext(Dispatchers.IO) {
                    encryptedFileDao.getFilesInFolderSync(folderId)
                }
                
                val sortedFiles = files.sortedByDescending { it.fileSize }
                fileAdapter.submitList(sortedFiles)
                Toast.makeText(this@NewFolderContentsActivity, "Sorted by size", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.util.Log.e("NewFolderContentsActivity", "Error sorting files by size", e)
            }
        }
    }
    
    private fun setGridView() {
        filesRecyclerView.layoutManager = GridLayoutManager(this, 2)
        Toast.makeText(this, "Grid view", Toast.LENGTH_SHORT).show()
    }
    
    private fun setListView() {
        filesRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        Toast.makeText(this, "List view", Toast.LENGTH_SHORT).show()
    }
    
    private fun showFolderInfo() {
        lifecycleScope.launch {
            try {
                val files = withContext(Dispatchers.IO) {
                    encryptedFileDao.getFilesInFolderSync(folderId)
                }

                // Get folder position for shortcut info
                val folders = withContext(Dispatchers.IO) {
                    database.encryptedFolderDao().getAllFoldersSync()
                }
                val folderPosition = folders.indexOfFirst { it.id == folderId }

                val totalSize = files.sumOf { it.fileSize }
                val imageCount = files.count { it.isImage }
                val videoCount = files.count { it.isVideo }
                val audioCount = files.count { it.mimeType.startsWith("audio/") }
                val otherCount = files.size - imageCount - videoCount - audioCount

                val message = buildString {
                    append("📁 Folder: $folderName\n\n")
                    append("📊 Total Files: ${files.size}\n")
                    append("📏 Total Size: ${formatFileSize(totalSize)}\n\n")
                    append("📷 Images: $imageCount\n")
                    append("🎬 Videos: $videoCount\n")
                    append("🎵 Audio: $audioCount\n")
                    append("📄 Other: $otherCount\n\n")
                    if (folderPosition >= 0) {
                        append("⚡ Calculator Shortcut: Press '$folderPosition' then long-press =\n")
                        append("💡 Tip: Opens this folder directly from calculator")
                    }
                }

                MaterialAlertDialogBuilder(this@NewFolderContentsActivity)
                    .setTitle("Folder Information")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show()

            } catch (e: Exception) {
                android.util.Log.e("NewFolderContentsActivity", "Error showing folder info", e)
                Toast.makeText(this@NewFolderContentsActivity, "Error retrieving folder information", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // Add these methods at the class level, not inside another method
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
                    // Process the selected files - use the existing encryptAndStoreFiles method
                    encryptAndStoreFiles(uris)
                }
            }
        }
    }
    
    // Add this method to the NewFolderContentsActivity class
    fun importFiles() {
        // Check permissions before showing file picker
        if (!hasMediaPermissions()) {
            requestMediaPermissions()
            return
        }
        
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
            android.util.Log.e("NewFolderContentsActivity", "Error launching file picker", e)
        }
    }
    
    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (kotlin.math.log10(size.toDouble()) / kotlin.math.log10(1024.0)).toInt()
        return String.format("%.1f %s", size / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
    }
    
    private fun hasMediaPermissions(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestMediaPermissions() {
        val permissions = mutableListOf<String>()

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
        }
    }

    private fun isMediaFile(uri: Uri): Boolean {
        val mimeType = getMimeTypeFromUri(uri)

        // Check MIME type first
        if (mimeType.startsWith("image/") ||
            mimeType.startsWith("video/") ||
            mimeType.startsWith("audio/")) {
            return true
        }

        // Check file extension for additional support
        val fileName = getFileName(uri) ?: return false
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

    private fun getMimeTypeFromUri(uri: Uri): String {
        return contentResolver.getType(uri) ?: run {
            val fileName = getFileName(uri) ?: return "application/octet-stream"
            val extension = fileName.substringAfterLast('.', "").lowercase()
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
        }
    }
}
