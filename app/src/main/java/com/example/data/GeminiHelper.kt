package com.example.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiHelper {
    private const val MODEL = "google/gemini-3.5-flash-lite"
    private const val API_URL = "https://openrouter.ai/api/v1/chat/completions"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val isDemoMode = BuildConfig.IS_DEMO_MODE

    fun cleanJsonResponse(raw: String): String {
        var cleaned = raw.trim()
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substringAfter("```json").substringBeforeLast("```").trim()
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substringAfter("```").substringBeforeLast("```").trim()
        }
        return cleaned
    }

    suspend fun generateResponse(prompt: String, systemInstruction: String): String = withContext(Dispatchers.IO) {
        if (isDemoMode) {
            return@withContext "{\"demo\": true, \"message\": \"Demo mode: AI response simulated. Prompt: $prompt\"}"
        }
        // Real path: AI key lives server-side in a Supabase Edge Function.
        // The app only calls the proxy URL (no secrets in the APK).
        val proxyUrl = BuildConfig.AI_PROXY_URL
        if (proxyUrl.isEmpty()) {
            return@withContext "{\"error\": \"AI proxy not configured. Set AI_PROXY_URL.\"}"
        }
        try {
            val requestBody = JSONObject().apply {
                put("prompt", prompt)
                put("systemInstruction", systemInstruction)
                put("model", MODEL)
            }
            val request = Request.Builder()
                .url(proxyUrl)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    return@withContext "{\"error\": \"AI request failed (${response.code}): $errBody\"}"
                }
                val body = response.body?.string() ?: return@withContext "{\"error\": \"Empty AI response.\"}"
                val content = JSONObject(body).optString("content", "")
                if (content.isEmpty()) return@withContext "{\"error\": \"AI response could not be parsed.\"}"
                return@withContext cleanJsonResponse(content)
            }
        } catch (e: Exception) {
            return@withContext "{\"error\": \"AI request failed: ${e.localizedMessage ?: "unknown"}\"}"
        }
    }

    suspend fun generateVisionResponse(
        prompt: String,
        systemInstruction: String,
        base64Image: String,
        mimeType: String = "image/jpeg"
    ): String = withContext(Dispatchers.IO) {
        if (isDemoMode) {
            return@withContext "{\"demo\": true, \"message\": \"Demo mode: AI vision response simulated. Prompt: $prompt\"}"
        }
        val proxyUrl = BuildConfig.AI_PROXY_URL
        if (proxyUrl.isEmpty()) {
            return@withContext "{\"error\": \"AI proxy not configured. Set AI_PROXY_URL.\"}"
        }
        try {
            val requestBody = JSONObject().apply {
                put("prompt", prompt)
                put("systemInstruction", systemInstruction)
                put("model", MODEL)
                put("imageBase64", base64Image)
                put("mimeType", mimeType)
            }
            val request = Request.Builder()
                .url(proxyUrl)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "{\"error\": \"AI vision error (${response.code})\"}"
                }
                val body = response.body?.string() ?: return@withContext "{\"error\": \"Empty vision response\"}"
                val content = JSONObject(body).optString("content", "")
                if (content.isEmpty()) return@withContext "{\"error\": \"Failed to parse vision response\"}"
                return@withContext cleanJsonResponse(content)
            }
        } catch (e: Exception) {
            return@withContext "{\"error\": \"Vision request failed: ${e.localizedMessage}\"}"
        }
    }

    fun encodeUriToBase64(context: Context, uri: Uri): Pair<String, String>? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
            val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            Pair(base64, mimeType)
        } catch (e: Exception) {
            android.util.Log.e("GeminiHelper", "Error encoding image: ${e.message}", e)
            null
        }
    }
}
