package com.example.paddleocr_v5_api

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.paddleocr_v5_api.ui.theme.PaddleOCRV5APITheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val REQUEST_WRITE_EXTERNAL_STORAGE = 1

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logToFile("MainActivity onCreate.")

        // Request storage permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            logToFile("Requesting storage permission.")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_WRITE_EXTERNAL_STORAGE
            )
        }

        setContent {
            PaddleOCRV5APITheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val isServiceEnabled = isAccessibilityServiceEnabled(this)
                    logToFile("Accessibility service enabled: $isServiceEnabled")
                    if (isServiceEnabled) {
                        ServiceRunningScreen()
                    } else {
                        AccessibilitySettingsScreen { openAccessibilitySettings() }
                    }
                }
            }
        }
    }

    private fun openAccessibilitySettings() {
        logToFile("Opening accessibility settings.")
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        logToFile("Checking accessibility service status.")
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val myService = ComponentName(context, OcrAccessibilityService::class.java)

        if (enabledServices.isEmpty()) {
            logToFile("No accessibility services are enabled.")
        } else {
            logToFile("Enabled accessibility services:")
            enabledServices.forEach { service ->
                logToFile("  - ID: ${service.id}, ComponentName: ${ComponentName.unflattenFromString(service.id)}")
            }
        }
        logToFile("My service ComponentName: $myService")

        val isEnabled = enabledServices.any { 
            val serviceComponent = ComponentName.unflattenFromString(it.id)
            serviceComponent == myService
        }
        logToFile("Service with expected ComponentName found: $isEnabled")
        return isEnabled
    }
}

@Composable
fun AccessibilitySettingsScreen(onButtonClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Please enable the accessibility service for this app to allow screen capture.",
            textAlign = TextAlign.Center
        )
        Button(onClick = onButtonClick, modifier = Modifier.padding(top = 16.dp)) {
            Text("Open Accessibility Settings")
        }
    }
}

@Composable
fun ServiceRunningScreen() {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Accessibility Service is running correctly.",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = "You can now test the OCR action directly.",
            modifier = Modifier.padding(top = 8.dp),
            textAlign = TextAlign.Center
        )
        Button(
            onClick = {
                // Manually send the broadcast to test the FireReceiver
                val intent = Intent(Constants.ACTION_FIRE_SETTING)
                intent.setPackage(context.packageName) // Important for targeting the broadcast
                context.sendBroadcast(intent)
            },
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text("Manual Trigger Test")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AccessibilitySettingsScreenPreview() {
    PaddleOCRV5APITheme {
        AccessibilitySettingsScreen {}
    }
}

@Preview(showBackground = true)
@Composable
fun ServiceRunningScreenPreview() {
    PaddleOCRV5APITheme {
        ServiceRunningScreen()
    }
}
