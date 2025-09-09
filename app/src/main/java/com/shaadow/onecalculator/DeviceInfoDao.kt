package com.shaadow.onecalculator

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface DeviceInfoDao {
    @Query("SELECT * FROM device_info WHERE id = 'device_id' LIMIT 1")
    suspend fun getDeviceInfo(): DeviceInfoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeviceInfo(deviceInfo: DeviceInfoEntity)

    @Update
    suspend fun updateDeviceInfo(deviceInfo: DeviceInfoEntity)

    @Query("DELETE FROM device_info WHERE id = 'device_id'")
    suspend fun deleteDeviceInfo()
}