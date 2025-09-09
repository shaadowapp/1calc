package com.shaadow.onecalculator

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.google.android.material.button.MaterialButton
import com.shaadow.onecalculator.databinding.ActivityMediaPlayerBinding
import java.io.File

class MediaPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMediaPlayerBinding
    private var player: ExoPlayer? = null
    private var currentFile: File? = null
    private var isTemporaryFile: Boolean = false

    companion object {
        const val EXTRA_FILE_PATH = "file_path"
        const val EXTRA_FILE_NAME = "file_name"
        const val EXTRA_IS_TEMPORARY = "is_temporary"
        const val EXTRA_MIME_TYPE = "mime_type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Prevent screenshots and screen recording for security
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        setupViews()
        loadMedia()
    }

    private fun setupViews() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnShare.setOnClickListener {
            shareMedia()
        }
    }

    private fun loadMedia() {
        val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
        val fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: "Unknown"
        val mimeType = intent.getStringExtra(EXTRA_MIME_TYPE) ?: ""
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

        // Initialize ExoPlayer
        initializePlayer()

        // Create MediaItem from file
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            currentFile!!
        )

        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMimeType(mimeType)
            .build()

        // Set media item and prepare
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.playWhenReady = true
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player

        // Add listener for playback state changes
        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        // Media is ready to play
                        binding.progressBar.visibility = View.GONE
                    }
                    Player.STATE_BUFFERING -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    Player.STATE_ENDED -> {
                        // Playback ended
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                android.util.Log.e("MediaPlayerActivity", "Playback error", error)
                Toast.makeText(this@MediaPlayerActivity, "Error playing media: ${error.message}", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
            }
        })
    }

    private fun shareMedia() {
        try {
            currentFile?.let { file ->
                val uri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    file
                )

                val mimeType = intent.getStringExtra(EXTRA_MIME_TYPE) ?: "*/*"

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
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
            android.util.Log.e("MediaPlayerActivity", "Error sharing media", e)
            Toast.makeText(this, "Error sharing file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null

        // Clean up temporary file if needed
        if (isTemporaryFile && currentFile != null && currentFile!!.exists()) {
            try {
                val fileEncryptionService = com.shaadow.onecalculator.services.FileEncryptionService(this)
                fileEncryptionService.cleanupTempFile(currentFile!!)
            } catch (e: Exception) {
                android.util.Log.w("MediaPlayerActivity", "Error cleaning up temp file", e)
            }
        }
    }

    override fun onBackPressed() {
        if (player?.isPlaying == true) {
            player?.pause()
        }
        super.onBackPressed()
    }
}