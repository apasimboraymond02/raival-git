package com.example.data

import com.example.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object SupabaseConfig {
    val SUPABASE_URL: String = BuildConfig.SUPABASE_URL
    val SUPABASE_ANON_KEY: String = BuildConfig.SUPABASE_ANON_KEY

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    val client: io.github.jan.supabase.SupabaseClient by lazy {
        if (SUPABASE_URL.isEmpty() || SUPABASE_ANON_KEY.isEmpty()) {
            throw IllegalStateException("Supabase config missing: Check .env file and BuildConfig")
        }
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }
    }

    suspend fun signUpWithEmail(email: String, password: String, username: String): String? = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("email", email)
                put("password", password)
                put("data", JSONObject().put("username", username))
            }
            val request = Request.Builder()
                .url("$SUPABASE_URL/auth/v1/signup")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()
            if (response.isSuccessful && responseBody != null) {
                val json = JSONObject(responseBody)
                json.optString("id", null)
            } else {
                android.util.Log.e("SupabaseAuth", "signUp failed")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseAuth", "signUp failed", e)
            null
        }
    }

    suspend fun signInWithEmail(email: String, password: String): String? = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("email", email)
                put("password", password)
            }
            val request = Request.Builder()
                .url("$SUPABASE_URL/auth/v1/token?grant_type=password")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()
            if (response.isSuccessful && responseBody != null) {
                val json = JSONObject(responseBody)
                val user = json.optJSONObject("user")
                user?.optString("id", null)
            } else {
                android.util.Log.e("SupabaseAuth", "signIn failed")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseAuth", "signIn failed", e)
            null
        }
    }

    /**
     * Real anonymous sign-in via GoTrue. Returns the auth UID, or null when
     * offline / when anonymous sign-ins are disabled in the Supabase dashboard
     * (Authentication > Sign In > Anonymous Sign-Ins must be enabled).
     */
    suspend fun signInAnonymously(username: String): String? = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("data", JSONObject().put("username", username))
            }
            val request = Request.Builder()
                .url("$SUPABASE_URL/auth/v1/signup")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()
            if (response.isSuccessful && responseBody != null) {
                val json = JSONObject(responseBody)
                val user = json.optJSONObject("user")
                (user?.optString("id", null) ?: json.optString("id", null))
                    .takeIf { !it.isNullOrEmpty() }
            } else {
                android.util.Log.e("SupabaseAuth", "anonymous signIn failed: ${response.code}")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseAuth", "anonymous signIn failed", e)
            null
        }
    }

    suspend fun signOut() {
        try {
            val request = Request.Builder()
                .url("$SUPABASE_URL/auth/v1/logout")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .post("".toRequestBody("application/json".toMediaType()))
                .build()
            httpClient.newCall(request).execute()
        } catch (_: Exception) {}
    }
}
