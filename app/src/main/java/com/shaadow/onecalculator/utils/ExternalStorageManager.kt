package com.shaadow.onecalculator.utils

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.IOException

object ExternalStorageManager {
    
    private const val HIDDEN_FOLDER_NAME = ".1calculator"
    private const val ENCRYPTED_FILES_FOLDER = "encrypted_files"
    private const val THUMBNAILS_FOLDER = "thumbnails"
    private const val NOMEDIA_FILE = ".nomedia"
    
    /**
     * Get the hidden calculator directory on external storage
     * Creates .1calculator folder directly in device external storage root for visibility in file manager
     */
    fun getHiddenCalculatorDir(context: Context): File? {
        return try {
            android.util.Log.d("ExternalStorageManager", "Getting hidden calculator directory...")
            android.util.Log.d("ExternalStorageManager", "Android version: ${android.os.Build.VERSION.SDK_INT}")

            // Try external storage root first for all Android versions to make folder visible in file manager
            if (isExternalStorageWritable()) {
                val externalDir = Environment.getExternalStorageDirectory()
                val hiddenDir = File(externalDir, HIDDEN_FOLDER_NAME)
                android.util.Log.d("ExternalStorageManager", "External storage directory: ${hiddenDir.absolutePath}")

                // Check if we can actually write to this location
                if (hiddenDir.exists() || hiddenDir.mkdirs()) {
                    if (hiddenDir.canWrite()) {
                        android.util.Log.d("ExternalStorageManager", "External storage directory is writable")
                        return hiddenDir
                    } else {
                        android.util.Log.w("ExternalStorageManager", "External storage directory not writable")
                    }
                } else {
                    android.util.Log.w("ExternalStorageManager", "Cannot create external storage directory")
                }
            } else {
                android.util.Log.e("ExternalStorageManager", "External storage is not writable")
            }

            // Fallback to app's external files directory
            val appDir = context.getExternalFilesDir(null)
            if (appDir != null) {
                val hiddenDir = File(appDir, HIDDEN_FOLDER_NAME)
                android.util.Log.w("ExternalStorageManager", "Using app external files directory as fallback: ${hiddenDir.absolutePath}")
                return hiddenDir
            } else {
                android.util.Log.e("ExternalStorageManager", "Cannot get app external files directory")
                // Final fallback to internal storage
                val internalDir = File(context.filesDir, HIDDEN_FOLDER_NAME)
                android.util.Log.w("ExternalStorageManager", "Using internal storage as final fallback: ${internalDir.absolutePath}")
                return internalDir
            }

        } catch (e: Exception) {
            android.util.Log.e("ExternalStorageManager", "Error getting hidden calculator directory", e)
            // Final fallback to internal storage
            try {
                val internalDir = File(context.filesDir, HIDDEN_FOLDER_NAME)
                android.util.Log.w("ExternalStorageManager", "Exception fallback to internal storage: ${internalDir.absolutePath}")
                return internalDir
            } catch (e2: Exception) {
                android.util.Log.e("ExternalStorageManager", "Critical error: Cannot access any storage", e2)
                return null
            }
        }
    }
    
    /**
     * Get the encrypted files directory
     */
    fun getEncryptedFilesDir(context: Context): File? {
        val hiddenDir = getHiddenCalculatorDir(context) ?: return null
        return File(hiddenDir, ENCRYPTED_FILES_FOLDER)
    }
    
    /**
     * Get the thumbnails directory
     */
    fun getThumbnailsDir(context: Context): File? {
        val hiddenDir = getHiddenCalculatorDir(context) ?: return null
        return File(hiddenDir, THUMBNAILS_FOLDER)
    }
    
