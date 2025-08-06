package com.shaadow.onecalculator

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EncryptedFolderDao {
    
    @Query("""
        SELECT * FROM encrypted_folders
        ORDER BY
            CASE name
                WHEN 'Photos' THEN 1
                WHEN 'Videos' THEN 2
                WHEN 'Audios' THEN 3
                WHEN 'Others' THEN 4
                ELSE 5
            END,
            name ASC
    """)
    fun getAllFolders(): Flow<List<EncryptedFolderEntity>>
    
    @Query("SELECT * FROM encrypted_folders WHERE id = :id")
    suspend fun getFolderById(id: Long): EncryptedFolderEntity?
    
    @Query("SELECT * FROM encrypted_folders WHERE name = :name")
    suspend fun getFolderByName(name: String): EncryptedFolderEntity?
    
    @Insert
    suspend fun insertFolder(folder: EncryptedFolderEntity): Long
    
    @Update
    suspend fun updateFolder(folder: EncryptedFolderEntity)
    
    @Delete
    suspend fun deleteFolder(folder: EncryptedFolderEntity)

    // Note: Item counts are now calculated from encrypted_files table
    // Use EncryptedFileDao.getFileCountInFolder(folderId) instead
}