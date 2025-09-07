package com.shaadow.onecalculator.calculators.converters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment

class TemperatureConverterFragment : Fragment() {
    private lateinit var inputTemperature: EditText
    private lateinit var spinnerFrom: Spinner
    private lateinit var spinnerTo: Spinner
    private lateinit var buttonConvert: Button
    private lateinit var buttonSwap: Button
    private lateinit var resultText: TextView

    private val temperatureUnits = arrayOf("Celsius", "Fahrenheit", "Kelvin")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Create root LinearLayout
        val rootLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        // Create title
        val titleText = TextView(requireContext()).apply {
            text = "Temperature Converter"
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

        inputTemperature = EditText(requireContext()).apply {
            hint = "Enter temperature value"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
        }

        inputLayout.addView(inputLabel)
        inputLayout.addView(inputTemperature)

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
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, temperatureUnits)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFrom.adapter = adapter
        spinnerTo.adapter = adapter

        // Set default selections
        spinnerFrom.setSelection(0) // Celsius
        spinnerTo.setSelection(1)   // Fahrenheit

        buttonConvert.setOnClickListener {
            convertTemperature()
        }

        buttonSwap.setOnClickListener {
            swapUnits()
        }

        return rootLayout
    }

    private fun convertTemperature() {
        val input = inputTemperature.text.toString().trim()
        if (input.isEmpty()) {
            Toast.makeText(context, "Please enter a value", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val value = input.toDouble()
            val fromUnit = spinnerFrom.selectedItem.toString()
            val toUnit = spinnerTo.selectedItem.toString()

            val result = convert(value, fromUnit, toUnit)
            resultText.text = "Result: ${String.format("%.2f", result)} $toUnit"
        } catch (e: NumberFormatException) {
            Toast.makeText(context, "Invalid number", Toast.LENGTH_SHORT).show()
        }
    }

    private fun convert(value: Double, from: String, to: String): Double {
        // Convert to Celsius first
        val celsius = when (from) {
            "Celsius" -> value
            "Fahrenheit" -> (value - 32) * 5/9
            "Kelvin" -> value - 273.15
            else -> value
        }

        // Convert from Celsius to target unit
        return when (to) {
            "Celsius" -> celsius
            "Fahrenheit" -> celsius * 9/5 + 32
            "Kelvin" -> celsius + 273.15
            else -> celsius
        }
    }

    private fun swapUnits() {
        val fromPos = spinnerFrom.selectedItemPosition
        val toPos = spinnerTo.selectedItemPosition

        spinnerFrom.setSelection(toPos)
        spinnerTo.setSelection(fromPos)

        // If there's a current result, update it
        val currentResult = resultText.text.toString()
        if (currentResult != "Result: ") {
            convertTemperature()
        }
    }
}

