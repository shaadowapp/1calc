package com.shaadow.onecalculator

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

/**
 * RecyclerView adapter for displaying recent calculation history items in a horizontal
 * scrollable format. This adapter uses DiffUtil for efficient list updates and provides
 * click handling for individual history items.
 *
 * @param onItemClick Callback function invoked when a history item is clicked
 * @author Calculator Team
 * @since 1.0
 */
class RecentHistoryAdapter(
    private val onItemClick: (HistoryEntity) -> Unit
) : ListAdapter<HistoryEntity, RecentHistoryAdapter.ViewHolder>(HistoryDiffCallback()) {

    /**
     * DiffUtil callback for efficiently comparing HistoryEntity items.
     * Enables smooth animations and performance optimization when the list updates.
     */
    class HistoryDiffCallback : DiffUtil.ItemCallback<HistoryEntity>() {
        override fun areItemsTheSame(oldItem: HistoryEntity, newItem: HistoryEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HistoryEntity, newItem: HistoryEntity): Boolean {
            return oldItem == newItem
        }
    }

    /**
     * Creates a new ViewHolder for the recent history item.
     *
     * @param parent The parent ViewGroup
     * @param viewType The view type (unused in this adapter)
     * @return A new ViewHolder instance
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recent_history_item_card, parent, false)
        return ViewHolder(view)
    }

    /**
     * Binds the HistoryEntity data to the ViewHolder.
     *
     * @param holder The ViewHolder to bind data to
     * @param position The position of the item in the list
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder class for recent history items.
     * Manages the view binding and click handling for individual history entries.
     *
     * @param itemView The root view of the history item
     */
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.recent_history_item)

        /**
         * Binds the history entity data to the view elements and sets up click handling.
         *
         * @param item The HistoryEntity to display
         */
        fun bind(item: HistoryEntity) {
            // Display expression and result in a single TextView
            textView.text = "${item.expression}\n= ${item.result}"

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
