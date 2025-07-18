package com.shaadow.onecalculator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.net.Uri
import android.view.View

class AboutUsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_us)

        findViewById<View>(R.id.back_button)?.setOnClickListener { finish() }

        // CTA: Play Store
        findViewById<View>(R.id.cta_playstore)?.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.shaadow.onecalculator"))
            startActivity(intent)
        }
        // CTA: Website
        findViewById<View>(R.id.cta_website)?.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://1calculator.app"))
            startActivity(intent)
        }
        // CTA: Email
        findViewById<View>(R.id.cta_email)?.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@1calculator.app"))
            startActivity(intent)
        }
    }
} 