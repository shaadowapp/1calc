package com.shaadow.onecalculator.utils

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

object EncryptionUtils {
    
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALGORITHM = "AES"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 16
    
    /**
     * Generate a random salt for password hashing
     */
    fun generateSalt(): String {
        val salt = ByteArray(32)
        SecureRandom().nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }
    
    /**
     * Hash password with salt using SHA-256
     */
    fun hashPassword(password: String, salt: String): String {
        val saltBytes = Base64.decode(salt, Base64.NO_WRAP)
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(saltBytes)
        val hashedBytes = digest.digest(password.toByteArray())
        return Base64.encodeToString(hashedBytes, Base64.NO_WRAP)
    }
    
    /**
     * Verify password against hash
     */
    fun verifyPassword(password: String, salt: String, hashedPassword: String): Boolean {
        val computedHash = hashPassword(password, salt)
        return computedHash == hashedPassword
    }
    
    /**
     * Generate AES key from password and salt
     */
    private fun generateKeyFromPassword(password: String, salt: String): SecretKeySpec {
        val saltBytes = Base64.decode(salt, Base64.NO_WRAP)
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(password.toByteArray())
        digest.update(saltBytes)
        val keyBytes = digest.digest()
        return SecretKeySpec(keyBytes, KEY_ALGORITHM)
    }
    
    /**
     * Encrypt data using AES-256-GCM
     */
    fun encrypt(data: ByteArray, password: String, salt: String): ByteArray {
        val key = generateKeyFromPassword(password, salt)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data)
        
        // Combine IV and encrypted data
        val result = ByteArray(iv.size + encryptedData.size)
        System.arraycopy(iv, 0, result, 0, iv.size)
        System.arraycopy(encryptedData, 0, result, iv.size, encryptedData.size)
        
        return result
    }
    
    /**
     * Decrypt data using AES-256-GCM
     */
    fun decrypt(encryptedData: ByteArray, password: String, salt: String): ByteArray {
        val key = generateKeyFromPassword(password, salt)
        
        // Extract IV and encrypted data
        val iv = ByteArray(GCM_IV_LENGTH)
        val cipherData = ByteArray(encryptedData.size - GCM_IV_LENGTH)
        System.arraycopy(encryptedData, 0, iv, 0, iv.size)
        System.arraycopy(encryptedData, iv.size, cipherData, 0, cipherData.size)
        
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        
        return cipher.doFinal(cipherData)
    }
    
    /**
     * Generate a secure random filename for encrypted files
     */
    fun generateSecureFileName(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..32)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("") + ".enc"
    }

    /**
     * Generate a unique encryption key for a file
     */
    fun generateFileEncryptionKey(): String {
        val keyBytes = ByteArray(32) // 256-bit key
        SecureRandom().nextBytes(keyBytes)
        return Base64.encodeToString(keyBytes, Base64.NO_WRAP)
    }

    /**
     * Encrypt a file encryption key with master password
     */
    fun encryptFileKey(fileKey: String, masterPassword: String, salt: String): String {
        val fileKeyBytes = Base64.decode(fileKey, Base64.NO_WRAP)
        val encryptedKey = encrypt(fileKeyBytes, masterPassword, salt)
        return Base64.encodeToString(encryptedKey, Base64.NO_WRAP)
    }

    /**
     * Decrypt a file encryption key with master password
     */
    fun decryptFileKey(encryptedFileKey: String, masterPassword: String, salt: String): String {
        try {
            android.util.Log.d("EncryptionUtils", "Decrypting file key...")
            val encryptedKeyBytes = Base64.decode(encryptedFileKey, Base64.NO_WRAP)
            android.util.Log.d("EncryptionUtils", "Encrypted key bytes length: ${encryptedKeyBytes.size}")

            val decryptedKey = decrypt(encryptedKeyBytes, masterPassword, salt)
            android.util.Log.d("EncryptionUtils", "Decrypted key bytes length: ${decryptedKey.size}")

            val result = Base64.encodeToString(decryptedKey, Base64.NO_WRAP)
            android.util.Log.d("EncryptionUtils", "File key decrypted successfully")
            return result
        } catch (e: Exception) {
            android.util.Log.e("EncryptionUtils", "Error decrypting file key", e)
            throw e
        }
    }

    /**
     * Encrypt file with its unique key
     */
    fun encryptFileWithKey(data: ByteArray, fileKey: String): ByteArray {
        val keyBytes = Base64.decode(fileKey, Base64.NO_WRAP)
        val key = SecretKeySpec(keyBytes, KEY_ALGORITHM)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data)

        // Combine IV and encrypted data
        val result = ByteArray(iv.size + encryptedData.size)
        System.arraycopy(iv, 0, result, 0, iv.size)
        System.arraycopy(encryptedData, 0, result, iv.size, encryptedData.size)

        return result
    }

    /**
     * Decrypt file with its unique key
     */
    fun decryptFileWithKey(encryptedData: ByteArray, fileKey: String): ByteArray {
        try {
            android.util.Log.d("EncryptionUtils", "Decrypting file data...")
            android.util.Log.d("EncryptionUtils", "Encrypted data size: ${encryptedData.size}")

            if (encryptedData.size < GCM_IV_LENGTH) {
                throw IllegalArgumentException("Encrypted data too small, expected at least $GCM_IV_LENGTH bytes")
            }

            val keyBytes = Base64.decode(fileKey, Base64.NO_WRAP)
            android.util.Log.d("EncryptionUtils", "Key bytes length: ${keyBytes.size}")

            val key = SecretKeySpec(keyBytes, KEY_ALGORITHM)

            // Extract IV and encrypted data
            val iv = ByteArray(GCM_IV_LENGTH)
            val cipherData = ByteArray(encryptedData.size - GCM_IV_LENGTH)
            System.arraycopy(encryptedData, 0, iv, 0, iv.size)
            System.arraycopy(encryptedData, iv.size, cipherData, 0, cipherData.size)

            android.util.Log.d("EncryptionUtils", "IV length: ${iv.size}, Cipher data length: ${cipherData.size}")

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            val result = cipher.doFinal(cipherData)
            android.util.Log.d("EncryptionUtils", "File decryption successful, result size: ${result.size}")
            return result
        } catch (e: Exception) {
            android.util.Log.e("EncryptionUtils", "Error in decryptFileWithKey", e)
            android.util.Log.e("EncryptionUtils", "Error details: ${e.message}")
            throw e
        }
    }
}