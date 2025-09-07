package com.shaadow.onecalculator.utils

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

class AnalyticsHelper(private val context: Context) {

    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)

    fun logEvent(eventName: String, params: Bundle? = null) {
        firebaseAnalytics.logEvent(eventName, params)
    }

    fun logScreenView(screenName: String, screenClass: String? = null) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass ?: screenName)
        }
        logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    fun logCalculation(operation: String, result: String) {
        val bundle = Bundle().apply {
            putString("operation", operation)
            putString("result", result)
        }
        logEvent("calculation_performed", bundle)
    }

    fun logFeatureUsed(featureName: String) {
        val bundle = Bundle().apply {
            putString("feature_name", featureName)
        }
        logEvent("feature_used", bundle)
    }

    fun logError(errorType: String, errorMessage: String) {
        val bundle = Bundle().apply {
            putString("error_type", errorType)
            putString("error_message", errorMessage)
        }
        logEvent("app_error", bundle)
    }

    fun logUserAction(action: String, details: String? = null) {
        val bundle = Bundle().apply {
            putString("action", action)
            details?.let { putString("details", it) }
        }
        logEvent("user_action", bundle)
    }

    companion object {
        // Predefined event names for consistency
        const val EVENT_CALCULATION = "calculation_performed"
        const val EVENT_FEATURE_USED = "feature_used"
        const val EVENT_ERROR = "app_error"
        const val EVENT_USER_ACTION = "user_action"
        const val EVENT_SCREEN_VIEW = FirebaseAnalytics.Event.SCREEN_VIEW
    }
}