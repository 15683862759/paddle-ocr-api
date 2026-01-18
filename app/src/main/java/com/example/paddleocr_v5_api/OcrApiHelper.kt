package com.example.paddleocr_v5_api

import android.content.Context
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object OcrApiHelper {

    private const val API_URL = "https://9bp24cr2r086318a.aistudio-app.com/"

    private val json = Json { ignoreUnknownKeys = true }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(API_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val apiService: OcrApiService = retrofit.create(OcrApiService::class.java)

    fun getAuthHeader(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences("ocr_tokens", Context.MODE_PRIVATE)
        val tokens = sharedPreferences.getStringSet("tokens", emptySet())?.toList() ?: emptyList()

        if (tokens.isEmpty()) {
            return null
        }

        val lastUsedIndexKey = "last_used_token_index"
        val lastIndex = sharedPreferences.getInt(lastUsedIndexKey, -1)
        val nextIndex = (lastIndex + 1) % tokens.size

        with(sharedPreferences.edit()) {
            putInt(lastUsedIndexKey, nextIndex)
            apply()
        }

        return "token ${tokens[nextIndex]}"
    }
}