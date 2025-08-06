package com.shaadow.onecalculator.services

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.shaadow.onecalculator.EncryptedFileEntity
import com.shaadow.onecalculator.utils.EncryptionUtils
import com.shaadow.onecalculator.utils.ExternalStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class FileEncryptionService(private val context: Context) {
    
    /**
     * Encrypt and store a file from URI
     */
    suspend fun encryptAndStoreFile(
        uri: Uri,
        folderId: Long,
        masterPassword: String,
        salt: String
    ): EncryptedFileEntity? = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("FileEncryptionService", "Starting encryption for URI: $uri")

            // Get original file info
            val originalFileName = getFileNameFromUri(uri) ?: "unknown_file_${System.currentTimeMillis()}"
            val mimeType = getMimeTypeFromUri(uri)
            val fileSize = getFileSizeFromUri(uri)

            android.util.Log.d("FileEncryptionService", "File info - Name: $originalFileName, Size: $fileSize, Type: $mimeType")

            if (fileSize <= 0) {
                android.util.Log.e("FileEncryptionService", "File size is 0 or negative: $fileSize")
                return@withContext null
            }

            // Generate unique encryption key and filename
            val fileKey = EncryptionUtils.generateFileEncryptionKey()
            val encryptedFileName = EncryptionUtils.generateSecureFileName()
            val encryptedFileKey = EncryptionUtils.encryptFileKey(fileKey, masterPassword, salt)

            android.util.Log.d("FileEncryptionService", "Generated encrypted filename: $encryptedFileName")

            // Get destination file
            val encryptedDir = ExternalStorageManager.getEncryptedFilesDir(context)
            if (encryptedDir == null) {
                android.util.Log.e("FileEncryptionService", "Cannot access encrypted files directory")
                throw IOException("Cannot access encrypted files directory")
            }

            if (!encryptedDir.exists()) {
                android.util.Log.e("FileEncryptionService", "Encrypted directory does not exist: ${encryptedDir.absolutePath}")
                throw IOException("Encrypted directory does not exist")
            }

            if (!encryptedDir.canWrite()) {
                android.util.Log.e("FileEncryptionService", "Cannot write to encrypted directory: ${encryptedDir.absolutePath}")
                throw IOException("Cannot write to encrypted directory")
            }

            val destinationFile = File(encryptedDir, encryptedFileName)
            android.util.Log.d("FileEncryptionService", "Destination file: ${destinationFile.absolutePath}")
            
            // Read, encrypt, and write file
            android.util.Log.d("FileEncryptionService", "Reading file data...")
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val originalData = inputStream.readBytes()
                android.util.Log.d("FileEncryptionService", "Read ${originalData.size} bytes from file")

                if (originalData.isEmpty()) {
                    android.util.Log.e("FileEncryptionService", "File data is empty")
                    throw IOException("File data is empty")
                }

                android.util.Log.d("FileEncryptionService", "Encrypting file data...")
                val encryptedData = EncryptionUtils.encryptFileWithKey(originalData, fileKey)
                android.util.Log.d("FileEncryptionService", "Encrypted data size: ${encryptedData.size} bytes")

                android.util.Log.d("FileEncryptionService", "Writing encrypted data to: ${destinationFile.absolutePath}")
                FileOutputStream(destinationFile).use { outputStream ->
                    outputStream.write(encryptedData)
                    outputStream.flush()
                }

                android.util.Log.d("FileEncryptionService", "File written successfully, size: ${destinationFile.length()} bytes")
            } ?: throw IOException("Cannot open input stream for URI: $uri")
            
            // Create database entity
            EncryptedFileEntity(
                originalFileName = originalFileName,
                encryptedFileName = encryptedFileName,
                folderId = folderId,
                fileSize = fileSize,
                encryptedSize = destinationFile.length(),
                mimeType = mimeType,
                encryptionKey = encryptedFileKey,
                isImage = mimeType.startsWith("image/"),
                isVideo = mimeType.startsWith("video/"),
                dateAdded = System.currentTimeMillis(),
                dateModified = System.currentTimeMillis()
            )
            
        } catch (e: Exception) {
            android.util.Log.e("FileEncryptionService", "Error encrypting file from URI: $uri", e)
            null
        }
    }
    
    /**
     * Decrypt a file to temporary location for viewing
     */
    suspend fun decryptFileForViewing(
        fileEntity: EncryptedFileEntity,
        masterPassword: String,
        salt: String
    ): File? = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("FileEncryptionService", "Starting decryption for: ${fileEntity.originalFileName}")

            // Decrypt file key
            android.util.Log.d("FileEncryptionService", "Decrypting file key...")
            val fileKey = EncryptionUtils.decryptFileKey(fileEntity.encryptionKey, masterPassword, salt)
            android.util.Log.d("FileEncryptionService", "File key decrypted successfully")

            // Get encrypted file
            val encryptedDir = ExternalStorageManager.getEncryptedFilesDir(context)
            if (encryptedDir == null) {
                android.util.Log.e("FileEncryptionService", "Cannot access encrypted files directory")
                throw IOException("Cannot access encrypted files directory")
            }

            val encryptedFile = File(encryptedDir, fileEntity.encryptedFileName)
            android.util.Log.d("FileEncryptionService", "Looking for encrypted file: ${encryptedFile.absolutePath}")

            if (!encryptedFile.exists()) {
                android.util.Log.e("FileEncryptionService", "Encrypted file not found: ${encryptedFile.absolutePath}")
                throw IOException("Encrypted file not found: ${fileEntity.encryptedFileName}")
            }

            android.util.Log.d("FileEncryptionService", "Encrypted file found, size: ${encryptedFile.length()} bytes")

            // Create temporary file for viewing
            val tempDir = File(context.cacheDir, "decrypted_temp")
            if (!tempDir.exists()) {
                val created = tempDir.mkdirs()
                android.util.Log.d("FileEncryptionService", "Created temp directory: $created")
            }

            val tempFile = File(tempDir, fileEntity.originalFileName)
            android.util.Log.d("FileEncryptionService", "Temp file will be: ${tempFile.absolutePath}")

            // Decrypt and write to temp file
            android.util.Log.d("FileEncryptionService", "Reading encrypted data...")
            FileInputStream(encryptedFile).use { inputStream ->
                val encryptedData = inputStream.readBytes()
                android.util.Log.d("FileEncryptionService", "Read ${encryptedData.size} bytes of encrypted data")

                if (encryptedData.isEmpty()) {
                    throw IOException("Encrypted file is empty")
                }

                android.util.Log.d("FileEncryptionService", "Decrypting file data...")
                val decryptedData = EncryptionUtils.decryptFileWithKey(encryptedData, fileKey)
                android.util.Log.d("FileEncryptionService", "Decrypted ${decryptedData.size} bytes")

                android.util.Log.d("FileEncryptionService", "Writing decrypted data to temp file...")
                FileOutputStream(tempFile).use { outputStream ->
                    outputStream.write(decryptedData)
                    outputStream.flush()
                }

                android.util.Log.d("FileEncryptionService", "Temp file created successfully, size: ${tempFile.length()} bytes")
            }

            tempFile

        } catch (e: Exception) {
            android.util.Log.e("FileEncryptionService", "Error decrypting file: ${fileEntity.originalFileName}", e)
            android.util.Log.e("FileEncryptionService", "Error details: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Delete encrypted file from storage
     */
    suspend fun deleteEncryptedFile(fileEntity: EncryptedFileEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val encryptedDir = ExternalStorageManager.getEncryptedFilesDir(context) ?: return@withContext false
            val encryptedFile = File(encryptedDir, fileEntity.encryptedFileName)
            
            if (encryptedFile.exists()) {
                encryptedFile.delete()
            } else {
                true // File already doesn't exist
            }
        } catch (e: Exception) {
            android.util.Log.e("FileEncryptionService", "Error deleting encrypted file: ${fileEntity.encryptedFileName}", e)
            false
        }
    }
    
    /**
     * Clean up temporary decrypted files
     */
    fun cleanupTempFiles() {
        try {
            val tempDir = File(context.cacheDir, "decrypted_temp")
            if (tempDir.exists()) {
                tempDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("FileEncryptionService", "Error cleaning up temp files", e)
        }
    }
    
    /**
     * Get file name from URI
     */
    private fun getFileNameFromUri(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get MIME type from URI
     */
    private fun getMimeTypeFromUri(uri: Uri): String {
        return context.contentResolver.getType(uri) ?: run {
            val fileName = getFileNameFromUri(uri) ?: return "application/octet-stream"
            val extension = fileName.substringAfterLast('.', "")
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) 
                ?: "application/octet-stream"
        }
    }
    
    /**
     * Get file size from URI
     */
    private fun getFileSizeFromUri(uri: Uri): Long {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (sizeIndex != -1 && cursor.moveToFirst()) {
                    cursor.getLong(sizeIndex)
                } else {
                    0L
                }
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
