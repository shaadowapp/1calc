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
            val inputUnits = mutableMapOf<String, String>()
            for ((id, pair) in inputViews) {
                val value = pair.first.text.toString().toDoubleOrNull()
                if (value == null) {
                    pair.first.error = "Required"
                    return@setOnClickListener
                }
                inputValues[id] = value
                pair.second?.let { inputUnits[id] = it.text.toString() }
            }
            val result = calculateArea(inputValues, inputUnits)
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

    // Only for Area Calculator for now
    private fun calculateArea(values: Map<String, Double>, units: Map<String, String>): String {
        // Convert both to meters for calculation
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
        return "Area = %.4f m²".format(area)
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
                        formula = calc.optString("formula", null),
                        example = calc.optString("example", null),
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