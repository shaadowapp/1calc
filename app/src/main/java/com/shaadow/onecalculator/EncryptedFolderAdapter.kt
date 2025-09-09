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

data class FolderWithCount(
    val folder: EncryptedFolderEntity,
    val fileCount: Int
) {
    val id: Long get() = folder.id
    val name: String get() = folder.name
    val itemCountText: String get() = if (fileCount == 1) "1 item" else "$fileCount items"
    
    fun toEncryptedFolderEntity(): EncryptedFolderEntity = folder
}

class EncryptedFolderAdapter(
    private val onFolderClick: (FolderWithCount) -> Unit,
    private val onFolderLongClick: (FolderWithCount) -> Unit,
    private val onFolderMenuClick: (FolderWithCount, View) -> Unit,
    private val onAddFolderClick: () -> Unit,
    private val onSelectionChanged: ((Boolean) -> Unit)? = null
) : ListAdapter<Any, RecyclerView.ViewHolder>(FolderDiffCallback()) {

    private var isSelectionMode = false
    private val selectedFolders = mutableSetOf<Long>()

    fun selectAll() {
        isSelectionMode = true
        selectedFolders.clear()
        currentList.forEach { item ->
            if (item is FolderWithCount) {
                selectedFolders.add(item.id)
            }
        }
        notifyDataSetChanged()
    }
    
    fun selectAllFolders() {
        selectAll()
    }
    
    fun deselectAll() {
        isSelectionMode = false
        selectedFolders.clear()
        notifyDataSetChanged()
    }

    fun clearSelection() {
        isSelectionMode = false
        selectedFolders.clear()
        notifyDataSetChanged()
    }

    fun getSelectedFolders(): List<EncryptedFolderEntity> {
        return currentList.filterIsInstance<FolderWithCount>()
            .filter { selectedFolders.contains(it.id) }
            .map { it.toEncryptedFolderEntity() }
    }

    private fun toggleFolderSelection(folderId: Long) {
        if (selectedFolders.contains(folderId)) {
            selectedFolders.remove(folderId)
        } else {
            selectedFolders.add(folderId)
        }

        if (selectedFolders.isEmpty()) {
            isSelectionMode = false
        }

        // Check if all folders are selected and notify
        val allFoldersSelected = checkIfAllFoldersSelected()
        onSelectionChanged?.invoke(allFoldersSelected)

        notifyDataSetChanged()
    }

    private fun checkIfAllFoldersSelected(): Boolean {
        val folderItems = currentList.filterIsInstance<FolderWithCount>()
        return folderItems.isNotEmpty() && selectedFolders.size == folderItems.size
    }

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
        private val selectionTick: ImageView = itemView.findViewById(R.id.selection_tick)

        fun bind(folderWithCount: FolderWithCount) {
            folderName.text = folderWithCount.name
            folderItemCount.text = folderWithCount.itemCountText

            // Show/hide lock icon based on folder lock status
            val isLocked = folderWithCount.folder.isLocked
            lockIcon.visibility = if (isLocked) View.VISIBLE else View.GONE

            // Handle selection state
            val isSelected = selectedFolders.contains(folderWithCount.id)
            itemView.isSelected = isSelected

            // Update background and selection indicator based on selection
            if (isSelected) {
                itemView.setBackgroundResource(R.drawable.bg_folder_selected)
                selectionTick.visibility = View.VISIBLE
                selectionTick.alpha = 0.8f // Make tick slightly transparent for better aesthetics
                folderMenu.visibility = View.GONE // Hide menu when selected

                // Add subtle scale animation for selected state
                itemView.scaleX = 0.98f
                itemView.scaleY = 0.98f

                // Apply green theme to the card
                (itemView as? com.google.android.material.card.MaterialCardView)?.apply {
                    strokeColor = context.getColor(R.color.success_green)
                    strokeWidth = 3
                }
            } else {
                itemView.setBackgroundResource(R.drawable.bg_folder_normal)
                selectionTick.visibility = View.GONE
                folderMenu.visibility = View.VISIBLE // Show menu when not selected

                // Reset scale for unselected state
                itemView.scaleX = 1.0f
                itemView.scaleY = 1.0f

                // Reset card stroke to default
                (itemView as? com.google.android.material.card.MaterialCardView)?.apply {
                    strokeColor = context.getColor(R.color.card_stroke_3f3c4b)
                    strokeWidth = 1
                }
            }

            itemView.setOnClickListener {
                if (isSelectionMode) {
                    toggleFolderSelection(folderWithCount.id)
                } else {
                    onFolderClick(folderWithCount)
                }
            }

            itemView.setOnLongClickListener {
                if (!isSelectionMode) {
                    isSelectionMode = true
                    toggleFolderSelection(folderWithCount.id)
                }
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
                oldItem is FolderWithCount && newItem is FolderWithCount ->
                    oldItem.id == newItem.id
                oldItem == ADD_BUTTON_ITEM && newItem == ADD_BUTTON_ITEM -> true
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is FolderWithCount && newItem is FolderWithCount ->
                    oldItem == newItem
                oldItem == ADD_BUTTON_ITEM && newItem == ADD_BUTTON_ITEM -> true
                else -> false
            }
        }
    }
}