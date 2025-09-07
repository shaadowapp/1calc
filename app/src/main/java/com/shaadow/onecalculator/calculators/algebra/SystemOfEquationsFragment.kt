package com.shaadow.onecalculator.calculators.algebra

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment

class SystemOfEquationsFragment : Fragment() {
    private lateinit var inputA1: EditText
    private lateinit var inputB1: EditText
    private lateinit var inputC1: EditText
    private lateinit var inputA2: EditText
    private lateinit var inputB2: EditText
    private lateinit var inputC2: EditText
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
            text = "Solve System:\na₁x + b₁y = c₁\na₂x + b₂y = c₂"
            textSize = 16f
            setPadding(0, 0, 0, 24)
            gravity = android.view.Gravity.CENTER
        }

        // Create first equation inputs
        val equation1Text = TextView(requireContext()).apply {
            text = "First Equation:"
            textSize = 14f
            setPadding(0, 0, 0, 8)
        }

        val eq1Layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 16)
        }

        inputA1 = EditText(requireContext()).apply {
            hint = "a₁"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val x1Text = TextView(requireContext()).apply {
            text = "x + "
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        inputB1 = EditText(requireContext()).apply {
            hint = "b₁"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val y1Text = TextView(requireContext()).apply {
            text = "y = "
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        inputC1 = EditText(requireContext()).apply {
            hint = "c₁"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        eq1Layout.addView(inputA1)
        eq1Layout.addView(x1Text)
        eq1Layout.addView(inputB1)
        eq1Layout.addView(y1Text)
        eq1Layout.addView(inputC1)

        // Create second equation inputs
        val equation2Text = TextView(requireContext()).apply {
            text = "Second Equation:"
            textSize = 14f
            setPadding(0, 0, 0, 8)
        }

        val eq2Layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 16)
        }

        inputA2 = EditText(requireContext()).apply {
            hint = "a₂"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val x2Text = TextView(requireContext()).apply {
            text = "x + "
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        inputB2 = EditText(requireContext()).apply {
            hint = "b₂"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val y2Text = TextView(requireContext()).apply {
            text = "y = "
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        inputC2 = EditText(requireContext()).apply {
            hint = "c₂"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        eq2Layout.addView(inputA2)
        eq2Layout.addView(x2Text)
        eq2Layout.addView(inputB2)
        eq2Layout.addView(y2Text)
        eq2Layout.addView(inputC2)

        // Create solve button
        buttonSolve = Button(requireContext()).apply {
            text = "Solve System"
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
        rootLayout.addView(equation1Text)
        rootLayout.addView(eq1Layout)
        rootLayout.addView(equation2Text)
        rootLayout.addView(eq2Layout)
        rootLayout.addView(buttonSolve)
        rootLayout.addView(resultText)

        buttonSolve.setOnClickListener {
            val a1Str = inputA1.text.toString().trim()
            val b1Str = inputB1.text.toString().trim()
            val c1Str = inputC1.text.toString().trim()
            val a2Str = inputA2.text.toString().trim()
            val b2Str = inputB2.text.toString().trim()
            val c2Str = inputC2.text.toString().trim()

            if (a1Str.isEmpty() || b1Str.isEmpty() || c1Str.isEmpty() ||
                a2Str.isEmpty() || b2Str.isEmpty() || c2Str.isEmpty()) {
                Toast.makeText(context, "Please enter all coefficients", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val a1 = a1Str.toDouble()
                val b1 = b1Str.toDouble()
                val c1 = c1Str.toDouble()
                val a2 = a2Str.toDouble()
                val b2 = b2Str.toDouble()
                val c2 = c2Str.toDouble()

                val result = solveSystemOfEquations(a1, b1, c1, a2, b2, c2)
                resultText.text = result
            } catch (e: NumberFormatException) {
                Toast.makeText(context, "Invalid number format", Toast.LENGTH_SHORT).show()
            }
        }

        return rootLayout
    }

    private fun solveSystemOfEquations(a1: Double, b1: Double, c1: Double,
                                     a2: Double, b2: Double, c2: Double): String {
        // Cramer's rule for 2x2 system:
        // a₁x + b₁y = c₁
        // a₂x + b₂y = c₂
        // x = (c₁b₂ - c₂b₁)/(a₁b₂ - a₂b₁)
        // y = (a₁c₂ - a₂c₁)/(a₁b₂ - a₂b₁)

        val determinant = a1 * b2 - a2 * b1

        if (determinant == 0.0) {
            return if (a1 / a2 == b1 / b2 && b1 / b2 == c1 / c2) {
                "Infinite solutions (dependent system)"
            } else {
                "No solution (inconsistent system)"
            }
        }

        val x = (c1 * b2 - c2 * b1) / determinant
        val y = (a1 * c2 - a2 * c1) / determinant

        return "x = ${String.format("%.2f", x)}, y = ${String.format("%.2f", y)}"
    }
}

