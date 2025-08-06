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
    private val onFolderClick: (FolderWithCount) -> Unit,
    private val onFolderLongClick: (FolderWithCount) -> Unit,
    private val onFolderMenuClick: (FolderWithCount, View) -> Unit,
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
            is FolderViewHolder -> holder.bind(getItem(position) as FolderWithCount)
            is AddFolderViewHolder -> holder.bind()
        }
    }

    fun submitFoldersWithAddButton(folders: List<FolderWithCount>) {
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
        private val folderMenu: ImageView = itemView.findViewById(R.id.folder_menu)

        fun bind(folderWithCount: FolderWithCount) {
            folderName.text = folderWithCount.name
            folderItemCount.text = folderWithCount.itemCountText

            itemView.setOnClickListener {
                onFolderClick(folderWithCount)
            }

            itemView.setOnLongClickListener {
                onFolderLongClick(folderWithCount)
                true
            }

            folderMenu.setOnClickListener { view ->
                onFolderMenuClick(folderWithCount, view)
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