package com.visionclaw.android.camera

import android.util.Log
import com.visionclaw.android.gemini.GeminiConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Captures video frames from Meta Ray-Ban glasses via DAT SDK.
 *
 * Requires: com.meta.wearable:mwdat-camera dependency.
 *
 * TODO: Implement when Meta DAT SDK is integrated.
 * - Initialize Wearables SDK
 * - Subscribe to videoFramePublisher
 * - Throttle to ~1fps
 * - Convert frames to JPEG
 * - Emit via callback
 *
 * For now, use PhoneCameraManager for development/testing.
 * The DAT SDK also provides a MockDevice for testing without hardware.
 */
@Singleton
class GlassesCameraManager @Inject constructor() {
    companion object {
        private const val TAG = "GlassesCamera"
    }

    fun startStreaming(onFrame: (ByteArray) -> Unit) {
        TODO("Implement Meta DAT SDK video streaming — requires mwdat-camera dependency")
    }

    fun stopStreaming() {
        TODO("Stop DAT SDK streaming")
    }
}
