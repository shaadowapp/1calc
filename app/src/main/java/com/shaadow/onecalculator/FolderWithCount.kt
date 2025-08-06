package com.shaadow.onecalculator

/**
 * Data class that combines folder entity with its file count
 */
data class FolderWithCount(
    val folder: EncryptedFolderEntity,
    val fileCount: Int
) {
    val id: Long get() = folder.id
    val name: String get() = folder.name
    val passwordHash: String get() = folder.passwordHash
    val salt: String get() = folder.salt
    val createdAt: Long get() = folder.createdAt
    val lastModified: Long get() = folder.lastModified
    val parentFolderId: Long? get() = folder.parentFolderId
    
    val itemCountText: String get() = when (fileCount) {
        0 -> "Empty"
        1 -> "1 item"
        else -> "$fileCount items"
    }
}
