package com.shaadow.onecalculator

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "encrypted_files",
    foreignKeys = [ForeignKey(
        entity = EncryptedFolderEntity::class,
        parentColumns = ["id"],
        childColumns = ["folderId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["folderId"])]
)
data class EncryptedFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalFileName: String,        // Original filename (e.g., "vacation_photo.jpg")
    val encryptedFileName: String,       // Encrypted filename on disk (e.g., "a7k9mN2p.enc")
    val folderId: Long,                  // Reference to parent folder
    val fileSize: Long,                  // Original file size in bytes
    val encryptedSize: Long,             // Encrypted file size in bytes
    val mimeType: String,                // MIME type (image/jpeg, video/mp4, etc.)
    val dateAdded: Long = System.currentTimeMillis(),
    val dateModified: Long = System.currentTimeMillis(),
    val encryptionKey: String,           // Encrypted file-specific key
    val thumbnailPath: String? = null,   // Optional thumbnail for images/videos
    val isVideo: Boolean = false,        // Quick check for video files
    val isImage: Boolean = false,        // Quick check for image files
    val duration: Long? = null,          // Video duration in milliseconds
    val width: Int? = null,              // Image/video width
    val height: Int? = null              // Image/video height
)
