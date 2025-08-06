package com.shaadow.onecalculator

import android.app.Application
import com.shaadow.onecalculator.utils.ExternalStorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CalculatorApplication : Application() {
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize external storage for hidden gallery
        initializeExternalStorage()
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
}
