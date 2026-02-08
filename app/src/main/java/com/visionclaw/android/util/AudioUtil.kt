package com.visionclaw.android.util

import android.util.Base64

/**
 * Audio format utilities.
 */
object AudioUtil {

    /** Encode raw PCM bytes to base64 string for Gemini WebSocket. */
    fun encodeToBase64(pcmData: ByteArray): String {
        return Base64.encodeToString(pcmData, Base64.NO_WRAP)
    }

    /** Decode base64 string from Gemini WebSocket to raw PCM bytes. */
    fun decodeFromBase64(base64Data: String): ByteArray {
        return Base64.decode(base64Data, Base64.DEFAULT)
    }
}
