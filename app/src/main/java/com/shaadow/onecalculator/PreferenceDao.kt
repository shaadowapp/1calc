package com.shaadow.onecalculator

import androidx.room.*

@Dao
interface PreferenceDao {
    @Query("SELECT * FROM preference WHERE key = :key LIMIT 1")
    suspend fun getPreference(key: String): PreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setPreference(preference: PreferenceEntity)

    @Query("DELETE FROM preference")
    suspend fun clearAllPreferences()

    @Query("DELETE FROM preference WHERE key = :key")
    suspend fun deletePreference(key: String)

    @Query("SELECT * FROM preference")
    fun observeAllPreferences(): kotlinx.coroutines.flow.Flow<List<PreferenceEntity>>
} 