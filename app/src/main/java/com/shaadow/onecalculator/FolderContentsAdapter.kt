package com.shaadow.onecalculator

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.io.File // Import File
import java.text.SimpleDateFormat // For last modified date, optional
import java.util.Date // For last modified date, optional
import java.util.Locale // For last modified date, optional

class FolderContentsAdapter(
    private val onItemClick: (File) -> Unit, // Changed to File
    private val onItemLongClick: (File) -> Unit // Changed to File
) : ListAdapter<File, FolderContentsAdapter.ContentViewHolder>(ContentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_folder_content, parent, false)
        return ContentViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ContentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemIcon: ImageView = itemView.findViewById(R.id.item_icon)
        private val itemName: TextView = itemView.findViewById(R.id.item_name)
        private val itemDetails: TextView = itemView.findViewById(R.id.item_details)
        private val itemMenu: ImageView = itemView.findViewById(R.id.item_menu) // For options like rename, delete, share

        fun bind(item: File) {
            itemName.text = item.name

            if (item.isDirectory) {
                itemIcon.setImageResource(R.drawable.ic_folder) // Use custom folder icon
                // Optionally, count items in subfolder or show "Folder"
                // val itemCount = item.listFiles()?.size ?: 0
                // itemDetails.text = "$itemCount items"
                itemDetails.text = "Folder" // Simple detail for folder
            } else {
                itemIcon.setImageResource(R.drawable.ic_file) // Use custom file icon
                // itemDetails.text = "📄 ${formatFileSize(item.length())}" // Example with file size
                itemDetails.text = "File - ${formatFileSize(item.length())}"

                // More advanced: Set icon based on MIME type or extension
                // val mimeType = getMimeType(item) // This helper would need to be accessible here or passed
                // if (mimeType?.startsWith("image/") == true) {
                //     itemIcon.setImageResource(R.drawable.ic_image_file) // Specific image icon
                // } else if (mimeType?.startsWith("video/") == true) {
                //     itemIcon.setImageResource(R.drawable.ic_video_file) // Specific video icon
                // } else {
                //     itemIcon.setImageResource(R.drawable.ic_file) // Generic file icon
                // }
            }

            itemView.setOnClickListener {
                onItemClick(item)
            }

            itemView.setOnLongClickListener {
                onItemLongClick(item)
                true
            }

            // Make sure your R.id.item_menu exists in item_folder_content.xml
            // and you want the same action as long click, otherwise implement specific menu popup
            itemMenu.setOnClickListener {
                // Typically, this would open a popup menu for the item
                onItemLongClick(item) // Or showPopupMenu(it, item)
            }
        }
    }

    private class ContentDiffCallback : DiffUtil.ItemCallback<File>() {
        override fun areItemsTheSame(oldItem: File, newItem: File): Boolean {
            return oldItem.absolutePath == newItem.absolutePath // Compare by path
        }

        override fun areContentsTheSame(oldItem: File, newItem: File): Boolean {
            // For files, could also check lastModified and size for more robust comparison
            return oldItem.name == newItem.name &&
                   oldItem.isDirectory == newItem.isDirectory &&
                   oldItem.length() == newItem.length() &&
                   oldItem.lastModified() == newItem.lastModified()
        }
    }

    // Helper function to format file size (optional)
    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(Locale.getDefault(), "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
