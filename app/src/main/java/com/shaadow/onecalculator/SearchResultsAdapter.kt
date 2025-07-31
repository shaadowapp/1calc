package com.shaadow.onecalculator

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView adapter for displaying search results in sectioned format.
 * Supports both header items (section titles) and regular items (search results).
 * Uses multiple view types to distinguish between headers and content items.
 *
 * @param sections List of search result sections to display
 * @param onItemClick Callback function invoked when a search result item is clicked
 * @author Calculator Team
 * @since 1.0
 */
class SearchResultsAdapter(
    private val onItemClick: (SearchResult) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /** Internal list containing flattened sections and items for RecyclerView */
    private val allItems = mutableListOf<Any>()

    companion object {
        /** View type constant for section header items */
        const val TYPE_HEADER = 0
        /** View type constant for search result items */
        const val TYPE_ITEM = 1
    }

    init {
        // Initialize with empty list
    }

    /**
     * Updates the adapter with new search result sections.
     * Flattens the sections into a single list for efficient RecyclerView handling.
     *
     * @param newSections The new list of search result sections to display
     */
    fun updateSections(newSections: List<SearchResultSection>) {
        allItems.clear()
        for (section in newSections) {
            allItems.add(section.title)
            allItems.addAll(section.items)
        }
        notifyDataSetChanged()
    }

    /**
     * Clears all items from the adapter.
     */
    fun clearAll() {
        allItems.clear()
        notifyDataSetChanged()
    }

    /**
     * Determines the view type for the item at the given position.
     *
     * @param position The position of the item
     * @return TYPE_HEADER for section titles, TYPE_ITEM for search results
     */
    override fun getItemViewType(position: Int): Int {
        return when (allItems[position]) {
            is String -> TYPE_HEADER
            is SearchResult -> TYPE_ITEM
            else -> TYPE_ITEM
        }
    }

    /**
     * Creates the appropriate ViewHolder based on the view type.
     *
     * @param parent The parent ViewGroup
     * @param viewType The view type (TYPE_HEADER or TYPE_ITEM)
     * @return The created ViewHolder
     * @throws IllegalArgumentException if viewType is unknown
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> {
                val view = inflater.inflate(R.layout.search_header_item, parent, false)
                HeaderViewHolder(view)
            }
            TYPE_ITEM -> {
                val view = inflater.inflate(R.layout.search_result_item_card, parent, false)
                ItemViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    /**
     * Binds data to the ViewHolder at the specified position.
     *
     * @param holder The ViewHolder to bind data to
     * @param position The position of the item in the list
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = allItems[position]
        when (holder) {
            is HeaderViewHolder -> {
                holder.bind(item as String)
            }
            is ItemViewHolder -> {
                holder.bind(item as SearchResult, onItemClick)
            }
        }
    }

    /** @return The total number of items in the adapter */
    override fun getItemCount(): Int = allItems.size

    /**
     * ViewHolder for section header items.
     * Displays the section title in the search results.
     *
     * @param itemView The root view of the header item
     */
    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.header_text)

        /**
         * Binds the section title to the header view.
         *
         * @param title The section title to display
         */
        fun bind(title: String) {
            textView.text = title
        }
    }

    /**
     * ViewHolder for search result items.
     * Handles both history items and calculator items with appropriate formatting.
     *
     * @param itemView The root view of the search result item
     */
    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.item_text)

        /**
         * Binds the search result data to the view and sets up click handling.
         * Formats the text differently based on the search result type.
         *
         * @param item The SearchResult to display
         * @param onItemClick Callback function for item clicks
         */
        fun bind(item: SearchResult, onItemClick: (SearchResult) -> Unit) {
            val text = when (item) {
                is SearchResult.HistoryItem -> "${item.entity.expression} = ${item.entity.result}"
                is SearchResult.CalculatorItem -> item.label
                is SearchResult.TodoItem -> {
                    val status = if (item.entity.done) "✓" else "○"
                    "$status ${item.entity.text}"
                }
            }
            textView.text = text
            itemView.setOnClickListener { onItemClick(item) }
        }
    }
} 