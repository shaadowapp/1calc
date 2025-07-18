package com.shaadow.onecalculator

import android.app.Service
import android.content.Intent
import android.os.IBinder

class FloatingMicService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        // Not a bound service
        return null
    }
} 