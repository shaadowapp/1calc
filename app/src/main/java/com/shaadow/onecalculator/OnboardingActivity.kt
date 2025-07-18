package com.shaadow.onecalculator

import android.content.Intent
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.content.SharedPreferences
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

class OnboardingActivity : AppCompatActivity() {
    companion object {
        private const val PREFS_NAME = "onboarding_prefs"
        private const val KEY_IS_NEW_USER = "is_new_user"
        private const val REQ_PERMISSIONS = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_IS_NEW_USER, true)) {
            startActivity(Intent(this, BasicActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_onboarding)
        // Always check and request permissions if not granted
        val permissionsNeeded = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.RECORD_AUDIO)
        }
        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), REQ_PERMISSIONS)
        }

        // Gradient headline
        val headline = findViewById<TextView>(R.id.onboarding_headline)
        headline.post {
            val width = headline.width.toFloat()
            val textShader = LinearGradient(
                0f, 0f, width, headline.textSize,
                intArrayOf(
                    ContextCompat.getColor(this, R.color.headline_gradient_start),
                    ContextCompat.getColor(this, R.color.headline_gradient_middle),
                    ContextCompat.getColor(this, R.color.headline_gradient_end)
                ),
                null, Shader.TileMode.CLAMP
            )
            headline.paint.shader = textShader
            headline.invalidate()
        }

        // Card 1: Core & Advance
        findViewById<ImageView>(R.id.icon_core).setImageResource(R.drawable.ic_calc_icon)
        findViewById<TextView>(R.id.card_title_core).text = "Core & Advance"
        findViewById<TextView>(R.id.card_desc_core).text = "Calculate from everyday math to pro-level functions with ease."
        findViewById<TextView>(R.id.new_badge_core).visibility = View.GONE

        // Card 2: Camera Solver
        findViewById<ImageView>(R.id.icon_camera).setImageResource(R.drawable.ic_scan)
        findViewById<TextView>(R.id.card_title_camera).text = "Scanner Invoice"
        findViewById<TextView>(R.id.card_desc_camera).text = "Scan QR or Bar codes to make invoice."
        findViewById<TextView>(R.id.new_badge_camera).visibility = View.GONE

        // Card 3: Mathly AI
        findViewById<ImageView>(R.id.icon_ai).setImageResource(R.drawable.ic_mathly_logo)
        findViewById<TextView>(R.id.card_title_ai).text = "Mathly AI"
        findViewById<TextView>(R.id.card_desc_ai).text = "Ask. Chat. Solve — with your AI-powered math buddy."
        findViewById<TextView>(R.id.new_badge_ai).visibility = View.VISIBLE
        // TODO: Add animated glow ring for AI card if desired

        // Button action
        val getStartedBtn = findViewById<Button>(R.id.getStartedBtn)
        getStartedBtn.setOnClickListener {
            val permissionsNeeded = mutableListOf<String>()
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.CAMERA)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.RECORD_AUDIO)
            }
            if (permissionsNeeded.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), REQ_PERMISSIONS)
            } else {
                prefs.edit().putBoolean(KEY_IS_NEW_USER, false).apply()
                startActivity(Intent(this, BasicActivity::class.java))
                finish()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_PERMISSIONS) {
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_IS_NEW_USER, false).apply()
            startActivity(Intent(this, BasicActivity::class.java))
            finish()
        }
    }
} 