package com.shaadow.onecalculator

import android.os.Bundle
import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.widget.Toast
import android.content.Intent
import com.shaadow.onecalculator.databinding.FragmentHomeBinding
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import com.shaadow.onecalculator.model.UnitCalculator
import com.shaadow.onecalculator.model.CalculatorSection
import com.shaadow.onecalculator.UnitCalculatorsAdapter
import com.shaadow.onecalculator.DynamicCalculatorFragment
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var searchResultsAdapter: SearchResultsAdapter
    private lateinit var searchManager: SearchManager
    private lateinit var unitCalculatorsAdapter: UnitCalculatorsAdapter
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private var hasUserInteracted = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSearchManager()
        setupSearchBar()
        setupSearchResults()
        setupCategoryCards()
        setupAllCalculators()
        setupNotificationIcon()

        // Ensure search results are hidden initially
        hideSearchResults()
        val versionText = view.findViewById<TextView>(R.id.appVersion)
        val versionName = requireContext().packageManager
            .getPackageInfo(requireContext().packageName, 0).versionName
        versionText?.text = "App Version: $versionName"
    }

    private fun setupSearchManager() {
        searchManager = SearchManager(requireContext())
    }

    private fun setupSearchResults() {
        searchResultsAdapter = SearchResultsAdapter { searchResult ->
            handleSearchResultClick(searchResult)
        }

        binding.searchResultsRecyclerView.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            adapter = searchResultsAdapter
        }
    }

    private fun handleSearchResultClick(searchResult: SearchResult) {
        when (searchResult) {
            is SearchResult.CalculatorItem -> {
                // Open calculator dialog
                val dialog = CalculatorHostDialog.newInstance(searchResult.label)
                dialog.show(parentFragmentManager, "calculator_dialog")

                // Clear search and hide results
                binding.searchInput.text.clear()
                hasUserInteracted = false // Reset interaction flag
                hideSearchResults()
            }
            is SearchResult.HistoryItem -> {
                // Handle history item click - copy expression to basic calculator
                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.putExtra("expression", searchResult.entity.expression)
                intent.putExtra("result", searchResult.entity.result)
                startActivity(intent)

                // Clear search and hide results
                binding.searchInput.text.clear()
                hasUserInteracted = false // Reset interaction flag
                hideSearchResults()
            }
        }
    }

    private fun showSearchResults() {
        binding.searchResultsContainer.visibility = android.view.View.VISIBLE
    }

    private fun hideSearchResults() {
        binding.searchResultsContainer.visibility = android.view.View.GONE
        // Clear the adapter data to ensure no results are shown
        searchResultsAdapter.clearAll()
        binding.searchInput.clearFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchInput.windowToken, 0)
    }

    private fun setupSearchBar() {
        // Hide search results when focus is lost
        binding.searchInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                hideSearchResults()
                hasUserInteracted = false // Reset interaction flag when losing focus
            }
            // Don't show anything automatically on focus - only show results when user types
        }

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""

                // Mark that user has interacted with search if they type something
                if (query.isNotEmpty()) {
                    hasUserInteracted = true
                }

                // Show/hide clear button immediately
                binding.searchClearButton.visibility = if (query.isNotEmpty()) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }

                // Cancel previous search if still pending
                searchRunnable?.let { searchHandler.removeCallbacks(it) }

                // Create new search runnable with slight delay for better performance
                searchRunnable = Runnable {
                    if (query.isEmpty()) {
                        // Always hide search results when input is empty
                        hideSearchResults()
                    } else {
                        performSearch(query)
                    }
                }

                // Execute search immediately for empty query, with small delay for non-empty
                if (query.isEmpty()) {
                    searchRunnable?.run()
                    // Additional safety: ensure search results are hidden when query is empty
                    binding.searchResultsContainer.visibility = android.view.View.GONE
                } else {
                    searchHandler.postDelayed(searchRunnable!!, 150) // 150ms delay
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Clear button functionality
        binding.searchClearButton.setOnClickListener {
            binding.searchInput.text.clear()
            hasUserInteracted = false // Reset interaction flag
            // Explicitly hide search results and clear adapter
            hideSearchResults()
            // Double-check that the container is hidden
            binding.searchResultsContainer.visibility = android.view.View.GONE
        }

        // Clear focus when clicking outside
        binding.root.setOnClickListener {
            hasUserInteracted = false // Reset interaction flag
            hideSearchResults()
        }
    }

    // Removed showRecentHistory() method - we don't want to show recent history anymore
    // Search results should only be shown when user actively types something

    private fun performSearch(query: String) {
        if (query.isEmpty()) {
            // Always hide search results when query is empty
            hideSearchResults()
            return
        }

        // Perform search using SearchManager in a coroutine
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val searchResults = searchManager.search(query)

                if (searchResults.isNotEmpty()) {
                    searchResultsAdapter.updateSections(searchResults)
                    binding.searchNoResultsText.visibility = android.view.View.GONE
                    binding.searchResultsRecyclerView.visibility = android.view.View.VISIBLE
                    showSearchResults()
                } else {
                    binding.searchResultsRecyclerView.visibility = android.view.View.GONE
                    binding.searchNoResultsText.visibility = android.view.View.VISIBLE
                    showSearchResults()
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "Error performing search: ${e.message}")
                binding.searchResultsRecyclerView.visibility = android.view.View.GONE
                binding.searchNoResultsText.visibility = android.view.View.VISIBLE
                showSearchResults()
            }
        }
    }

    private fun setupCategoryCards() {
        // Basic Calculator
        binding.cardBasicCalculator.setOnClickListener {
            val intent = Intent(requireContext(), MainActivity::class.java)
            startActivity(intent)
        }

        // Unit Converter
        binding.cardUnitConverter.setOnClickListener {
            // Scroll to the calculator grid section
            binding.calculatorsGridLayout.post {
                binding.nestedScrollView.smoothScrollTo(0, binding.calculatorsGridLayout.top)
            }
        }

        // Hot Apps
        binding.cardHotApps.setOnClickListener {
            // TODO: Navigate to hot apps section
            Toast.makeText(requireContext(), getString(R.string.toast_hot_apps_coming_soon), Toast.LENGTH_SHORT).show()
        }

        // Mathly Voice
        binding.cardMathlyVoice.setOnClickListener {
            // TODO: Navigate to Mathly Voice section
            Toast.makeText(requireContext(), getString(R.string.toast_mathly_voice_coming_soon), Toast.LENGTH_SHORT).show()
        }

        // Mathly Chat
        binding.cardMathlyChat.setOnClickListener {
            // TODO: Navigate to Mathly Chat section
            Toast.makeText(requireContext(), getString(R.string.toast_mathly_chat_coming_soon), Toast.LENGTH_SHORT).show()
        }

        // Scan to Invoice
        binding.cardScanInvoice.setOnClickListener {
            // TODO: Navigate to scan invoice section
            Toast.makeText(requireContext(), getString(R.string.toast_scan_to_invoice_coming_soon), Toast.LENGTH_SHORT).show()
        }



        // Hidden Gallery
        binding.cardHiddenGallery.setOnClickListener {
            // TODO: Navigate to hidden gallery section
            Toast.makeText(requireContext(), getString(R.string.toast_hidden_gallery_coming_soon), Toast.LENGTH_SHORT).show()
        }

        // Todo
        binding.cardTodo.setOnClickListener {
            val intent = Intent(requireContext(), TodoActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupNotificationIcon() {
        binding.notificationIcon.setOnClickListener {
            showSettingsPopupMenu(it)
        }
    }





    private fun showSettingsPopupMenu(view: View) {
        val items = listOf(
            com.shaadow.onecalculator.utils.PopupMenuBuilder.Item(
                id = 1,
                title = "Settings",
                iconRes = R.drawable.ic_settings,
                onClick = {
                    val intent = Intent(requireContext(), SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
            ),
            com.shaadow.onecalculator.utils.PopupMenuBuilder.Item(
                id = 2,
                title = "About Us",
                iconRes = R.drawable.ic_info,
                onClick = {
                    val intent = Intent(requireContext(), AboutUsActivity::class.java)
                    startActivity(intent)
                    true
                }
            ),
            com.shaadow.onecalculator.utils.PopupMenuBuilder.Item(
                id = 3,
                title = "History",
                iconRes = R.drawable.ic_history,
                onClick = {
                    val intent = Intent(requireContext(), HistoryActivity::class.java)
                    startActivity(intent)
                    true
                }
            )
        )
        com.shaadow.onecalculator.utils.PopupMenuBuilder.show(requireContext(), view, items)
    }

    private fun setupAllCalculators() {
        // Setup RecyclerView for all calculators with linear layout (categories with internal grids)
        binding.unitCalculatorsRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.unitCalculatorsRecyclerView.setHasFixedSize(true)

        // Get calculator sections from SearchManager
        val calculatorSections = searchManager.getAllSections()

        // Setup adapter with click handling
        unitCalculatorsAdapter = UnitCalculatorsAdapter(
            onCalculatorClick = { calculator ->
                // Open the appropriate calculator dialog using CalculatorHostDialog
                val dialog = CalculatorHostDialog.newInstance(calculator.title)
                dialog.show(parentFragmentManager, "calculator_dialog")
            },
            onSearchResultsChanged = { hasResults, showNoResults ->
                // Show/hide no results message only when there's an active search with no results
                binding.noCalculatorsText.visibility = if (showNoResults) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
                // Always show the RecyclerView (it will be empty if no results, but that's fine)
                binding.unitCalculatorsRecyclerView.visibility = android.view.View.VISIBLE
            }
        )
        unitCalculatorsAdapter.updateSections(calculatorSections)
        binding.unitCalculatorsRecyclerView.adapter = unitCalculatorsAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up search handler
        searchRunnable?.let { searchHandler.removeCallbacks(it) }
        _binding = null
    }
}