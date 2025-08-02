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

class EncryptedFolderAdapter(
    private val onFolderClick: (EncryptedFolderEntity) -> Unit,
    private val onFolderLongClick: (EncryptedFolderEntity) -> Unit
) : ListAdapter<EncryptedFolderEntity, EncryptedFolderAdapter.FolderViewHolder>(FolderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_encrypted_folder, parent, false)
        return FolderViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val folderIcon: ImageView = itemView.findViewById(R.id.folder_icon)
        private val lockIcon: ImageView = itemView.findViewById(R.id.lock_icon)
        private val folderName: TextView = itemView.findViewById(R.id.folder_name)
        private val folderItemCount: TextView = itemView.findViewById(R.id.folder_item_count)

        fun bind(folder: EncryptedFolderEntity) {
            folderName.text = folder.name
            folderItemCount.text = when (folder.itemCount) {
                0 -> "Empty"
                1 -> "1 item"
                else -> "${folder.itemCount} items"
            }

            itemView.setOnClickListener {
                onFolderClick(folder)
            }

            itemView.setOnLongClickListener {
                onFolderLongClick(folder)
                true
            }
        }
    }

    private class FolderDiffCallback : DiffUtil.ItemCallback<EncryptedFolderEntity>() {
        override fun areItemsTheSame(oldItem: EncryptedFolderEntity, newItem: EncryptedFolderEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: EncryptedFolderEntity, newItem: EncryptedFolderEntity): Boolean {
            return oldItem == newItem
        }
    }
}