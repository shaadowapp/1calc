package com.shaadow.onecalculator

import android.content.Context
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat

/**
 * Comprehensive cache management utility for the calculator app.
 * Handles clearing of various cache types while preserving important data.
 */
class CacheManager(private val context: Context) {
    
    data class CacheClearResult(
        val totalSize: Long,
        val clearedFiles: Int,
        val clearedDirectories: Int,
        val cacheTypes: List<String>
    )
    
    data class DirectoryClearResult(
        val size: Long,
        val files: Int,
        val directories: Int
    )
    
    companion object {
        private const val TAG = "CacheManager"
        
        // Important files that should not be deleted
        private val IMPORTANT_FILES = listOf(
            "preferences.xml", "settings.xml", "config.xml",
            "database.db", "history.db", "user_data.db",
            "todo.db", "calculator_settings.xml", "widget_prefs.xml"
        )
        
        // Important directories that should not be deleted
        private val IMPORTANT_DIRECTORIES = listOf(
            "databases", "shared_prefs", "lib", "assets", "files"
        )
        
        // Cache directory names to look for
        private val CACHE_DIRECTORIES = listOf(
            "cache", "tmp", "temp", "logs", "downloads", "webview",
            "image_cache", "media_cache", "code_cache"
        )
    }
    
    /**
     * Calculate total cache size across all cache locations
     */
    suspend fun calculateTotalCacheSize(): Long = withContext(Dispatchers.IO) {
        var totalSize = 0L
        
        try {
            // App cache directory
            totalSize += calculateDirectorySize(context.cacheDir)
            
            // External cache directory
            context.externalCacheDir?.let { totalSize += calculateDirectorySize(it) }
            
            // Shared preferences directory
            val sharedPrefsDir = File(context.filesDir.parent, "shared_prefs")
            if (sharedPrefsDir.exists()) {
                totalSize += calculateDirectorySize(sharedPrefsDir)
            }
            
            // Database directory
            val databaseDir = File(context.filesDir.parent, "databases")
            if (databaseDir.exists()) {
                totalSize += calculateDirectorySize(databaseDir)
            }
            
            // Files directory
            totalSize += calculateDirectorySize(context.filesDir)
            
            // Additional cache directories
            val appPrivateDir = context.filesDir.parent
            if (appPrivateDir != null) {
                for (dirName in CACHE_DIRECTORIES) {
                    val dir = File(appPrivateDir, dirName)
                    if (dir.exists()) {
                        totalSize += calculateDirectorySize(dir)
                    }
                }
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        totalSize
    }
    
    /**
     * Clear all cache data with detailed reporting
     */
    suspend fun clearAllCache(): CacheClearResult = withContext(Dispatchers.IO) {
        var totalSize = 0L
        var totalFiles = 0
        var totalDirectories = 0
        val clearedCacheTypes = mutableListOf<String>()
        
        try {
            // Clear app cache directory
            val cacheResult = clearDirectory(context.cacheDir)
            if (cacheResult.size > 0) {
                totalSize += cacheResult.size
                totalFiles += cacheResult.files
                totalDirectories += cacheResult.directories
                clearedCacheTypes.add("App Cache")
            }
            
            // Clear external cache directory
            context.externalCacheDir?.let { externalDir ->
                val externalResult = clearDirectory(externalDir)
                if (externalResult.size > 0) {
                    totalSize += externalResult.size
                    totalFiles += externalResult.files
                    totalDirectories += externalResult.directories
                    clearedCacheTypes.add("External Cache")
                }
            }
            
            // Clear shared preferences cache
            val sharedPrefsResult = clearSharedPrefsCache()
            if (sharedPrefsResult.size > 0) {
                totalSize += sharedPrefsResult.size
                totalFiles += sharedPrefsResult.files
                clearedCacheTypes.add("Shared Preferences Cache")
            }
            
            // Clear database cache
            val databaseResult = clearDatabaseCache()
            if (databaseResult.size > 0) {
                totalSize += databaseResult.size
                totalFiles += databaseResult.files
                clearedCacheTypes.add("Database Cache")
            }
            
            // Clear files directory cache
            val filesResult = clearFilesDirectoryCache()
            if (filesResult.size > 0) {
                totalSize += filesResult.size
                totalFiles += filesResult.files
                totalDirectories += filesResult.directories
                clearedCacheTypes.add("Files Cache")
            }
            
            // Clear WebView cache
            val webViewResult = clearWebViewCache()
            if (webViewResult.size > 0) {
                totalSize += webViewResult.size
                totalFiles += webViewResult.files
                totalDirectories += webViewResult.directories
                clearedCacheTypes.add("WebView Cache")
            }
            
            // Clear image cache
            val imageResult = clearImageCache()
            if (imageResult.size > 0) {
                totalSize += imageResult.size
                totalFiles += imageResult.files
                totalDirectories += imageResult.directories
                clearedCacheTypes.add("Image Cache")
            }
            
            // Clear media cache
            val mediaResult = clearMediaCache()
            if (mediaResult.size > 0) {
                totalSize += mediaResult.size
                totalFiles += mediaResult.files
                totalDirectories += mediaResult.directories
                clearedCacheTypes.add("Media Cache")
            }
            
            // Clear code cache
            val codeResult = clearCodeCache()
            if (codeResult.size > 0) {
                totalSize += codeResult.size
                totalFiles += codeResult.files
                totalDirectories += codeResult.directories
                clearedCacheTypes.add("Code Cache")
            }
            
            // Clear additional cache directories
            val additionalResult = clearAdditionalCacheDirectories()
            if (additionalResult.size > 0) {
                totalSize += additionalResult.size
                totalFiles += additionalResult.files
                totalDirectories += additionalResult.directories
                clearedCacheTypes.add("Additional Cache")
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        CacheClearResult(totalSize, totalFiles, totalDirectories, clearedCacheTypes)
    }
    
    /**
     * Clear specific cache type
     */
    suspend fun clearSpecificCache(cacheType: String): DirectoryClearResult = withContext(Dispatchers.IO) {
        when (cacheType.lowercase()) {
            "app" -> clearDirectory(context.cacheDir)
            "external" -> context.externalCacheDir?.let { clearDirectory(it) } ?: DirectoryClearResult(0, 0, 0)
            "shared_prefs" -> clearSharedPrefsCache()
            "database" -> clearDatabaseCache()
            "files" -> clearFilesDirectoryCache()
            "webview" -> clearWebViewCache()
            "image" -> clearImageCache()
            "media" -> clearMediaCache()
            "code" -> clearCodeCache()
            else -> DirectoryClearResult(0, 0, 0)
        }
    }
    
    private fun calculateDirectorySize(directory: File): Long {
        var size = 0L
        try {
            if (directory.exists() && directory.isDirectory) {
                val files = directory.listFiles()
                if (files != null) {
                    for (file in files) {
                        if (file.isFile) {
                            size += file.length()
                        } else if (file.isDirectory) {
                            size += calculateDirectorySize(file)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return size
    }
    
    private fun clearDirectory(directory: File): DirectoryClearResult {
        var clearedSize = 0L
        var clearedFiles = 0
        var clearedDirectories = 0
        
        try {
            if (directory.exists() && directory.isDirectory) {
                val files = directory.listFiles()
                if (files != null) {
                    for (file in files) {
                        if (file.isFile) {
                            clearedSize += file.length()
                            if (file.delete()) {
                                clearedFiles++
                            }
                        } else if (file.isDirectory) {
                            val result = clearDirectory(file)
                            clearedSize += result.size
                            clearedFiles += result.files
                            clearedDirectories += result.directories
                            if (file.delete()) {
                                clearedDirectories++
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return DirectoryClearResult(clearedSize, clearedFiles, clearedDirectories)
    }
    
    private fun clearSharedPrefsCache(): DirectoryClearResult {
        var clearedSize = 0L
        var clearedFiles = 0
        
        try {
            val sharedPrefsDir = File(context.filesDir.parent, "shared_prefs")
            if (sharedPrefsDir.exists() && sharedPrefsDir.isDirectory) {
                val files = sharedPrefsDir.listFiles()
                if (files != null) {
                    for (file in files) {
                        if (file.isFile && !isImportantFile(file.name)) {
                            clearedSize += file.length()
                            if (file.delete()) {
                                clearedFiles++
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return DirectoryClearResult(clearedSize, clearedFiles, 0)
    }
    
    private fun clearDatabaseCache(): DirectoryClearResult {
        var clearedSize = 0L
        var clearedFiles = 0
        
        try {
            val databaseDir = File(context.filesDir.parent, "databases")
            if (databaseDir.exists() && databaseDir.isDirectory) {
                val files = databaseDir.listFiles()
                if (files != null) {
                    for (file in files) {
                        if (file.isFile && isDatabaseCacheFile(file.name)) {
                            clearedSize += file.length()
                            if (file.delete()) {
                                clearedFiles++
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return DirectoryClearResult(clearedSize, clearedFiles, 0)
    }
    
    private fun clearFilesDirectoryCache(): DirectoryClearResult {
        var clearedSize = 0L
        var clearedFiles = 0
        var clearedDirectories = 0
        
        try {
            if (context.filesDir.exists() && context.filesDir.isDirectory) {
                val files = context.filesDir.listFiles()
                if (files != null) {
                    for (file in files) {
                        if (file.isFile) {
                            if (shouldDeleteFile(file.name)) {
                                clearedSize += file.length()
                                if (file.delete()) {
                                    clearedFiles++
                                }
                            }
                        } else if (file.isDirectory) {
                            if (shouldDeleteDirectory(file.name)) {
                                val result = clearDirectory(file)
                                clearedSize += result.size
                                clearedFiles += result.files
                                clearedDirectories += result.directories
                                if (file.delete()) {
                                    clearedDirectories++
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return DirectoryClearResult(clearedSize, clearedFiles, clearedDirectories)
    }
    
    private fun clearWebViewCache(): DirectoryClearResult {
        var totalSize = 0L
        var totalFiles = 0
        var totalDirectories = 0
        
        try {
            // Clear WebView cache directory
            val webViewCacheDir = File(context.cacheDir, "webview")
            if (webViewCacheDir.exists()) {
                val result = clearDirectory(webViewCacheDir)
                totalSize += result.size
                totalFiles += result.files
                totalDirectories += result.directories
            }
            
            // Clear WebView database directory
            val webViewDbDir = File(context.filesDir.parent, "webview")
            if (webViewDbDir.exists()) {
                val result = clearDirectory(webViewDbDir)
                totalSize += result.size
                totalFiles += result.files
                totalDirectories += result.directories
            }
            
            // Clear WebView cookies and data
            try {
                WebView(context).clearCache(true)
                WebView(context).clearHistory()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return DirectoryClearResult(totalSize, totalFiles, totalDirectories)
    }
    
    private fun clearImageCache(): DirectoryClearResult {
        var totalSize = 0L
        var totalFiles = 0
        var totalDirectories = 0
        
        try {
            // Clear image cache directory
            val imageCacheDir = File(context.cacheDir, "image_cache")
            if (imageCacheDir.exists()) {
                val result = clearDirectory(imageCacheDir)
                totalSize += result.size
                totalFiles += result.files
                totalDirectories += result.directories
            }
            
            // Clear Glide cache
            val glideCacheDir = File(context.cacheDir, "image_manager_disk_cache")
            if (glideCacheDir.exists()) {
                val result = clearDirectory(glideCacheDir)
                totalSize += result.size
                totalFiles += result.files
                totalDirectories += result.directories
            }
            
            // Clear Glide memory cache
            val glideMemoryCacheDir = File(context.cacheDir, "image_manager_memory_cache")
            if (glideMemoryCacheDir.exists()) {
                val result = clearDirectory(glideMemoryCacheDir)
                totalSize += result.size
                totalFiles += result.files
                totalDirectories += result.directories
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return DirectoryClearResult(totalSize, totalFiles, totalDirectories)
    }
    
    private fun clearMediaCache(): DirectoryClearResult {
        var totalSize = 0L
        var totalFiles = 0
        var totalDirectories = 0
        
        try {
            // Clear media cache directory
            val mediaCacheDir = File(context.cacheDir, "media_cache")
            if (mediaCacheDir.exists()) {
                val result = clearDirectory(mediaCacheDir)
                totalSize += result.size
                totalFiles += result.files
                totalDirectories += result.directories
            }
            
            // Clear temporary media files
            val tempDir = File(context.filesDir, "temp")
            if (tempDir.exists()) {
                val result = clearDirectory(tempDir)
                totalSize += result.size
                totalFiles += result.files
                totalDirectories += result.directories
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return DirectoryClearResult(totalSize, totalFiles, totalDirectories)
    }
    
    private fun clearCodeCache(): DirectoryClearResult {
        var totalSize = 0L
        var totalFiles = 0
        var totalDirectories = 0
        
        try {
            // Clear code cache directory
            val codeCacheDir = File(context.cacheDir, "code_cache")
            if (codeCacheDir.exists()) {
                val result = clearDirectory(codeCacheDir)
                totalSize += result.size
                totalFiles += result.files
                totalDirectories += result.directories
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return DirectoryClearResult(totalSize, totalFiles, totalDirectories)
    }
    
    private fun clearAdditionalCacheDirectories(): DirectoryClearResult {
        var totalSize = 0L
        var totalFiles = 0
        var totalDirectories = 0
        
        try {
            val appPrivateDir = context.filesDir.parent
            if (appPrivateDir != null) {
                for (dirName in CACHE_DIRECTORIES) {
                    val dir = File(appPrivateDir, dirName)
                    if (dir.exists()) {
                        val result = clearDirectory(dir)
                        totalSize += result.size
                        totalFiles += result.files
                        totalDirectories += result.directories
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return DirectoryClearResult(totalSize, totalFiles, totalDirectories)
    }
    
    private fun isImportantFile(fileName: String): Boolean {
        return IMPORTANT_FILES.any { fileName.contains(it, ignoreCase = true) }
    }
    
    private fun shouldDeleteFile(fileName: String): Boolean {
        return !isImportantFile(fileName)
    }
    
    private fun shouldDeleteDirectory(dirName: String): Boolean {
        return !IMPORTANT_DIRECTORIES.any { dirName.contains(it, ignoreCase = true) }
    }
    
    private fun isDatabaseCacheFile(fileName: String): Boolean {
        return fileName.endsWith("-shm") || fileName.endsWith("-wal") || fileName.endsWith("-journal")
    }
    
    /**
     * Format file size in human-readable format
     */
    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        val decimalFormat = DecimalFormat("#,##0.#")
        return decimalFormat.format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
    }
    
    /**
     * Get cache statistics
     */
    suspend fun getCacheStatistics(): Map<String, Long> = withContext(Dispatchers.IO) {
        val stats = mutableMapOf<String, Long>()
        
        try {
            stats["app_cache"] = calculateDirectorySize(context.cacheDir)
            stats["external_cache"] = context.externalCacheDir?.let { calculateDirectorySize(it) } ?: 0L
            stats["shared_prefs"] = calculateDirectorySize(File(context.filesDir.parent, "shared_prefs"))
            stats["databases"] = calculateDirectorySize(File(context.filesDir.parent, "databases"))
            stats["files"] = calculateDirectorySize(context.filesDir)
            
            // Additional cache directories
            val appPrivateDir = context.filesDir.parent
            if (appPrivateDir != null) {
                for (dirName in CACHE_DIRECTORIES) {
                    val dir = File(appPrivateDir, dirName)
                    if (dir.exists()) {
                        stats[dirName] = calculateDirectorySize(dir)
                    }
                }
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        stats
    }
} 