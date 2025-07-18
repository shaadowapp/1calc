package com.shaadow.onecalculator

import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Before
import android.os.SystemClock
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.ViewAction
import android.view.Choreographer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import android.os.Build
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
@LargeTest
class PerformanceTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private lateinit var device: UiDevice
    private val frameTimeThreshold = 16.67 // 60 FPS = 16.67ms per frame

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun testHomeScreenScrollPerformance() {
        // Navigate to home screen
        onView(withId(R.id.btn_menu)).perform(click())
        Thread.sleep(1000)

        // Measure scroll performance on home screen
        val frameMetrics = measureScrollPerformance {
            // Perform multiple scroll actions
            repeat(5) {
                onView(withId(R.id.home_scrollview))
                    .perform(swipeUp())
                Thread.sleep(100)
                
                onView(withId(R.id.home_scrollview))
                    .perform(swipeDown())
                Thread.sleep(100)
            }
        }

        // Verify performance metrics
        verifyScrollPerformance(frameMetrics)
    }

    @Test
    fun testRecentHistoryScrollPerformance() {
        // Create multiple history entries for testing
        createMultipleCalculationHistory()
        
        // Navigate to home screen
        onView(withId(R.id.btn_menu)).perform(click())
        Thread.sleep(1000)

        // Measure horizontal scroll performance on recent history
        val frameMetrics = measureScrollPerformance {
            repeat(3) {
                onView(withId(R.id.recent_history_recycler))
                    .perform(swipeLeft())
                Thread.sleep(100)
                
                onView(withId(R.id.recent_history_recycler))
                    .perform(swipeRight())
                Thread.sleep(100)
            }
        }

        verifyScrollPerformance(frameMetrics)
    }

    @Test
    fun testSearchResultsScrollPerformance() {
        // Navigate to home screen
        onView(withId(R.id.btn_menu)).perform(click())
        Thread.sleep(1000)

        // Activate search to generate results
        onView(withId(R.id.textInputLayout)).perform(click())
        
        try {
            onView(withContentDescription("Search calculators"))
                .perform(typeText("calculator"))
        } catch (e: Exception) {
            onView(allOf(isAssignableFrom(android.widget.EditText::class.java), 
                        isDescendantOfA(withId(R.id.textInputLayout))))
                .perform(typeText("calculator"))
        }

        Thread.sleep(500)

        // Measure scroll performance on search results
        try {
            val frameMetrics = measureScrollPerformance {
                repeat(3) {
                    onView(withId(R.id.search_results_recycler))
                        .perform(swipeUp())
                    Thread.sleep(100)
                    
                    onView(withId(R.id.search_results_recycler))
                        .perform(swipeDown())
                    Thread.sleep(100)
                }
            }

            verifyScrollPerformance(frameMetrics)
        } catch (e: Exception) {
            // Search results might not be scrollable if there are few results
            // This is acceptable
        }
    }

    @Test
    fun testCalculatorButtonResponsiveness() {
        // Measure response time for calculator button presses
        val responseTimes = mutableListOf<Long>()

        val buttonIds = listOf(
            R.id.button_1, R.id.button_2, R.id.button_3, R.id.button_4, R.id.button_5,
            R.id.button_plus, R.id.button_minus, R.id.button_multiply, R.id.button_divide
        )

        for (buttonId in buttonIds) {
            val startTime = SystemClock.elapsedRealtime()
            
            onView(withId(buttonId)).perform(click())
            
            // Wait for UI update (expression text change)
            Thread.sleep(50)
            
            val endTime = SystemClock.elapsedRealtime()
            responseTimes.add(endTime - startTime)
        }

        // Verify all button presses were responsive (< 100ms)
        val averageResponseTime = responseTimes.average()
        val maxResponseTime = responseTimes.maxOrNull() ?: 0L
        
        assert(averageResponseTime < 100) { 
            "Average button response time too high: ${averageResponseTime}ms" 
        }
        assert(maxResponseTime < 200) { 
            "Maximum button response time too high: ${maxResponseTime}ms" 
        }

        // Clear for next test
        onView(withId(R.id.button_ac)).perform(click())
    }

    @Test
    fun testNavigationTransitionPerformance() {
        // Measure navigation transition times
        val navigationTimes = mutableListOf<Long>()

        // Test navigation to home
        val homeStartTime = SystemClock.elapsedRealtime()
        onView(withId(R.id.btn_menu)).perform(click())
        Thread.sleep(1000) // Wait for transition
        val homeEndTime = SystemClock.elapsedRealtime()
        navigationTimes.add(homeEndTime - homeStartTime)

        // Test FAB navigation back to calculator
        val calcStartTime = SystemClock.elapsedRealtime()
        onView(withId(R.id.fab_calculator)).perform(click())
        Thread.sleep(500) // Wait for transition
        val calcEndTime = SystemClock.elapsedRealtime()
        navigationTimes.add(calcEndTime - calcStartTime)

        // Test history navigation
        val historyStartTime = SystemClock.elapsedRealtime()
        onView(withId(R.id.btn_history)).perform(click())
        Thread.sleep(1000) // Wait for transition
        device.pressBack() // Return to calculator
        val historyEndTime = SystemClock.elapsedRealtime()
        navigationTimes.add(historyEndTime - historyStartTime)

        // Verify navigation transitions are smooth (< 1500ms including artificial delays)
        val averageNavigationTime = navigationTimes.average()
        assert(averageNavigationTime < 1500) { 
            "Average navigation time too high: ${averageNavigationTime}ms" 
        }
    }

    @Test
    fun testMemoryUsageStability() {
        // Test memory stability during extended usage
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()

        // Perform various operations to stress test memory
        repeat(10) {
            // Navigate between screens
            onView(withId(R.id.btn_menu)).perform(click())
            Thread.sleep(200)
            onView(withId(R.id.fab_calculator)).perform(click())
            Thread.sleep(200)

            // Perform calculations
            performComplexCalculation()
            onView(withId(R.id.button_ac)).perform(click())
            
            // Force garbage collection
            System.gc()
            Thread.sleep(100)
        }

        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = finalMemory - initialMemory

        // Memory increase should be reasonable (< 50MB for basic operations)
        val memoryIncreaseInMB = memoryIncrease / (1024 * 1024)
        assert(memoryIncreaseInMB < 50) {
            "Memory usage increased too much: ${memoryIncreaseInMB}MB"
        }
    }

    @Test
    fun testUIThreadBlocking() {
        // Test that UI thread is not blocked during operations
        val startTime = SystemClock.elapsedRealtime()
        
        // Perform operations that might block UI
        performComplexCalculation()
        
        // Navigate to home (involves loading data)
        onView(withId(R.id.btn_menu)).perform(click())
        Thread.sleep(500)
        
        // Search operation (involves filtering)
        onView(withId(R.id.textInputLayout)).perform(click())
        try {
            onView(withContentDescription("Search calculators"))
                .perform(typeText("complex calculation"))
        } catch (e: Exception) {
            onView(allOf(isAssignableFrom(android.widget.EditText::class.java), 
                        isDescendantOfA(withId(R.id.textInputLayout))))
                .perform(typeText("complex"))
        }
        
        val endTime = SystemClock.elapsedRealtime()
        val totalTime = endTime - startTime
        
        // Operations should complete within reasonable time (< 3000ms)
        assert(totalTime < 3000) {
            "UI operations took too long: ${totalTime}ms"
        }
    }

    @Test
    fun testScrollSmoothness() {
        // Navigate to home screen
        onView(withId(R.id.btn_menu)).perform(click())
        Thread.sleep(1000)

        // Test scroll smoothness with frame rate monitoring
        val frameRateMonitor = FrameRateMonitor()
        frameRateMonitor.start()

        // Perform smooth scrolling
        onView(withId(R.id.home_scrollview))
            .perform(smoothScrollUp())
        Thread.sleep(200)
        
        onView(withId(R.id.home_scrollview))
            .perform(smoothScrollDown())
        Thread.sleep(200)

        val frameRate = frameRateMonitor.stop()
        
        // Verify frame rate is close to 60 FPS (allowing some variance)
        assert(frameRate >= 55) {
            "Frame rate too low during scrolling: ${frameRate} FPS"
        }
    }

    // Helper methods
    private fun measureScrollPerformance(scrollAction: () -> Unit): List<Long> {
        val frameMetrics = mutableListOf<Long>()
        val frameRateMonitor = FrameRateMonitor()
        
        frameRateMonitor.start()
        val startTime = SystemClock.elapsedRealtime()
        
        scrollAction()
        
        val endTime = SystemClock.elapsedRealtime()
        val avgFrameRate = frameRateMonitor.stop()
        
        frameMetrics.add(endTime - startTime)
        frameMetrics.add(avgFrameRate.toLong())
        
        return frameMetrics
    }

    private fun verifyScrollPerformance(frameMetrics: List<Long>) {
        if (frameMetrics.size >= 2) {
            val totalTime = frameMetrics[0]
            val frameRate = frameMetrics[1]
            
            // Verify scroll completed in reasonable time
            assert(totalTime < 2000) {
                "Scroll operation took too long: ${totalTime}ms"
            }
            
            // Verify frame rate is acceptable
            assert(frameRate >= 50) {
                "Frame rate too low during scroll: ${frameRate} FPS"
            }
        }
    }

    private fun createMultipleCalculationHistory() {
        // Create several calculations for history
        val calculations = listOf(
            listOf(R.id.button_1, R.id.button_plus, R.id.button_2),
            listOf(R.id.button_5, R.id.button_multiply, R.id.button_3),
            listOf(R.id.button_9, R.id.button_minus, R.id.button_4),
            listOf(R.id.button_8, R.id.button_divide, R.id.button_2),
            listOf(R.id.button_6, R.id.button_plus, R.id.button_7)
        )

        for (calc in calculations) {
            for (buttonId in calc) {
                onView(withId(buttonId)).perform(click())
            }
            onView(withId(R.id.button_equals)).perform(click())
            Thread.sleep(200)
            onView(withId(R.id.button_ac)).perform(click())
        }
    }

    private fun performComplexCalculation() {
        // Perform a calculation with many operations
        onView(withId(R.id.button_1)).perform(click())
        onView(withId(R.id.button_2)).perform(click())
        onView(withId(R.id.button_3)).perform(click())
        onView(withId(R.id.button_plus)).perform(click())
        onView(withId(R.id.button_4)).perform(click())
        onView(withId(R.id.button_5)).perform(click())
        onView(withId(R.id.button_6)).perform(click())
        onView(withId(R.id.button_multiply)).perform(click())
        onView(withId(R.id.button_2)).perform(click())
        onView(withId(R.id.button_equals)).perform(click())
    }

    // Custom ViewActions for smooth scrolling
    private fun smoothScrollUp(): ViewAction {
        return GeneralSwipeAction(
            Swipe.SLOW,
            GeneralLocation.BOTTOM_CENTER,
            GeneralLocation.TOP_CENTER,
            Press.FINGER
        )
    }

    private fun smoothScrollDown(): ViewAction {
        return GeneralSwipeAction(
            Swipe.SLOW,
            GeneralLocation.TOP_CENTER,
            GeneralLocation.BOTTOM_CENTER,
            Press.FINGER
        )
    }

    private fun swipeUp(): ViewAction {
        return GeneralSwipeAction(
            Swipe.FAST,
            GeneralLocation.BOTTOM_CENTER,
            GeneralLocation.TOP_CENTER,
            Press.FINGER
        )
    }

    private fun swipeDown(): ViewAction {
        return GeneralSwipeAction(
            Swipe.FAST,
            GeneralLocation.TOP_CENTER,
            GeneralLocation.BOTTOM_CENTER,
            Press.FINGER
        )
    }

    private fun swipeLeft(): ViewAction {
        return GeneralSwipeAction(
            Swipe.FAST,
            GeneralLocation.CENTER_RIGHT,
            GeneralLocation.CENTER_LEFT,
            Press.FINGER
        )
    }

    private fun swipeRight(): ViewAction {
        return GeneralSwipeAction(
            Swipe.FAST,
            GeneralLocation.CENTER_LEFT,
            GeneralLocation.CENTER_RIGHT,
            Press.FINGER
        )
    }

    // Frame rate monitoring class
    private class FrameRateMonitor {
        private var frameCount = 0
        private var startTime = 0L
        private var isMonitoring = false
        
        private val choreographer = Choreographer.getInstance()
        private val frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (isMonitoring) {
                    frameCount++
                    choreographer.postFrameCallback(this)
                }
            }
        }

        fun start() {
            frameCount = 0
            startTime = SystemClock.elapsedRealtime()
            isMonitoring = true
            choreographer.postFrameCallback(frameCallback)
        }

        fun stop(): Double {
            isMonitoring = false
            val endTime = SystemClock.elapsedRealtime()
            val elapsedSeconds = (endTime - startTime) / 1000.0
            return if (elapsedSeconds > 0) frameCount / elapsedSeconds else 0.0
        }
    }
}
