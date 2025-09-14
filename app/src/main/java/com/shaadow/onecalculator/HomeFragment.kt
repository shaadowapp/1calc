package com.shaadow.onecalculator

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.shaadow.onecalculator.databinding.FragmentHomeBinding
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

    // Permission request launcher for storage permissions
    private val requestStoragePermissionsLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            android.util.Log.d("HomeFragment", "All storage permissions granted")
            openHiddenGallery()
        } else {
            android.util.Log.w("HomeFragment", "Storage permissions denied")
            showPermissionDeniedDialog()
        }
    }



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

        // Add analytics tracking for home screen
        val analyticsHelper = com.shaadow.onecalculator.utils.AnalyticsHelper(requireContext())
        analyticsHelper.logScreenView("Home Screen", "HomeFragment")

        setupSearchManager()
        setupSearchBar()
        setupSearchResults()
        setupCategoryCards()
        setupAllCalculators()
        setupNotificationIcon()
        setupTimeBasedGreeting()

        // Ensure search results are hidden initially
        hideSearchResults()
        val versionText = view.findViewById<TextView>(R.id.appVersion)
        val versionName = requireContext().packageManager
            .getPackageInfo(requireContext().packageName, 0).versionName
        versionText?.text = "App Version: $versionName"

        // Observe preference changes in real time for immediate effect (like MainTabActivity)
        val dao = HistoryDatabase.getInstance(requireContext()).preferenceDao()
        lifecycleScope.launch {
            dao.observeAllPreferences().collect { prefs ->
                val hiddenGalleryVisible = prefs.find { it.key == "hidden_gallery_visible" }?.value?.toBooleanStrictOrNull() ?: true
                android.util.Log.d("HomeFragment", "Real-time preference update - hidden gallery visible: $hiddenGalleryVisible")
                updateHiddenGalleryButtonVisibility(hiddenGalleryVisible)
            }
        }
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
            is SearchResult.TodoItem -> {
                // Handle todo item click - navigate to todo activity
                val intent = Intent(requireContext(), TodoActivity::class.java)
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
            val intent = Intent(requireContext(), HotAppsActivity::class.java)
            startActivity(intent)
        }

        // Mathly Voice - HIDDEN for launch (in development)
        binding.cardMathlyVoice.visibility = View.GONE

        // Mathly Chat - HIDDEN for launch (in development)
        binding.cardMathlyChat.visibility = View.GONE

        // Scan to Invoice
        binding.cardScanInvoice.setOnClickListener {
            // TODO: Navigate to scan invoice section
            Toast.makeText(requireContext(), getString(R.string.toast_scan_to_invoice_coming_soon), Toast.LENGTH_SHORT).show()
        }



        // Hidden Gallery
        binding.cardHiddenGallery.setOnClickListener {
            checkPermissionsAndOpenGallery()
        }

        // Todo
        binding.cardTodo.setOnClickListener {
            val intent = Intent(requireContext(), TodoActivity::class.java)
            startActivity(intent)
        }


        // Hidden Gallery menu button
        binding.hiddenGalleryMenuButton.setOnClickListener { view ->
            showHiddenGalleryMenu(view)
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
                title = "Report Bug",
                iconRes = R.drawable.ic_action_name,
                onClick = {
                    showBugReport()
                    true
                }
            ),
            com.shaadow.onecalculator.utils.PopupMenuBuilder.Item(
                id = 3,
                title = "Send Feedback",
                iconRes = R.drawable.ic_feedback,
                onClick = {
                    showFeedback()
                    true
                }
            ),
            com.shaadow.onecalculator.utils.PopupMenuBuilder.Item(
                id = 3,
                title = "About Us",
                iconRes = R.drawable.ic_info,
                onClick = {
                    val intent = Intent(requireContext(), AboutUsActivity::class.java)
                    startActivity(intent)
                    true
                }
            ),
            com.shaadow.onecalculator.utils.PopupMenuBuilder.Item(
                id = 4,
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

    private fun showBugReport() {
        val bugReportSheet = BugReportBottomSheet.newInstance()
        bugReportSheet.show(parentFragmentManager, BugReportBottomSheet.TAG)
    }

    private fun showFeedback() {
        val feedbackSheet = FeedbackBottomSheet.newInstance()
        feedbackSheet.show(parentFragmentManager, FeedbackBottomSheet.TAG)
    }

    private fun showHiddenGalleryMenu(view: View) {
        val items = listOf(
            com.shaadow.onecalculator.utils.PopupMenuBuilder.Item(
                id = 1,
                title = "Hide",
                iconRes = R.drawable.ic_remove,
                onClick = {
                    // Hide the hidden gallery card
                    binding.cardHiddenGallery.visibility = View.GONE
                    // Update preference to remember the hidden state
                    viewLifecycleOwner.lifecycleScope.launch {
                        val dao = HistoryDatabase.getInstance(requireContext()).preferenceDao()
                        dao.setPreference(
                            PreferenceEntity(
                                key = "hidden_gallery_visible",
                                value = "false"
                            )
                        )
                    }
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

    private fun setupTimeBasedGreeting() {
        val greeting = getTimeBasedGreeting()
        binding.timeBasedGreeting.text = greeting
    }

    private fun getTimeBasedGreeting(): String {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)

        return when (hour) {
            in 5..11 -> getString(R.string.good_morning)    // 5:00 AM - 11:59 AM
            in 12..16 -> getString(R.string.good_afternoon)  // 12:00 PM - 4:59 PM
            in 17..21 -> getString(R.string.good_evening)   // 5:00 PM - 9:59 PM
            else -> getString(R.string.good_night)          // 10:00 PM - 4:59 AM
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up search handler
        searchRunnable?.let { searchHandler.removeCallbacks(it) }
        _binding = null
    }



    private fun updateHiddenGalleryButtonVisibility(isVisible: Boolean) {
        android.util.Log.d("HomeFragment", "Updating hidden gallery button visibility: $isVisible")

        // Animate the visibility change for smooth effect
        if (isVisible) {
            if (binding.cardHiddenGallery.visibility != View.VISIBLE) {
                binding.cardHiddenGallery.visibility = View.VISIBLE
                binding.cardHiddenGallery.alpha = 0f
                binding.cardHiddenGallery.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()
            }
        } else {
            if (binding.cardHiddenGallery.visibility == View.VISIBLE) {
                binding.cardHiddenGallery.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        binding.cardHiddenGallery.visibility = View.GONE
                        binding.cardHiddenGallery.alpha = 1f // Reset alpha for next show
                    }
                    .start()
            }
        }
    }

    private fun checkPermissionsAndOpenGallery() {
        android.util.Log.d("HomeFragment", "Checking storage permissions for hidden gallery")

        val permissions = mutableListOf<String>()

        // Check for media permissions based on Android version
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.READ_MEDIA_IMAGES
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.READ_MEDIA_VIDEO
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.READ_MEDIA_VIDEO)
            }
        } else {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            android.util.Log.d("HomeFragment", "Requesting permissions: $permissions")
            requestStoragePermissionsLauncher.launch(permissions.toTypedArray())
        } else {
            android.util.Log.d("HomeFragment", "All permissions already granted")
            authenticateAndOpenGallery()
        }
    }


    private fun authenticateAndOpenGallery() {
        android.util.Log.d("HomeFragment", "Starting fingerprint authentication for hidden gallery")

        // Check if biometric authentication is available
        val biometricManager = androidx.biometric.BiometricManager.from(requireContext())
        when (biometricManager.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS -> {
                android.util.Log.d("HomeFragment", "Biometric authentication is available")
                showBiometricPrompt()
            }
            androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                android.util.Log.e("HomeFragment", "No biometric hardware available")
                showBiometricError("Biometric authentication not available on this device")
            }
            androidx.biometric.BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                android.util.Log.e("HomeFragment", "Biometric hardware unavailable")
                showBiometricError("Biometric hardware is currently unavailable")
            }
            androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                android.util.Log.e("HomeFragment", "No biometric credentials enrolled")
                showBiometricError("No fingerprints enrolled. Please set up fingerprint authentication in device settings")
            }
            else -> {
                android.util.Log.e("HomeFragment", "Biometric authentication not available")
                showBiometricError("Biometric authentication is not available")
            }
        }
    }

    private fun showBiometricPrompt() {
        val executor: java.util.concurrent.Executor = androidx.core.content.ContextCompat.getMainExecutor(requireContext())
        val biometricPrompt = androidx.biometric.BiometricPrompt(this, executor,
            object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    android.util.Log.e("HomeFragment", "Biometric authentication error: $errorCode - $errString")

                    when (errorCode) {
                        androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED,
                        androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                            // User cancelled, do nothing
                            android.util.Log.d("HomeFragment", "User cancelled biometric authentication")
                        }
                        androidx.biometric.BiometricPrompt.ERROR_LOCKOUT,
                        androidx.biometric.BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                            showBiometricError("Too many failed attempts. Please try again later.")
                        }
                        else -> {
                            Toast.makeText(requireContext(), "Authentication error: $errString", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    android.util.Log.d("HomeFragment", "Biometric authentication succeeded")
                    openHiddenGallery()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    android.util.Log.d("HomeFragment", "Biometric authentication failed - fingerprint not recognized")
                    // Biometric prompt already shows feedback - no additional toast needed
                }
            })

        val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Hidden Gallery")
            .setSubtitle("Use your fingerprint to access the hidden gallery")
            .setDescription("Place your finger on the fingerprint sensor")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            android.util.Log.e("HomeFragment", "Error showing biometric prompt", e)
            showBiometricError("Failed to show fingerprint authentication: ${e.message}")
        }
    }

    private fun showBiometricError(message: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Authentication Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setCancelable(true)
            .show()
    }

    private fun openHiddenGallery() {
        try {
            val intent = android.content.Intent(requireContext(), MediaGalleryActivity::class.java)
            intent.putExtra("is_first_launch", true)
            intent.putExtra("authentication_done", true) // Indicate authentication is already completed
            intent.putExtra("session_authenticated", true) // Also set session authentication flag
            startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("HomeFragment", "Error opening hidden gallery", e)
            Toast.makeText(requireContext(), "Error opening hidden gallery", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPermissionDeniedDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Storage Permissions Required")
            .setMessage("Hidden Gallery needs storage permissions to access and encrypt your files. Please grant the permissions to continue.")
            .setPositiveButton("Grant Permissions") { _, _ ->
                checkPermissionsAndOpenGallery()
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(true)
            .show()
    }
}