package com.shaadow.onecalculator.calculators.converters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText

class AreaConverterFragment : Fragment() {
    private lateinit var inputArea: TextInputEditText
    private lateinit var spinnerFrom: Spinner
    private lateinit var spinnerTo: Spinner
    private lateinit var buttonConvert: Button
    private lateinit var buttonSwap: Button
    private lateinit var resultText: TextView

    private val areaUnits = arrayOf("Square Meter", "Square Kilometer", "Square Centimeter", "Square Millimeter", "Square Mile", "Square Yard", "Square Foot", "Square Inch", "Acre", "Hectare")
    private val conversions = mapOf(
        "Square Meter" to 1.0,
        "Square Kilometer" to 1000000.0,
        "Square Centimeter" to 0.0001,
        "Square Millimeter" to 0.000001,
        "Square Mile" to 2589988.11,
        "Square Yard" to 0.836127,
        "Square Foot" to 0.092903,
        "Square Inch" to 0.00064516,
        "Acre" to 4046.86,
        "Hectare" to 10000.0
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Create root LinearLayout
        val rootLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        // Create title
        val titleText = TextView(requireContext()).apply {
            text = "Area Converter"
            textSize = 20f
            setPadding(0, 0, 0, 24)
            gravity = android.view.Gravity.CENTER
        }

        // Create input field
        val inputLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 16)
        }

        val inputLabel = TextView(requireContext()).apply {
            text = "Value:"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        inputArea = TextInputEditText(requireContext()).apply {
            hint = "Enter area value"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
        }

        inputLayout.addView(inputLabel)
        inputLayout.addView(inputArea)

        // Create from spinner
        val fromLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 16)
        }

        val fromLabel = TextView(requireContext()).apply {
            text = "From:"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        spinnerFrom = Spinner(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
        }

        fromLayout.addView(fromLabel)
        fromLayout.addView(spinnerFrom)

        // Create to spinner
        val toLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 16)
        }

        val toLabel = TextView(requireContext()).apply {
            text = "To:"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        spinnerTo = Spinner(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
        }

        toLayout.addView(toLabel)
        toLayout.addView(spinnerTo)

        // Create buttons layout
        val buttonsLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 16)
        }

        buttonConvert = Button(requireContext()).apply {
            text = "Convert"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, 8, 0)
            }
        }

        buttonSwap = Button(requireContext()).apply {
            text = "Swap"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(8, 0, 0, 0)
            }
        }

        buttonsLayout.addView(buttonConvert)
        buttonsLayout.addView(buttonSwap)

        // Create result text view
        resultText = TextView(requireContext()).apply {
            text = "Result will appear here"
            textSize = 16f
            setPadding(0, 16, 0, 0)
        }

        // Add all views to root layout
        rootLayout.addView(titleText)
        rootLayout.addView(inputLayout)
        rootLayout.addView(fromLayout)
        rootLayout.addView(toLayout)
        rootLayout.addView(buttonsLayout)
        rootLayout.addView(resultText)

        // Set up spinners
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, areaUnits)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFrom.adapter = adapter
        spinnerTo.adapter = adapter

        // Set default selections
        spinnerFrom.setSelection(0) // Square Meter
        spinnerTo.setSelection(2)   // Square Centimeter

        buttonConvert.setOnClickListener {
            convertArea()
        }

        buttonSwap.setOnClickListener {
            swapUnits()
        }

        return rootLayout
    }

    private fun convertArea() {
        val input = inputArea.text.toString().trim()
        if (input.isEmpty()) {
            Toast.makeText(context, "Please enter a value", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val value = input.toDouble()
            val fromUnit = spinnerFrom.selectedItem.toString()
            val toUnit = spinnerTo.selectedItem.toString()

            val result = convert(value, fromUnit, toUnit)
            resultText.text = "Result: ${String.format("%.4f", result)} $toUnit"
        } catch (e: NumberFormatException) {
            Toast.makeText(context, "Invalid number", Toast.LENGTH_SHORT).show()
        }
    }

    private fun convert(value: Double, from: String, to: String): Double {
        val fromInSquareMeters = value * conversions[from]!!
        return fromInSquareMeters / conversions[to]!!
    }

    private fun swapUnits() {
        val fromPos = spinnerFrom.selectedItemPosition
        val toPos = spinnerTo.selectedItemPosition

        spinnerFrom.setSelection(toPos)
        spinnerTo.setSelection(fromPos)

        // If there's a current result, update it
        val currentResult = resultText.text.toString()
        if (currentResult != "Result: ") {
            convertArea()
        }
    }
}

