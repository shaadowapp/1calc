package com.shaadow.onecalculator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.net.Uri
import android.view.View

class PrivacyPermissionsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_permissions)

        findViewById<View>(R.id.back_button)?.setOnClickListener { finish() }
        findViewById<View>(R.id.cta_website)?.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://1calculator.app/privacy"))
            startActivity(intent)
        }
    }
} 