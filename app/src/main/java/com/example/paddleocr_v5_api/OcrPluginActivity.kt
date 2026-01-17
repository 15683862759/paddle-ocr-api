package com.example.paddleocr_v5_api

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class OcrPluginActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This is a simple plugin with no UI. We just need to return the
        // configuration to MacroDroid immediately.

        val resultIntent = Intent()

        // This is the text that will be displayed in the MacroDroid action list.
        val blurb = "Perform OCR via Screenshot"
        resultIntent.putExtra("com.twofortyfouram.locale.intent.extra.BLURB", blurb)

        // This is an optional bundle of data that will be sent to the FireReceiver.
        // We don't have any settings, so we can pass an empty bundle.
        val bundle = Bundle()
        resultIntent.putExtra("com.twofortyfouram.locale.intent.extra.BUNDLE", bundle)

        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}
