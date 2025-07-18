package com.shaadow.onecalculator

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.shaadow.onecalculator.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

open class CalculatorWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action?.startsWith(ACTION_BUTTON_PREFIX) == true) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, this::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (appWidgetId in appWidgetIds) {
                handleButtonPress(context, appWidgetManager, appWidgetId, intent.action!!)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        // Clear widget state when widget is removed
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    open fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val expression = prefs.getString(KEY_EXPRESSION, "") ?: ""
        val resultShown = prefs.getBoolean(KEY_RESULT_SHOWN, false)
        val views = RemoteViews(context.packageName, R.layout.widget_calculator_black)
        
        // Display result if resultShown is true, otherwise show expression or "0"
        val displayText = when {
            resultShown -> expression
            expression.isEmpty() -> "0"
            else -> expression
        }
        
        // Debug: Let's see what we're displaying
        android.util.Log.d("WidgetDebug", "UpdateWidget - Expression: '$expression', ResultShown: $resultShown, DisplayText: '$displayText'")
        
        views.setTextViewText(R.id.widget_expression, displayText)

        // Set up button click handlers
        val buttons = listOf(
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "+", "-", "×", "÷", "=", "C"
        )
        val buttonIds = listOf(
            R.id.widget_btn_0, R.id.widget_btn_1, R.id.widget_btn_2, R.id.widget_btn_3, R.id.widget_btn_4,
            R.id.widget_btn_5, R.id.widget_btn_6, R.id.widget_btn_7, R.id.widget_btn_8, R.id.widget_btn_9,
            R.id.widget_btn_dot, R.id.widget_btn_plus, R.id.widget_btn_minus, R.id.widget_btn_multiply, R.id.widget_btn_divide, R.id.widget_btn_equals
        )
        for ((i, btn) in buttons.withIndex()) {
            if (i < buttonIds.size) {
                views.setOnClickPendingIntent(buttonIds[i], getButtonIntent(context, btn))
            }
        }
        // Add backspace button support
        views.setOnClickPendingIntent(R.id.widget_btn_backspace, getButtonIntent(context, "⌫"))
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    open fun getButtonIntent(context: Context, button: String): PendingIntent {
        val intent = Intent(context, this::class.java).apply {
            action = ACTION_BUTTON_PREFIX + button
        }
        return PendingIntent.getBroadcast(context, button.hashCode() + this::class.java.name.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
    }

    fun handleButtonPress(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, action: String) {
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        var expression = prefs.getString(KEY_EXPRESSION, "") ?: ""
        var resultShown = prefs.getBoolean(KEY_RESULT_SHOWN, false)
        val button = action.removePrefix(ACTION_BUTTON_PREFIX)
        
        if (resultShown && button != "=" && button != "C") {
            expression = ""
            resultShown = false
        }
        
        when (button) {
            in "0".."9", ".", "+", "-", "×", "÷" -> {
                // Validate input before adding
                if (isValidInput(expression, button)) {
                    expression += button
                }
            }
            "C" -> {
                expression = ""
                resultShown = false
            }
            "=" -> {
                if (expression.isNotEmpty() && !expression.endsWith("+") && !expression.endsWith("-") && 
                    !expression.endsWith("×") && !expression.endsWith("÷")) {
                    val originalExpression = expression
                    val result = safeCalculate(expression)
                    // Debug: Let's see what we're getting
                    android.util.Log.d("WidgetDebug", "Expression: '$originalExpression', Result: '$result'")
                    // Save to history in background
                    CoroutineScope(Dispatchers.IO).launch {
                        saveToHistory(context, originalExpression, result)
                    }
                    expression = result
                    resultShown = true
                    android.util.Log.d("WidgetDebug", "After calculation - Expression: '$expression', ResultShown: $resultShown")
                }
            }
            "⌫" -> {
                if (expression.isNotEmpty()) {
                    expression = expression.dropLast(1)
                }
            }
        }
        prefs.edit().putString(KEY_EXPRESSION, expression).putBoolean(KEY_RESULT_SHOWN, resultShown).apply()
        
        // Update widget immediately
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    private fun isValidInput(currentExpression: String, newValue: String): Boolean {
        // Define operators
        val operators = setOf("+", "-", "×", "÷")
        
        // Check for consecutive operators
        val consecutiveOperators = listOf("++", "--", "××", "÷÷")
        for (consecutive in consecutiveOperators) {
            if ((currentExpression + newValue).contains(consecutive)) {
                return false
            }
        }
        
        // Check for operator at the beginning (except minus for negative numbers and dot for decimals)
        if (currentExpression.isEmpty() && newValue in operators && newValue != "-" && newValue != ".") {
            return false
        }
        
        // Check for operator after another operator (except minus after other operators for negative numbers)
        if (currentExpression.isNotEmpty()) {
            val lastChar = currentExpression.last().toString()
            if (lastChar in operators && newValue in operators) {
                // Allow minus after other operators (for negative numbers)
                if (newValue != "-") {
                    return false
                }
            }
        }
        
        return true
    }

    private fun safeCalculate(expr: String): String {
        return try {
            val cleanExpr = expr.replace("×", "*").replace("÷", "/")
            // Use simple calculation instead of Expression.kt for debugging
            val result = simpleCalculate(cleanExpr)
            if (result % 1.0 == 0.0) result.toLong().toString() else result.toString()
        } catch (e: Exception) {
            "Error"
        }
    }

    private fun simpleCalculate(expr: String): Double {
        // Simple calculation for basic operations: +, -, *, /
        val tokens = expr.replace(" ", "").split(Regex("(?<=[-+*/])|(?=[-+*/])"))
        if (tokens.size < 3) return 0.0
        
        var result = tokens[0].toDoubleOrNull() ?: return 0.0
        var i = 1
        
        while (i < tokens.size - 1) {
            val operator = tokens[i]
            val nextNumber = tokens[i + 1].toDoubleOrNull() ?: return 0.0
            
            result = when (operator) {
                "+" -> result + nextNumber
                "-" -> result - nextNumber
                "*" -> result * nextNumber
                "/" -> {
                    if (nextNumber == 0.0) return 0.0
                    result / nextNumber
                }
                else -> return 0.0
            }
            i += 2
        }
        
        return result
    }

    private suspend fun saveToHistory(context: Context, expression: String, result: String) {
        if (expression.isBlank() || result == "Error") return
        try {
            val db = HistoryDatabase.getInstance(context)
            val recent = db.historyDao().getRecentHistory()
            if (recent.isNotEmpty() && recent[0].expression == expression && recent[0].result == result && recent[0].source == "Widget") {
                return
            }
            // Save the original expression and calculated result properly
            db.historyDao().insert(HistoryEntity(expression = expression, result = result, source = "Widget"))
        } catch (e: Exception) {
            android.util.Log.e("WidgetDebug", "Error saving to history: ${e.message}")
        }
    }

    companion object {
        private const val ACTION_BUTTON_PREFIX = "com.shaadow.onecalculator.widget.BUTTON_"
        private const val KEY_EXPRESSION = "widget_expression"
        private const val KEY_RESULT_SHOWN = "widget_result_shown"
    }
} 