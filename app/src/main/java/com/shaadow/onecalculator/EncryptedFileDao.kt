package com.shaadow.onecalculator

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EncryptedFileDao {
    
    @Query("SELECT * FROM encrypted_files WHERE folderId = :folderId ORDER BY dateAdded DESC")
    fun getFilesInFolder(folderId: Long): Flow<List<EncryptedFileEntity>>
    
    @Query("SELECT * FROM encrypted_files WHERE id = :fileId")
    suspend fun getFileById(fileId: Long): EncryptedFileEntity?
    
    @Query("SELECT * FROM encrypted_files WHERE encryptedFileName = :encryptedFileName")
    suspend fun getFileByEncryptedName(encryptedFileName: String): EncryptedFileEntity?
    
    @Query("SELECT * FROM encrypted_files WHERE folderId = :folderId AND isImage = 1 ORDER BY dateAdded DESC")
    fun getImagesInFolder(folderId: Long): Flow<List<EncryptedFileEntity>>
    
    @Query("SELECT * FROM encrypted_files WHERE folderId = :folderId AND isVideo = 1 ORDER BY dateAdded DESC")
    fun getVideosInFolder(folderId: Long): Flow<List<EncryptedFileEntity>>
    
    @Query("SELECT COUNT(*) FROM encrypted_files WHERE folderId = :folderId")
    suspend fun getFileCountInFolder(folderId: Long): Int
    
    @Query("SELECT SUM(fileSize) FROM encrypted_files WHERE folderId = :folderId")
    suspend fun getTotalSizeInFolder(folderId: Long): Long?
    
    @Query("SELECT * FROM encrypted_files ORDER BY dateAdded DESC LIMIT :limit")
    fun getRecentFiles(limit: Int = 20): Flow<List<EncryptedFileEntity>>
    
    @Query("SELECT * FROM encrypted_files WHERE originalFileName LIKE '%' || :searchQuery || '%' ORDER BY dateAdded DESC")
    fun searchFiles(searchQuery: String): Flow<List<EncryptedFileEntity>>
    
    @Insert
    suspend fun insertFile(file: EncryptedFileEntity): Long
    
    @Insert
    suspend fun insertFiles(files: List<EncryptedFileEntity>): List<Long>
    
    @Update
    suspend fun updateFile(file: EncryptedFileEntity)
    
    @Delete
    suspend fun deleteFile(file: EncryptedFileEntity)
    
    @Query("DELETE FROM encrypted_files WHERE id = :fileId")
    suspend fun deleteFileById(fileId: Long)
    
    @Query("DELETE FROM encrypted_files WHERE folderId = :folderId")
    suspend fun deleteAllFilesInFolder(folderId: Long)
    
    @Query("DELETE FROM encrypted_files WHERE encryptedFileName = :encryptedFileName")
    suspend fun deleteFileByEncryptedName(encryptedFileName: String)
    
    // Statistics queries
    @Query("SELECT COUNT(*) FROM encrypted_files")
    suspend fun getTotalFileCount(): Int
    
    @Query("SELECT SUM(fileSize) FROM encrypted_files")
    suspend fun getTotalStorageUsed(): Long?
    
    @Query("SELECT COUNT(*) FROM encrypted_files WHERE isImage = 1")
    suspend fun getTotalImageCount(): Int
    
    @Query("SELECT COUNT(*) FROM encrypted_files WHERE isVideo = 1")
    suspend fun getTotalVideoCount(): Int
}
