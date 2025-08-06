package com.shaadow.onecalculator

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import kotlin.math.log10
import kotlin.math.pow

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var btnBack: MaterialButton
    private lateinit var fabShare: FloatingActionButton
    private lateinit var titleTextView: TextView
    private lateinit var currentFile: File

    companion object {
        const val EXTRA_FILE_PATH = "file_path"
        const val EXTRA_FILE_NAME = "file_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        setupViews()
        loadImage()
    }

    private fun setupViews() {
        imageView = findViewById(R.id.image_view)
        btnBack = findViewById(R.id.btn_back)
        fabShare = findViewById(R.id.fab_share)
        titleTextView = findViewById(R.id.title_text)

        btnBack.setOnClickListener {
            finish()
        }

        fabShare.setOnClickListener {
            shareImage()
        }
    }

    private fun loadImage() {
        val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
        val fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: "Unknown"

        if (filePath == null) {
            Toast.makeText(this, "Error: File path not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentFile = File(filePath)
        
        if (!currentFile.exists() || !currentFile.isFile) {
            Toast.makeText(this, "Error: File not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        try {
            // Load image based on file type
            val mimeType = getMimeTypeFromFile(currentFile)
            
            when {
                mimeType?.startsWith("image/") == true -> {
                    loadImageFile()
                }
                mimeType?.startsWith("video/") == true -> {
                    // For videos, show a placeholder and open with external app
                    imageView.setImageResource(R.drawable.ic_gallery)
                    Toast.makeText(this, "Tap share to open video with external app", Toast.LENGTH_LONG).show()
                }
                else -> {
                    // For other files, show a generic file icon
                    imageView.setImageResource(R.drawable.ic_file)
                    Toast.makeText(this, "File type: ${mimeType ?: "Unknown"}", Toast.LENGTH_SHORT).show()
                }
            }

            // Set title in the toolbar with file info
            val fileSize = formatFileSize(currentFile.length())
            val fileExtension = currentFile.extension.uppercase()
            val fileInfo = if (fileExtension.isNotEmpty()) {
                "$fileExtension • $fileSize"
            } else {
                fileSize
            }

            // Create styled text with different colors
            val fullText = "$fileName\n$fileInfo"
            val spannable = SpannableString(fullText)

            // Make file info text slightly muted
            val mutedColor = ContextCompat.getColor(this, R.color.text_muted_b0aec0)
            val fileInfoStart = fileName.length + 1
            spannable.setSpan(
                ForegroundColorSpan(mutedColor),
                fileInfoStart,
                fullText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            titleTextView.text = spannable

        } catch (e: Exception) {
            android.util.Log.e("ImageViewerActivity", "Error loading file: ${currentFile.name}", e)
            Toast.makeText(this, "Error loading file: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadImageFile() {
        try {
            val bitmap = BitmapFactory.decodeFile(currentFile.absolutePath)
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
                imageView.scaleType = ImageView.ScaleType.FIT_CENTER
            } else {
                Toast.makeText(this, "Error: Could not decode image", Toast.LENGTH_SHORT).show()
                imageView.setImageResource(R.drawable.ic_gallery)
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageViewerActivity", "Error decoding image: ${currentFile.name}", e)
            Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
            imageView.setImageResource(R.drawable.ic_gallery)
        }
    }

    private fun shareImage() {
        try {
            val authority = "${applicationContext.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(this, authority, currentFile)
            val mimeType = getMimeTypeFromFile(currentFile)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Share '${currentFile.name}' via")
            if (chooser.resolveActivity(packageManager) != null) {
                startActivity(chooser)
            } else {
                // Fallback: try to open with external app
                openWithExternalApp()
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageViewerActivity", "Error sharing file: ${currentFile.name}", e)
            Toast.makeText(this, "Error sharing file: ${e.message}", Toast.LENGTH_SHORT).show()
            
            // Fallback: try to open with external app
            openWithExternalApp()
        }
    }

    private fun openWithExternalApp() {
        try {
            val authority = "${applicationContext.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(this, authority, currentFile)
            val mimeType = getMimeTypeFromFile(currentFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "No app found to open this file type", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageViewerActivity", "Error opening with external app: ${currentFile.name}", e)
            Toast.makeText(this, "Error opening file with external app", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getMimeTypeFromFile(file: File): String? {
        if (file.isDirectory) {
            return null
        }
        val extension = file.extension
        if (extension.isEmpty()) {
            return "application/octet-stream"
        }
        val lowerExt = extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(lowerExt) ?: "application/octet-stream"
    }

    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
        return String.format("%.1f %s", size / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
    }
}
