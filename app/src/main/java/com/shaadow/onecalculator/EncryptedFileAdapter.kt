package com.shaadow.onecalculator

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.log10
import kotlin.math.pow

class EncryptedFileAdapter(
    private val onFileClick: (EncryptedFileEntity) -> Unit
) : ListAdapter<EncryptedFileEntity, EncryptedFileAdapter.FileViewHolder>(FileDiffCallback()) {

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

        fun bind(file: EncryptedFileEntity) {
            fileName.text = file.originalFileName
            
            // Set appropriate icon based on file type
            when {
                file.isImage -> fileIcon.setImageResource(R.drawable.ic_gallery)
                file.isVideo -> fileIcon.setImageResource(R.drawable.ic_gallery) // Could use video icon
                file.mimeType.startsWith("audio/") -> fileIcon.setImageResource(R.drawable.ic_gallery)
                file.mimeType.contains("pdf") -> fileIcon.setImageResource(R.drawable.ic_file)
                file.mimeType.contains("document") -> fileIcon.setImageResource(R.drawable.ic_file)
                else -> fileIcon.setImageResource(R.drawable.ic_file)
            }
            
            // Format file info
            val fileSize = formatFileSize(file.fileSize)
            val fileType = getFileTypeFromMime(file.mimeType)
            fileInfo.text = "$fileType • $fileSize"
            
            // Format date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            fileDate.text = dateFormat.format(Date(file.dateAdded))
            
            // Set click listener
            itemView.setOnClickListener {
                onFileClick(file)
            }
            
            // Add long click for future context menu
            itemView.setOnLongClickListener {
                // TODO: Show context menu for delete, rename, etc.
                true
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
                mimeType.contains("pdf") -> "PDF"
                mimeType.contains("document") -> "Document"
                mimeType.contains("text") -> "Text"
                mimeType.contains("zip") || mimeType.contains("archive") -> "Archive"
                else -> "File"
            }
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
