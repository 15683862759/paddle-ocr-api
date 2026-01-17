package com.example.paddleocr_v5_api

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OcrApiService {
    @POST("ocr")
    suspend fun getOcrResult(
        @Header("Authorization") token: String,
        @Body request: OcrRequest
    ): OcrResponse
}
