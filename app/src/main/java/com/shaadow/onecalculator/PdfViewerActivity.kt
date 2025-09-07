package com.shaadow.onecalculator

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.pdf.viewer.fragment.PdfViewerFragment
import com.google.android.material.button.MaterialButton
import com.shaadow.onecalculator.databinding.ActivityPdfViewerBinding
import java.io.File

class PdfViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfViewerBinding
    private var currentFile: File? = null
    private var isTemporaryFile: Boolean = false

    companion object {
        const val EXTRA_FILE_PATH = "file_path"
        const val EXTRA_FILE_NAME = "file_name"
        const val EXTRA_IS_TEMPORARY = "is_temporary"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        loadPdf()
    }

    private fun setupViews() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnShare.setOnClickListener {
            sharePdf()
        }
    }

    private fun loadPdf() {
        val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
        val fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: "Unknown"
        isTemporaryFile = intent.getBooleanExtra(EXTRA_IS_TEMPORARY, false)

        if (filePath == null) {
            Toast.makeText(this, "Error: File path not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentFile = File(filePath)

        if (!currentFile!!.exists() || !currentFile!!.isFile) {
            Toast.makeText(this, "Error: File not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Set title
        binding.titleText.text = fileName

        try {
            // Create PDF Viewer Fragment using the correct AndroidX API
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                currentFile!!
            )

            android.util.Log.d("PdfViewerActivity", "Loading PDF from URI: $uri")

            // Use the correct AndroidX PDF Viewer Fragment API
            val args = Bundle().apply {
                putParcelable("uri", uri)
            }

            val pdfFragment = PdfViewerFragment().apply {
                arguments = args
            }

            // Add fragment to container
            supportFragmentManager.beginTransaction()
                .replace(R.id.pdf_container, pdfFragment)
                .commit()

            android.util.Log.d("PdfViewerActivity", "PDF fragment added to container")
            binding.progressBar.visibility = View.GONE

        } catch (e: Exception) {
            android.util.Log.e("PdfViewerActivity", "Error loading PDF with AndroidX viewer", e)
            android.util.Log.e("PdfViewerActivity", "Error details: ${e.message}")

            // Try fallback to external PDF viewer
            try {
                val uri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    currentFile!!
                )

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    // Add FLAG_ACTIVITY_NEW_TASK for better compatibility
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                if (intent.resolveActivity(packageManager) != null) {
                    android.util.Log.d("PdfViewerActivity", "Opening PDF with external viewer")
                    startActivity(intent)
                    finish() // Close this activity since we're opening external viewer
                } else {
                    android.util.Log.w("PdfViewerActivity", "No external PDF viewer found")
                    Toast.makeText(this, "No PDF viewer found on device", Toast.LENGTH_LONG).show()
                    finish()
                }
            } catch (fallbackException: Exception) {
                android.util.Log.e("PdfViewerActivity", "Fallback PDF viewer also failed", fallbackException)
                Toast.makeText(this, "Error opening PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }

            binding.progressBar.visibility = View.GONE
        }
    }

    private fun sharePdf() {
        try {
            currentFile?.let { file ->
                val uri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(shareIntent, "Share '${file.name}' via")
                if (chooser.resolveActivity(packageManager) != null) {
                    startActivity(chooser)
                } else {
                    Toast.makeText(this, "No app found to share this file", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PdfViewerActivity", "Error sharing PDF", e)
            Toast.makeText(this, "Error sharing file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clean up temporary file if needed
        if (isTemporaryFile && currentFile != null && currentFile!!.exists()) {
            try {
                val fileEncryptionService = com.shaadow.onecalculator.services.FileEncryptionService(this)
                fileEncryptionService.cleanupTempFile(currentFile!!)
            } catch (e: Exception) {
                android.util.Log.w("PdfViewerActivity", "Error cleaning up temp file", e)
            }
        }
    }
}