package com.shaadow.onecalculator

import android.content.Context
import androidx.room.Room
import com.shaadow.onecalculator.model.UnitCalculator
import com.shaadow.onecalculator.model.CalculatorSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Manager class for handling search functionality in the calculator app.
 * Provides methods to search through calculator features and history.
 *
 * @param context The application context
 * @author Calculator Team
 * @since 1.0
 */
class SearchManager(private val context: Context) {

    private val historyDatabase by lazy {
        Room.databaseBuilder(
            context,
            HistoryDatabase::class.java,
            "history_database"
        ).build()
    }

    private val todoDatabase by lazy {
        TodoDatabase.getInstance(context)
    }

    private val calculatorSections by lazy {
        loadAllCalculatorSections()
    }

    /**
     * Searches for calculators and history items based on the query.
     *
     * @param query The search query string
     * @return List of SearchResultSection containing matching results
     */
    suspend fun search(query: String): List<SearchResultSection> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SearchResultSection>()
        
        if (query.isBlank()) {
            return@withContext results
        }

        // Search in calculator features
        val calculatorResults = searchCalculators(query)
        if (calculatorResults.isNotEmpty()) {
            results.add(SearchResultSection("Calculators", calculatorResults))
        }

        // Search in history
        val historyResults = searchHistory(query)
        if (historyResults.isNotEmpty()) {
            results.add(SearchResultSection("History", historyResults))
        }

        // Search in todos
        val todoResults = searchTodos(query)
        if (todoResults.isNotEmpty()) {
            results.add(SearchResultSection("Tasks", todoResults))
        }

        results
    }

    /**
     * Gets recent calculation history.
     *
     * @return List of SearchResult.HistoryItem containing recent calculations
     */
    suspend fun getRecentHistory(): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            val historyDao = historyDatabase.historyDao()
            val recentHistory = historyDao.getRecentHistory(10) // Get last 10 items
            recentHistory.map { SearchResult.HistoryItem(it) }
        } catch (e: Exception) {
            android.util.Log.e("SearchManager", "Error getting recent history: ${e.message}")
            emptyList()
        }
    }

    /**
     * Gets all calculator sections.
     *
     * @return List of CalculatorSection containing all available calculators
     */
    fun getAllSections(): List<CalculatorSection> {
        return calculatorSections
    }

    /**
     * Searches for calculators matching the query.
     *
     * @param query The search query string
     * @return List of SearchResult.CalculatorItem containing matching calculators
     */
    private fun searchCalculators(query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        val lowerQuery = query.lowercase()

        for (section in calculatorSections) {
            for (calculator in section.calculators) {
                if (calculator.title.lowercase().contains(lowerQuery) ||
                    section.title.lowercase().contains(lowerQuery)) {
                    results.add(SearchResult.CalculatorItem(section.title, calculator.title))
                }
            }
        }

        return results
    }

    /**
     * Searches for history items matching the query.
     *
     * @param query The search query string
     * @return List of SearchResult.HistoryItem containing matching history items
     */
    private suspend fun searchHistory(query: String): List<SearchResult> {
        return try {
            val historyDao = historyDatabase.historyDao()
            val historyItems = historyDao.searchHistorySync("%$query%")
            historyItems.map { historyEntity -> SearchResult.HistoryItem(historyEntity) }
        } catch (e: Exception) {
            android.util.Log.e("SearchManager", "Error searching history: ${e.message}")
            emptyList()
        }
    }

    /**
     * Searches for todo items matching the query.
     *
     * @param query The search query string
     * @return List of SearchResult.TodoItem containing matching todo items
     */
    private suspend fun searchTodos(query: String): List<SearchResult> {
        return try {
            val todoDao = todoDatabase.todoDao()
            val todoItems = todoDao.searchTodos("%$query%")
            todoItems.map { SearchResult.TodoItem(it) }
        } catch (e: Exception) {
            android.util.Log.e("SearchManager", "Error searching todos: ${e.message}")
            emptyList()
        }
    }

    /**
     * Loads all calculator sections from the JSON configuration file.
     *
     * @return List of CalculatorSection containing all available calculators
     */
    private fun loadAllCalculatorSections(): List<CalculatorSection> {
        val sections = mutableListOf<CalculatorSection>()
        
        try {
            // Read JSON from assets
            val inputStream = context.assets.open("home_categories.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.use { it.readText() }
            
            // Parse JSON
            val root = JSONObject(jsonString)
            val categoriesArray = root.getJSONArray("categories")
            
            // Process all categories and create sections
            for (i in 0 until categoriesArray.length()) {
                val category = categoriesArray.getJSONObject(i)
                val categoryName = category.getString("name")
                val buttons = category.getJSONArray("buttons")
                
                val calculators = mutableListOf<UnitCalculator>()
                
                for (j in 0 until buttons.length()) {
                    val buttonName = buttons.getString(j)
                    // Generate calculator ID based on category and button
                    val calculatorId = generateCalculatorId(categoryName, buttonName)
                    
                    calculators.add(UnitCalculator(
                        id = calculatorId,
                        title = buttonName,
                        description = "", // No description needed for new design
                        icon = R.drawable.ic_calc_icon
                    ))
                }
                
                sections.add(CalculatorSection(
                    title = categoryName,
                    calculators = calculators
                ))
            }
            
        } catch (e: Exception) {
            android.util.Log.e("SearchManager", "Error loading calculator sections", e)
        }
        
        return sections
    }

    /**
     * Generates a consistent calculator ID based on category and button name.
     *
     * @param categoryName The category name
     * @param buttonName The button/calculator name
     * @return A unique calculator ID string
     */
    private fun generateCalculatorId(categoryName: String, buttonName: String): String {
        val categoryPrefix = when (categoryName) {
            "Algebra" -> "alg"
            "Geometry" -> "geo"
            "Finance" -> "fin"
            "Insurance" -> "ins"
            "Health" -> "hlth"
            "Date & Time" -> "dt"
            "Unit Converters" -> "unit"
            "Others" -> "other"
            else -> "calc"
        }
        
        val buttonSuffix = buttonName.lowercase().replace(" ", "_").replace("&", "and")
        return "${categoryPrefix}_${buttonSuffix}"
    }
}
