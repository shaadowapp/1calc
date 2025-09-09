package com.shaadow.onecalculator

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.shaadow.onecalculator.databinding.BottomSheetBugReportBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class BugReportBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetBugReportBinding? = null
    private val binding get() = _binding!!

    private val priorities = arrayOf("Low", "Medium", "High", "Critical")
    private val categories = arrayOf(
        "UI/UX Issue",
        "Crash",
        "Performance",
        "Feature Request",
        "Calculation Error",
        "Data Loss",
        "Security Issue",
        "Other"
    )

    private var isFormValid = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetBugReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDropdowns()
        setupDeviceInfo()
        setupFormValidation()
        setupClickListeners()
    }

    private fun setupDropdowns() {
        // Priority dropdown
        val priorityAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            priorities
        )
        binding.bugPriorityDropdown.setAdapter(priorityAdapter)
        binding.bugPriorityDropdown.setText("Medium", false) // Default to Medium

        // Category dropdown
        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories
        )
        binding.bugCategoryDropdown.setAdapter(categoryAdapter)
        binding.bugCategoryDropdown.setText("UI/UX Issue", false) // Default to UI/UX Issue
    }

    private fun setupDeviceInfo() {
        // Get device ID asynchronously
        val deviceIdManager = DeviceIdManager(requireContext())
        lifecycleScope.launch {
            val deviceId = deviceIdManager.getOrCreateDeviceId()

            val deviceInfo = buildString {
                append("🆔 Device ID: $deviceId\n")
                append("📱 Device: ${Build.MODEL} (${Build.DEVICE})\n")
                append("🔧 Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
                append("🏗️ Build: ${Build.ID}\n")
                append("📦 App Version: ${getAppVersion()}\n")
                append("⏰ Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
                append("🌐 Locale: ${Locale.getDefault()}\n")
                append("💾 Available Memory: ${getAvailableMemory()} MB\n")
                append("📊 Total Memory: ${getTotalMemory()} MB")
            }

            binding.deviceInfoText.text = deviceInfo
        }
    }

    private fun setupFormValidation() {
        // Title validation
        binding.bugTitleInput.doAfterTextChanged { validateForm() }

        // Description validation
        binding.bugDescriptionInput.doAfterTextChanged { validateForm() }

        // Priority dropdown validation
        binding.bugPriorityDropdown.setOnItemClickListener { _, _, _, _ -> validateForm() }

        // Category dropdown validation
        binding.bugCategoryDropdown.setOnItemClickListener { _, _, _, _ -> validateForm() }
    }

    private fun validateForm() {
        val title = binding.bugTitleInput.text?.toString()?.trim() ?: ""
        val description = binding.bugDescriptionInput.text?.toString()?.trim() ?: ""
        val priority = binding.bugPriorityDropdown.text?.toString()?.trim() ?: ""
        val category = binding.bugCategoryDropdown.text?.toString()?.trim() ?: ""

        val isTitleValid = title.isNotEmpty() && title.length >= 3
        val isDescriptionValid = description.isNotEmpty() && description.length >= 10
        val isPriorityValid = priority.isNotEmpty()
        val isCategoryValid = category.isNotEmpty()

        isFormValid = isTitleValid && isDescriptionValid && isPriorityValid && isCategoryValid

        // Update submit button state
        binding.btnSubmitBugReport.isEnabled = isFormValid
        binding.btnSubmitBugReport.alpha = if (isFormValid) 1.0f else 0.5f

        // Update error states
        binding.bugTitleLayout.error = if (!isTitleValid && title.isNotEmpty()) "Title must be at least 3 characters" else null
        binding.bugDescriptionLayout.error = if (!isDescriptionValid && description.isNotEmpty()) "Description must be at least 10 characters" else null
    }

    private fun setupClickListeners() {
        binding.btnCancelBugReport.setOnClickListener {
            dismiss()
        }

        binding.btnSubmitBugReport.setOnClickListener {
            if (isFormValid) {
                submitBugReport()
            } else {
                Toast.makeText(requireContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun submitBugReport() {
        val title = binding.bugTitleInput.text?.toString()?.trim()
        val description = binding.bugDescriptionInput.text?.toString()?.trim()
        val priority = binding.bugPriorityDropdown.text?.toString()?.trim()
        val category = binding.bugCategoryDropdown.text?.toString()?.trim()

        // Additional validation (should already be validated by form validation)
        if (title.isNullOrEmpty() || description.isNullOrEmpty() ||
            priority.isNullOrEmpty() || category.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Clear any existing errors
        binding.bugTitleLayout.error = null
        binding.bugDescriptionLayout.error = null

        // Show loading state
        binding.btnSubmitBugReport.isEnabled = false
        binding.btnSubmitBugReport.text = "Submitting..."
        binding.btnSubmitBugReport.alpha = 0.6f

        // Submit to Firestore
        submitToFirestore(title, description, priority, category)
    }

    private fun submitToFirestore(title: String, description: String, priority: String, category: String) {
        try {
            val firestore = FirebaseFirestore.getInstance()

            // Get current screen information
            val screenInfo = getBasicScreenInfo()

            // Create bug report data
            val bugReportData = hashMapOf(
                "title" to title,
                "description" to description,
                "priority" to priority,
                "category" to category,
                "screenName" to screenInfo["screen_name"],
                "activity" to screenInfo["activity"],
                "fragment" to screenInfo.getOrDefault("fragment", null),
                "deviceInfo" to getDeviceInfoMap(),
                "timestamp" to com.google.firebase.Timestamp.now(),
                "appVersion" to getAppVersion(),
                "userId" to getDeviceId(), // Anonymous device ID for tracking
                "status" to "new" // new, in_progress, resolved, closed
            )

            // Add to Firestore
            firestore.collection("bug_reports")
                .add(bugReportData)
                .addOnSuccessListener { documentReference ->
                    android.util.Log.d(TAG, "Bug report submitted with ID: ${documentReference.id}")

                    // Also create local file for backup
                    createBugReportFile(title, description, priority, category, documentReference.id)

                    Toast.makeText(requireContext(), "🐛 Bug report submitted successfully!", Toast.LENGTH_LONG).show()
                    dismiss()
                }
                .addOnFailureListener { e ->
                    android.util.Log.e(TAG, "Error submitting bug report", e)

                    // Fallback to local file only
                    val bugReport = createBugReportFile(title, description, priority, category)
                    if (bugReport != null) {
                        shareBugReport(bugReport)
                    }

                    Toast.makeText(requireContext(), "⚠️ Submitted locally (offline mode)", Toast.LENGTH_LONG).show()
                    dismiss()
                }

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error in submitToFirestore", e)

            // Fallback to local file
            val bugReport = createBugReportFile(title, description, priority, category)
            if (bugReport != null) {
                shareBugReport(bugReport)
            }

            Toast.makeText(requireContext(), "❌ Error submitting report", Toast.LENGTH_SHORT).show()

            // Reset button state
            binding.btnSubmitBugReport.isEnabled = true
            binding.btnSubmitBugReport.text = "📤 Submit Report"
            binding.btnSubmitBugReport.alpha = 1.0f
        }
    }

    private fun getDeviceInfoMap(): Map<String, Any> {
        return mapOf(
            "manufacturer" to android.os.Build.MANUFACTURER,
            "model" to android.os.Build.MODEL,
            "device" to android.os.Build.DEVICE,
            "androidVersion" to android.os.Build.VERSION.RELEASE,
            "apiLevel" to android.os.Build.VERSION.SDK_INT,
            "buildId" to android.os.Build.ID
        )
    }

    private fun getDeviceId(): String {
        // This method is called synchronously, so we need to get the device ID synchronously
        // We'll use a simple approach for now, but ideally this should be async
        val deviceIdManager = DeviceIdManager(requireContext())

        // For now, return a placeholder - the actual device ID will be set in setupDeviceInfo
        // This method is used in submitToFirestore which is called synchronously
        return try {
            // Try to get existing device ID synchronously if available
            val deviceInfoDao = HistoryDatabase.getInstance(requireContext()).deviceInfoDao()
            val existingDeviceInfo = runBlocking { deviceInfoDao.getDeviceInfo() }
            existingDeviceInfo?.deviceId ?: "generating..."
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun createBugReportFile(
        title: String,
        description: String,
        priority: String,
        category: String,
        firestoreId: String? = null
    ): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "bug_report_${timestamp}.txt"

            val reportsDir = File(requireContext().getExternalFilesDir(null), "bug_reports")
            if (!reportsDir.exists()) {
                reportsDir.mkdirs()
            }

            val reportFile = File(reportsDir, fileName)

            // Get screen information
            val screenInfo = getBasicScreenInfo()

            FileWriter(reportFile).use { writer ->
                writer.write("🐛 BUG REPORT\n")
                writer.write("================\n\n")

                writer.write("📋 REPORT DETAILS\n")
                writer.write("Title: $title\n")
                writer.write("Priority: $priority\n")
                writer.write("Category: $category\n")
                writer.write("Report Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
                if (firestoreId != null) {
                    writer.write("Firestore ID: $firestoreId\n")
                }
                writer.write("\n")

                writer.write("📱 CURRENT SCREEN\n")
                writer.write("Screen: ${screenInfo["screen_name"]}\n")
                writer.write("Activity: ${screenInfo["activity"]}\n")
                if (screenInfo.containsKey("fragment")) {
                    writer.write("Fragment: ${screenInfo["fragment"]}\n")
                }
                writer.write("\n")

                writer.write("📝 DESCRIPTION\n")
                writer.write("$description\n\n")

                writer.write("📱 DEVICE INFORMATION\n")
                writer.write("Device: ${Build.MODEL} (${Build.DEVICE})\n")
                writer.write("Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
                writer.write("Build ID: ${Build.ID}\n")
                writer.write("App Version: ${getAppVersion()}\n")
                writer.write("Locale: ${Locale.getDefault()}\n")
                writer.write("Available Memory: ${getAvailableMemory()} MB\n")
                writer.write("Total Memory: ${getTotalMemory()} MB\n\n")

                writer.write("🔧 SYSTEM DETAILS\n")
                writer.write("Manufacturer: ${Build.MANUFACTURER}\n")
                writer.write("Brand: ${Build.BRAND}\n")
                writer.write("Product: ${Build.PRODUCT}\n")
                writer.write("Hardware: ${Build.HARDWARE}\n")
                writer.write("Serial: ${Build.SERIAL}\n")
                writer.write("Bootloader: ${Build.BOOTLOADER}\n")
                writer.write("Radio: ${Build.RADIO}\n")
                writer.write("Tags: ${Build.TAGS}\n")
                writer.write("Type: ${Build.TYPE}\n")
                writer.write("User: ${Build.USER}\n")
                writer.write("Host: ${Build.HOST}\n")
                writer.write("Fingerprint: ${Build.FINGERPRINT}\n\n")

                writer.write("⚠️ ADDITIONAL NOTES\n")
                writer.write("- This bug report was generated automatically\n")
                writer.write("- Please attach screenshots if available\n")
                writer.write("- Include steps to reproduce the issue\n")
                if (firestoreId != null) {
                    writer.write("- Report also submitted to Firebase Firestore\n")
                }
            }

            reportFile
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error creating bug report file", e)
            null
        }
    }

    private fun shareBugReport(reportFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                reportFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Bug Report: ${reportFile.nameWithoutExtension}")
                putExtra(Intent.EXTRA_TEXT, "Please find the bug report attached.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, "Share Bug Report"))
        } catch (e: Exception) {
            android.util.Log.e("BugReportBottomSheet", "Error sharing bug report", e)
            Toast.makeText(requireContext(), "Failed to share bug report", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo: PackageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            "${packageInfo.versionName} (${packageInfo.versionCode})"
        } catch (e: PackageManager.NameNotFoundException) {
            "Unknown"
        }
    }

    private fun getAvailableMemory(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.freeMemory() + (runtime.maxMemory() - runtime.totalMemory())) / (1024 * 1024)
    }

    private fun getTotalMemory(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.maxMemory() / (1024 * 1024)
    }

    private fun getBasicScreenInfo(): Map<String, String> {
        return mapOf(
            "screen_name" to getCurrentScreenName(),
            "activity" to requireActivity().javaClass.simpleName,
            "fragment" to getCurrentFragmentName()
        )
    }

    private fun getCurrentScreenName(): String {
        return when (requireActivity().javaClass.simpleName) {
            "MainTabActivity" -> "Main Calculator"
            "SettingsActivity" -> "Settings"
            "MediaGalleryActivity" -> "Hidden Gallery"
            "AboutUsActivity" -> "About Us"
            "FeedbackActivity" -> "Feedback"
            "RateUsActivity" -> "Rate Us"
            "PrivacyPermissionsActivity" -> "Privacy & Permissions"
            "CheckUpdateActivity" -> "Check for Updates"
            "HotAppsActivity" -> "Hot Apps"
            "TodoActivity" -> "Todo List"
            else -> "Unknown Screen"
        }
    }

    private fun getCurrentFragmentName(): String {
        return try {
            // Try to get the current fragment from the activity
            if (requireActivity() is androidx.fragment.app.FragmentActivity) {
                val fragmentManager = requireActivity().supportFragmentManager
                val currentFragment = fragmentManager.findFragmentById(android.R.id.content)
                currentFragment?.javaClass?.simpleName ?: "No Fragment"
            } else {
                "No Fragment"
            }
        } catch (e: Exception) {
            "Unknown Fragment"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "BugReportBottomSheet"

        fun newInstance(): BugReportBottomSheet {
            return BugReportBottomSheet()
        }
    }
}