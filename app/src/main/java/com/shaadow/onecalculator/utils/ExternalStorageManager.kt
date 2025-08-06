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
     * Creates .1calculator folder directly in device storage root
     */
    fun getHiddenCalculatorDir(context: Context): File? {
        return try {
            // Always try to create in external storage root first
            if (isExternalStorageWritable()) {
                val externalDir = Environment.getExternalStorageDirectory()
                val hiddenDir = File(externalDir, HIDDEN_FOLDER_NAME)
                android.util.Log.d("ExternalStorageManager", "Target directory: ${hiddenDir.absolutePath}")
                return hiddenDir
            } else {
                android.util.Log.e("ExternalStorageManager", "External storage is not writable")
                // Fallback to app's external files directory only if main storage fails
                context.getExternalFilesDir(null)?.let { appDir ->
                    android.util.Log.w("ExternalStorageManager", "Using fallback app directory: ${appDir.absolutePath}")
                    File(appDir, HIDDEN_FOLDER_NAME)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ExternalStorageManager", "Error getting hidden calculator directory", e)
            // Final fallback to app's external files directory
            context.getExternalFilesDir(null)?.let { appDir ->
                android.util.Log.w("ExternalStorageManager", "Using final fallback directory: ${appDir.absolutePath}")
                File(appDir, HIDDEN_FOLDER_NAME)
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

            val hiddenDir = getHiddenCalculatorDir(context)
            if (hiddenDir == null) {
                android.util.Log.e("ExternalStorageManager", "Cannot get hidden calculator directory")
                return false
            }

            android.util.Log.d("ExternalStorageManager", "Hidden directory path: ${hiddenDir.absolutePath}")

            val encryptedDir = getEncryptedFilesDir(context)
            val thumbnailsDir = getThumbnailsDir(context)

            if (encryptedDir == null || thumbnailsDir == null) {
                android.util.Log.e("ExternalStorageManager", "Cannot get subdirectories")
                return false
            }

            // Create directories if they don't exist
            if (!hiddenDir.exists()) {
                android.util.Log.d("ExternalStorageManager", "Creating hidden directory...")
                val created = hiddenDir.mkdirs()
                android.util.Log.d("ExternalStorageManager", "Hidden directory created: $created")
            }

            if (!encryptedDir.exists()) {
                android.util.Log.d("ExternalStorageManager", "Creating encrypted files directory...")
                val created = encryptedDir.mkdirs()
                android.util.Log.d("ExternalStorageManager", "Encrypted directory created: $created")
            }

            if (!thumbnailsDir.exists()) {
                android.util.Log.d("ExternalStorageManager", "Creating thumbnails directory...")
                val created = thumbnailsDir.mkdirs()
                android.util.Log.d("ExternalStorageManager", "Thumbnails directory created: $created")
            }

            // Verify directories are writable
            if (!hiddenDir.canWrite()) {
                android.util.Log.e("ExternalStorageManager", "Hidden directory is not writable: ${hiddenDir.absolutePath}")
                return false
            }

            if (!encryptedDir.canWrite()) {
                android.util.Log.e("ExternalStorageManager", "Encrypted directory is not writable: ${encryptedDir.absolutePath}")
                return false
            }

            // Create .nomedia files to hide from media scanner
            createNoMediaFile(hiddenDir)
            createNoMediaFile(encryptedDir)
            createNoMediaFile(thumbnailsDir)

            android.util.Log.d("ExternalStorageManager", "Hidden directory initialization completed successfully")
            return true
        } catch (e: Exception) {
            android.util.Log.e("ExternalStorageManager", "Failed to initialize hidden directory", e)
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
