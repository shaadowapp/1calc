package com.shaadow.onecalculator.mathly

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.util.Log
import com.shaadow.onecalculator.R
import com.shaadow.onecalculator.databinding.FragmentMathlyVoiceBinding
import com.shaadow.onecalculator.mathly.stt.VoiceInputManager
import android.content.Intent
import com.shaadow.onecalculator.PreferenceDao
import com.shaadow.onecalculator.PreferenceEntity
import com.shaadow.onecalculator.HistoryDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.shaadow.onecalculator.SettingsActivity
import androidx.lifecycle.lifecycleScope
import android.widget.TextView
import kotlinx.coroutines.flow.collect

class MathlyVoiceFragment : Fragment() {
    
    private var _binding: FragmentMathlyVoiceBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var voiceInputManager: VoiceInputManager
    
    companion object {
        private const val TAG = "MathlyVoiceFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMathlyVoiceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Settings icon click listener
        view.findViewById<View>(R.id.voice_settings)?.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
        val dao = HistoryDatabase.getInstance(requireContext()).preferenceDao()
        viewLifecycleOwner.lifecycleScope.launch {
            dao.observeAllPreferences().collect { prefs ->
                val enabled = prefs.find { it.key == "mathly_voice" }?.value?.toBooleanStrictOrNull() != false
                val parent = view as ViewGroup
                parent.removeAllViews()
                if (!enabled) {
                    val disabledView = layoutInflater.inflate(R.layout.fragment_feature_disabled, parent, false)
                    parent.addView(disabledView)
                    val messageView = disabledView.findViewById<TextView>(R.id.disabled_message)
                    messageView?.text = "Mathly Voice is currently turned off."
                    val openSettingsBtn = disabledView.findViewById<View>(R.id.open_settings_button)
                    openSettingsBtn?.setOnClickListener {
                        startActivity(Intent(requireContext(), SettingsActivity::class.java))
                    }
                } else {
                    val enabledView = layoutInflater.inflate(R.layout.fragment_mathly_voice, parent, false)
                    parent.addView(enabledView)
                    enabledView.findViewById<View>(R.id.voice_settings)?.setOnClickListener { v ->
                        showSettingsPopupMenu(v)
                    }
                    // Re-bind views and re-setup logic as needed
                    // (You may need to refactor binding logic for this to work smoothly)
                }
            }
        }
        setupVoiceInputManager()
        checkPermissions()
    }

    private fun setupVoiceInputManager() {
        // Set up click listeners immediately
        setupButtonListeners()
        
        voiceInputManager = VoiceInputManager(
            context = requireContext(),
            onFinalResult = { text -> 
                _binding?.transcriptionBox?.text = text
                _binding?.mathlyStatus?.text = "Final result received"
                // TODO: Send to parser, speak with TTS
            },
            onPartialResultCallback = { partial -> 
                _binding?.transcriptionBox?.text = partial
                _binding?.mathlyStatus?.text = "Listening..."
            },
            onError = { error -> 
                Log.e(TAG, "Voice manager error: $error")
                _binding?.transcriptionBox?.text = "Error: $error"
                _binding?.mathlyStatus?.text = "Error occurred"
                _binding?.btnListen?.isEnabled = false
                _binding?.btnStop?.isEnabled = false
                _binding?.btnRepeat?.isEnabled = false
            }
        )

        voiceInputManager.initModel {
            Log.d(TAG, "Model initialized successfully, enabling buttons")
            // Enable buttons after model is ready
            _binding?.btnListen?.isEnabled = true
            _binding?.btnStop?.isEnabled = false
            _binding?.btnRepeat?.isEnabled = true
            _binding?.mathlyStatus?.text = "Ready to listen"
        }
    }
    
    private fun setupButtonListeners() {
        Log.d(TAG, "Setting up button listeners")
        Log.d(TAG, "Button enabled state: ${binding.btnListen.isEnabled}")
        
        binding.btnListen.setOnClickListener {
            Log.d(TAG, "Listen button clicked")
            if (::voiceInputManager.isInitialized) {
                Log.d(TAG, "Voice manager initialized, starting listening")
                voiceInputManager.startListening()
                _binding?.btnListen?.isEnabled = false
                _binding?.btnStop?.isEnabled = true
                _binding?.mathlyStatus?.text = "Listening..."
            } else {
                Log.e(TAG, "Voice manager not initialized")
                _binding?.mathlyStatus?.text = "Voice manager not initialized"
            }
        }

        binding.btnStop.setOnClickListener {
            if (::voiceInputManager.isInitialized) {
                voiceInputManager.stopListening()
                _binding?.btnListen?.isEnabled = true
                _binding?.btnStop?.isEnabled = false
                _binding?.mathlyStatus?.text = "Ready to listen"
            }
        }
        
        binding.btnRepeat.setOnClickListener {
            // TODO: Implement repeat functionality
            _binding?.mathlyStatus?.text = "Repeating..."
        }
        
        // Initially disable buttons until model is ready
        _binding?.btnListen?.isEnabled = false
        _binding?.btnStop?.isEnabled = false
        _binding?.btnRepeat?.isEnabled = false
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            101 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    binding.mathlyStatus.text = "Permission granted, initializing..."
                    setupVoiceInputManager()
                } else {
                    binding.mathlyStatus.text = "Microphone permission required"
                    binding.btnListen.isEnabled = false
                }
            }
        }
    }

    private fun showNormalState() {
        _binding?.blankStateContainer?.visibility = View.GONE
        _binding?.mathlyTitle?.visibility = View.VISIBLE
        _binding?.mathlyStatus?.visibility = View.VISIBLE
        _binding?.micButtonContainer?.visibility = View.VISIBLE
        _binding?.transcriptionBox?.visibility = View.VISIBLE
        _binding?.buttonRow?.visibility = View.VISIBLE
        _binding?.footerInfo?.visibility = View.VISIBLE
    }

    private fun showSettingsPopupMenu(view: View) {
        val items = listOf(
            com.shaadow.onecalculator.utils.PopupMenuBuilder.Item(
                id = 1,
                title = "Restart Mathly",
                iconRes = R.drawable.ic_add,
                onClick = {
                    // Reset Mathly Voice UI logic (similar to New Chat in chat tab)
                    // For example, clear transcription, reset status, etc.
                    _binding?.transcriptionBox?.text = ""
                    _binding?.mathlyStatus?.text = "Ready to listen"
                    true
                }
            ),
            com.shaadow.onecalculator.utils.PopupMenuBuilder.Item(
                id = 2,
                title = "Settings",
                iconRes = R.drawable.ic_settings,
                onClick = {
                    val intent = android.content.Intent(requireContext(), com.shaadow.onecalculator.SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
            ),
            com.shaadow.onecalculator.utils.PopupMenuBuilder.Item(
                id = 3,
                title = "About Us",
                iconRes = R.drawable.ic_info,
                onClick = {
                    val intent = android.content.Intent(requireContext(), com.shaadow.onecalculator.AboutUsActivity::class.java)
                    startActivity(intent)
                    true
                }
            )
        )
        com.shaadow.onecalculator.utils.PopupMenuBuilder.show(requireContext(), view, items)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 