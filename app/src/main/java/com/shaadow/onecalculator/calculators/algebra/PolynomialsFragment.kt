package com.shaadow.onecalculator.calculators.algebra

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class PolynomialsFragment : Fragment() {
    private lateinit var inputPolynomial: EditText
    private lateinit var buttonSolve: Button
    private lateinit var resultText: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Create root LinearLayout
        val rootLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        // Create instruction text
        val instructionText = TextView(requireContext()).apply {
            text = "Enter coefficients separated by commas (e.g., 1,2,3 for x²+2x+3)"
            textSize = 14f
            setPadding(0, 0, 0, 16)
        }

        // Create input field
        val inputLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 16)
        }

        val inputLabel = TextView(requireContext()).apply {
            text = "Coefficients:"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        inputPolynomial = EditText(requireContext()).apply {
            hint = "1,2,3"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
        }

        inputLayout.addView(inputLabel)
        inputLayout.addView(inputPolynomial)

        // Create solve button
        buttonSolve = Button(requireContext()).apply {
            text = "Factor Polynomial"
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 16, 0, 16)
            }
        }

        // Create result text view
        resultText = TextView(requireContext()).apply {
            text = "Factored form will appear here"
            textSize = 16f
            setPadding(0, 16, 0, 0)
        }

        // Add all views to root layout
        rootLayout.addView(instructionText)
        rootLayout.addView(inputLayout)
        rootLayout.addView(buttonSolve)
        rootLayout.addView(resultText)

        buttonSolve.setOnClickListener {
            val polynomial = inputPolynomial.text.toString().trim()
            if (polynomial.isEmpty()) {
                Toast.makeText(context, "Please enter a polynomial", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val result = processPolynomial(polynomial)
            resultText.text = result
        }

        return rootLayout
    }

    private fun processPolynomial(input: String): String {
        try {
            // Parse comma-separated coefficients
            val coeffs = input.split(",").map { it.trim().toDouble() }

            return when (coeffs.size) {
                1 -> "Constant: ${coeffs[0]}"
                2 -> factorLinear(coeffs[0], coeffs[1])
                3 -> factorQuadratic(coeffs[0], coeffs[1], coeffs[2])
                else -> "Higher degree polynomials not supported yet"
            }
        } catch (e: Exception) {
            return "Error parsing coefficients"
        }
    }


    private fun factorLinear(a: Double, b: Double): String {
        // Linear polynomial: ax + b
        if (a == 0.0) return "Constant: $b"
        return "${String.format("%.0f", a)}x + ${String.format("%.0f", b)}"
    }

    private fun factorQuadratic(a: Double, b: Double, c: Double): String {
        // Quadratic polynomial: ax² + bx + c
        if (a == 0.0) return factorLinear(b, c)

        // Try to factor as (px + q)(rx + s) = pr x² + (ps + qr)x + qs
        val aInt = a.toInt()
        val bInt = b.toInt()
        val cInt = c.toInt()

        if (a != aInt.toDouble() || b != bInt.toDouble() || c != cInt.toDouble()) {
            return "Coefficients must be integers for factoring"
        }

        // Find factors of a*c that add up to b
        val target = aInt * cInt
        val factors = findFactorPairs(target)

        for ((f1, f2) in factors) {
            if (f1 + f2 == bInt) {
                // Check if we can factor
                val gcd1 = gcd(abs(aInt), abs(f1))
                val gcd2 = gcd(abs(f2), abs(cInt))

                if (gcd1 > 1 || gcd2 > 1) {
                    val factor1 = aInt / gcd1
                    val factor2 = f1 / gcd1
                    val factor3 = f2 / gcd2
                    val factor4 = cInt / gcd2

                    return "(${factor1}x + $factor2)(${factor3}x + $factor4)"
                }
            }
        }

        return "Cannot be factored with rational numbers"
    }

    private fun findFactorPairs(n: Int): List<Pair<Int, Int>> {
        val factors = mutableListOf<Pair<Int, Int>>()
        val absN = abs(n)

        for (i in 1..absN) {
            if (absN % i == 0) {
                factors.add(Pair(i, absN / i))
                factors.add(Pair(-i, -absN / i))
                factors.add(Pair(i, -absN / i))
                factors.add(Pair(-i, absN / i))
            }
        }

        return factors
    }

    private fun gcd(a: Int, b: Int): Int {
        var x = a
        var y = b
        while (y != 0) {
            val temp = y
            y = x % y
            x = temp
        }
        return x
    }
}

