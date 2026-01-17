package com.example.paddleocr_v5_api

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OcrFireReceiver : BroadcastReceiver() {

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
            logFile.appendText("$timestamp: $text\n")
        } catch (e: Exception) {
            // If logging fails, there is not much we can do if not connected to PC.
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        logToFile("OcrFireReceiver onReceive triggered.")
        if (context == null || intent == null) {
            logToFile("Context or intent is null.")
            return
        }

        logToFile("Action: ${intent.action}")
        if (Constants.ACTION_FIRE_SETTING == intent.action) {
            logToFile("Received FIRE_SETTING action, sending command to service.")

            // Create an explicit intent to start the service and pass a command.
            val serviceIntent = Intent(context, OcrAccessibilityService::class.java)
            serviceIntent.action = Constants.ACTION_PERFORM_OCR

            // Starting the service ensures it's running. If it's already running,
            // onStartCommand will be called with the new intent.
            context.startService(serviceIntent)
            logToFile("startService called for OcrAccessibilityService.")
        }
    }
}
