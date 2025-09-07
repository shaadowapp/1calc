package com.shaadow.onecalculator.calculators.algebra

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import kotlin.math.abs

class FactoringFragment : Fragment() {
    private lateinit var inputExpression: EditText
    private lateinit var buttonFactor: Button
    private lateinit var resultText: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Create root LinearLayout
        val rootLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        // Create input field
        val inputLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 16)
        }

        val inputLabel = TextView(requireContext()).apply {
            text = "Number to Factor:"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        inputExpression = EditText(requireContext()).apply {
            hint = "Enter a number"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
        }

        inputLayout.addView(inputLabel)
        inputLayout.addView(inputExpression)

        // Create factor button
        buttonFactor = Button(requireContext()).apply {
            text = "Factor Number"
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 16, 0, 16)
            }
        }

        // Create result text view
        resultText = TextView(requireContext()).apply {
            text = "Prime factors will appear here"
            textSize = 16f
            setPadding(0, 16, 0, 0)
        }

        // Add all views to root layout
        rootLayout.addView(inputLayout)
        rootLayout.addView(buttonFactor)
        rootLayout.addView(resultText)

        buttonFactor.setOnClickListener {
            val expression = inputExpression.text.toString().trim()
            if (expression.isEmpty()) {
                Toast.makeText(context, "Please enter an expression", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val result = factorExpression(expression)
            resultText.text = result
        }

        return rootLayout
    }

    private fun factorExpression(input: String): String {
        try {
            val number = input.toInt()
            if (number <= 1) return "Please enter a number greater than 1"

            return factorNumber(number)
        } catch (e: Exception) {
            return "Error parsing number"
        }
    }

    private fun factorNumber(n: Int): String {
        if (n <= 1) return "Not applicable for numbers ≤ 1"

        val factors = mutableListOf<Int>()
        var num = n

        // Check for factor of 2
        while (num % 2 == 0) {
            factors.add(2)
            num /= 2
        }

        // Check for odd factors
        var i = 3
        while (i * i <= num) {
            while (num % i == 0) {
                factors.add(i)
                num /= i
            }
            i += 2
        }

        // If num is a prime number greater than 2
        if (num > 1) {
            factors.add(num)
        }

        return factors.joinToString(" × ")
    }
}

