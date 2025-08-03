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
    private val onFolderLongClick: (EncryptedFolderEntity) -> Unit,
    private val onAddFolderClick: () -> Unit
) : ListAdapter<Any, RecyclerView.ViewHolder>(FolderDiffCallback()) {

    companion object {
        private const val TYPE_FOLDER = 0
        private const val TYPE_ADD_BUTTON = 1
        private const val ADD_BUTTON_ITEM = "ADD_BUTTON"
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is EncryptedFolderEntity -> TYPE_FOLDER
            ADD_BUTTON_ITEM -> TYPE_ADD_BUTTON
            else -> TYPE_FOLDER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_FOLDER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_encrypted_folder, parent, false)
                FolderViewHolder(view)
            }
            TYPE_ADD_BUTTON -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_add_folder, parent, false)
                AddFolderViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is FolderViewHolder -> holder.bind(getItem(position) as EncryptedFolderEntity)
            is AddFolderViewHolder -> holder.bind()
        }
    }

    fun submitFoldersWithAddButton(folders: List<EncryptedFolderEntity>) {
        val itemsWithAddButton = folders.toMutableList<Any>().apply {
            add(ADD_BUTTON_ITEM)
        }
        submitList(itemsWithAddButton)
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

    inner class AddFolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind() {
            itemView.setOnClickListener {
                onAddFolderClick()
            }
        }
    }

    private class FolderDiffCallback : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is EncryptedFolderEntity && newItem is EncryptedFolderEntity ->
                    oldItem.id == newItem.id
                oldItem == ADD_BUTTON_ITEM && newItem == ADD_BUTTON_ITEM -> true
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is EncryptedFolderEntity && newItem is EncryptedFolderEntity ->
                    oldItem == newItem
                oldItem == ADD_BUTTON_ITEM && newItem == ADD_BUTTON_ITEM -> true
                else -> false
            }
        }
    }
}