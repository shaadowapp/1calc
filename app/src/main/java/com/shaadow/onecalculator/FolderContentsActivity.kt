package com.shaadow.onecalculator

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.EditText // Explicit import
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

class FolderContentsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var contentsAdapter: FolderContentsAdapter
    private lateinit var folder: EncryptedFolderEntity
    private lateinit var emptyStateLayout: LinearLayout

    companion object {
        const val EXTRA_FOLDER_ID = "folder_id" // Consistent naming with intent extra key
        const val REQUEST_CODE_PICK_FILES = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_folder_contents)

        val folderIdFromIntent = intent.getLongExtra(EXTRA_FOLDER_ID, -1L)
        if (folderIdFromIntent == -1L) {
            android.util.Log.e("FolderContentsActivity", "Invalid folder ID received.")
            Toast.makeText(this, "Error: Folder ID missing.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        android.util.Log.d("FolderContentsActivity", "Activity created for folder ID: $folderIdFromIntent")

        setupViews()
        setupRecyclerView()
        loadFolderData(folderIdFromIntent) // Renamed for clarity
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.contents_recycler_view)
        emptyStateLayout = findViewById(R.id.empty_state_layout)

        findViewById<ImageButton>(R.id.btn_back)?.setOnClickListener {
            finish()
        }

        // Setup for both the button in empty state and the FAB
        val addContentClickListener = View.OnClickListener { showAddContentOptions() }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_add_files)?.setOnClickListener(addContentClickListener)
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_add_content)?.setOnClickListener(addContentClickListener)
    }

    private fun setupRecyclerView() {
        contentsAdapter = FolderContentsAdapter(
            onItemClick = { file -> openItem(file) },
            onItemLongClick = { file -> showItemOptions(file) }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@FolderContentsActivity)
            adapter = contentsAdapter
        }
    }

    private fun loadFolderData(folderId: Long) { // Renamed parameter for clarity
        lifecycleScope.launch {
            try {
                val database = HistoryDatabase.getInstance(this@FolderContentsActivity)
                val loadedFolder = withContext(Dispatchers.IO) {
                    database.encryptedFolderDao().getFolderById(folderId)
                }

                if (loadedFolder == null) {
                    android.util.Log.e("FolderContentsActivity", "Folder not found in database with ID: $folderId")
                    Toast.makeText(this@FolderContentsActivity, "Folder data missing. Please try again.", Toast.LENGTH_LONG).show()
                    finish()
                    return@launch
                }
                folder = loadedFolder // Assign to class member
                android.util.Log.d("FolderContentsActivity", "Successfully loaded folder: ${folder.name} at path: ${folder.folderPath}")

                findViewById<TextView>(R.id.folder_name_title)?.text = folder.name
                loadFolderContents() // Renamed for clarity

            } catch (e: Exception) {
                android.util.Log.e("FolderContentsActivity", "Exception while loading folder data for ID: $folderId", e)
                Toast.makeText(this@FolderContentsActivity, "Error loading folder details.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun loadFolderContents() { // Renamed for clarity
        lifecycleScope.launch {
            try {
                val currentFolderFile = File(folder.folderPath)
                if (!currentFolderFile.exists() || !currentFolderFile.isDirectory) {
                    android.util.Log.e("FolderContentsActivity", "Folder path does not exist or is not a directory: ${folder.folderPath}")
                    Toast.makeText(this@FolderContentsActivity, "Error: Folder storage is missing or invalid.", Toast.LENGTH_LONG).show()
                    updateEmptyState(true)
                    return@launch
                }

                val items = withContext(Dispatchers.IO) {
                    val allFilesAndDirs = currentFolderFile.listFiles() ?: emptyArray()
                    // Sort folders first, then files, both alphabetically
                    allFilesAndDirs.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.getDefault()) }))
                }

                android.util.Log.d("FolderContentsActivity", "Found ${items.size} items in folder '${folder.name}'.")
                contentsAdapter.submitList(items.toList())
                updateEmptyState(items.isEmpty())

            } catch (se: SecurityException) {
                android.util.Log.e("FolderContentsActivity", "SecurityException loading contents for ${folder.folderPath}", se)
                Toast.makeText(this@FolderContentsActivity, "Permission denied to access folder contents.", Toast.LENGTH_LONG).show()
                updateEmptyState(true)
            } catch (e: Exception) {
                android.util.Log.e("FolderContentsActivity", "Generic error loading contents for ${folder.folderPath}", e)
                Toast.makeText(this@FolderContentsActivity, "Error loading folder contents.", Toast.LENGTH_SHORT).show()
                updateEmptyState(true)
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        android.util.Log.d("FolderContentsActivity", "Updating empty state view: isEmpty = $isEmpty")
        emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun showAddContentOptions() {
        val options = arrayOf("📁 New Subfolder", "➕ Add Files") // Simplified options

        AlertDialog.Builder(this)
            .setTitle("Add to '${folder.name}'")
            .setItems(options) { dialog, which ->
                when (options[which]) {
                    "📁 New Subfolder" -> showCreateSubfolderDialog()
                    "➕ Add Files" -> openFilePicker("*/*")
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun showCreateSubfolderDialog() {
        val input = EditText(this) // Using explicit EditText
        input.hint = "Enter subfolder name"
        input.isSingleLine = true

        AlertDialog.Builder(this)
            .setTitle("Create New Subfolder")
            .setView(input)
            .setPositiveButton("Create") { dialogItself, _ ->
                val subfolderName = input.text.toString().trim()
                if (subfolderName.isNotEmpty()) {
                    // Simplified validation
                    if (subfolderName.contains("/") || subfolderName.contains("\\")) {
                        Toast.makeText(this, "Folder name cannot contain slashes.", Toast.LENGTH_SHORT).show()
                        // return@setPositiveButton // No return here, just show Toast and let dialog stay or be dismissed by user
                    } else {
                        createSubfolder(subfolderName)
                        dialogItself.dismiss() // Dismiss only on success or valid input path
                    }
                } else {
                    Toast.makeText(this, "Folder name cannot be empty.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialogItself, _ -> dialogItself.cancel() }
            .show()
    }

    private fun createSubfolder(name: String) {
        lifecycleScope.launch {
            try {
                val database = HistoryDatabase.getInstance(this@FolderContentsActivity)
                val parentFolderPath = folder.folderPath
                val subfolderFile = File(parentFolderPath, name)

                if (subfolderFile.exists()) {
                    Toast.makeText(this@FolderContentsActivity, "A folder or file named '$name' already exists.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val success = withContext(Dispatchers.IO) {
                    subfolderFile.mkdirs()
                }

                if (success) {
                    val subfolderEntity = EncryptedFolderEntity(
                        name = name,
                        passwordHash = folder.passwordHash,
                        salt = folder.salt,
                        parentFolderId = folder.id
                    )
                    withContext(Dispatchers.IO) {
                        database.encryptedFolderDao().insertFolder(subfolderEntity)
                    }
                    Toast.makeText(this@FolderContentsActivity, "Subfolder '$name' created successfully.", Toast.LENGTH_SHORT).show()
                    loadFolderContents()
                } else {
                    Toast.makeText(this@FolderContentsActivity, "Failed to create subfolder directory on disk.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("FolderContentsActivity", "Error creating subfolder '$name'", e)
                Toast.makeText(this@FolderContentsActivity, "Error creating subfolder: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openFilePicker(mimeType: String) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = mimeType
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        try {
            startActivityForResult(Intent.createChooser(intent, "Select files to add"), REQUEST_CODE_PICK_FILES)
        } catch (e: android.content.ActivityNotFoundException) {
            Toast.makeText(this, "No file manager app found to pick files.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            android.util.Log.e("FolderContentsActivity", "Error opening file picker", e)
            Toast.makeText(this, "Could not open file picker.", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Deprecated in Java. Consider using ActivityResultLauncher for new code.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_FILES && resultCode == Activity.RESULT_OK) {
            lifecycleScope.launch {
                var filesCopiedSuccessfully = 0
                val urisToProcess = mutableListOf<Uri>()

                data?.clipData?.let { clipData ->
                    for (i in 0 until clipData.itemCount) {
                        urisToProcess.add(clipData.getItemAt(i).uri)
                    }
                } ?: data?.data?.let { uri ->
                    urisToProcess.add(uri)
                }

                if (urisToProcess.isEmpty()) {
                    Toast.makeText(this@FolderContentsActivity, "No files were selected.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                Toast.makeText(this@FolderContentsActivity, "Adding ${urisToProcess.size} file(s)...", Toast.LENGTH_SHORT).show()

                for (uri in urisToProcess) {
                    if (copyFileToFolder(uri)) {
                        filesCopiedSuccessfully++
                    }
                }

                if (filesCopiedSuccessfully > 0) {
                    Toast.makeText(this@FolderContentsActivity, "$filesCopiedSuccessfully file(s) added to '${folder.name}'.", Toast.LENGTH_LONG).show()
                    loadFolderContents()
                } else if (urisToProcess.isNotEmpty()) {
                    Toast.makeText(this@FolderContentsActivity, "Failed to add the selected file(s).", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun copyFileToFolder(uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = getFileNameFromUri(uri) ?: "imported_file_${System.currentTimeMillis()}"
                var destinationFile = File(folder.folderPath, fileName)
                var counter = 1
                // Handle potential name conflicts
                while (destinationFile.exists()) {
                    val nameWithoutExt = fileName.substringBeforeLast('.', fileName)
                    val fileExtension = fileName.substringAfterLast('.', "")
                    val newName = if (fileExtension.isNotEmpty()) "$nameWithoutExt($counter).$fileExtension" else "$nameWithoutExt($counter)"
                    destinationFile = File(folder.folderPath, newName)
                    counter++
                }

                contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(destinationFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: return@withContext false // Could not open input stream
                android.util.Log.d("FolderContentsActivity", "File copied to ${destinationFile.absolutePath}")
                true
            } catch (ioe: IOException) {
                android.util.Log.e("FolderContentsActivity", "IOException copying file from URI $uri", ioe)
                // Optionally show specific error to user on main thread
                false
            } catch (se: SecurityException) {
                android.util.Log.e("FolderContentsActivity", "SecurityException copying file URI $uri", se)
                false
            } catch (e: Exception) {
                android.util.Log.e("FolderContentsActivity", "General error copying file URI $uri", e)
                false
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var fileName: String? = null
        try {
            if (uri.scheme == "content") {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (displayNameIndex != -1) {
                            fileName = cursor.getString(displayNameIndex)
                        }
                    }
                }
            }
            if (fileName == null) {
                fileName = uri.path?.let { File(it).name }
            }
        } catch (e: Exception) {
            android.util.Log.e("FolderContentsActivity","Error getting file name from URI: $uri", e)
        }
        return fileName?.replace("[\\\\/:*?\"<>|]".toRegex(), "_") // Sanitize file name
    }

    private fun openItem(item: File) {
        if (item.isDirectory) {
            lifecycleScope.launch {
                try {
                    val database = HistoryDatabase.getInstance(this@FolderContentsActivity)
                    val subfolderEntity = withContext(Dispatchers.IO) {
                        // getAllFolders returns Flow, so we need to get first value
                        val folders = database.encryptedFolderDao().getAllFolders().first()
                        folders.firstOrNull { it.folderPath == item.absolutePath }
                    }

                    if (subfolderEntity != null) {
                        val intent = Intent(this@FolderContentsActivity, FolderContentsActivity::class.java).apply {
                            putExtra(EXTRA_FOLDER_ID, subfolderEntity.id)
                        }
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@FolderContentsActivity, "Subfolder not found in database.", Toast.LENGTH_SHORT).show()
                        android.util.Log.w("FolderContentsActivity", "Clicked directory not registered: ${item.absolutePath}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FolderContentsActivity", "Error opening subfolder ${item.name}", e)
                    Toast.makeText(this@FolderContentsActivity, "Error opening folder: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else { // It's a file
            try {
                val mimeType = getMimeTypeFromFile(item)
                android.util.Log.d("FolderContentsActivity", "Opening file: ${item.name}, MIME: $mimeType")

                // For images, videos, and other media files, use our custom viewer
                when {
                    mimeType?.startsWith("image/") == true ||
                    mimeType?.startsWith("video/") == true -> {
                        val intent = Intent(this@FolderContentsActivity, ImageViewerActivity::class.java).apply {
                            putExtra(ImageViewerActivity.EXTRA_FILE_PATH, item.absolutePath)
                            putExtra(ImageViewerActivity.EXTRA_FILE_NAME, item.name)
                        }
                        startActivity(intent)
                    }
                    else -> {
                        // For other file types, try to open with external apps
                        openWithExternalApp(item, mimeType)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FolderContentsActivity", "Error opening file ${item.name}", e)
                Toast.makeText(this, "Error opening file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openWithExternalApp(item: File, mimeType: String?) {
        try {
            val authority = "${applicationContext.packageName}.fileprovider"

            // Debug logging
            android.util.Log.d("FolderContentsActivity", "Attempting to open file:")
            android.util.Log.d("FolderContentsActivity", "  File path: ${item.absolutePath}")
            android.util.Log.d("FolderContentsActivity", "  File exists: ${item.exists()}")
            android.util.Log.d("FolderContentsActivity", "  File readable: ${item.canRead()}")
            android.util.Log.d("FolderContentsActivity", "  Authority: $authority")
            android.util.Log.d("FolderContentsActivity", "  MIME type: $mimeType")

            val uriForFile = FileProvider.getUriForFile(this@FolderContentsActivity, authority, item)
            android.util.Log.d("FolderContentsActivity", "  Generated URI: $uriForFile")

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uriForFile, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "No app found to open this file type: ${mimeType ?: "Unknown"}", Toast.LENGTH_LONG).show()
            }
        } catch (iae: IllegalArgumentException) {
            android.util.Log.e("FolderContentsActivity", "FileProvider error for ${item.name}. File path: ${item.absolutePath}", iae)
            android.util.Log.e("FolderContentsActivity", "Files dir: ${filesDir.absolutePath}")
            android.util.Log.e("FolderContentsActivity", "External files dir: ${getExternalFilesDir(null)?.absolutePath}")
            Toast.makeText(this, "Error: Could not open file. Check FileProvider setup.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            android.util.Log.e("FolderContentsActivity", "Error opening file ${item.name}", e)
            Toast.makeText(this, "Error opening file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getMimeTypeFromFile(file: File): String? {
        if (file.isDirectory) {
            return null
        }
        val extension = file.extension
        if (extension.isEmpty()) {
            return "application/octet-stream" // Default for unknown binary files
        }
        val lowerExt = extension.lowercase(Locale.ROOT)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(lowerExt) ?: "application/octet-stream"
    }

    private fun showItemOptions(item: File) {
        val options = mutableListOf("📖 Open")
        if (item.isFile) {
            options.add("📤 Share")
        }
        options.addAll(listOf("✏️ Rename", "🗑️ Delete"))

        AlertDialog.Builder(this)
            .setTitle(item.name)
            .setItems(options.toTypedArray()) { dialog, which ->
                dialog.dismiss() // Dismiss dialog after selection
                when (options[which]) {
                    "📖 Open" -> openItem(item)
                    "📤 Share" -> if (item.isFile) shareFile(item)
                    "✏️ Rename" -> renameFileOrFolder(item)
                    "🗑️ Delete" -> deleteFileOrFolder(item)
                }
            }
            .show()
    }

    private fun shareFile(file: File) {
        if (!file.isFile) return
        try {
            val authority = "${applicationContext.packageName}.fileprovider"

            // Debug logging for sharing
            android.util.Log.d("FolderContentsActivity", "Attempting to share file:")
            android.util.Log.d("FolderContentsActivity", "  File path: ${file.absolutePath}")
            android.util.Log.d("FolderContentsActivity", "  File exists: ${file.exists()}")
            android.util.Log.d("FolderContentsActivity", "  Authority: $authority")

            val uri = FileProvider.getUriForFile(this, authority, file)
            val mimeType = getMimeTypeFromFile(file)

            android.util.Log.d("FolderContentsActivity", "  Generated URI: $uri")
            android.util.Log.d("FolderContentsActivity", "  MIME type: $mimeType")

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Share '${file.name}' via")
            if (chooser.resolveActivity(packageManager) != null) {
                startActivity(chooser)
            } else {
                if(intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "No app found to share this file.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (iae: IllegalArgumentException) {
            android.util.Log.e("FolderContentsActivity", "FileProvider error sharing ${file.name}. File path: ${file.absolutePath}", iae)
            android.util.Log.e("FolderContentsActivity", "Files dir: ${filesDir.absolutePath}")
            Toast.makeText(this, "Error: Could not share file. Check FileProvider setup.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            android.util.Log.e("FolderContentsActivity", "Error sharing file ${file.name}", e)
            Toast.makeText(this, "Error sharing file.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun renameFileOrFolder(item: File) {
        val input = EditText(this)
        val originalName = item.nameWithoutExtension
        val originalExtension = if (item.isFile) item.extension else "" // Folders don't have extensions in this context

        input.setText(originalName)
        input.hint = if (item.isDirectory) "New folder name" else "New file name (without extension)"

        AlertDialog.Builder(this)
            .setTitle("Rename ${if (item.isDirectory) "Folder" else "File"}")
            .setView(input)
            .setPositiveButton("Rename") { dialog, _ ->
                val newNameWithoutExt = input.text.toString().trim()
                if (newNameWithoutExt.isNotEmpty()) {
                    val newFullName = if (item.isFile && originalExtension.isNotEmpty()) {
                        "$newNameWithoutExt.$originalExtension"
                    } else {
                        newNameWithoutExt // For folders or files being renamed without extension shown
                    }

                    if (newFullName == item.name) {
                        dialog.dismiss()
                        return@setPositiveButton
                    }

                    val newFile = File(item.parentFile, newFullName)

                    if (newFile.exists()){
                        Toast.makeText(this, "'$newFullName' already exists.", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton // Keep dialog open for user to correct
                    }

                    // Store old path before renaming
                    val oldPath = item.absolutePath

                    if (item.renameTo(newFile)) {
                        if (item.isDirectory) {
                            lifecycleScope.launch {
                                val db = HistoryDatabase.getInstance(this@FolderContentsActivity)

                                withContext(Dispatchers.IO) {
                                    // Find folder by original path and update with new path
                                    val folders = db.encryptedFolderDao().getAllFolders().first()
                                    val folderToUpdate = folders.firstOrNull { it.folderPath == oldPath }
                                    folderToUpdate?.let {
                                        val updatedEntity = it.copy(
                                            name = newFile.name,
                                            lastModified = System.currentTimeMillis()
                                        )
                                        db.encryptedFolderDao().updateFolder(updatedEntity)
                                    }
                                }
                            }
                        }
                        Toast.makeText(this, "Renamed to '$newFullName'", Toast.LENGTH_SHORT).show()
                        loadFolderContents()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(this, "Failed to rename.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Name cannot be empty.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") {dialog, _ -> dialog.dismiss()}
            .show()
    }

    private fun deleteFileOrFolder(item: File) {
        AlertDialog.Builder(this)
            .setTitle("Delete ${if (item.isDirectory) "Folder" else "File"}")
            .setMessage("Are you sure you want to delete '${item.name}'? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                lifecycleScope.launch {
                    try {
                        val isDirectory = item.isDirectory
                        val originalPath = item.absolutePath // Path before deletion

                        val success = withContext(Dispatchers.IO) {
                            if (isDirectory) item.deleteRecursively() else item.delete()
                        }

                        if (success) {
                            if (isDirectory) {
                                val db = HistoryDatabase.getInstance(this@FolderContentsActivity)
                                withContext(Dispatchers.IO) {
                                    // Find folder by its original path to delete from DB
                                    val folders = db.encryptedFolderDao().getAllFolders().first()
                                    val folderEntityToDelete = folders.firstOrNull { it.folderPath == originalPath }
                                    folderEntityToDelete?.let {
                                        db.encryptedFolderDao().deleteFolder(it)
                                    }
                                }
                            }
                            Toast.makeText(this@FolderContentsActivity, "'${item.name}' deleted.", Toast.LENGTH_SHORT).show()
                            loadFolderContents()
                        } else {
                            Toast.makeText(this@FolderContentsActivity, "Failed to delete '${item.name}'.", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FolderContentsActivity", "Error deleting item ${item.name}", e)
                        Toast.makeText(this@FolderContentsActivity, "Error deleting: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") {dialog, _ -> dialog.dismiss()}
            .show()
    }
} // End of FolderContentsActivity class
