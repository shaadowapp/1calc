package com.shaadow.onecalculator

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.shaadow.onecalculator.databinding.BottomSheetFeedbackBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

class FeedbackBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetFeedbackBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val TAG = "FeedbackBottomSheet"

        fun newInstance(): FeedbackBottomSheet {
            return FeedbackBottomSheet()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetFeedbackBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDeviceInfo()
        setupFormValidation()
        setupClickListeners()
    }

    private fun setupDeviceInfo() {
        // Get device ID asynchronously
        val deviceIdManager = DeviceIdManager(requireContext())
        lifecycleScope.launch {
            try {
                val deviceId = deviceIdManager.getOrCreateDeviceId()

                val deviceInfo = buildString {
                    append("🆔 Device ID: $deviceId\n")
                    append("📱 Device: ${android.os.Build.MODEL} (${android.os.Build.DEVICE})\n")
                    append("🔧 Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})\n")
                    append("🏗️ Build: ${android.os.Build.ID}\n")
                    append("📦 App Version: ${getAppVersion()}\n")
                    append("⏰ Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
                    append("🌐 Locale: ${Locale.getDefault()}\n")
                    append("💾 Available Memory: ${getAvailableMemory()} MB\n")
                    append("📊 Total Memory: ${getTotalMemory()} MB")
                }
                binding.deviceInfoText.text = deviceInfo
            } catch (e: Exception) {
                binding.deviceInfoText.text = "Unable to retrieve device information"
            }
        }
    }

    private fun setupFormValidation() {
        // Feedback description validation
        binding.feedbackInput.doAfterTextChanged { validateForm() }

        // Initial validation
        validateForm()
    }

    private fun validateForm() {
        val feedbackText = binding.feedbackInput.text?.toString()?.trim() ?: ""

        val isValid = feedbackText.isNotEmpty() && feedbackText.length >= 60

        // Update submit button state
        binding.submitFeedbackButton.isEnabled = isValid
        binding.submitFeedbackButton.alpha = if (isValid) 1.0f else 0.5f

        // Update error state
        binding.feedbackLayout.error = when {
            feedbackText.isNotEmpty() && feedbackText.length < 60 ->
                "Feedback must be at least 60 characters (${feedbackText.length}/60)"
            else -> null
        }
    }

    private fun setupClickListeners() {
        binding.submitFeedbackButton.setOnClickListener {
            submitFeedback()
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }

    private fun submitFeedback() {
        val feedbackText = binding.feedbackInput.text?.toString()?.trim()

        if (feedbackText.isNullOrEmpty() || feedbackText.length < 60) {
            Toast.makeText(requireContext(), "Please enter at least 60 characters", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading state
        binding.submitFeedbackButton.isEnabled = false
        binding.submitFeedbackButton.text = "Submitting..."
        binding.submitFeedbackButton.alpha = 0.6f

        // Submit to Firestore
        submitToFirestore(feedbackText)
    }

    private fun submitToFirestore(feedbackText: String) {
        try {
            val firestore = FirebaseFirestore.getInstance()

            // Get current screen information
            val screenInfo = getBasicScreenInfo()

            // Get device ID
            val deviceId = getDeviceId()

            // Create feedback data
            val feedbackData = hashMapOf(
                "feedback" to feedbackText,
                "screenName" to screenInfo["screen_name"],
                "activity" to screenInfo["activity"],
                "fragment" to screenInfo.getOrDefault("fragment", null),
                "deviceInfo" to getDeviceInfoMap(),
                "timestamp" to com.google.firebase.Timestamp.now(),
                "appVersion" to getAppVersion(),
                "userId" to deviceId, // Anonymous device ID for tracking
                "status" to "new" // new, in_progress, resolved, closed
            )

            // Add to Firestore
            firestore.collection("feedback")
                .add(feedbackData)
                .addOnSuccessListener { documentReference ->
                    android.util.Log.d(TAG, "Feedback submitted with ID: ${documentReference.id}")
                    Toast.makeText(requireContext(), "Thank you for your feedback!", Toast.LENGTH_LONG).show()
                    dismiss()
                }
                .addOnFailureListener { e ->
                    android.util.Log.e(TAG, "Error submitting feedback", e)
                    Toast.makeText(requireContext(), "Failed to submit feedback. Please try again.", Toast.LENGTH_SHORT).show()

                    // Reset button state
                    binding.submitFeedbackButton.isEnabled = true
                    binding.submitFeedbackButton.text = "Submit Feedback"
                    binding.submitFeedbackButton.alpha = 1.0f
                }

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error in submitToFirestore", e)
            Toast.makeText(requireContext(), "Error submitting feedback", Toast.LENGTH_SHORT).show()

            // Reset button state
            binding.submitFeedbackButton.isEnabled = true
            binding.submitFeedbackButton.text = "Submit Feedback"
            binding.submitFeedbackButton.alpha = 1.0f
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
}