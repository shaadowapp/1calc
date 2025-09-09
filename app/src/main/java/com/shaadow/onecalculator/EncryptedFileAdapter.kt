package com.shaadow.onecalculator

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.shaadow.onecalculator.services.FileEncryptionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.log10
import kotlin.math.pow

class EncryptedFileAdapter(
    private val context: Context,
    private val fileEncryptionService: FileEncryptionService,
    private val masterPassword: String,
    private val salt: String,
    private val onFileClick: (EncryptedFileEntity) -> Unit,
    private val onFileLongClick: (EncryptedFileEntity, View) -> Unit = { _, _ -> },
    private val onFileDelete: (EncryptedFileEntity) -> Unit = { },
    private val onFileRename: (EncryptedFileEntity) -> Unit = { },
    private val onFileShare: (EncryptedFileEntity) -> Unit = { }
) : ListAdapter<EncryptedFileEntity, EncryptedFileAdapter.FileViewHolder>(FileDiffCallback()) {

    private var isSelectionMode = false
    private var selectedFiles = mutableSetOf<Long>()

    fun setSelectionMode(enabled: Boolean) {
        isSelectionMode = enabled
        if (!enabled) {
            selectedFiles.clear()
        }
        notifyDataSetChanged()
    }

    fun setSelectedFiles(selected: Set<Long>) {
        selectedFiles.clear()
        selectedFiles.addAll(selected)
        notifyDataSetChanged()
    }
    
    fun getSelectedCount(): Int {
        return selectedFiles.size
    }
    
    fun selectAll() {
        selectedFiles.clear()
        currentList.forEach { file ->
            selectedFiles.add(file.id)
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_encrypted_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val fileIcon: ImageView = itemView.findViewById(R.id.file_icon)
        private val fileName: TextView = itemView.findViewById(R.id.file_name)
        private val fileInfo: TextView = itemView.findViewById(R.id.file_info)
        private val fileDate: TextView = itemView.findViewById(R.id.file_date)
        private val menuButton: ImageView = itemView.findViewById(R.id.file_menu_button)
        private val selectionCheckbox: androidx.appcompat.widget.AppCompatCheckBox? = itemView.findViewById(R.id.selection_checkbox)

        fun bind(file: EncryptedFileEntity) {
            fileName.text = file.originalFileName
            
            // Load thumbnail or set appropriate icon
            loadThumbnail(file)
            
            // Format file info
            val fileSize = formatFileSize(file.fileSize)
            val fileType = getFileTypeFromMime(file.mimeType)
            fileInfo.text = "$fileType • $fileSize"
            
            // Format date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            fileDate.text = dateFormat.format(Date(file.dateAdded))
            
            // Handle selection mode
            if (isSelectionMode) {
                selectionCheckbox?.visibility = View.VISIBLE
                selectionCheckbox?.isChecked = selectedFiles.contains(file.id)
                menuButton.visibility = View.GONE
                
                // Update background for selected items
                itemView.isSelected = selectedFiles.contains(file.id)
            } else {
                selectionCheckbox?.visibility = View.GONE
                menuButton.visibility = View.VISIBLE
                itemView.isSelected = false
            }
            
            // Set click listener
            itemView.setOnClickListener {
                onFileClick(file)
            }
            
            // Set long click listener for context menu
            itemView.setOnLongClickListener {
                onFileLongClick(file, itemView)
                if (!isSelectionMode) {
                    showContextMenu(file, itemView)
                }
                true
            }
            
            // Set menu button click listener
            menuButton.setOnClickListener {
                showContextMenu(file, it)
            }
            
            // Set checkbox click listener
            selectionCheckbox?.setOnClickListener {
                onFileClick(file)
            }
        }
        
        private fun loadThumbnail(file: EncryptedFileEntity) {
            when {
                file.isImage -> {
                    // Load image thumbnail
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val thumbnail = generateImageThumbnail(file)
                            withContext(Dispatchers.Main) {
                                if (thumbnail != null) {
                                    fileIcon.setImageBitmap(thumbnail)
                                    fileIcon.scaleType = ImageView.ScaleType.CENTER_CROP
                                } else {
                                    fileIcon.setImageResource(R.drawable.ic_image)
                                    fileIcon.scaleType = ImageView.ScaleType.CENTER_INSIDE
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                fileIcon.setImageResource(R.drawable.ic_image)
                                fileIcon.scaleType = ImageView.ScaleType.CENTER_INSIDE
                            }
                        }
                    }
                }
                file.isVideo -> {
                    // Load video thumbnail
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val thumbnail = generateVideoThumbnail(file)
                            withContext(Dispatchers.Main) {
                                if (thumbnail != null) {
                                    fileIcon.setImageBitmap(thumbnail)
                                    fileIcon.scaleType = ImageView.ScaleType.CENTER_CROP
                                } else {
                                    fileIcon.setImageResource(R.drawable.ic_video)
                                    fileIcon.scaleType = ImageView.ScaleType.CENTER_INSIDE
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                fileIcon.setImageResource(R.drawable.ic_video)
                                fileIcon.scaleType = ImageView.ScaleType.CENTER_INSIDE
                            }
                        }
                    }
                }
                file.mimeType.startsWith("audio/") -> {
                    fileIcon.setImageResource(R.drawable.ic_music_note)
                    fileIcon.scaleType = ImageView.ScaleType.CENTER_INSIDE
                }
                else -> {
                    fileIcon.setImageResource(R.drawable.ic_file)
                    fileIcon.scaleType = ImageView.ScaleType.CENTER_INSIDE
                }
            }
        }
        
        private suspend fun generateImageThumbnail(file: EncryptedFileEntity): Bitmap? {
            return try {
                android.util.Log.d("EncryptedFileAdapter", "Generating thumbnail for: ${file.originalFileName}")
                val tempFile = fileEncryptionService.decryptFileForViewing(file, masterPassword, salt)
                if (tempFile != null && tempFile.exists()) {
                    android.util.Log.d("EncryptedFileAdapter", "Temp file created: ${tempFile.absolutePath}, size: ${tempFile.length()}")

                    // Use BitmapFactory for thumbnail generation
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeFile(tempFile.absolutePath, options)

                    if (options.outWidth <= 0 || options.outHeight <= 0) {
                        android.util.Log.w("EncryptedFileAdapter", "Invalid image dimensions: ${options.outWidth}x${options.outHeight}")
                        tempFile.delete()
                        return null
                    }

                    // Calculate sample size for high-quality thumbnail
                    val targetSize = 300 // Higher resolution for better quality
                    options.inSampleSize = calculateInSampleSize(options, targetSize, targetSize)
                    options.inJustDecodeBounds = false
                    options.inPreferredConfig = Bitmap.Config.RGB_565 // Memory efficient

                    val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath, options)

                    // Clean up temp file immediately
                    tempFile.delete()

                    // Create a square cropped thumbnail for consistent display
                    bitmap?.let { createSquareThumbnail(it) }
                } else {
                    android.util.Log.w("EncryptedFileAdapter", "Failed to decrypt file for thumbnail: ${file.originalFileName}")
                    null
                }
            } catch (e: Exception) {
                android.util.Log.e("EncryptedFileAdapter", "Error generating image thumbnail for ${file.originalFileName}", e)
                null
            }
        }
        
        private fun createSquareThumbnail(bitmap: Bitmap): Bitmap {
            val size = minOf(bitmap.width, bitmap.height)
            val x = (bitmap.width - size) / 2
            val y = (bitmap.height - size) / 2
            
            return try {
                val squareBitmap = Bitmap.createBitmap(bitmap, x, y, size, size)
                if (squareBitmap != bitmap) {
                    bitmap.recycle() // Free original bitmap memory
                }
                squareBitmap
            } catch (e: Exception) {
                bitmap // Return original if cropping fails
            }
        }
        
        private suspend fun generateVideoThumbnail(file: EncryptedFileEntity): Bitmap? {
            return try {
                android.util.Log.d("EncryptedFileAdapter", "Generating video thumbnail for: ${file.originalFileName}")
                val tempFile = fileEncryptionService.decryptFileForViewing(file, masterPassword, salt)
                if (tempFile != null && tempFile.exists()) {
                    android.util.Log.d("EncryptedFileAdapter", "Video temp file created: ${tempFile.absolutePath}, size: ${tempFile.length()}")

                    var thumbnail: Bitmap? = null

                    // Try modern approach first (API 29+)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        try {
                            val size = android.util.Size(512, 512) // Larger size for better quality
                            thumbnail = ThumbnailUtils.createVideoThumbnail(tempFile, size, null)
                            android.util.Log.d("EncryptedFileAdapter", "Modern thumbnail creation successful")
                        } catch (e: Exception) {
                            android.util.Log.w("EncryptedFileAdapter", "Modern thumbnail creation failed, using fallback", e)
                        }
                    }

                    // If modern approach failed or not available, use deprecated method
                    if (thumbnail == null) {
                        try {
                            @Suppress("DEPRECATION")
                            thumbnail = ThumbnailUtils.createVideoThumbnail(
                                tempFile.absolutePath,
                                MediaStore.Images.Thumbnails.MINI_KIND
                            )
                            android.util.Log.d("EncryptedFileAdapter", "Fallback thumbnail creation successful")
                        } catch (e: Exception) {
                            android.util.Log.w("EncryptedFileAdapter", "Fallback thumbnail creation also failed", e)
                        }
                    }

                    // Clean up temp file immediately
                    tempFile.delete()

                    // Create square thumbnail for consistent display
                    thumbnail?.let {
                        android.util.Log.d("EncryptedFileAdapter", "Creating square thumbnail")
                        createSquareThumbnail(it)
                    }
                } else {
                    android.util.Log.w("EncryptedFileAdapter", "Failed to decrypt video file for thumbnail: ${file.originalFileName}")
                    null
                }
            } catch (e: Exception) {
                android.util.Log.e("EncryptedFileAdapter", "Error generating video thumbnail for ${file.originalFileName}", e)
                null
            }
        }
        
        private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
            val height = options.outHeight
            val width = options.outWidth
            var inSampleSize = 1

            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2

                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize
        }
        
        private fun showContextMenu(file: EncryptedFileEntity, anchorView: View) {
            val popup = PopupMenu(context, anchorView)
            popup.menuInflater.inflate(R.menu.menu_file_options, popup.menu)
            
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_open -> {
                        onFileClick(file)
                        true
                    }
                    R.id.action_rename -> {
                        onFileRename(file)
                        true
                    }
                    R.id.action_share -> {
                        onFileShare(file)
                        true
                    }
                    R.id.action_delete -> {
                        onFileDelete(file)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }
    
    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
        return String.format("%.1f %s", size / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
    }
    
    private fun getFileTypeFromMime(mimeType: String): String {
        return when {
            mimeType.startsWith("image/") -> "Image"
            mimeType.startsWith("video/") -> "Video"
            mimeType.startsWith("audio/") -> "Audio"
            mimeType.contains("text") -> "Text"
            mimeType.contains("zip") || mimeType.contains("archive") -> "Archive"
            else -> "File"
        }
    }

    class FileDiffCallback : DiffUtil.ItemCallback<EncryptedFileEntity>() {
        override fun areItemsTheSame(oldItem: EncryptedFileEntity, newItem: EncryptedFileEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: EncryptedFileEntity, newItem: EncryptedFileEntity): Boolean {
            return oldItem == newItem
        }
    }
}
