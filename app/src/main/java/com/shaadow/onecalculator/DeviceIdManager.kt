package com.shaadow.onecalculator

import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

class DeviceIdManager(private val context: Context) {

    private val deviceInfoDao = HistoryDatabase.getInstance(context).deviceInfoDao()

    suspend fun getOrCreateDeviceId(): String {
        return withContext(Dispatchers.IO) {
            // First, try to get existing device ID from database
            val existingDeviceInfo = deviceInfoDao.getDeviceInfo()
            if (existingDeviceInfo != null) {
                return@withContext existingDeviceInfo.deviceId
            }

            // Generate new device ID
            val deviceId = generateDeviceId()

            // Store in database
            val deviceInfo = DeviceInfoEntity(
                deviceId = deviceId,
                createdAt = System.currentTimeMillis(),
                lastUpdated = System.currentTimeMillis()
            )
            deviceInfoDao.insertDeviceInfo(deviceInfo)

            return@withContext deviceId
        }
    }

    private fun generateDeviceId(): String {
        try {
            // Try to get Android ID first (more stable than UUID)
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

            if (!androidId.isNullOrEmpty() && androidId != "9774d56d682e549c") {
                // Hash the Android ID for privacy
                return hashString("android_id_$androidId")
            }
        } catch (e: Exception) {
            // Fall back to UUID if Android ID is not available
        }

        // Fallback: Generate a UUID and hash it
        val uuid = UUID.randomUUID().toString()
        return hashString("uuid_$uuid")
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.substring(0, 16).uppercase()
    }
}