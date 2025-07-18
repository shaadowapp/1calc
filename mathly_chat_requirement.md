<img src="https://r2cdn.perplexity.ai/pplx-full-logo-primary-dark%402x.png" class="logo" width="120"/>

# write all planning codes etc to integrate the above model with 1calculator app's mathly chat screen, md file

Below is a **complete Markdown plan and code structure** for integrating the `deepseek/deepseek-r1:free` model (via OpenRouter AI API) into the "mathly" chat screen of your `1calculator` app. This workflow includes all essential steps: UI, data flow, API integration, math query validation, and user guidance.

# Mathly Chat Integration with DeepSeek-R1 API

## **Project Scope**

Integrate the **DeepSeek-R1** OpenRouter AI model as the backend for a math-only AI assistant on the `mathly` chat screen of the `1calculator` app (Kotlin/Android Studio).

## **Workflow \& Architecture**

1. **User enters query in chat input**
2. **App validates input:** Only allows math-related expressions/questions; otherwise, shows a “math only” message.
3. **If valid:** App sends query to DeepSeek-R1 via OpenRouter API, receives AI’s math solution.
4. **Displays plain text response** in chat.
5. **Non-math queries:** Shows a static message, does not send to AI.

## **1. UI/UX Plan**

- **Input Field:** For user text input.
- **Send Button:** Triggers validation + API request.
- **Chat Display:** Shows alternating user/AI messages.
- **Error/Info Messages:** Show for rejection or network/API errors.


## **2. Dependencies**

Add to `build.gradle`:

```kotlin
implementation("com.squareup.okhttp3:okhttp:4.11.0")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
```


## **3. Math Query Validation**

```kotlin
fun isMathQuery(input: String): Boolean {
    val mathKeywords = listOf(
        "integrate", "derivative", "derive", "solve", "limit", "simplify", "factor", "expand",
        "evaluate", "differentiate", "find", "calculate", "+", "-", "*", "/", "=", "^", "log", "ln",
        "sin", "cos", "tan", "cot", "sec", "csc", "matrix", "determinant", "abs", "pi", "sqrt"
    )
    // Accept if contains math keyword or symbols
    return mathKeywords.any { input.contains(it, ignoreCase = true) }
}
```


## **4. API Client (OkHttp, Coroutines)**

```kotlin
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject

val OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions"
// Optionally: val OPENROUTER_API_KEY = "YOUR_API_KEY" (can skip for free tier with daily limit)

suspend fun sendMathQuery(query: String): String {
    val client = OkHttpClient()
    val json = JSONObject().apply {
        put("model", "deepseek/deepseek-r1:free")
        put("messages", JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("content", query)
            })
        })
    }
    val body = RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())
    val request = Request.Builder()
        .url(OPENROUTER_API_URL)
        .addHeader("Content-Type", "application/json")
        //.addHeader("Authorization", "Bearer $OPENROUTER_API_KEY") // Optional for paid/free acct
        .post(body)
        .build()

    return withContext(Dispatchers.IO) {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("API error: ${response.code}")
            val resJson = JSONObject(response.body?.string() ?: "")
            resJson
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        }
    }
}
```


## **5. Chat Submission Logic**

```kotlin
fun onSendButtonClicked() {
    val userInput = inputEditText.text.toString().trim()
    if (!isMathQuery(userInput)) {
        addChatMessage("Sorry, I can only help with math expressions and questions.", isUser = false)
        return
    }
    addChatMessage(userInput, isUser = true)
    CoroutineScope(Dispatchers.Main).launch {
        try {
            val aiReply = sendMathQuery(userInput)
            addChatMessage(aiReply, isUser = false)
        } catch (e: Exception) {
            addChatMessage("Error connecting to AI: ${e.message}", isUser = false)
        }
    }
}
```


## **6. Displaying Messages**

Assume a `RecyclerView`/adapter or simple chat bubble list; define a function:

```kotlin
fun addChatMessage(message: String, isUser: Boolean) {
    // Append message to list and update UI
    // isUser == true: user bubble; false: AI response
}
```


## **7. Example UI XML (main components only)**

```xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">
    <RecyclerView
        android:id="@+id/chatRecyclerView"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"/>
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal">
        <EditText
            android:id="@+id/inputEditText"
            android:layout_width="0dp"
            android:layout_weight="1"
            android:layout_height="wrap_content"
            android:hint="Enter a math problem"/>
        <Button
            android:id="@+id/sendButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Send"/>
    </LinearLayout>
</LinearLayout>
```


## **8. User Guidance**

- **Mathly will only help with math expressions and questions.**
- If user enters non-math query: *“Sorry, I can only help with math expressions and questions.”*
- Typical valid inputs:
    - `"integrate x^2 dx"`
    - `"solve 2x + 3 = 9"`
    - `"What is the derivative of sin(x)?"`


## **9. Error Handling**

- For connectivity or API failures:
Show: *“Error connecting to AI. Please try again.”*
- Ensure the app does NOT send input if validation fails.


## **10. Example Message Flow**

| User | Mathly AI |
| :-- | :-- |
| integrate x^2 dx | The integral of x^2 dx is (1/3)x³ + C |
| Who is Albert Einstein? | Sorry, I can only help with math expressions and questions. |

## **11. Optional: Prompt Restriction (for extra safety)**

Prepend AI prompt with:
> “You are Mathly, a math assistant. Only answer math expressions and questions. For anything else, reply with: ‘Sorry, I can only help with math expressions and questions.’”

## **12. File Organization Summary**

```
- mathlychat/
    - MathlyChatActivity.kt   // main activity logic
    - ChatMessageAdapter.kt   // RecyclerView adapter
    - ai/
        - MathAiClient.kt     // sendMathQuery function
    - utils/
        - Validation.kt       // isMathQuery function
    - res/layout/
        - activity_mathly_chat.xml
```


# **End-to-End: User Experience**

- Open mathly chat screen
- Type math query → Receive step-by-step AI answer
- Type non-math or general query → Get polite static refusal
- Reliable, fast, math-focused chat interface

*This structure ensures your chat screen is entirely math-only, user-friendly, and seamlessly powered by DeepSeek-R1 via OpenRouter.*

<div style="text-align: center">⁂</div>

[^1]: https://www.c-sharpcorner.com/article/how-to-make-a-simple-calculator-using-kotlin-in-android/

[^2]: https://www.youtube.com/watch?v=2hSHgungOKI

[^3]: https://www.geeksforgeeks.org/kotlin/how-to-make-a-scientific-calculator-android-app-using-kotlin/

[^4]: https://www.soubhagyajit.com/blogs/Build-a-Simple-Calculator-App-with-Simple-UI-in-Android-Studio-Using-Kotlin

[^5]: https://www.packtpub.com/en-us/learning/how-to-tutorials/chat-application-kotlin-node-js-javascript?srsltid=AfmBOoqp6K03areNdX14jQnyuHugcX5_LcBN0XfPJSaG0lUjKWF-HAIS

[^6]: https://dev.to/josmel/build-your-own-chatbot-in-kotlin-with-gpt-a-step-by-step-guide-27fd

[^7]: https://www.youtube.com/watch?v=ATcpiNSk6X0

[^8]: https://www.youtube.com/watch?v=Scw4t8Ag-YA

[^9]: https://techvidvan.com/tutorials/android-simple-calculator/

[^10]: https://projectgurukul.org/android-kotlin-scientific-calculator-app/

