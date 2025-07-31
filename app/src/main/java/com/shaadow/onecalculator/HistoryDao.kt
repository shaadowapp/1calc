package com.shaadow.onecalculator

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>
    
    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentHistory(limit: Int = 5): List<HistoryEntity>

    @Query("SELECT * FROM history WHERE expression LIKE '%' || :query || '%' OR result LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchHistory(query: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE expression LIKE :query OR result LIKE :query ORDER BY timestamp DESC")
    suspend fun searchHistorySync(query: String): List<HistoryEntity>
    
    @Insert
    suspend fun insert(history: HistoryEntity)
    
    @Delete
    suspend fun delete(history: HistoryEntity)
    
    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: Int)
    
    @Query("DELETE FROM history")
    suspend fun clearAll()
} 