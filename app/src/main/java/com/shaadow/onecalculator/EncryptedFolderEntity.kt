package com.shaadow.onecalculator

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "encrypted_folders")
data class EncryptedFolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val passwordHash: String, // SHA-256 hash of the password
    val salt: String, // Salt used for encryption
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val itemCount: Int = 0,
    val folderPath: String // Internal app directory path for this folder
)