    /**
     * Initialize the hidden directory structure
     */
    fun initializeHiddenDirectory(context: Context): Boolean {
        try {
            android.util.Log.d("ExternalStorageManager", "Initializing hidden directory...")
            android.util.Log.d("ExternalStorageManager", "Android SDK: ${android.os.Build.VERSION.SDK_INT}")

            val hiddenDir = getHiddenCalculatorDir(context)
            if (hiddenDir == null) {
                android.util.Log.e("ExternalStorageManager", "CRITICAL: Cannot get hidden calculator directory")
                return false
            }

            android.util.Log.d("ExternalStorageManager", "Hidden directory path: ${hiddenDir.absolutePath}")
            android.util.Log.d("ExternalStorageManager", "Hidden directory parent exists: ${hiddenDir.parentFile?.exists()}")
            android.util.Log.d("ExternalStorageManager", "Hidden directory parent writable: ${hiddenDir.parentFile?.canWrite()}")

            val encryptedDir = getEncryptedFilesDir(context)
            val thumbnailsDir = getThumbnailsDir(context)

            if (encryptedDir == null || thumbnailsDir == null) {
                android.util.Log.e("ExternalStorageManager", "CRITICAL: Cannot get subdirectories")
                android.util.Log.e("ExternalStorageManager", "Encrypted dir: $encryptedDir")
                android.util.Log.e("ExternalStorageManager", "Thumbnails dir: $thumbnailsDir")
                return false
            }

            android.util.Log.d("ExternalStorageManager", "Encrypted files directory: ${encryptedDir.absolutePath}")
            android.util.Log.d("ExternalStorageManager", "Thumbnails directory: ${thumbnailsDir.absolutePath}")

            // Create directories if they don't exist
            if (!hiddenDir.exists()) {
                android.util.Log.d("ExternalStorageManager", "Creating hidden directory...")
                try {
                    val created = hiddenDir.mkdirs()
                    android.util.Log.d("ExternalStorageManager", "Hidden directory created: $created")
                    if (!created) {
                        android.util.Log.e("ExternalStorageManager", "Failed to create hidden directory")
                        // Try alternative approach
                        if (!hiddenDir.mkdir()) {
                            android.util.Log.e("ExternalStorageManager", "Failed to create hidden directory with mkdir()")
                            return false
                        }
                    }
                } catch (e: SecurityException) {
                    android.util.Log.e("ExternalStorageManager", "Security exception creating hidden directory", e)
                    return false
                }
            } else {
                android.util.Log.d("ExternalStorageManager", "Hidden directory already exists")
            }

            if (!encryptedDir.exists()) {
                android.util.Log.d("ExternalStorageManager", "Creating encrypted files directory...")
                try {
                    val created = encryptedDir.mkdirs()
                    android.util.Log.d("ExternalStorageManager", "Encrypted directory created: $created")
                    if (!created) {
                        android.util.Log.e("ExternalStorageManager", "Failed to create encrypted directory")
                        return false
                    }
                } catch (e: SecurityException) {
                    android.util.Log.e("ExternalStorageManager", "Security exception creating encrypted directory", e)
                    return false
                }
            } else {
                android.util.Log.d("ExternalStorageManager", "Encrypted directory already exists")
            }

            if (!thumbnailsDir.exists()) {
                android.util.Log.d("ExternalStorageManager", "Creating thumbnails directory...")
                try {
                    val created = thumbnailsDir.mkdirs()
                    android.util.Log.d("ExternalStorageManager", "Thumbnails directory created: $created")
                    if (!created) {
                        android.util.Log.w("ExternalStorageManager", "Failed to create thumbnails directory (non-critical)")
                    }
                } catch (e: SecurityException) {
                    android.util.Log.w("ExternalStorageManager", "Security exception creating thumbnails directory (non-critical)", e)
                }
            } else {
                android.util.Log.d("ExternalStorageManager", "Thumbnails directory already exists")
            }

            // Verify directories are accessible
            android.util.Log.d("ExternalStorageManager", "Verifying directory accessibility...")
            android.util.Log.d("ExternalStorageManager", "Hidden dir exists: ${hiddenDir.exists()}")
            android.util.Log.d("ExternalStorageManager", "Hidden dir readable: ${hiddenDir.canRead()}")
            android.util.Log.d("ExternalStorageManager", "Hidden dir writable: ${hiddenDir.canWrite()}")
            android.util.Log.d("ExternalStorageManager", "Encrypted dir exists: ${encryptedDir.exists()}")
            android.util.Log.d("ExternalStorageManager", "Encrypted dir readable: ${encryptedDir.canRead()}")
            android.util.Log.d("ExternalStorageManager", "Encrypted dir writable: ${encryptedDir.canWrite()}")

            if (!hiddenDir.canWrite()) {
                android.util.Log.e("ExternalStorageManager", "CRITICAL: Hidden directory is not writable: ${hiddenDir.absolutePath}")
                return false
            }

            if (!encryptedDir.canWrite()) {
                android.util.Log.e("ExternalStorageManager", "CRITICAL: Encrypted directory is not writable: ${encryptedDir.absolutePath}")
                return false
            }

            // Test write access by creating a test file
            try {
                val testFile = File(encryptedDir, ".test_write_access")
                val canWrite = testFile.createNewFile()
                if (canWrite) {
                    testFile.delete()
                    android.util.Log.d("ExternalStorageManager", "Write access test successful")
                } else {
                    android.util.Log.e("ExternalStorageManager", "Write access test failed")
                    return false
                }
            } catch (e: Exception) {
                android.util.Log.e("ExternalStorageManager", "Write access test exception", e)
                return false
            }

            // Create .nomedia files to hide from media scanner
            try {
                createNoMediaFile(hiddenDir)
                createNoMediaFile(encryptedDir)
                createNoMediaFile(thumbnailsDir)
            } catch (e: Exception) {
                android.util.Log.w("ExternalStorageManager", "Failed to create .nomedia files (non-critical)", e)
            }

            // Create a visible marker file to ensure directory is visible in file manager
            try {
                val markerFile = File(hiddenDir, ".directory_info.txt")
                val info = """
                    |Hidden Calculator Gallery Directory
                    |Created: ${java.util.Date()}
                    |This directory contains encrypted media files.
                    |Do not modify files directly - use the app instead.
                    |Location: ${hiddenDir.absolutePath}
                """.trimMargin()
                markerFile.writeText(info)
                android.util.Log.d("ExternalStorageManager", "Created directory marker file: ${markerFile.absolutePath}")
            } catch (e: Exception) {
                android.util.Log.w("ExternalStorageManager", "Failed to create directory marker file (non-critical)", e)
            }

            android.util.Log.d("ExternalStorageManager", "Hidden directory initialization completed successfully")
            android.util.Log.d("ExternalStorageManager", "Directory should be visible at: ${hiddenDir.absolutePath}")
            return true
        } catch (e: Exception) {
            android.util.Log.e("ExternalStorageManager", "CRITICAL: Failed to initialize hidden directory", e)
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Create .nomedia file to hide directory from media scanner
     */
    private fun createNoMediaFile(directory: File) {
        try {
            val noMediaFile = File(directory, NOMEDIA_FILE)
            if (!noMediaFile.exists()) {
                noMediaFile.createNewFile()
            }
        } catch (e: IOException) {
            android.util.Log.w("ExternalStorageManager", "Failed to create .nomedia file in ${directory.path}", e)
        }
    }
    
    /**
     * Check if external storage is available for read and write
     */
    fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }
    
    /**
     * Check if external storage is available for read
     */
    fun isExternalStorageReadable(): Boolean {
        return Environment.getExternalStorageState() in setOf(
            Environment.MEDIA_MOUNTED,
            Environment.MEDIA_MOUNTED_READ_ONLY
        )
    }
    
    /**
     * Get available space in the hidden directory
     */
    fun getAvailableSpace(context: Context): Long {
        val hiddenDir = getHiddenCalculatorDir(context) ?: return 0L
        return try {
            hiddenDir.usableSpace
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Get used space in the hidden directory
     */
    fun getUsedSpace(context: Context): Long {
        val encryptedDir = getEncryptedFilesDir(context) ?: return 0L
        return try {
            calculateDirectorySize(encryptedDir)
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Calculate total size of a directory
     */
    private fun calculateDirectorySize(directory: File): Long {
        var size = 0L
        if (directory.exists()) {
            directory.listFiles()?.forEach { file ->
                size += if (file.isDirectory) {
                    calculateDirectorySize(file)
                } else {
                    file.length()
                }
            }
        }
        return size
    }
    
    /**
     * Clean up temporary files and empty directories
     */
    fun cleanupHiddenDirectory(context: Context) {
        try {
            val hiddenDir = getHiddenCalculatorDir(context) ?: return
            
            // Remove empty directories
            hiddenDir.listFiles()?.forEach { file ->
                if (file.isDirectory && file.listFiles()?.isEmpty() == true) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("ExternalStorageManager", "Failed to cleanup hidden directory", e)
        }
    }
    
    /**
     * Check if the hidden directory is properly set up
     */
    fun isHiddenDirectoryReady(context: Context): Boolean {
        try {
            val hiddenDir = getHiddenCalculatorDir(context)
            if (hiddenDir == null) {
                android.util.Log.e("ExternalStorageManager", "Hidden directory is null")
                return false
            }

            val encryptedDir = getEncryptedFilesDir(context)
            if (encryptedDir == null) {
                android.util.Log.e("ExternalStorageManager", "Encrypted directory is null")
                return false
            }

            android.util.Log.d("ExternalStorageManager", "Checking directory readiness:")
            android.util.Log.d("ExternalStorageManager", "  Hidden dir exists: ${hiddenDir.exists()}")
            android.util.Log.d("ExternalStorageManager", "  Hidden dir readable: ${hiddenDir.canRead()}")
            android.util.Log.d("ExternalStorageManager", "  Hidden dir writable: ${hiddenDir.canWrite()}")
            android.util.Log.d("ExternalStorageManager", "  Encrypted dir exists: ${encryptedDir.exists()}")
            android.util.Log.d("ExternalStorageManager", "  Encrypted dir readable: ${encryptedDir.canRead()}")
            android.util.Log.d("ExternalStorageManager", "  Encrypted dir writable: ${encryptedDir.canWrite()}")

            val isReady = hiddenDir.exists() &&
                   hiddenDir.canRead() &&
                   hiddenDir.canWrite() &&
                   encryptedDir.exists() &&
                   encryptedDir.canRead() &&
                   encryptedDir.canWrite()

            android.util.Log.d("ExternalStorageManager", "Directory ready: $isReady")
            return isReady
        } catch (e: Exception) {
            android.util.Log.e("ExternalStorageManager", "Error checking directory readiness", e)
            return false
        }
    }
}
