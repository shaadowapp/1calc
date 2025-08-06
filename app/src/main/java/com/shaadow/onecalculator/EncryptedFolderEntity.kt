package com.shaadow.onecalculator

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "encrypted_folders",
    foreignKeys = [ForeignKey(
        entity = EncryptedFolderEntity::class,
        parentColumns = ["id"],
        childColumns = ["parentFolderId"],
        onDelete = ForeignKey.CASCADE // Optional: define behavior on parent delete
    )],
    indices = [Index(value = ["parentFolderId"])] // Optional: improves query performance
)
data class EncryptedFolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val passwordHash: String, // SHA-256 hash of the password
    val salt: String, // Salt used for encryption
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val parentFolderId: Long? = null // ID of the parent folder, null for root folders
) {
    // Backward compatibility property for old FolderContentsActivity
    // This is deprecated and should not be used in new code
    @Deprecated("Use virtual folder system instead")
    val folderPath: String get() = "/deprecated/virtual/folder/$id"
}
