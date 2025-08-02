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
            .joinToString("")
    }
}