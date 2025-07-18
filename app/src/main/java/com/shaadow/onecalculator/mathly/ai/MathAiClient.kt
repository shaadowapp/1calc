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
            .addHeader("Authorization", "Bearer ${BuildConfig.MATHLY_API_KEY}")
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
} 