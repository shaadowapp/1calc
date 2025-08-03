package com.shaadow.onecalculator.mathly.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import com.shaadow.onecalculator.BuildConfig

object MathAiClient {
    private const val OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions"

    suspend fun sendMathQuery(query: String): String {
        // Debug logging
        android.util.Log.d("MathAiClient", "=== MathAiClient Debug Start ===")
        android.util.Log.d("MathAiClient", "API Key: ${if (BuildConfig.MATHLY_API_KEY.isNotEmpty()) "Present (${BuildConfig.MATHLY_API_KEY.length} chars)" else "EMPTY!"}")
        android.util.Log.d("MathAiClient", "API Key Value: ${BuildConfig.MATHLY_API_KEY}")
        android.util.Log.d("MathAiClient", "Query: $query")

        if (BuildConfig.MATHLY_API_KEY.isEmpty()) {
            android.util.Log.e("MathAiClient", "API key is empty!")
            throw Exception("API key is not configured. Please check local.properties file.")
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        val json = JSONObject().apply {
            put("model", "deepseek/deepseek-r1")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are Mathly, a helpful math assistant. Provide clear, step-by-step solutions to math problems.")
                })
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
            .addHeader("Authorization", "Bearer ${BuildConfig.MATHLY_API_KEY}")
            .post(body)
            .build()

        android.util.Log.d("MathAiClient", "Request URL: ${request.url}")
        android.util.Log.d("MathAiClient", "Request Body: ${json.toString()}")

        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("MathAiClient", "Making HTTP request...")
                client.newCall(request).execute().use { response ->
                    android.util.Log.d("MathAiClient", "Response Code: ${response.code}")
                    android.util.Log.d("MathAiClient", "Response Message: ${response.message}")
                    android.util.Log.d("MathAiClient", "Response Headers: ${response.headers}")

                    val responseBody = response.body?.string() ?: ""
                    android.util.Log.d("MathAiClient", "Response Body: $responseBody")

                    if (!response.isSuccessful) {
                        android.util.Log.e("MathAiClient", "HTTP Error: ${response.code} - ${response.message}")
                        throw Exception("API error: ${response.code} - $responseBody")
                    }

                    if (responseBody.isEmpty()) {
                        android.util.Log.e("MathAiClient", "Empty response body")
                        throw Exception("Empty response from API")
                    }

                    try {
                        android.util.Log.d("MathAiClient", "Parsing JSON response...")
                        val resJson = JSONObject(responseBody)

                        if (!resJson.has("choices")) {
                            android.util.Log.e("MathAiClient", "No 'choices' field in response")
                            throw Exception("Invalid API response format: missing 'choices'")
                        }

                        val choices = resJson.getJSONArray("choices")
                        if (choices.length() == 0) {
                            android.util.Log.e("MathAiClient", "Empty choices array")
                            throw Exception("No choices in API response")
                        }

                        val firstChoice = choices.getJSONObject(0)
                        if (!firstChoice.has("message")) {
                            android.util.Log.e("MathAiClient", "No 'message' field in first choice")
                            throw Exception("Invalid choice format: missing 'message'")
                        }

                        val message = firstChoice.getJSONObject("message")
                        if (!message.has("content")) {
                            android.util.Log.e("MathAiClient", "No 'content' field in message")
                            throw Exception("Invalid message format: missing 'content'")
                        }

                        val content = message.getString("content")
                        android.util.Log.d("MathAiClient", "Successfully extracted content: $content")
                        android.util.Log.d("MathAiClient", "=== MathAiClient Debug End ===")
                        content

                    } catch (e: Exception) {
                        android.util.Log.e("MathAiClient", "JSON parsing error: ${e.message}", e)
                        throw Exception("Failed to parse API response: ${e.message}")
                    }
                }
            } catch (e: java.net.SocketTimeoutException) {
                android.util.Log.e("MathAiClient", "Network timeout", e)
                throw Exception("Network timeout - please check your internet connection")
            } catch (e: java.net.UnknownHostException) {
                android.util.Log.e("MathAiClient", "Unknown host", e)
                throw Exception("Cannot reach OpenRouter API - please check your internet connection")
            } catch (e: java.net.ConnectException) {
                android.util.Log.e("MathAiClient", "Connection failed", e)
                throw Exception("Connection failed - please check your internet connection")
            } catch (e: Exception) {
                android.util.Log.e("MathAiClient", "Unexpected error: ${e.message}", e)
                throw e
            }
        }
    }
} 