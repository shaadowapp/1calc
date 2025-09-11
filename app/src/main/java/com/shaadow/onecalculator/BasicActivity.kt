package com.shaadow.onecalculator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.shaadow.onecalculator.parser.Expression
import com.shaadow.onecalculator.parser.Expression.insertImplicitMultiplication
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.EditText
import android.widget.Toast
import android.widget.LinearLayout
import android.widget.ImageView
import android.content.Intent
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.FrameLayout
import android.view.Gravity
import kotlin.math.abs
import android.util.Log
import com.shaadow.onecalculator.utils.AnalyticsHelper
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.shaadow.onecalculator.MediaGalleryActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.text.InputFilter

class BasicActivity : AppCompatActivity() {
    private var isResultShown = false
    private lateinit var analyticsHelper: AnalyticsHelper

    private lateinit var expressionTv: EditText
    private lateinit var solutionTv: TextView
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_basic)

        // Show popup if in landscape (should not happen, but as a fallback)
        if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            android.widget.Toast.makeText(this, "Landscape mode is not supported.", android.widget.Toast.LENGTH_LONG).show()
        }

        expressionTv = findViewById(R.id.expression_tv)
        solutionTv = findViewById(R.id.solution_tv)

        Log.d("Gesture", "GestureDetector initialized")
        gestureDetector = GestureDetector(this, object : SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                Log.d("Gesture", "onFling called: e1=$e1, e2=$e2")
                Log.d("Gesture", "Velocities: velocityX=$velocityX, velocityY=$velocityY")
                if (e1 == null) {
                    Log.d("Gesture", "e1 is null, returning false")
                    return false
                }
                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x
                Log.d("Gesture", "Differences: diffY=$diffY, diffX=$diffX")
                Log.d("Gesture", "Abs values: abs(diffY)=${abs(diffY)}, abs(diffX)=${abs(diffX)}")

                val isVertical = abs(diffY) > abs(diffX)
                val isDown = diffY > 10
                val isFast = abs(velocityY) > 50

                Log.d("Gesture", "Conditions: isVertical=$isVertical, isDown=$isDown, isFast=$isFast")

                if (isVertical && isDown && isFast) {
                    Log.d("Gesture", "Swipe down detected, opening history")
                    startActivity(Intent(this@BasicActivity, HistoryActivity::class.java))
                    return true
                } else {
                    Log.d("Gesture", "Conditions not met for swipe down")
                }
                return false
            }
        })


        // Set input filter to restrict to digits and operators
        val allowedChars = "0123456789+-×÷%.()√^!πe"
        val filter = InputFilter { source, start, end, dest, dstart, dend ->
            for (i in start until end) {
                if (source[i] !in allowedChars) {
                    return@InputFilter ""
                }
            }
            null
        }
        expressionTv.filters = arrayOf(filter)

        // Initialize Analytics
        analyticsHelper = AnalyticsHelper(this)
        analyticsHelper.logScreenView("Basic Calculator", "BasicActivity")

        // Prevent soft keyboard from showing up
        expressionTv.showSoftInputOnFocus = false

        // Hide keyboard when EditText gets focus
        expressionTv.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                hideSoftKeyboard()
            }
        }

        // Dynamically adjust expression text size
        expressionTv.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                adjustExpressionTextSize()
                updateSolutionVisibility()
                // Don't adjust solution text size when showing calculation result
                if (!isResultShown) {
                    adjustSolutionTextSize()
                }
            }
        })

        // Handle incoming intent with expression and result
        handleIncomingIntent()

        val buttons = listOf(
            R.id.button_0, R.id.button_1, R.id.button_2, R.id.button_3, R.id.button_4,
            R.id.button_5, R.id.button_6, R.id.button_7, R.id.button_8, R.id.button_9,
            R.id.button_dual_zero, R.id.button_plus, R.id.button_minus, R.id.button_multiply, R.id.button_divide,
            R.id.button_dot, R.id.button_percent, R.id.button_brackets,
            R.id.button_sqrt, R.id.button_power, R.id.button_factorial,
            R.id.button_pi, R.id.button_e
        )

        for (id in buttons) {
            val button = findViewById<MaterialButton>(id)
            button.setOnClickListener {
                val input = when (id) {
                    R.id.button_multiply -> "×"
                    R.id.button_divide -> "÷"
                    R.id.button_plus -> "+"
                    R.id.button_minus -> "-"
                    R.id.button_percent -> "%"
                    R.id.button_sqrt -> "√"
                    R.id.button_power -> "^"
                    R.id.button_factorial -> "!"
                    R.id.button_pi -> "π"
                    R.id.button_e -> "e"
                    R.id.button_dot -> "."
                    R.id.button_dual_zero -> "00"
                    R.id.button_brackets -> getNextBracket(expressionTv.text.toString())
                    else -> button.text.toString()
                }

                if (isResultShown) {
                    expressionTv.setText(solutionTv.text.toString().removeSuffix(".0"))
                    isResultShown = false
                }

                expressionTv.visibility = View.VISIBLE
                adjustSolutionTextSize()
                appendToExpression(input)

                val expressionToEvaluate = getExpressionForCalculation()
                if (isExpressionComplete(expressionToEvaluate)) {
                    try {
                        val result = safeCalculate(expressionToEvaluate)
                        val formattedResult = formatNumberWithCommas(doubleToStringWithoutScientificNotation(result))
                        solutionTv.text = formattedResult
                        adjustSolutionTextSize()
                    } catch (_: Exception) {
                        solutionTv.text = ""
                        adjustSolutionTextSize()
                    }
                } else {
                    solutionTv.text = ""
                    adjustSolutionTextSize()
                }
            }
        }

        findViewById<MaterialButton>(R.id.button_ac).setOnClickListener {
            expressionTv.setText("")
            solutionTv.text = "0"
            expressionTv.visibility = View.VISIBLE
            solutionTv.visibility = View.VISIBLE
            solutionTv.textSize = 50f
            adjustSolutionTextSize()
            isResultShown = false
        }

        findViewById<MaterialButton>(R.id.button_backspace).setOnClickListener {
            val text = expressionTv.text.toString()
            val selectionStart = expressionTv.selectionStart
            val selectionEnd = expressionTv.selectionEnd

            if (text.isNotEmpty()) {
                if (selectionStart == selectionEnd) {
                    // No text selected, delete character before cursor
                    if (selectionStart > 0) {
                        val beforeCursor = text.substring(0, selectionStart - 1)
                        val afterCursor = text.substring(selectionStart)
                        expressionTv.setText(beforeCursor + afterCursor)
                        expressionTv.setSelection(selectionStart - 1)
                    }
                } else {
                    // Text is selected, delete selection
                    val beforeSelection = text.substring(0, selectionStart)
                    val afterSelection = text.substring(selectionEnd)
                    expressionTv.setText(beforeSelection + afterSelection)
                    expressionTv.setSelection(selectionStart)
                }

                val expressionToEvaluate = getExpressionForCalculation()
                if (isExpressionComplete(expressionToEvaluate)) {
                    try {
                        val result = safeCalculate(expressionToEvaluate)
                        val formattedResult = formatNumberWithCommas(doubleToStringWithoutScientificNotation(result))
                        solutionTv.text = formattedResult
                        adjustSolutionTextSize()
                    } catch (_: Exception) {
                        solutionTv.text = ""
                        adjustSolutionTextSize()
                    }
                } else {
                    solutionTv.text = ""
                    adjustSolutionTextSize()
                }
            }
        }

        val equalsButton = findViewById<MaterialButton>(R.id.button_equals)

        // Handle normal click (short press)
        equalsButton.setOnClickListener {
            val expression = expressionTv.text.toString()

            // Check if it's a single digit for potential shortcut
            if (isSingleDigitShortcut(expression)) {
                // For single digits, we could show a hint or just proceed with calculation
                // For now, proceed with normal calculation
            }

            val formattedExpression = getExpressionForCalculation()
            try {
                val result = safeCalculate(formattedExpression)
                // Solution already has the correct result from live calculation
                solutionTv.visibility = View.VISIBLE
                isResultShown = true
                // If expression ends without operator (complete), hide expression
                if (isExpressionComplete(expression)) {
                    expressionTv.visibility = View.GONE
                    expressionTv.setText("")
                }
                // Save to Room DB
                val expr = expression
                val res = doubleToStringWithoutScientificNotation(result)
                lifecycleScope.launch {
                    val db = HistoryDatabase.getInstance(this@BasicActivity)
                    db.historyDao().insert(HistoryEntity(expression = expr, result = res))
                }

                // Track calculation in Analytics
                analyticsHelper.logCalculation(expr, res)
            } catch (e: Exception) {
                // Track error in Crashlytics
                FirebaseCrashlytics.getInstance().recordException(e)
                analyticsHelper.logError("calculation_error", e.message ?: "Unknown calculation error")

                solutionTv.text = getString(R.string.error_text)
                solutionTv.visibility = View.VISIBLE
                isResultShown = true
            }
        }

        // Handle long press (4 seconds) for gallery shortcut
        equalsButton.setOnLongClickListener {
            val expression = expressionTv.text.toString()

            if (isSingleDigitShortcut(expression)) {
                val position = expression.toInt()
                // Clear the calculator input immediately when shortcut is triggered
                expressionTv.setText("")
                solutionTv.text = ""
                solutionTv.visibility = View.VISIBLE
                isResultShown = false
                startGalleryShortcutFlow(position)
                return@setOnLongClickListener true // Consume the long click
            }

            false // Don't consume if not a shortcut
        }

        // Use the included custom_toolbar in the layout
        val toolbarInclude = findViewById<LinearLayout>(R.id.custom_toolbar)
        toolbarInclude.visibility = View.GONE

        // Set up click listeners for custom toolbar buttons
        val btnCut = toolbarInclude.findViewById<MaterialButton>(R.id.btnCut)
        val btnCopy = toolbarInclude.findViewById<MaterialButton>(R.id.btnCopy)
        val btnPaste = toolbarInclude.findViewById<MaterialButton>(R.id.btnPaste)

        // Dynamically inflate the single-copy toolbar when needed
        var copyToolbar: LinearLayout? = null

        // Helper to show toolbar below a view, right-aligned
        fun showToolbarBelow(view: View, toolbar: View) {
            toolbar.post {
                val location = IntArray(2)
                view.getLocationOnScreen(location)
                val parentLocation = IntArray(2)
                (findViewById<View>(android.R.id.content)).getLocationOnScreen(parentLocation)
                val y = location[1] - parentLocation[1] + view.height + dpToPx(4)
                val parentWidth = (findViewById<View>(android.R.id.content)).width
                val x = parentWidth - (location[0] - parentLocation[0] + view.width)

                val parent = toolbar.parent
                if (parent is RelativeLayout) {
                    val params = toolbar.layoutParams as RelativeLayout.LayoutParams
                    params.topMargin = y
                    params.marginEnd = x
                    params.addRule(RelativeLayout.ALIGN_PARENT_END)
                    toolbar.layoutParams = params
                } else if (parent is FrameLayout) {
                    val params = toolbar.layoutParams as? FrameLayout.LayoutParams ?: FrameLayout.LayoutParams(toolbar.width, toolbar.height)
                    params.topMargin = y
                    params.marginEnd = x
                    params.gravity = Gravity.END or Gravity.TOP
                    toolbar.layoutParams = params
                }
                toolbar.visibility = View.VISIBLE
            }
        }

        // Track which text view triggered the toolbox
        var activeToolboxTextView: View? = null

        // Expression EditText: show 3-button toolbar on long press
        val inputField = findViewById<EditText>(R.id.expression_tv)

        // Disable default text selection toolbar
        inputField.customSelectionActionModeCallback = object : android.view.ActionMode.Callback {
            override fun onCreateActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?): Boolean = false
            override fun onPrepareActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?): Boolean = false
            override fun onActionItemClicked(mode: android.view.ActionMode?, item: android.view.MenuItem?): Boolean = false
            override fun onDestroyActionMode(mode: android.view.ActionMode?) {}
        }

        inputField.setOnLongClickListener {
            // Select all text
            expressionTv.selectAll()

            // Hide any other toolbar
            copyToolbar?.visibility = View.GONE
            showToolbarBelow(inputField, toolbarInclude)
            activeToolboxTextView = inputField

            // Show/hide buttons and dividers based on expression content
            val isExpressionEmpty = expressionTv.text.isNullOrEmpty()
            btnCut.visibility = if (isExpressionEmpty) View.GONE else View.VISIBLE
            // Hide/show first divider (between cut and copy)
            (toolbarInclude.getChildAt(1) as View).visibility = if (isExpressionEmpty) View.GONE else View.VISIBLE
            btnCopy.visibility = if (isExpressionEmpty) View.GONE else View.VISIBLE
            // Hide/show second divider (between copy and paste)
            (toolbarInclude.getChildAt(3) as View).visibility = if (isExpressionEmpty) View.GONE else View.VISIBLE
            btnPaste.visibility = View.VISIBLE

            true
        }

        btnCut.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = expressionTv.text
            val clip = ClipData.newPlainText("Cut Text", text)
            clipboard.setPrimaryClip(clip)
            expressionTv.setText("")
            toolbarInclude.visibility = View.GONE
        }

        btnCopy.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = expressionTv.text
            val clip = ClipData.newPlainText("Copied Text", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied!", Toast.LENGTH_SHORT).show()
            toolbarInclude.visibility = View.GONE
        }

        btnPaste.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val paste = clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString() ?: ""
            val allowedChars = "0123456789+-×÷%.()√^!πe"
            val filteredPaste = paste.filter { it in allowedChars }
            val start = expressionTv.selectionStart
            val end = expressionTv.selectionEnd
            val text = expressionTv.text.toString()
            val newText = text.substring(0, start) + filteredPaste + text.substring(end)
            expressionTv.setText(newText)
            expressionTv.setSelection(start + filteredPaste.length)
            toolbarInclude.visibility = View.GONE
        }

        // Disable default text selection toolbar for solution TextView
        solutionTv.customSelectionActionModeCallback = object : android.view.ActionMode.Callback {
            override fun onCreateActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?): Boolean = false
            override fun onPrepareActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?): Boolean = false
            override fun onActionItemClicked(mode: android.view.ActionMode?, item: android.view.MenuItem?): Boolean = false
            override fun onDestroyActionMode(mode: android.view.ActionMode?) {}
        }

        // Solution TextView: show single-copy toolbar on long press
        solutionTv.setOnLongClickListener {
            toolbarInclude.visibility = View.GONE
            // Inflate if not already
            if (copyToolbar == null) {
                copyToolbar = layoutInflater.inflate(R.layout.custom_toolbar_copy, findViewById<ViewGroup>(android.R.id.content), false) as LinearLayout
                (findViewById<ViewGroup>(android.R.id.content) as ViewGroup).addView(copyToolbar)
                val btnCopy = copyToolbar!!.findViewById<Button>(R.id.btnCopy)
                btnCopy.setOnClickListener {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("text", solutionTv.text.toString()))
                    copyToolbar?.visibility = View.GONE
                }
            }
            showToolbarBelow(solutionTv, copyToolbar!!)
            activeToolboxTextView = solutionTv
            true
        }

        // Hide all toolbars when clicking outside both the toolbox and the active text view
        val rootView = window.decorView
        var isTrackingGesture = false
        rootView.setOnTouchListener { v, event ->
            Log.d("Gesture", "Received event: ${event.action} at (${event.x}, ${event.y})")
            val handled = gestureDetector.onTouchEvent(event)
            Log.d("Gesture", "onTouchEvent handled: $handled for event: ${event.action}")

            if (handled) {
                isTrackingGesture = true
                return@setOnTouchListener true
            }

            if (isTrackingGesture) {
                gestureDetector.onTouchEvent(event)
                if (event.action == MotionEvent.ACTION_UP) {
                    isTrackingGesture = false
                }
                return@setOnTouchListener true
            }

            val isToolbarVisible = toolbarInclude.visibility == View.VISIBLE || (copyToolbar?.visibility == View.VISIBLE)
            if (isToolbarVisible) {
                val toolbox = if (toolbarInclude.visibility == View.VISIBLE) toolbarInclude else copyToolbar
                val toolboxLocation = IntArray(2)
                toolbox?.getLocationOnScreen(toolboxLocation)
                val toolboxLeft = toolboxLocation[0]
                val toolboxTop = toolboxLocation[1]
                val toolboxRight = toolboxLeft + (toolbox?.width ?: 0)
                val toolboxBottom = toolboxTop + (toolbox?.height ?: 0)

                val textView = activeToolboxTextView
                val textViewLocation = IntArray(2)
                textView?.getLocationOnScreen(textViewLocation)
                val textViewLeft = textViewLocation[0]
                val textViewTop = textViewLocation[1]
                val textViewRight = textViewLeft + (textView?.width ?: 0)
                val textViewBottom = textViewTop + (textView?.height ?: 0)

                val x = event.rawX.toInt()
                val y = event.rawY.toInt()
                val outsideToolbox = x < toolboxLeft || x > toolboxRight || y < toolboxTop || y > toolboxBottom
                val outsideTextView = x < textViewLeft || x > textViewRight || y < textViewTop || y > textViewBottom

                if (outsideToolbox && outsideTextView) {
                    toolbarInclude.visibility = View.GONE
                    copyToolbar?.visibility = View.GONE
                    activeToolboxTextView = null
                }
            }
            false
        }

        findViewById<android.widget.ImageButton>(R.id.btn_menu).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }
        findViewById<TextView>(R.id.btn_history).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        // Toggle button for scientific functions row
        val toggleButton = findViewById<ImageView>(R.id.button_toggle_row1)
        val row1Layout = findViewById<LinearLayout>(R.id.row1_layout)
        // Set initial rotation to up arrow since scientific functions are hidden by default
        toggleButton.rotation = 180f
        toggleButton.setOnClickListener {
            if (row1Layout.visibility == View.VISIBLE) {
                row1Layout.visibility = View.GONE
                toggleButton.rotation = 180f // Point up when hidden
            } else {
                row1Layout.visibility = View.VISIBLE
                toggleButton.rotation = 0f // Point down when shown
            }
        }

        // Remove gestureDetector initialization and outputArea.setOnTouchListener
        // Removed click listeners for btn_settings and btn_hot_apps since those icons are no longer in the layout
        // findViewById<ImageView>(R.id.btn_settings)?.setOnClickListener { v ->
        //     val popup = android.widget.PopupMenu(this, v)
        //     popup.menu.add(0, 1, 0, "History")
        //     popup.menu.add(0, 2, 1, "Settings")
        //     popup.setOnMenuItemClickListener { item ->
        //         when (item.itemId) {
        //             1 -> {
        //                 val intent = Intent(this, HistoryActivity::class.java)
        //                 startActivity(intent)
        //                 true
        //             }
        //             2 -> {
        //                 val intent = Intent(this, SettingsActivity::class.java)
        //                 startActivity(intent)
        //                 true
        //             }
        //             else -> false
        //         }
        //     }
        //     popup.show()
        // }
        // findViewById<ImageView>(R.id.btn_hot_apps)?.setOnClickListener {
        //     // TODO: Implement Hot Apps action
        //     Toast.makeText(this, "Hot Apps clicked", Toast.LENGTH_SHORT).show()
        // }
    }

    // --- Helper methods migrated from old MainActivity ---

    private fun hideSoftKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(expressionTv.windowToken, 0)
    }

    private fun adjustExpressionTextSize() {
        val length = expressionTv.text.length
        expressionTv.textSize = when {
            length > 20 -> 22f
            length > 12 -> 28f
            else -> 36f
        }
    }

    private fun updateSolutionVisibility() {
        // Don't hide solution when showing calculation result
        if (!isResultShown) {
            solutionTv.visibility = if (expressionTv.text.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
        }
    }

    private fun getNextBracket(expr: String): String {
        val open = expr.count { it == '(' }
        val close = expr.count { it == ')' }
        return if (open > close) ")" else "("
    }

    private fun safeCalculate(expr: String): Double {
        return Expression.calculate(expr)
    }

    private fun formatNumberWithCommas(number: String): String {
        return try {
            if (number.contains(".")) {
                val parts = number.split(".")
                val intPart = parts[0].toLongOrNull()?.let { java.text.NumberFormat.getInstance().format(it) } ?: parts[0]
                "$intPart.${parts[1]}"
            } else {
                number.toLongOrNull()?.let { java.text.NumberFormat.getInstance().format(it) } ?: number
            }
        } catch (e: Exception) {
            number
        }
    }

    private fun doubleToStringWithoutScientificNotation(d: Double): String {
        return if (d % 1.0 == 0.0) {
            d.toLong().toString()
        } else {
            java.math.BigDecimal(d).stripTrailingZeros().toPlainString()
        }
    }

    private fun showCustomPopup(view: View, isExpression: Boolean) {
        val popup = PopupWindow(this)
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.HORIZONTAL
        layout.setPadding(0, 0, 0, 0)
        layout.setBackgroundColor(android.graphics.Color.parseColor("#23232A"))

        // Set parent background to #121212, rounded corners, and vertical padding
        val bgDrawable = android.graphics.drawable.GradientDrawable()
        bgDrawable.setColor(android.graphics.Color.parseColor("#121212"))
        bgDrawable.cornerRadii = floatArrayOf(18f, 18f, 18f, 18f, 18f, 18f, 18f, 18f)
        layout.background = bgDrawable
        layout.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
        layout.minimumHeight = dpToPx(48)

        // Copy button
        val copyBtn = MaterialButton(this)
        copyBtn.text = "Copy"
        // Remove all background, border, and shadow customizations from buttons
        copyBtn.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        copyBtn.strokeWidth = 0
        copyBtn.elevation = 0f
        copyBtn.textSize = 14f
        copyBtn.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT)
        copyBtn.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = if (isExpression) expressionTv.text else solutionTv.text
            val clip = ClipData.newPlainText("Copied Text", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied!", Toast.LENGTH_SHORT).show()
            popup.dismiss()
        }
        layout.addView(copyBtn)

        // Divider function
        fun addDivider() {
            val divider = View(this)
            val params = LinearLayout.LayoutParams(1, LinearLayout.LayoutParams.MATCH_PARENT)
            params.setMargins(0, 8, 0, 8)
            divider.layoutParams = params
            divider.setBackgroundColor(getColor(android.R.color.darker_gray))
            layout.addView(divider)
        }

        // Paste button (only if editable)
        if (isExpression) {
            addDivider()
            val pasteBtn = MaterialButton(this)
            pasteBtn.text = "Paste"
            pasteBtn.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            pasteBtn.strokeWidth = 0
            pasteBtn.elevation = 0f
            pasteBtn.textSize = 14f
            pasteBtn.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT)
            pasteBtn.setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val paste = clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString() ?: ""
                val allowedChars = "0123456789+-×÷%.()√^!πe"
                val filteredPaste = paste.filter { it in allowedChars }
                val start = expressionTv.selectionStart
                val end = expressionTv.selectionEnd
                val text = expressionTv.text.toString()
                val newText = text.substring(0, start) + filteredPaste + text.substring(end)
                expressionTv.setText(newText)
                expressionTv.setSelection(start + filteredPaste.length)
                popup.dismiss()
            }
            layout.addView(pasteBtn)
        }

        // Cut button (only if editable)
        if (isExpression) {
            addDivider()
            val cutBtn = MaterialButton(this)
            cutBtn.text = "Cut"
            cutBtn.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            cutBtn.strokeWidth = 0
            cutBtn.elevation = 0f
            cutBtn.textSize = 14f
            cutBtn.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT)
            cutBtn.setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val text = expressionTv.text
                val clip = ClipData.newPlainText("Cut Text", text)
                clipboard.setPrimaryClip(clip)
                expressionTv.setText("")
                popup.dismiss()
            }
            layout.addView(cutBtn)
        }

        layout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        popup.contentView = layout
        popup.isFocusable = true
        popup.width = LinearLayout.LayoutParams.WRAP_CONTENT
        popup.height = LinearLayout.LayoutParams.WRAP_CONTENT
        popup.setBackgroundDrawable(null)
        popup.elevation = 0f
        // Position the popup so its right edge aligns with the right edge of the selected view, just below it
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val xOffset = view.width - layout.measuredWidth
        popup.showAsDropDown(view, xOffset, 0)
    }

    private fun handleIncomingIntent() {
        val expr = intent.getStringExtra("expression")
        val res = intent.getStringExtra("result")
        if (!expr.isNullOrEmpty()) {
            expressionTv.setText(expr)
        }
        if (!res.isNullOrEmpty()) {
            solutionTv.text = res
        }
    }

    private fun appendToExpression(input: String) {
        val start = expressionTv.selectionStart
        val end = expressionTv.selectionEnd
        val text = expressionTv.text.toString()
        val newText = text.substring(0, start) + input + text.substring(end)
        expressionTv.setText(newText)
        expressionTv.setSelection(start + input.length)
    }

    private fun getExpressionForCalculation(): String {
        return expressionTv.text.toString().replace(",", "")
    }

    private fun isExpressionComplete(expr: String): Boolean {
        if (expr.isEmpty()) return false
        val open = expr.count { it == '(' }
        val close = expr.count { it == ')' }
        if (open != close) return false
        val last = expr.lastOrNull() ?: return false
        return last.isDigit() || last == ')' || last == '!' || last == 'π' || last == 'e'
    }

    private fun adjustSolutionTextSize() {
        val length = solutionTv.text.length
        if (isResultShown) {
            solutionTv.textSize = when {
                length > 18 -> 35f
                length > 10 -> 45f
                else -> 60f
            }
        } else {
            solutionTv.textSize = when {
                length > 18 -> 28f
                length > 10 -> 38f
                else -> 50f
            }
        }
    }

    // Helper to convert dp to px
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    // Test method for Crashlytics (remove in production)
    private fun testCrashlytics() {
        // This will cause a test crash to verify Crashlytics is working
        throw RuntimeException("Test Crash for Crashlytics")
    }

    // Gallery shortcut helper methods
    private fun isSingleDigitShortcut(expression: String): Boolean {
        // Only allow pure single digits (0-9) without any mathematical operators
        if (expression.length != 1) return false
        if (!expression[0].isDigit() || expression[0] !in '0'..'9') return false

        // Check if expression contains any mathematical operators that would indicate a calculation
        val hasOperators = expression.contains(Regex("[+\\-×÷^%!√πe\\(\\)]"))
        return !hasOperators
    }

    private fun startGalleryShortcutFlow(position: Int) {
        lifecycleScope.launch {
            try {
                // Special case: position 0 opens the main gallery screen
                if (position == 0) {
                    val intent = Intent(this@BasicActivity, MediaGalleryActivity::class.java).apply {
                        putExtra("is_shortcut_access", true)
                        putExtra("shortcut_position", 0)
                    }
                    startActivity(intent)
                    return@launch
                }

                // For positions 1-9, open specific folders
                // Get database instance
                val db = HistoryDatabase.getInstance(this@BasicActivity)
                val folderDao = db.encryptedFolderDao()

                // Get all folders ordered by creation time (position 1 = first folder, 2 = second, etc.)
                val folders = withContext(Dispatchers.IO) {
                    folderDao.getAllFoldersSync()
                }

                // Adjust position for 1-based indexing (position 1 = folders[0], position 2 = folders[1], etc.)
                val folderIndex = position - 1

                if (folderIndex >= folders.size) {
                    Toast.makeText(this@BasicActivity, "No folder at position $position", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val targetFolder = folders[folderIndex]

                // Start gallery activity with shortcut intent
                val intent = Intent(this@BasicActivity, MediaGalleryActivity::class.java).apply {
                    putExtra("shortcut_folder_id", targetFolder.id)
                    putExtra("shortcut_position", position)
                    putExtra("is_shortcut_access", true)
                }

                startActivity(intent)

            } catch (e: Exception) {
                android.util.Log.e("BasicActivity", "Error starting gallery shortcut", e)
                Toast.makeText(this@BasicActivity, "Error accessing gallery", Toast.LENGTH_SHORT).show()
            }
        }
    }
}