# Firebase Integration Guide

This document explains how Firebase services are integrated into the One Calculator app.

## Services Integrated

### 1. Firebase Cloud Messaging (FCM)
- **Purpose**: Send push notifications to users
- **Location**: `FirebaseMessagingService.kt`
- **Features**:
  - Automatic token registration
  - Notification display with custom channels
  - Background message handling

### 2. Firebase Crashlytics
- **Purpose**: Crash reporting and error tracking
- **Location**: `CalculatorApplication.kt` (initialization)
- **Features**:
  - Automatic crash reporting
  - Exception tracking in calculations
  - Custom error logging

### 3. Firebase Analytics
- **Purpose**: User behavior tracking and analytics
- **Location**:
  - `CalculatorApplication.kt` (initialization)
  - `AnalyticsHelper.kt` (utility class)
  - `BasicActivity.kt` (usage examples)
- **Features**:
  - Screen view tracking
  - Calculation event tracking
  - Custom event logging

## Usage Examples

### Analytics Tracking

```kotlin
// Initialize in Activity
private lateinit var analyticsHelper: AnalyticsHelper

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    analyticsHelper = AnalyticsHelper(this)

    // Track screen view
    analyticsHelper.logScreenView("Calculator", "BasicActivity")

    // Track custom events
    analyticsHelper.logCalculation("2+2", "4")
    analyticsHelper.logFeatureUsed("scientific_calculator")
    analyticsHelper.logUserAction("button_pressed", "equals")
}
```

### Crash Reporting

```kotlin
try {
    // Your code that might crash
    riskyOperation()
} catch (e: Exception) {
    // Report to Crashlytics
    FirebaseCrashlytics.getInstance().recordException(e)

    // Also track in Analytics
    analyticsHelper.logError("operation_failed", e.message ?: "Unknown error")
}
```

### Custom Crash for Testing

```kotlin
// Test Crashlytics (remove in production)
throw RuntimeException("Test crash for Crashlytics")
```

## Configuration

### Dependencies Added
```kotlin
// In build.gradle.kts
plugins {
    alias(libs.plugins.google.firebase.crashlytics)
}

dependencies {
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)
}
```

### Initialization
Firebase services are automatically initialized in `CalculatorApplication.onCreate()`:

```kotlin
// Crashlytics
FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

// Analytics
FirebaseAnalytics.getInstance(this)
```

## Firebase Console Setup

1. **Create Firebase Project**: Go to [Firebase Console](https://console.firebase.google.com)
2. **Add Android App**: Register your app with package name `com.shaadow.onecalculator`
3. **Download google-services.json**: Place in `app/` directory
4. **Enable Services**:
   - Cloud Messaging (for FCM)
   - Crashlytics (for crash reporting)
   - Analytics (for user tracking)

## Testing

### FCM Testing
- Send test notifications from Firebase Console
- Check Android logs for token registration
- Verify notifications appear on device

### Crashlytics Testing
- Use `testCrashlytics()` method to trigger test crash
- Check Firebase Console for crash reports
- Verify error details are captured

### Analytics Testing
- Perform actions in app (calculations, navigation)
- Check Firebase Console Analytics dashboard
- Verify events are being tracked

## Best Practices

1. **Privacy**: Respect user privacy and GDPR requirements
2. **Data Collection**: Only collect necessary data
3. **Error Handling**: Use try-catch blocks around risky operations
4. **Custom Events**: Use meaningful event names and parameters
5. **Testing**: Test all Firebase services before production release

## Troubleshooting

### FCM Issues
- Check `google-services.json` is present and correct
- Verify Google Play Services is installed on device
- Check notification permissions (Android 13+)

### Crashlytics Issues
- Ensure Crashlytics is enabled in Firebase Console
- Check internet connection for crash uploads
- Wait 24-48 hours for crash reports to appear

### Analytics Issues
- Verify Analytics is enabled in Firebase Console
- Check device has internet connection
- Allow 24-48 hours for data to appear in dashboard

## Production Considerations

- **Remove test code**: Remove `testCrashlytics()` and any debug logging
- **Privacy policy**: Update app privacy policy to include Firebase data collection
- **Consent**: Consider implementing user consent for analytics
- **Data retention**: Configure data retention settings in Firebase Console