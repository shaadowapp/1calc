package com.shaadow.onecalculator

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EncryptedFolderDao {
    
    @Query("SELECT * FROM encrypted_folders ORDER BY lastModified DESC")
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
    
    @Query("UPDATE encrypted_folders SET itemCount = :count, lastModified = :timestamp WHERE id = :id")
    suspend fun updateItemCount(id: Long, count: Int, timestamp: Long = System.currentTimeMillis())
}