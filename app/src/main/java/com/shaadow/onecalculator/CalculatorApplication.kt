package com.shaadow.onecalculator

import android.app.Application
import com.shaadow.onecalculator.utils.ExternalStorageManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CalculatorApplication : Application() {
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase Crashlytics
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

        // Initialize Firebase Analytics
        FirebaseAnalytics.getInstance(this)

        // Initialize external storage for hidden gallery
        initializeExternalStorage()

        // Clean up any leftover temporary files from previous sessions
        cleanupTempFiles()
    }
    
    private fun initializeExternalStorage() {
        applicationScope.launch {
            try {
                val success = ExternalStorageManager.initializeHiddenDirectory(this@CalculatorApplication)
                if (success) {
                    android.util.Log.d("CalculatorApplication", "Hidden directory initialized successfully")
                } else {
                    android.util.Log.w("CalculatorApplication", "Failed to initialize hidden directory")
                }
            } catch (e: Exception) {
                android.util.Log.e("CalculatorApplication", "Error initializing external storage", e)
            }
        }
    }

    private fun cleanupTempFiles() {
        applicationScope.launch {
            try {
                val fileEncryptionService = com.shaadow.onecalculator.services.FileEncryptionService(this@CalculatorApplication)
                fileEncryptionService.cleanupTempFiles()
                android.util.Log.d("CalculatorApplication", "Cleaned up temporary files from previous sessions")
            } catch (e: Exception) {
                android.util.Log.e("CalculatorApplication", "Error cleaning up temporary files", e)
            }
        }
    }
}
