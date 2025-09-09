package com.shaadow.onecalculator

import android.app.Application

class CalculatorApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Application initialized - using button-based bug reporting
        android.util.Log.d("CalculatorApplication", "Application started")
    }
}
