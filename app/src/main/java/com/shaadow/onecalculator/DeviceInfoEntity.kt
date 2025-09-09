package com.shaadow.onecalculator

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_info")
data class DeviceInfoEntity(
    @PrimaryKey
    val id: String = "device_id", // Fixed ID for single device info record
    val deviceId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
)