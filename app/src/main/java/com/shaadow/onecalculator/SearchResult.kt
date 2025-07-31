package com.shaadow.onecalculator

/**
 * Sealed class representing different types of search results in the calculator app.
 * This allows for type-safe handling of both history items and calculator features.
 *
 * @author Calculator Team
 * @since 1.0
 */
sealed class SearchResult {
    /**
     * Represents a history item found in search results.
     *
     * @param entity The HistoryEntity containing the calculation expression and result
     */
    data class HistoryItem(val entity: HistoryEntity) : SearchResult()
    
    /**
     * Represents a calculator feature/function found in search results.
     *
     * @param category The category the calculator feature belongs to (e.g., "Geometry", "Finance")
     * @param label The display name of the calculator feature
     */
    data class CalculatorItem(val category: String, val label: String) : SearchResult()

    /**
     * Represents a todo item found in search results.
     *
     * @param entity The TodoEntity containing the todo text and completion status
     */
    data class TodoItem(val entity: TodoEntity) : SearchResult()
}

/**
 * Data class representing a section of search results with a title and list of items.
 * Used to group related search results together for display.
 *
 * @param title The title of the search result section (e.g., "History", "Calculators")
 * @param items The list of SearchResult items in this section
 * @author Calculator Team
 * @since 1.0
 */
data class SearchResultSection(val title: String, val items: List<SearchResult>)
