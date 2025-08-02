package com.shaadow.onecalculator

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.shaadow.onecalculator.databinding.ActivityMediaGalleryBinding
import com.shaadow.onecalculator.utils.EncryptionUtils
import kotlinx.coroutines.launch
import java.io.File

class MediaGalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMediaGalleryBinding
    private lateinit var folderAdapter: EncryptedFolderAdapter
    private lateinit var database: HistoryDatabase
    private lateinit var encryptedFolderDao: EncryptedFolderDao

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
        checkPermissions()
        setupClickListeners()
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

    private fun setupClickListeners() {
        binding.fabCreateFolder.setOnClickListener {
            showCreateFolderDialog()
        }

        binding.createFirstFolderBtn.setOnClickListener {
            showCreateFolderDialog()
        }
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