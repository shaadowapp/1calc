package com.shaadow.onecalculator

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONObject

class DynamicCalculatorFragment : DialogFragment() {
    companion object {
        private const val ARG_CALC_ID = "calculator_id"
        fun newInstance(calculatorId: String): DynamicCalculatorFragment {
            val fragment = DynamicCalculatorFragment()
            val args = Bundle()
            args.putString(ARG_CALC_ID, calculatorId)
            fragment.arguments = args
            return fragment
        }
    }

    private val inputViews = mutableMapOf<String, Pair<TextInputEditText, MaterialAutoCompleteTextView?>>()
    private lateinit var config: CalculatorConfig

    // Global UI constants
    private val fillColor by lazy { android.graphics.Color.parseColor("#121212") }
    private val boxPadding by lazy { (16 * requireContext().resources.displayMetrics.density).toInt() }
    private val boxHeight by lazy { (56 * requireContext().resources.displayMetrics.density).toInt() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_dynamic_calculator, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val calcId = arguments?.getString(ARG_CALC_ID) ?: return
        config = loadConfigForCalculator(requireContext(), calcId)

        // Ensure heading is always first, formula/method always second
        val titleView = view.findViewById<TextView>(R.id.dialog_title)
        val inputContainer = view.findViewById<LinearLayout>(R.id.input_container)
        inputContainer.removeAllViews()
        val parentLayout = inputContainer.parent as? LinearLayout
        parentLayout?.let {
            // Remove any previous formula/example/heading views
            for (i in it.childCount - 1 downTo 0) {
                val v = it.getChildAt(i)
                if (v.tag == "formula_example" || v.id == R.id.dialog_title) it.removeViewAt(i)
            }
            // Add heading as first child, ensuring no parent conflict
            titleView.text = config.name
            titleView.textAlignment = View.TEXT_ALIGNMENT_CENTER
            titleView.setTextColor(resources.getColor(R.color.brand_color, null))
            titleView.setPadding(0, 0, 0, boxPadding)
            (titleView.parent as? ViewGroup)?.removeView(titleView)
            it.addView(titleView, 0)
            // Add formula view as second child if present
            config.formula?.takeIf { it.isNotBlank() }?.let { formula ->
                val formulaText = TextView(requireContext())
                formulaText.text = "Method/Formula: $formula"
                formulaText.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                formulaText.setPadding(0, 0, 0, boxPadding)
                formulaText.tag = "formula_example"
                it.addView(formulaText, 1)
            }
        }
        // Add input fields dynamically
        if (config.id == "algb01sys01" && config.inputs.size == 6 &&
            config.inputs.map { it.id } == listOf("a1", "b1", "c1", "a2", "b2", "c2")) {
            // Special 2x3 grid for System of Equations
            val row1 = LinearLayout(requireContext())
            row1.orientation = LinearLayout.HORIZONTAL
            row1.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, boxPadding, 0, 0) }
            row1.gravity = android.view.Gravity.CENTER_VERTICAL
            row1.setPadding(boxPadding, 0, boxPadding, 0)
            val row2 = LinearLayout(requireContext())
            row2.orientation = LinearLayout.HORIZONTAL
            row2.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, boxPadding, 0, 0) }
            row2.gravity = android.view.Gravity.CENTER_VERTICAL
            row2.setPadding(boxPadding, 0, boxPadding, 0)
            val rowInputs = listOf(
                Triple(row1, config.inputs[0], "a1"),
                Triple(row1, config.inputs[1], "b1"),
                Triple(row1, config.inputs[2], "c1"),
                Triple(row2, config.inputs[3], "a2"),
                Triple(row2, config.inputs[4], "b2"),
                Triple(row2, config.inputs[5], "c2")
            )
            for ((row, input, id) in rowInputs) {
                val inputLayout = TextInputLayout(requireContext(), null, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_FilledBox)
                inputLayout.layoutParams = LinearLayout.LayoutParams(0, boxHeight, 1f).apply {
                    setMargins(boxPadding / 2, 0, boxPadding / 2, 0)
                }
                inputLayout.hint = input.label
                inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_FILLED)
                inputLayout.boxBackgroundColor = fillColor
                inputLayout.boxStrokeWidth = 0
                val editText = TextInputEditText(requireContext())
                editText.inputType = if (input.type == "number")
                    InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                else InputType.TYPE_CLASS_TEXT
                editText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16f)
                editText.background = null
                editText.minHeight = boxHeight
                editText.maxHeight = boxHeight
                editText.setPadding(boxPadding, 0, boxPadding, 0)
                inputLayout.addView(editText)
                row.addView(inputLayout)
                inputViews[id] = Pair(editText, null)
            }
            inputContainer.addView(row1)
            inputContainer.addView(row2)
        } else if (config.inputs.size > 2) {
            // Compact grid: 2 input boxes per row
            var i = 0
            while (i < config.inputs.size) {
                val row = LinearLayout(requireContext())
                row.orientation = LinearLayout.HORIZONTAL
                row.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, boxPadding, 0, 0) }
                row.gravity = android.view.Gravity.CENTER_VERTICAL
                row.setPadding(boxPadding, 0, boxPadding, 0)
                for (j in 0 until 2) {
                    if (i + j < config.inputs.size) {
                        val input = config.inputs[i + j]
                        val inputLayout = TextInputLayout(requireContext(), null, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_FilledBox)
                        inputLayout.layoutParams = LinearLayout.LayoutParams(0, boxHeight, 1f).apply {
                            setMargins(boxPadding / 2, 0, boxPadding / 2, 0)
                        }
                        inputLayout.hint = input.label
                        inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_FILLED)
                        inputLayout.boxBackgroundColor = fillColor
                        inputLayout.boxStrokeWidth = 0
                        val editText = TextInputEditText(requireContext())
                        editText.inputType = if (input.type == "number")
                            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                        else InputType.TYPE_CLASS_TEXT
                        editText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16f)
                        editText.background = null
                        editText.minHeight = boxHeight
                        editText.maxHeight = boxHeight
                        editText.setPadding(boxPadding, 0, boxPadding, 0)
                        inputLayout.addView(editText)
                        row.addView(inputLayout)
                        inputViews[input.id] = Pair(editText, null)
                    } else {
                        // Add empty space for alignment if odd number of fields
                        val spacer = View(requireContext())
                        spacer.layoutParams = LinearLayout.LayoutParams(0, boxHeight, 1f)
                        row.addView(spacer)
                    }
                }
                inputContainer.addView(row)
                i += 2
            }
        } else {
            // Default layout for 1 or 2 fields
            for (input in config.inputs) {
                val row = LinearLayout(requireContext())
                row.orientation = LinearLayout.HORIZONTAL
                row.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, boxPadding, 0, 0) }
                row.gravity = android.view.Gravity.CENTER_VERTICAL
                row.setPadding(boxPadding, 0, boxPadding, 0)

                // Input field (Material filled, rounded, no border)
                val inputLayout = TextInputLayout(requireContext(), null, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_FilledBox)
                inputLayout.layoutParams = LinearLayout.LayoutParams(0, boxHeight, 2f).apply {
                    setMargins(0, 0, boxPadding / 2, 0)
                }
                inputLayout.hint = input.label
                inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_FILLED)
                inputLayout.boxBackgroundColor = fillColor
                inputLayout.boxStrokeWidth = 0
                val editText = TextInputEditText(requireContext())
                editText.inputType = if (input.type == "number")
                    InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                else InputType.TYPE_CLASS_TEXT
                editText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16f)
                editText.background = null
                editText.minHeight = boxHeight
                editText.maxHeight = boxHeight
                editText.setPadding(boxPadding, 0, boxPadding, 0)
                inputLayout.addView(editText)
                row.addView(inputLayout)

                var unitDropdown: MaterialAutoCompleteTextView? = null
                if (input.unit != null) {
                    val hasLongUnit = input.unit.any { it.length > 4 }
                    val allShortUnits = input.unit.all { it.length <= 4 }
                    val unitWeight = if (hasLongUnit) 1.5f else 1f
                    val unitMaxWidth = if (allShortUnits) (120 * resources.displayMetrics.density).toInt() else (180 * resources.displayMetrics.density).toInt()
                    val unitLayout = TextInputLayout(
                        requireContext(), null,
                        com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_FilledBox_ExposedDropdownMenu
                    )
                    unitLayout.layoutParams = LinearLayout.LayoutParams(0, boxHeight, unitWeight)
                    unitLayout.hint = "Unit"
                    unitLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_FILLED)
                    unitLayout.boxBackgroundColor = fillColor
                    unitLayout.boxStrokeWidth = 0
                    unitLayout.setEndIconMode(TextInputLayout.END_ICON_DROPDOWN_MENU)
                    unitDropdown = MaterialAutoCompleteTextView(requireContext())
                    unitDropdown.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, input.unit))
                    if (input.unit.isNotEmpty()) {
                        unitDropdown.setText(input.unit[0], false)
                    }
                    unitDropdown.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16f)
                    unitDropdown.background = null
                    unitDropdown.minHeight = boxHeight
                    unitDropdown.maxHeight = boxHeight
                    unitDropdown.setPadding(boxPadding, 0, boxPadding, 0)
                    unitDropdown.maxLines = 1
                    unitDropdown.ellipsize = android.text.TextUtils.TruncateAt.END
                    unitDropdown.minWidth = (64 * resources.displayMetrics.density).toInt()
                    unitDropdown.maxWidth = unitMaxWidth
                    unitLayout.addView(unitDropdown)
                    row.addView(unitLayout)
                }
                inputViews[input.id] = Pair(editText, unitDropdown)
                inputContainer.addView(row)
            }
        }
        // Ensure example is always after input area and before calculate button
        val btnCalculate = view.findViewById<MaterialButton>(R.id.btn_calculate)
        parentLayout?.let {
            // Remove any previous example views
            for (i in it.childCount - 1 downTo 0) {
                val v = it.getChildAt(i)
                if (v.tag == "example_text") it.removeViewAt(i)
            }
            config.example?.takeIf { it.isNotBlank() }?.let { example ->
                val exampleText = TextView(requireContext())
                exampleText.text = "Example: $example"
                exampleText.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
                exampleText.setPadding(0, boxPadding, 0, boxPadding)
                exampleText.setTextColor(0xFFAAAAAA.toInt())
                exampleText.tag = "example_text"
                // Place example just before calculate button
                it.addView(exampleText, it.indexOfChild(btnCalculate))
            }
        }
        // Calculate button
        btnCalculate.setOnClickListener {
            val inputValues = mutableMapOf<String, Double>()
            val inputTexts = mutableMapOf<String, String>()
            val inputUnits = mutableMapOf<String, String>()
            for ((id, pair) in inputViews) {
                val text = pair.first.text.toString()
                if (text.isBlank()) {
                    pair.first.error = "Required"
                    return@setOnClickListener
                }
                val value = text.toDoubleOrNull()
                if (value != null) {
                    inputValues[id] = value
                } else {
                    inputTexts[id] = text
                }
                pair.second?.let { inputUnits[id] = it.text.toString() }
            }
            val result = calculateResult(config.logic, inputValues, inputTexts, inputUnits)
            showResult(view, result)
        }
        // Close button (removed, so skip this code)
        // view.findViewById<ImageButton>(R.id.btn_close)?.setOnClickListener { dismiss() }
    }

    private fun showResult(view: View, result: String) {
        val resultCard = view.findViewById<MaterialCardView>(R.id.result_card)
        val resultText = view.findViewById<TextView>(R.id.text_result)
        resultText.text = result
        resultText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18f)
        resultText.setPadding(boxPadding, boxPadding, boxPadding, boxPadding)
        // Use the same fill color and rounded corners for the result card, no border
        resultCard.setCardBackgroundColor(fillColor)
        resultCard.cardElevation = 4f
        resultCard.radius = 16f * requireContext().resources.displayMetrics.density
        resultCard.strokeWidth = 0
        resultCard.visibility = View.VISIBLE
    }

    private fun calculateResult(logic: String, values: Map<String, Double>, texts: Map<String, String>, units: Map<String, String>): String {
        return when (logic) {
            // Algebra calculators
            "solveLinearEquation" -> solveLinearEquation(values)
            "solveQuadraticEquation" -> solveQuadraticEquation(values)
            "solveSystemOfEquations" -> solveSystemOfEquations(values)
            "factorPolynomial" -> factorPolynomial(values)
            "factorNumber" -> factorNumber(values)
            "convertComplexNumber" -> convertComplexNumber(values)

            // Geometry calculators
            "calculateArea" -> calculateArea(values, units)
            "calculatePerimeter" -> calculatePerimeter(values, units)
            "calculateVolume" -> calculateVolume(values, units)
            "calculateSurfaceArea" -> calculateSurfaceArea(values, units)
            "calculatePythagorean" -> calculatePythagorean(values, units)
            "calculateTrigonometry" -> calculateTrigonometry(values, units)

            // Finance calculators
            "calculateSimpleInterest" -> calculateSimpleInterest(values, units)
            "calculateCompoundInterest" -> calculateCompoundInterest(values, units)
            "calculateLoanPayment" -> calculateLoanPayment(values, units)
            "calculateMortgagePayment" -> calculateMortgagePayment(values, units)
            "calculateInvestment" -> calculateInvestment(values, units)
            "calculateTax" -> calculateTax(values, units)

            // Insurance calculators
            "calculateLifeInsurancePremium" -> calculateLifeInsurancePremium(values, units)
            "calculateHealthInsurancePremium" -> calculateHealthInsurancePremium(values, units)
            "calculateAutoInsurancePremium" -> calculateAutoInsurancePremium(values, units)
            "calculateHomeInsurancePremium" -> calculateHomeInsurancePremium(values, units)
            "calculatePremium" -> calculatePremium(values, units)
            "calculateCoverage" -> calculateCoverage(values, units)

            // Health calculators
            "calculateBMI" -> calculateBMI(values, units)
            "calculateCalories" -> calculateCalories(values, units)
            "calculateBMR" -> calculateBMR(values, units)
            "calculateBodyFat" -> calculateBodyFat(values, units)
            "calculateIdealWeight" -> calculateIdealWeight(values, units)
            "calculateHealthScore" -> calculateHealthScore(values, units)

            // DateTime calculators
            "calculateDateDifference" -> calculateDateDifference(texts)
            "calculateTimeDifference" -> calculateTimeDifference(texts)
            "calculateAge" -> calculateAge(texts)
            "calculateCountdown" -> calculateCountdown(texts)
            "convertTimeZone" -> convertTimeZone(texts)
            "calculateCalendarInfo" -> calculateCalendarInfo(texts)

            // Unit converters
            "convertLength" -> convertLength(values, units)
            "convertWeight" -> convertWeight(values, units)
            "convertTemperature" -> convertTemperature(values, units)
            "convertArea" -> convertArea(values, units)
            "convertVolume" -> convertVolume(values, units)
            "convertSpeed" -> convertSpeed(values, units)

            // Others calculators
            "calculatePercentage" -> calculatePercentage(values)
            "calculateRatio" -> calculateRatio(values)
            "calculateStatistics" -> calculateStatistics(texts)
            "calculateProbability" -> calculateProbability(values)
            "calculateScientific" -> calculateScientific(texts)
            "calculateMatrix" -> calculateMatrix(texts)

            else -> "Calculator logic not implemented: $logic"
        }
    }

    // Algebra calculator implementations
    private fun solveLinearEquation(values: Map<String, Double>): String {
        val a = values["a"] ?: 0.0
        val b = values["b"] ?: 0.0
        val c = values["c"] ?: 0.0

        if (a == 0.0) {
            return if (b == c) "Infinite solutions" else "No solution"
        }

        val x = (c - b) / a
        return "x = ${String.format("%.2f", x)}"
    }

    private fun solveQuadraticEquation(values: Map<String, Double>): String {
        val a = values["a"] ?: 0.0
        val b = values["b"] ?: 0.0
        val c = values["c"] ?: 0.0

        if (a == 0.0) return "Not a quadratic equation"

        val discriminant = b * b - 4 * a * c

        return when {
            discriminant > 0 -> {
                val root1 = (-b + kotlin.math.sqrt(discriminant)) / (2 * a)
                val root2 = (-b - kotlin.math.sqrt(discriminant)) / (2 * a)
                "x₁ = ${String.format("%.2f", root1)}, x₂ = ${String.format("%.2f", root2)}"
            }
            discriminant == 0.0 -> {
                val root = -b / (2 * a)
                "x = ${String.format("%.2f", root)} (double root)"
            }
            else -> {
                val realPart = -b / (2 * a)
                val imaginaryPart = kotlin.math.sqrt(-discriminant) / (2 * a)
                "x = ${String.format("%.2f", realPart)} ± ${String.format("%.2f", imaginaryPart)}i"
            }
        }
    }

    private fun solveSystemOfEquations(values: Map<String, Double>): String {
        val a1 = values["a1"] ?: 0.0
        val b1 = values["b1"] ?: 0.0
        val c1 = values["c1"] ?: 0.0
        val a2 = values["a2"] ?: 0.0
        val b2 = values["b2"] ?: 0.0
        val c2 = values["c2"] ?: 0.0

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

    private fun factorPolynomial(values: Map<String, Double>): String {
        // This is a simplified implementation
        return "Polynomial factoring: Implementation needed"
    }

    private fun factorNumber(values: Map<String, Double>): String {
        val number = values["number"]?.toInt() ?: 0
        if (number <= 1) return "Number must be greater than 1"

        val factors = mutableListOf<Int>()
        var n = number

        // Check for 2
        while (n % 2 == 0) {
            factors.add(2)
            n /= 2
        }

        // Check for odd factors
        for (i in 3..kotlin.math.sqrt(n.toDouble()).toInt() step 2) {
            while (n % i == 0) {
                factors.add(i)
                n /= i
            }
        }

        if (n > 2) factors.add(n)

        return factors.joinToString(" × ")
    }

    private fun convertComplexNumber(values: Map<String, Double>): String {
        val real = values["real"] ?: 0.0
        val imag = values["imag"] ?: 0.0

        val magnitude = kotlin.math.sqrt(real * real + imag * imag)
        val angle = Math.toDegrees(kotlin.math.atan2(imag, real))

        return "r = ${String.format("%.2f", magnitude)}, θ = ${String.format("%.2f", angle)}°"
    }

    // Geometry calculator implementations
    private fun calculateArea(values: Map<String, Double>, units: Map<String, String>): String {
        fun toMeters(value: Double, unit: String): Double = when (unit) {
            "m" -> value
            "cm" -> value / 100.0
            "ft" -> value * 0.3048
            "in" -> value * 0.0254
            else -> value
        }
        val length = toMeters(values["length"] ?: 0.0, units["length"] ?: "m")
        val width = toMeters(values["width"] ?: 0.0, units["width"] ?: "m")
        val area = length * width
        return "Area = ${String.format("%.4f", area)} m²"
    }

    private fun calculatePerimeter(values: Map<String, Double>, units: Map<String, String>): String {
        fun toMeters(value: Double, unit: String): Double = when (unit) {
            "m" -> value
            "cm" -> value / 100.0
            "ft" -> value * 0.3048
            "in" -> value * 0.0254
            else -> value
        }
        val length = toMeters(values["length"] ?: 0.0, units["length"] ?: "m")
        val width = toMeters(values["width"] ?: 0.0, units["width"] ?: "m")
        val perimeter = 2 * (length + width)
        return "Perimeter = ${String.format("%.4f", perimeter)} m"
    }

    private fun calculateVolume(values: Map<String, Double>, units: Map<String, String>): String {
        fun toMeters(value: Double, unit: String): Double = when (unit) {
            "m" -> value
            "cm" -> value / 100.0
            "ft" -> value * 0.3048
            "in" -> value * 0.0254
            else -> value
        }
        val length = toMeters(values["length"] ?: 0.0, units["length"] ?: "m")
        val width = toMeters(values["width"] ?: 0.0, units["width"] ?: "m")
        val height = toMeters(values["height"] ?: 0.0, units["height"] ?: "m")
        val volume = length * width * height
        return "Volume = ${String.format("%.4f", volume)} m³"
    }

    private fun calculateSurfaceArea(values: Map<String, Double>, units: Map<String, String>): String {
        fun toMeters(value: Double, unit: String): Double = when (unit) {
            "m" -> value
            "cm" -> value / 100.0
            "ft" -> value * 0.3048
            "in" -> value * 0.0254
            else -> value
        }
        val length = toMeters(values["length"] ?: 0.0, units["length"] ?: "m")
        val width = toMeters(values["width"] ?: 0.0, units["width"] ?: "m")
        val height = toMeters(values["height"] ?: 0.0, units["height"] ?: "m")
        val surfaceArea = 2 * (length * width + length * height + width * height)
        return "Surface Area = ${String.format("%.4f", surfaceArea)} m²"
    }

    private fun calculatePythagorean(values: Map<String, Double>, units: Map<String, String>): String {
        fun toMeters(value: Double, unit: String): Double = when (unit) {
            "m" -> value
            "cm" -> value / 100.0
            "ft" -> value * 0.3048
            "in" -> value * 0.0254
            else -> value
        }
        val a = toMeters(values["a"] ?: 0.0, units["a"] ?: "m")
        val b = toMeters(values["b"] ?: 0.0, units["b"] ?: "m")
        val c = kotlin.math.sqrt(a * a + b * b)
        return "c = ${String.format("%.4f", c)} m"
    }

    private fun calculateTrigonometry(values: Map<String, Double>, units: Map<String, String>): String {
        // Simplified implementation - just return a placeholder
        return "Trigonometry calculation: Implementation needed"
    }

    // Unit converter implementations
    private fun convertLength(values: Map<String, Double>, units: Map<String, String>): String {
        val value = values["value"] ?: 0.0
        val fromUnit = units["from_unit"] ?: "m"
        val toUnit = units["to_unit"] ?: "m"

        val conversions = mapOf(
            "m" to 1.0, "km" to 1000.0, "cm" to 0.01, "mm" to 0.001,
            "mi" to 1609.344, "yd" to 0.9144, "ft" to 0.3048, "in" to 0.0254
        )

        val fromInMeters = value * (conversions[fromUnit] ?: 1.0)
        val result = fromInMeters / (conversions[toUnit] ?: 1.0)

        return "${String.format("%.4f", result)} $toUnit"
    }

    private fun convertWeight(values: Map<String, Double>, units: Map<String, String>): String {
        val value = values["value"] ?: 0.0
        val fromUnit = units["from_unit"] ?: "kg"
        val toUnit = units["to_unit"] ?: "kg"

        val conversions = mapOf(
            "kg" to 1.0, "g" to 0.001, "mg" to 0.000001,
            "lb" to 0.453592, "oz" to 0.0283495, "ton" to 1000.0
        )

        val fromInKg = value * (conversions[fromUnit] ?: 1.0)
        val result = fromInKg / (conversions[toUnit] ?: 1.0)

        return "${String.format("%.4f", result)} $toUnit"
    }

    private fun convertTemperature(values: Map<String, Double>, units: Map<String, String>): String {
        val value = values["value"] ?: 0.0
        val fromUnit = units["from_unit"] ?: "°C"
        val toUnit = units["to_unit"] ?: "°C"

        val celsius = when (fromUnit) {
            "°F" -> (value - 32) * 5/9
            "K" -> value - 273.15
            else -> value
        }

        val result = when (toUnit) {
            "°F" -> celsius * 9/5 + 32
            "K" -> celsius + 273.15
            else -> celsius
        }

        return "${String.format("%.2f", result)} $toUnit"
    }

    private fun convertArea(values: Map<String, Double>, units: Map<String, String>): String {
        val value = values["value"] ?: 0.0
        val fromUnit = units["from_unit"] ?: "m²"
        val toUnit = units["to_unit"] ?: "m²"

        val conversions = mapOf(
            "m²" to 1.0, "km²" to 1000000.0, "cm²" to 0.0001, "mm²" to 0.000001,
            "mi²" to 2589988.11, "yd²" to 0.836127, "ft²" to 0.092903, "in²" to 0.00064516,
            "ac" to 4046.86, "ha" to 10000.0
        )

        val fromInSquareMeters = value * (conversions[fromUnit] ?: 1.0)
        val result = fromInSquareMeters / (conversions[toUnit] ?: 1.0)

        return "${String.format("%.4f", result)} $toUnit"
    }

    private fun convertVolume(values: Map<String, Double>, units: Map<String, String>): String {
        val value = values["value"] ?: 0.0
        val fromUnit = units["from_unit"] ?: "m³"
        val toUnit = units["to_unit"] ?: "m³"

        val conversions = mapOf(
            "m³" to 1.0, "cm³" to 0.000001, "mm³" to 0.000000001,
            "l" to 0.001, "ml" to 0.000001, "in³" to 0.000016387,
            "ft³" to 0.0283168, "yd³" to 0.764555, "gal" to 0.00378541
        )

        val fromInCubicMeters = value * (conversions[fromUnit] ?: 1.0)
        val result = fromInCubicMeters / (conversions[toUnit] ?: 1.0)

        return "${String.format("%.4f", result)} $toUnit"
    }

    private fun convertSpeed(values: Map<String, Double>, units: Map<String, String>): String {
        val value = values["value"] ?: 0.0
        val fromUnit = units["from_unit"] ?: "m/s"
        val toUnit = units["to_unit"] ?: "m/s"

        val conversions = mapOf(
            "m/s" to 1.0, "km/h" to 0.277778, "mph" to 0.44704,
            "ft/s" to 0.3048, "kn" to 0.514444
        )

        val fromInMs = value * (conversions[fromUnit] ?: 1.0)
        val result = fromInMs / (conversions[toUnit] ?: 1.0)

        return "${String.format("%.4f", result)} $toUnit"
    }

    // Finance calculator implementations
    private fun calculateSimpleInterest(values: Map<String, Double>, units: Map<String, String>): String {
        val principal = values["principal"] ?: 0.0
        val rate = values["rate"] ?: 0.0
        val time = values["time"] ?: 0.0
        val interest = (principal * rate * time) / 100.0
        return "Simple Interest = ${String.format("%.2f", interest)} ${units["principal"] ?: "₹"}"
    }

    private fun calculateCompoundInterest(values: Map<String, Double>, units: Map<String, String>): String {
        val principal = values["principal"] ?: 0.0
        val rate = values["rate"] ?: 0.0
        val time = values["time"] ?: 0.0
        val frequency = values["frequency"] ?: 1.0
        val amount = principal * Math.pow(1 + (rate / 100.0) / frequency, frequency * time)
        val interest = amount - principal
        return "Compound Interest = ${String.format("%.2f", interest)} ${units["principal"] ?: "₹"}"
    }

    private fun calculateLoanPayment(values: Map<String, Double>, units: Map<String, String>): String {
        val amount = values["amount"] ?: 0.0
        val rate = values["rate"] ?: 0.0
        val term = values["term"] ?: 0.0
        val monthlyRate = rate / 100.0 / 12.0
        val numPayments = term * 12.0
        val payment = if (monthlyRate == 0.0) {
            amount / numPayments
        } else {
            amount * (monthlyRate * Math.pow(1 + monthlyRate, numPayments)) / (Math.pow(1 + monthlyRate, numPayments) - 1)
        }
        return "Monthly Payment = ${String.format("%.2f", payment)} ${units["amount"] ?: "₹"}"
    }

    private fun calculateMortgagePayment(values: Map<String, Double>, units: Map<String, String>): String {
        return calculateLoanPayment(values, units) // Same formula
    }

    private fun calculateInvestment(values: Map<String, Double>, units: Map<String, String>): String {
        val initial = values["initial"] ?: 0.0
        val rate = values["rate"] ?: 0.0
        val years = values["years"] ?: 0.0
        val contribution = values["contribution"] ?: 0.0
        val futureValue = initial * Math.pow(1 + rate / 100.0, years) +
                         contribution * ((Math.pow(1 + rate / 100.0, years) - 1) / (rate / 100.0))
        return "Future Value = ${String.format("%.2f", futureValue)} ${units["initial"] ?: "₹"}"
    }

    private fun calculateTax(values: Map<String, Double>, units: Map<String, String>): String {
        val income = values["income"] ?: 0.0
        val taxRate = values["taxrate"] ?: 0.0
        val tax = (income * taxRate) / 100.0
        return "Tax Amount = ${String.format("%.2f", tax)} ${units["income"] ?: "₹"}"
    }

    // Insurance calculator implementations
    private fun calculateLifeInsurancePremium(values: Map<String, Double>, units: Map<String, String>): String {
        val age = values["age"] ?: 30.0
        val sumAssured = values["sum_assured"] ?: 1000000.0
        val term = values["term"] ?: 20.0
        // Simplified calculation: premium = (sum assured * age factor) / term
        val ageFactor = 1 + (age - 30) * 0.01
        val premium = (sumAssured * ageFactor) / term / 12.0
        return "Monthly Premium = ${String.format("%.2f", premium)} ${units["sum_assured"] ?: "₹"}"
    }

    private fun calculateHealthInsurancePremium(values: Map<String, Double>, units: Map<String, String>): String {
        val age = values["age"] ?: 40.0
        val sumAssured = values["sum_assured"] ?: 500000.0
        val members = values["members"] ?: 2.0
        // Simplified: premium = base + age factor + member factor
        val basePremium = 5000.0
        val ageFactor = (age - 30) * 100
        val memberFactor = (members - 1) * 2000
        val premium = (basePremium + ageFactor + memberFactor) / 12.0
        return "Monthly Premium = ${String.format("%.2f", premium)} ${units["sum_assured"] ?: "₹"}"
    }

    private fun calculateAutoInsurancePremium(values: Map<String, Double>, units: Map<String, String>): String {
        val carValue = values["car_value"] ?: 500000.0
        val carAge = values["car_age"] ?: 3.0
        val driverAge = values["driver_age"] ?: 35.0
        // Simplified: premium = car value factor + age factors
        val valueFactor = carValue * 0.02 / 12.0
        val ageFactor = if (driverAge < 25) 2000.0 else if (driverAge > 60) 1500.0 else 1000.0
        val carAgeFactor = carAge * 500.0
        val premium = valueFactor + ageFactor + carAgeFactor
        return "Monthly Premium = ${String.format("%.2f", premium)} ${units["car_value"] ?: "₹"}"
    }

    private fun calculateHomeInsurancePremium(values: Map<String, Double>, units: Map<String, String>): String {
        val homeValue = values["home_value"] ?: 1000000.0
        val area = values["area"] ?: 2000.0
        val yearBuilt = values["year_built"] ?: 2000.0
        val currentYear = 2023.0
        val age = currentYear - yearBuilt
        // Simplified: premium = value factor + area factor + age factor
        val valueFactor = homeValue * 0.001 / 12.0
        val areaFactor = area * 2.0
        val ageFactor = age * 100.0
        val premium = valueFactor + areaFactor + ageFactor
        return "Monthly Premium = ${String.format("%.2f", premium)} ${units["home_value"] ?: "₹"}"
    }

    private fun calculatePremium(values: Map<String, Double>, units: Map<String, String>): String {
        val sumAssured = values["sum_assured"] ?: 1000000.0
        val term = values["term"] ?: 10.0
        val rate = values["rate"] ?: 5.0
        val premium = (sumAssured * rate * term) / 100.0 / 12.0
        return "Monthly Premium = ${String.format("%.2f", premium)} ${units["sum_assured"] ?: "₹"}"
    }

    private fun calculateCoverage(values: Map<String, Double>, units: Map<String, String>): String {
        val income = values["income"] ?: 1000000.0
        val liabilities = values["liabilities"] ?: 500000.0
        val expenses = values["expenses"] ?: 200000.0
        val coverage = income + liabilities + (expenses * 5) // 5 years of expenses
        return "Coverage Needed = ${String.format("%.2f", coverage)} ${units["income"] ?: "₹"}"
    }

    // Health calculator implementations
    private fun calculateBMI(values: Map<String, Double>, units: Map<String, String>): String {
        val weight = values["weight"] ?: 70.0
        val height = values["height"] ?: 170.0

        // Convert to metric if needed
        val weightKg = if (units["weight"] == "lb") weight * 0.453592 else weight
        val heightM = when (units["height"]) {
            "cm" -> height / 100.0
            "m" -> height
            "ft" -> height * 0.3048
            else -> height / 100.0
        }

        val bmi = weightKg / (heightM * heightM)
        val category = when {
            bmi < 18.5 -> "Underweight"
            bmi < 25 -> "Normal"
            bmi < 30 -> "Overweight"
            else -> "Obese"
        }
        return "BMI = ${String.format("%.2f", bmi)} ($category)"
    }

    private fun calculateCalories(values: Map<String, Double>, units: Map<String, String>): String {
        val age = values["age"] ?: 25.0
        val weight = values["weight"] ?: 60.0
        val height = values["height"] ?: 170.0
        val gender = values["gender"] ?: 0.0 // 0 for male, 1 for female
        val activity = values["activity"] ?: 1.0 // activity level multiplier

        val weightKg = if (units["weight"] == "lb") weight * 0.453592 else weight
        val heightCm = when (units["height"]) {
            "m" -> height * 100.0
            "ft" -> height * 30.48
            "cm" -> height
            else -> height
        }

        // Mifflin-St Jeor Equation
        val bmr = if (gender == 0.0) { // Male
            10 * weightKg + 6.25 * heightCm - 5 * age + 5
        } else { // Female
            10 * weightKg + 6.25 * heightCm - 5 * age - 161
        }

        val activityMultipliers = mapOf(
            0.0 to 1.2, // Sedentary
            1.0 to 1.375, // Light
            2.0 to 1.55, // Moderate
            3.0 to 1.725, // Active
            4.0 to 1.9 // Very Active
        )

        val calories = bmr * (activityMultipliers[activity] ?: 1.375)
        return "Daily Calories = ${String.format("%.0f", calories)} kcal"
    }

    private fun calculateBMR(values: Map<String, Double>, units: Map<String, String>): String {
        val age = values["age"] ?: 30.0
        val weight = values["weight"] ?: 70.0
        val height = values["height"] ?: 170.0
        val gender = values["gender"] ?: 0.0 // 0 for male, 1 for female

        val weightKg = if (units["weight"] == "lb") weight * 0.453592 else weight
        val heightCm = when (units["height"]) {
            "m" -> height * 100.0
            "ft" -> height * 30.48
            "cm" -> height
            else -> height
        }

        val bmr = if (gender == 0.0) { // Male
            88.362 + (13.397 * weightKg) + (4.799 * heightCm) - (5.677 * age)
        } else { // Female
            447.593 + (9.247 * weightKg) + (3.098 * heightCm) - (4.330 * age)
        }

        return "BMR = ${String.format("%.0f", bmr)} kcal/day"
    }

    private fun calculateBodyFat(values: Map<String, Double>, units: Map<String, String>): String {
        val age = values["age"] ?: 28.0
        val weight = values["weight"] ?: 65.0
        val waist = values["waist"] ?: 80.0
        val gender = values["gender"] ?: 0.0 // 0 for male, 1 for female

        val weightKg = if (units["weight"] == "lb") weight * 0.453592 else weight
        val waistCm = if (units["waist"] == "in") waist * 2.54 else waist

        // US Navy method
        val bodyFat = if (gender == 0.0) { // Male
            86.010 * kotlin.math.ln(waistCm / 2.54) - 70.041 * kotlin.math.ln(weightKg * 2.20462) + 36.76
        } else { // Female
            163.205 * kotlin.math.ln(waistCm / 2.54) - 97.684 * kotlin.math.ln(weightKg * 2.20462) - 78.387
        }

        return "Body Fat = ${String.format("%.1f", bodyFat)}%"
    }

    private fun calculateIdealWeight(values: Map<String, Double>, units: Map<String, String>): String {
        val height = values["height"] ?: 170.0
        val gender = values["gender"] ?: 0.0 // 0 for male, 1 for female

        val heightCm = when (units["height"]) {
            "m" -> height * 100.0
            "ft" -> height * 30.48
            "cm" -> height
            else -> height
        }

        // Devine formula
        val idealWeight = if (gender == 0.0) { // Male
            50.0 + 2.3 * ((heightCm / 2.54) - 60)
        } else { // Female
            45.5 + 2.3 * ((heightCm / 2.54) - 60)
        }

        return "Ideal Weight = ${String.format("%.1f", idealWeight)} kg"
    }

    private fun calculateHealthScore(values: Map<String, Double>, units: Map<String, String>): String {
        val age = values["age"] ?: 35.0
        val weight = values["weight"] ?: 70.0
        val height = values["height"] ?: 170.0
        val activity = values["activity"] ?: 1.0

        val weightKg = if (units["weight"] == "lb") weight * 0.453592 else weight
        val heightM = when (units["height"]) {
            "cm" -> height / 100.0
            "m" -> height
            "ft" -> height * 0.3048
            else -> height / 100.0
        }

        val bmi = weightKg / (heightM * heightM)
        var score = 100.0

        // BMI factor
        score -= kotlin.math.abs(bmi - 22) * 2

        // Age factor
        score -= (age - 25) * 0.5

        // Activity factor
        score += activity * 5

        score = score.coerceIn(0.0, 100.0)

        return "Health Score = ${String.format("%.0f", score)}/100"
    }

    // DateTime calculator implementations
    private fun calculateDateDifference(texts: Map<String, String>): String {
        val startDate = texts["start_date"] ?: "2023-01-01"
        val endDate = texts["end_date"] ?: "2023-01-10"

        try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val start = sdf.parse(startDate)
            val end = sdf.parse(endDate)
            val diff = end.time - start.time
            val days = diff / (1000 * 60 * 60 * 24)
            return "Days Between = $days days"
        } catch (e: Exception) {
            return "Invalid date format. Use yyyy-MM-dd"
        }
    }

    private fun calculateTimeDifference(texts: Map<String, String>): String {
        val startTime = texts["start_time"] ?: "10:00"
        val endTime = texts["end_time"] ?: "12:30"

        try {
            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val start = sdf.parse(startTime)
            val end = sdf.parse(endTime)
            val diff = end.time - start.time
            val hours = diff / (1000 * 60 * 60)
            val minutes = (diff % (1000 * 60 * 60)) / (1000 * 60)
            return "Time Difference = ${hours}h ${minutes}m"
        } catch (e: Exception) {
            return "Invalid time format. Use HH:mm"
        }
    }

    private fun calculateAge(texts: Map<String, String>): String {
        val dob = texts["dob"] ?: "1990-01-01"

        try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val birthDate = sdf.parse(dob)
            val today = java.util.Date()
            val age = today.year - birthDate.year
            val adjustedAge = if (today.month < birthDate.month ||
                                 (today.month == birthDate.month && today.date < birthDate.date)) {
                age - 1
            } else {
                age
            }
            return "Age = $adjustedAge years"
        } catch (e: Exception) {
            return "Invalid date format. Use yyyy-MM-dd"
        }
    }

    private fun calculateCountdown(texts: Map<String, String>): String {
        val targetDate = texts["target_date"] ?: "2024-12-31"
        val targetTime = texts["target_time"] ?: "12:00"

        try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            val target = sdf.parse("$targetDate $targetTime")
            val now = java.util.Date()
            val diff = target.time - now.time

            if (diff <= 0) return "Event has passed"

            val days = diff / (1000 * 60 * 60 * 24)
            val hours = (diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)
            val minutes = (diff % (1000 * 60 * 60)) / (1000 * 60)

            return "Time Left = ${days}d ${hours}h ${minutes}m"
        } catch (e: Exception) {
            return "Invalid date/time format"
        }
    }

    private fun convertTimeZone(texts: Map<String, String>): String {
        val time = texts["time"] ?: "10:00"
        val fromZone = texts["from_zone"] ?: "UTC"
        val toZone = texts["to_zone"] ?: "IST"

        try {
            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val date = sdf.parse(time)

            val timeZones = mapOf(
                "UTC" to 0, "IST" to 5.5, "EST" to -5.0, "PST" to -8.0, "CET" to 1.0
            )

            val fromOffsetHours = (timeZones[fromZone] ?: 0.0) as Double
            val toOffsetHours = (timeZones[toZone] ?: 0.0) as Double
            val offsetDifference = toOffsetHours + (fromOffsetHours * -1.0)

            val calendar = java.util.Calendar.getInstance()
            calendar.time = date
            calendar.add(java.util.Calendar.HOUR_OF_DAY, offsetDifference.toInt())
            val fractionalHours = offsetDifference - offsetDifference.toInt().toDouble()
            calendar.add(java.util.Calendar.MINUTE, (fractionalHours * 60).toInt())

            return "Converted Time = ${sdf.format(calendar.time)}"
        } catch (e: Exception) {
            return "Invalid time format. Use HH:mm"
        }
    }

    private fun calculateCalendarInfo(texts: Map<String, String>): String {
        val date = texts["date"] ?: "2023-10-27"

        try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val cal = java.util.Calendar.getInstance()
            cal.time = sdf.parse(date)

            val dayOfWeek = cal.getDisplayName(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.LONG, java.util.Locale.getDefault())
            val dayOfMonth = cal.get(java.util.Calendar.DAY_OF_MONTH)
            val month = cal.getDisplayName(java.util.Calendar.MONTH, java.util.Calendar.LONG, java.util.Locale.getDefault())
            val year = cal.get(java.util.Calendar.YEAR)

            return "It's a $dayOfWeek, $month $dayOfMonth, $year"
        } catch (e: Exception) {
            return "Invalid date format. Use yyyy-MM-dd"
        }
    }

    // Others calculator implementations
    private fun calculatePercentage(values: Map<String, Double>): String {
        val value = values["value"] ?: 100.0
        val percent = values["percent"] ?: 20.0
        val result = (value * percent) / 100.0
        return "Result = ${String.format("%.2f", result)}"
    }

    private fun calculateRatio(values: Map<String, Double>): String {
        val a = values["a"] ?: 10.0
        val b = values["b"] ?: 5.0
        val total = values["total"] ?: 15.0

        if (a + b != total) {
            return "A + B should equal Total"
        }

        val gcd = gcd(a.toInt(), b.toInt())
        val ratioA = a.toInt() / gcd
        val ratioB = b.toInt() / gcd

        return "Ratio = $ratioA:$ratioB"
    }

    private fun gcd(a: Int, b: Int): Int {
        return if (b == 0) a else gcd(b, a % b)
    }

    private fun calculateStatistics(texts: Map<String, String>): String {
        val valuesStr = texts["values"] ?: "1,2,3,4,5"
        val values = valuesStr.split(",").mapNotNull { it.trim().toDoubleOrNull() }

        if (values.isEmpty()) return "No valid numbers found"

        val mean = values.average()
        val sorted = values.sorted()
        val median = if (sorted.size % 2 == 0) {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2
        } else {
            sorted[sorted.size / 2]
        }

        val frequency = values.groupingBy { it }.eachCount()
        val mode = frequency.maxByOrNull { it.value }?.key ?: values[0]

        return "Mean = ${String.format("%.2f", mean)}, Median = ${String.format("%.2f", median)}, Mode = ${String.format("%.2f", mode)}"
    }

    private fun calculateProbability(values: Map<String, Double>): String {
        val favorable = values["favorable"] ?: 5.0
        val total = values["total"] ?: 10.0

        if (total == 0.0) return "Total outcomes cannot be zero"

        val probability = favorable / total
        return "Probability = ${String.format("%.2f", probability)} (${String.format("%.1f", probability * 100)}%)"
    }

    private fun calculateScientific(texts: Map<String, String>): String {
        val expression = texts["expression"] ?: "2 + 3 * 4"

        try {
            // Simple expression evaluator (basic implementation)
            val result = evaluateExpression(expression)
            return "Result = ${String.format("%.4f", result)}"
        } catch (e: Exception) {
            return "Invalid expression: ${e.message}"
        }
    }

    private fun evaluateExpression(expression: String): Double {
        // Very basic evaluator - replace with a proper math expression parser
        val expr = expression.replace(" ", "")
        return when {
            expr.contains("+") -> {
                val parts = expr.split("+", limit = 2)
                evaluateSimple(parts[0]) + evaluateSimple(parts[1])
            }
            expr.contains("-") -> {
                val parts = expr.split("-", limit = 2)
                evaluateSimple(parts[0]) - evaluateSimple(parts[1])
            }
            expr.contains("*") -> {
                val parts = expr.split("*", limit = 2)
                evaluateSimple(parts[0]) * evaluateSimple(parts[1])
            }
            expr.contains("/") -> {
                val parts = expr.split("/", limit = 2)
                evaluateSimple(parts[0]) / evaluateSimple(parts[1])
            }
            else -> evaluateSimple(expr)
        }
    }

    private fun evaluateSimple(expr: String): Double {
        return when (expr) {
            "pi" -> Math.PI
            "e" -> Math.E
            else -> expr.toDoubleOrNull() ?: 0.0
        }
    }

    private fun calculateMatrix(texts: Map<String, String>): String {
        val matrixA = texts["matrix_a"] ?: "1,2;3,4"
        val matrixB = texts["matrix_b"] ?: "5,6;7,8"

        try {
            val matA = parseMatrix(matrixA)
            val matB = parseMatrix(matrixB)

            if (matA[0].size != matB.size) {
                return "Matrices cannot be multiplied"
            }

            val result = multiplyMatrices(matA, matB)
            return "Result = ${formatMatrix(result)}"
        } catch (e: Exception) {
            return "Invalid matrix format"
        }
    }

    private fun parseMatrix(str: String): List<List<Double>> {
        return str.split(";").map { row ->
            row.split(",").map { it.trim().toDouble() }
        }
    }

    private fun multiplyMatrices(a: List<List<Double>>, b: List<List<Double>>): List<List<Double>> {
        val result = mutableListOf<MutableList<Double>>()
        for (i in a.indices) {
            result.add(mutableListOf())
            for (j in b[0].indices) {
                var sum = 0.0
                for (k in b.indices) {
                    sum += a[i][k] * b[k][j]
                }
                result[i].add(sum)
            }
        }
        return result
    }

    private fun formatMatrix(matrix: List<List<Double>>): String {
        return matrix.joinToString(";") { row ->
            row.joinToString(",") { String.format("%.1f", it) }
        }
    }

    // --- Config loading ---
    private fun loadConfigForCalculator(context: Context, calculatorId: String): CalculatorConfig {
        val jsonString = context.assets.open("calculator_config.json").bufferedReader().use { it.readText() }
        val root = JSONObject(jsonString)
        val categories = root.getJSONArray("categories")
        for (i in 0 until categories.length()) {
            val category = categories.getJSONObject(i)
            val calculators = category.getJSONArray("calculators")
            for (j in 0 until calculators.length()) {
                val calc = calculators.getJSONObject(j)
                if (calc.getString("id") == calculatorId) {
                    val inputs = mutableListOf<InputField>()
                    val inputsArray = calc.getJSONArray("inputs")
                    for (k in 0 until inputsArray.length()) {
                        val inputObj = inputsArray.getJSONObject(k)
                        val unitList = if (inputObj.has("unit")) {
                            val arr = inputObj.getJSONArray("unit")
                            List(arr.length()) { arr.getString(it) }
                        } else null
                        inputs.add(
                            InputField(
                                id = inputObj.getString("id"),
                                label = inputObj.getString("label"),
                                type = inputObj.getString("type"),
                                unit = unitList
                            )
                        )
                    }
                    return CalculatorConfig(
                        id = calc.getString("id"),
                        name = calc.getString("name"),
                        formula = calc.optString("formula", ""),
                        example = calc.optString("example", ""),
                        inputs = inputs,
                        calculateButton = ButtonConfig(calc.getJSONObject("calculateButton").getString("label")),
                        result = ResultConfig(calc.getJSONObject("result").getString("label")),
                        logic = calc.getString("logic")
                    )
                }
            }
        }
        throw IllegalArgumentException("Calculator config not found for id: $calculatorId")
    }

    // --- Data classes ---
    data class CalculatorConfig(
        val id: String,
        val name: String,
        val formula: String? = null,
        val example: String? = null,
        val inputs: List<InputField>,
        val calculateButton: ButtonConfig,
        val result: ResultConfig,
        val logic: String
    )
    data class InputField(val id: String, val label: String, val type: String, val unit: List<String>? = null)
    data class ButtonConfig(val label: String)
    data class ResultConfig(val label: String)
} 