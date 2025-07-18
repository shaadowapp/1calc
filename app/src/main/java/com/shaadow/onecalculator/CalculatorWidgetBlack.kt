package com.shaadow.onecalculator

class CalculatorWidgetBlack : CalculatorWidget() {
    override fun updateAppWidget(context: android.content.Context, appWidgetManager: android.appwidget.AppWidgetManager, appWidgetId: Int) {
        val prefs = context.getSharedPreferences("widget_prefs", android.content.Context.MODE_PRIVATE)
        val expression = prefs.getString("widget_expression", "") ?: ""
        val resultShown = prefs.getBoolean("widget_result_shown", false)
        val views = android.widget.RemoteViews(context.packageName, R.layout.widget_calculator_black)
        
        // Display result if resultShown is true, otherwise show expression or "0"
        val displayText = when {
            resultShown -> expression
            expression.isEmpty() -> "0"
            else -> expression
        }
        views.setTextViewText(R.id.widget_expression, displayText)
        
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
                views.setOnClickPendingIntent(buttonIds[i], this.getButtonIntent(context, btn))
            }
        }
        // Add backspace button support
        views.setOnClickPendingIntent(R.id.widget_btn_backspace, this.getButtonIntent(context, "⌫"))
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
} 