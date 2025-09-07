package com.shaadow.onecalculator.calculators.insurance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class LifeInsuranceFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Create root LinearLayout
        val rootLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            gravity = android.view.Gravity.CENTER
        }

        // Create title
        val titleText = TextView(requireContext()).apply {
            text = "Life Insurance Calculator"
            textSize = 24f
            setPadding(0, 0, 0, 32)
            gravity = android.view.Gravity.CENTER
        }

        // Create description
        val descriptionText = TextView(requireContext()).apply {
            text = "This calculator will help you calculate life insurance costs.\n\nFeature coming soon..."
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 16, 0, 0)
        }

        // Add views to layout
        rootLayout.addView(titleText)
        rootLayout.addView(descriptionText)

        return rootLayout
    }
}

