package com.shaadow.onecalculator.calculators.algebra

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import java.util.regex.Pattern

class LinearEquationsFragment : Fragment() {
    private lateinit var inputA: EditText
    private lateinit var inputB: EditText
    private lateinit var inputC: EditText
    private lateinit var buttonSolve: Button
    private lateinit var resultText: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Create root LinearLayout
        val rootLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        // Create equation display
        val equationText = TextView(requireContext()).apply {
            text = "Solve: ax + b = c"
            textSize = 18f
            setPadding(0, 0, 0, 24)
            gravity = android.view.Gravity.CENTER
        }

        // Create input fields
        val aLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 16)
        }

        val aLabel = TextView(requireContext()).apply {
            text = "Coefficient a:"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        inputA = EditText(requireContext()).apply {
            hint = "Enter a"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
        }

        aLayout.addView(aLabel)
        aLayout.addView(inputA)

        val bLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 16)
        }

        val bLabel = TextView(requireContext()).apply {
            text = "Coefficient b:"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        inputB = EditText(requireContext()).apply {
            hint = "Enter b"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
        }

        bLayout.addView(bLabel)
        bLayout.addView(inputB)

        val cLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 16)
        }

        val cLabel = TextView(requireContext()).apply {
            text = "Constant c:"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        inputC = EditText(requireContext()).apply {
            hint = "Enter c"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
        }

        cLayout.addView(cLabel)
        cLayout.addView(inputC)

        // Create solve button
        buttonSolve = Button(requireContext()).apply {
            text = "Solve Equation"
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 16, 0, 16)
            }
        }

        // Create result text view
        resultText = TextView(requireContext()).apply {
            text = "Solution will appear here"
            textSize = 16f
            setPadding(0, 16, 0, 0)
        }

        // Add all views to root layout
        rootLayout.addView(equationText)
        rootLayout.addView(aLayout)
        rootLayout.addView(bLayout)
        rootLayout.addView(cLayout)
        rootLayout.addView(buttonSolve)
        rootLayout.addView(resultText)

        buttonSolve.setOnClickListener {
            val aStr = inputA.text.toString().trim()
            val bStr = inputB.text.toString().trim()
            val cStr = inputC.text.toString().trim()

            if (aStr.isEmpty() || bStr.isEmpty() || cStr.isEmpty()) {
                Toast.makeText(context, "Please enter all values", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val a = aStr.toDouble()
                val b = bStr.toDouble()
                val c = cStr.toDouble()

                val result = solveLinearEquation(a, b, c)
                resultText.text = result
            } catch (e: NumberFormatException) {
                Toast.makeText(context, "Invalid number format", Toast.LENGTH_SHORT).show()
            }
        }

        return rootLayout
    }

    private fun solveLinearEquation(a: Double, b: Double, c: Double): String {
        // Formula: ax + b = c
        // Solution: x = (c - b) / a
        if (a == 0.0) {
            return if (b == c) "Infinite solutions" else "No solution"
        }

        val x = (c - b) / a
        return "x = ${String.format("%.2f", x)}"
    }
}

