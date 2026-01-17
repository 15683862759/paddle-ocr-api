package com.example.paddleocr_v5_api

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.util.Base64
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OcrAccessibilityService : AccessibilityService() {

    companion object {
        @Serializable
        data class SimplifiedOcrResult(val text: String, val center: Point)

        @Serializable
        data class Point(val x: Int, val y: Int)
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    private fun logToFile(text: String) {
        try {
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (documentsDir == null) {
                return // Cannot log
            }
            if (!documentsDir.exists()) {
                documentsDir.mkdirs()
            }
            val logFile = File(documentsDir, "ocr_log.txt")
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            logFile.appendText("$timestamp: $text" + System.lineSeparator())
        } catch (e: Exception) {
            // If logging fails, there is not much we can do if not connected to PC.
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        logToFile("Accessibility Service connected.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == Constants.ACTION_PERFORM_OCR) {
            logToFile("Received OCR command from receiver.")
            takeScreenshotAndPerformOcr()
        }
        // Use START_STICKY to ensure the service is restarted if killed.
        return START_STICKY
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    fun takeScreenshotAndPerformOcr() {
        logToFile("takeScreenshotAndPerformOcr called.")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            takeScreenshotInternal()
        } else {
            val errorMessage = "Screenshot API not available on this version of Android."
            logToFile(errorMessage)
            saveTextToFile(errorMessage, true)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun takeScreenshotInternal() {
        val displayId = Display.DEFAULT_DISPLAY
        logToFile("Attempting to take screenshot on display ID: $displayId")
        takeScreenshot(
            displayId,
            mainExecutor,
            object : TakeScreenshotCallback {
                override fun onSuccess(screenshot: ScreenshotResult) {
                    logToFile("Screenshot successful on display ID: $displayId")
                    val bitmap = Bitmap.wrapHardwareBuffer(screenshot.hardwareBuffer, screenshot.colorSpace)
                    if (bitmap != null) {
                        processScreenshot(bitmap.copy(Bitmap.Config.ARGB_8888, false))
                    } else {
                        val errorMessage = "Failed to create bitmap from screenshot."
                        logToFile(errorMessage)
                        saveTextToFile(errorMessage, true)
                    }
                    screenshot.hardwareBuffer.close()
                }

                override fun onFailure(errorCode: Int) {
                    val errorMessage = "Screenshot failed with error code: $errorCode on display ID: $displayId"
                    logToFile(errorMessage)
                    saveTextToFile(errorMessage, true)
                }
            }
        )
    }

    private fun processScreenshot(bitmap: Bitmap) {
        logToFile("Processing screenshot.")
        serviceScope.launch {
            try {
                var baos: ByteArrayOutputStream? = null
                val fileData: String?
                try {
                    val originalWidth = bitmap.width
                    val originalHeight = bitmap.height

                    logToFile("Compressing bitmap with JPEG quality 95.")
                    baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, baos)
                    val imageBytes = baos.toByteArray()
                    fileData = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

                    val fileSizeInKB = imageBytes.size / 1024
                    logToFile("Original image size: ${originalWidth}x${originalHeight}. Compressed size: ${fileSizeInKB} KB.")

                    val textDetLimitSideLen = 64
                    val request = OcrRequest(
                        file = fileData,
                        fileType = 1,
                        markdownIgnoreLabels = emptyList(),
                        useDocOrientationClassify = false,
                        useDocUnwarping = false,
                        useTextlineOrientation = false,
                        textDetLimitType = "min",
                        textDetLimitSideLen = textDetLimitSideLen,
                        textDetThresh = 0.3f,
                        textDetBoxThresh = 0.6f,
                        textDetUnclipRatio = 1.5f,
                        textRecScoreThresh = 0f,
                        parseLanguage = "default"
                    )

                    logToFile("Making OCR API call with full parameters.")
                    val startTime = System.currentTimeMillis()
                    val response = OcrApiHelper.apiService.getOcrResult("token ${OcrApiHelper.TOKEN}", request)
                    val endTime = System.currentTimeMillis()
                    val duration = endTime - startTime
                    logToFile("OCR API call successful. Duration: ${duration} ms.")

                    val simplifiedResults = response.result.ocrResults.flatMap { ocrResult ->
                        try {
                            val prunedObject = ocrResult.prunedResult.jsonObject
                            val texts = prunedObject["rec_texts"]?.jsonArray
                            val boxes = prunedObject["rec_polys"]?.jsonArray

                            if (texts != null && boxes != null && texts.size == boxes.size) {
                                texts.zip(boxes).mapNotNull { (textElement, boxElement) ->
                                    try {
                                        val text = textElement.jsonPrimitive.content
                                        if (text.isNullOrBlank()) {
                                            return@mapNotNull null
                                        }
                                        val box = boxElement.jsonArray

                                        if (box.size == 4) {
                                            val centerX = (box[0].jsonArray[0].jsonPrimitive.int + box[1].jsonArray[0].jsonPrimitive.int + box[2].jsonArray[0].jsonPrimitive.int + box[3].jsonArray[0].jsonPrimitive.int) / 4
                                            val centerY = (box[0].jsonArray[1].jsonPrimitive.int + box[1].jsonArray[1].jsonPrimitive.int + box[2].jsonArray[1].jsonPrimitive.int + box[3].jsonArray[1].jsonPrimitive.int) / 4

                                            SimplifiedOcrResult(
                                                text = text,
                                                center = Point(x = centerX, y = centerY)
                                            )
                                        } else {
                                            logToFile("A box didn't have 4 points: $boxElement")
                                            null
                                        }
                                    } catch (e: Exception) {
                                        logToFile("Failed to parse one text/box pair. Text: $textElement, Box: $boxElement, Error: ${e.message}")
                                        null
                                    }
                                }
                            } else {
                                logToFile("Failed to parse prunedResult, 'rec_texts' or 'rec_polys' array missing or size mismatch. Content: ${ocrResult.prunedResult}")
                                emptyList()
                            }
                        } catch (e: Exception) {
                            logToFile("Failed to parse one ocrResult, content: ${ocrResult.prunedResult}. Error: ${e.message}")
                            emptyList()
                        }
                    }

                    if(simplifiedResults.isNotEmpty()){
                        val json = Json { prettyPrint = true }
                        val resultText = json.encodeToString(simplifiedResults)
                        saveTextToFile(resultText)
                    } else {
                        logToFile("All items failed to parse. Saving raw response.")
                        saveTextToFile(response.toString(), true)
                    }

                } catch (e: Exception) {
                    val errorMessage = "Error during OCR processing: ${e.message}"
                    logToFile(errorMessage)
                    saveTextToFile(errorMessage, true)
                } finally {
                    // 确保流被正确关闭
                    try {
                        baos?.close()
                    } catch (e: Exception) {
                        logToFile("Error closing ByteArrayOutputStream: ${e.message}")
                    }
                }

            } catch (e: Exception) {
                val errorMessage = "Unexpected error during OCR processing: ${e.message}"
                logToFile(errorMessage)
                saveTextToFile(errorMessage, true)
            }
        }
    }

    @Synchronized
    private fun saveTextToFile(text: String, isError: Boolean = false) {
        val content = if (isError) "Error: $text" else text
        try {
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (documentsDir == null) {
                logToFile("Documents directory is not available for saving result.")
                return
            }
            if (!documentsDir.exists()) {
                documentsDir.mkdirs()
            }
            val file = File(documentsDir, "ocr_result.txt")
            if (file.exists()) {
                file.delete()
            }
            file.writeText(content)
            logToFile("Successfully wrote result to ${file.absolutePath}")
        } catch (e: Exception) {
            logToFile("Failed to save result to file: ${e.message}")
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        logToFile("Accessibility Service unbound.")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
