package com.shaadow.onecalculator.calculators.algebra

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import kotlin.math.sqrt

class ComplexNumbersFragment : Fragment() {
    private lateinit var inputReal: EditText
    private lateinit var inputImag: EditText
    private lateinit var buttonCalculate: Button
    private lateinit var resultText: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Create root LinearLayout
        val rootLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        // Create input fields
        val realLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 16)
        }

        val realLabel = TextView(requireContext()).apply {
            text = "Real Part:"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        inputReal = EditText(requireContext()).apply {
            hint = "Enter real number"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
        }

        realLayout.addView(realLabel)
        realLayout.addView(inputReal)

        val imagLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 16)
        }

        val imagLabel = TextView(requireContext()).apply {
            text = "Imaginary Part:"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        inputImag = EditText(requireContext()).apply {
            hint = "Enter imaginary number"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
        }

        imagLayout.addView(imagLabel)
        imagLayout.addView(inputImag)

        // Create calculate button
        buttonCalculate = Button(requireContext()).apply {
            text = "Calculate"
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 16, 0, 16)
            }
        }

        // Create result text view
        resultText = TextView(requireContext()).apply {
            text = "Result will appear here"
            textSize = 16f
            setPadding(0, 16, 0, 0)
        }

        // Add all views to root layout
        rootLayout.addView(realLayout)
        rootLayout.addView(imagLayout)
        rootLayout.addView(buttonCalculate)
        rootLayout.addView(resultText)

        buttonCalculate.setOnClickListener {
            val realStr = inputReal.text.toString().trim()
            val imagStr = inputImag.text.toString().trim()

            if (realStr.isEmpty() && imagStr.isEmpty()) {
                Toast.makeText(context, "Please enter real or imaginary part", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val real = realStr.toDoubleOrNull() ?: 0.0
                val imag = imagStr.toDoubleOrNull() ?: 0.0

                val result = convertComplexNumber(real, imag)
                resultText.text = result
            } catch (e: NumberFormatException) {
                Toast.makeText(context, "Invalid number format", Toast.LENGTH_SHORT).show()
            }
        }

        return rootLayout
    }

    private fun convertComplexNumber(real: Double, imag: Double): String {
        // Formula: z = a + bi
        // Polar: r = sqrt(a²+b²), θ = atan2(b,a)

        val magnitude = sqrt(real * real + imag * imag)
        val angle = Math.toDegrees(kotlin.math.atan2(imag, real))

        val rectangular = formatComplex(Pair(real, imag))
        val polar = "r = ${String.format("%.2f", magnitude)}, θ = ${String.format("%.2f", angle)}°"

        return "$rectangular → $polar"
    }

    private fun formatComplex(complex: Pair<Double, Double>): String {
        val real = complex.first
        val imag = complex.second

        return when {
            imag == 0.0 -> "${String.format("%.2f", real)}"
            real == 0.0 && imag == 1.0 -> "i"
            real == 0.0 && imag == -1.0 -> "-i"
            real == 0.0 -> "${String.format("%.2f", imag)}i"
            imag == 1.0 -> "${String.format("%.2f", real)}+i"
            imag == -1.0 -> "${String.format("%.2f", real)}-i"
            imag > 0 -> "${String.format("%.2f", real)}+${String.format("%.2f", imag)}i"
            else -> "${String.format("%.2f", real)}${String.format("%.2f", imag)}i"
        }
    }
}

