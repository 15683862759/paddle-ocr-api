package com.example.paddleocr_v5_api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// Data classes for the new simplified JSON output format
@Serializable
data class SimplifiedOcrResult(
    val text: String,
    val center: Point
)

@Serializable
data class Point(
    val x: Int,
    val y: Int
)

// Data classes for parsing the original server response
@Serializable
data class OcrRequest(
    val file: String,
    @SerialName("fileType")
    val fileType: Int,
    @SerialName("markdownIgnoreLabels")
    val markdownIgnoreLabels: List<String>,
    @SerialName("useDocOrientationClassify")
    val useDocOrientationClassify: Boolean,
    @SerialName("useDocUnwarping")
    val useDocUnwarping: Boolean,
    @SerialName("useTextlineOrientation")
    val useTextlineOrientation: Boolean,
    @SerialName("textDetLimitType")
    val textDetLimitType: String,
    @SerialName("textDetLimitSideLen")
    val textDetLimitSideLen: Int,
    @SerialName("textDetThresh")
    val textDetThresh: Float,
    @SerialName("textDetBoxThresh")
    val textDetBoxThresh: Float,
    @SerialName("textDetUnclipRatio")
    val textDetUnclipRatio: Float,
    @SerialName("textRecScoreThresh")
    val textRecScoreThresh: Float,
    @SerialName("parseLanguage")
    val parseLanguage: String
)

@Serializable
data class OcrResponse(
    val result: OcrResultData
)

@Serializable
data class OcrResultData(
    @SerialName("ocrResults")
    val ocrResults: List<OcrResult>
)

@Serializable
data class OcrResult(
    // Reverted to JsonElement to robustly handle any object structure from the server
    @SerialName("prunedResult")
    val prunedResult: JsonElement,
    @SerialName("ocrImage")
    val ocrImage: String
)
