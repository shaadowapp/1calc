package com.shaadow.onecalculator

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.GridLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.shaadow.onecalculator.R
import com.shaadow.onecalculator.model.UnitCalculator
import com.shaadow.onecalculator.model.CalculatorSection

/**
 * RecyclerView adapter for displaying calculator sections in a sectioned format.
 * Supports both header items (section titles) and calculator items.
 * Uses multiple view types to distinguish between headers and calculator items.
 *
 * @param onCalculatorClick Callback function invoked when a calculator item is clicked
 * @param onSearchResultsChanged Callback function invoked when search results change
 * @author Calculator Team
 * @since 1.0
 */
class UnitCalculatorsAdapter(
    private val onCalculatorClick: (UnitCalculator) -> Unit,
    private val onSearchResultsChanged: (Boolean, Boolean) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /** Internal list containing flattened sections and items for RecyclerView */
    private val allItems = mutableListOf<Any>()
    private var sections = listOf<CalculatorSection>()

    companion object {
        /** View type constant for category header items */
        const val TYPE_CATEGORY_HEADER = 0
        /** View type constant for category grid items */
        const val TYPE_CATEGORY_GRID = 1
    }

    /**
     * Updates the adapter with new calculator sections.
     * Creates category headers and grids for the new grouped layout.
     *
     * @param newSections The new list of calculator sections to display
     */
    fun updateSections(newSections: List<CalculatorSection>) {
        sections = newSections
        allItems.clear()
        for (section in newSections) {
            // Add category header
            allItems.add("HEADER:${section.title}")
            // Add category grid with calculators
            allItems.add("GRID:${section.title}")
        }
        notifyDataSetChanged()

        // Notify about search results change
        val hasResults = allItems.isNotEmpty()
        onSearchResultsChanged(hasResults, !hasResults)
    }

    /**
     * Determines the view type for the item at the given position.
     *
     * @param position The position of the item
     * @return TYPE_CATEGORY_HEADER for category headers, TYPE_CATEGORY_GRID for category grids
     */
    override fun getItemViewType(position: Int): Int {
        val item = allItems[position] as String
        return if (item.startsWith("HEADER:")) {
            TYPE_CATEGORY_HEADER
        } else {
            TYPE_CATEGORY_GRID
        }
    }

    /**
     * Creates the appropriate ViewHolder based on the view type.
     *
     * @param parent The parent ViewGroup
     * @param viewType The view type (TYPE_HEADER or TYPE_CALCULATOR)
     * @return The created ViewHolder
     * @throws IllegalArgumentException if viewType is unknown
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_CATEGORY_HEADER -> {
                val view = inflater.inflate(R.layout.item_category_header, parent, false)
                CategoryHeaderViewHolder(view)
            }
            TYPE_CATEGORY_GRID -> {
                val view = inflater.inflate(R.layout.item_category_grid, parent, false)
                CategoryGridViewHolder(view)
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
        val item = allItems[position] as String
        when (holder) {
            is CategoryHeaderViewHolder -> {
                val title = item.removePrefix("HEADER:")
                holder.bind(title)
            }
            is CategoryGridViewHolder -> {
                val categoryTitle = item.removePrefix("GRID:")
                val section = sections.find { it.title == categoryTitle }
                section?.let { holder.bind(it, onCalculatorClick) }
            }
        }
    }

    /** @return The total number of items in the adapter */
    override fun getItemCount(): Int = allItems.size

    /**
     * ViewHolder for category header items.
     * Displays the category title.
     *
     * @param itemView The root view of the header item
     */
    class CategoryHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.category_title)

        /**
         * Binds the category title to the header view.
         *
         * @param title The category title to display
         */
        fun bind(title: String) {
            titleView.text = title
        }
    }

    /**
     * ViewHolder for category grid items.
     * Handles the 2x3 grid of calculator buttons for each category.
     *
     * @param itemView The root view of the grid item
     */
    class CategoryGridViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val gridLayout: GridLayout = itemView.findViewById(R.id.category_grid)

        /**
         * Binds the calculator section to the grid view.
         *
         * @param section The CalculatorSection to display
         * @param onCalculatorClick Callback function for calculator clicks
         */
        fun bind(section: CalculatorSection, onCalculatorClick: (UnitCalculator) -> Unit) {
            gridLayout.removeAllViews()

            // Take up to 6 calculators (2 columns × 3 rows)
            val calculatorsToShow = section.calculators.take(6)

            for (calculator in calculatorsToShow) {
                val buttonView = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_calculator_button, gridLayout, false) as MaterialCardView

                val titleView = buttonView.findViewById<TextView>(R.id.calculator_title)

                titleView.text = calculator.title

                buttonView.setOnClickListener { onCalculatorClick(calculator) }

                gridLayout.addView(buttonView)
            }
        }
    }
}
