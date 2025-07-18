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

class HomeFragment : Fragment() {
    
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
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
        
        setupSearchBar()
        setupCategoryCards()
        setupNotificationIcon()
        val versionText = view.findViewById<TextView>(R.id.appVersion)
        val versionName = requireContext().packageManager
            .getPackageInfo(requireContext().packageName, 0).versionName
        versionText?.text = "App Version: $versionName"
    }
    
    private fun setupSearchBar() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                // TODO: Implement search functionality
                if (query.isNotEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.toast_searching_for, query), Toast.LENGTH_SHORT).show()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Clear focus when clicking outside
        binding.root.setOnClickListener {
            binding.searchInput.clearFocus()
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.searchInput.windowToken, 0)
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
            val dialog = CalculatorHostDialog.newInstance("Unit Converter")
            dialog.show(parentFragmentManager, "calculator_dialog")
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
        val popup = android.widget.PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.settings_popup_menu, popup.menu)
        
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_history -> {
                    val intent = Intent(requireContext(), HistoryActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.menu_settings -> {
                    val intent = Intent(requireContext(), SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.menu_about -> {
                    val intent = Intent(requireContext(), AboutUsActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }

    

    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
