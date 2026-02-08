package com.visionclaw.android.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import com.visionclaw.android.gemini.GeminiConfig
import com.visionclaw.android.util.ImageUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Captures frames from the phone's back camera using CameraX.
 *
 * Throttles to ~1fps, converts to JPEG at 50% quality.
 *
 * TODO: Implement CameraX capture pipeline.
 * - Bind ImageAnalysis use case
 * - Throttle frames to 1 per second
 * - Convert ImageProxy -> Bitmap -> JPEG bytes
 * - Emit JPEG via callback to GeminiLiveService
 */
@Singleton
class PhoneCameraManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "PhoneCamera"
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var lastFrameTime = 0L

    /**
     * Start camera preview and frame capture.
     *
     * @param lifecycleOwner Activity/Fragment lifecycle
     * @param onFrame Callback with JPEG bytes for each captured frame (~1fps)
     *
     * TODO: Implement:
     * 1. Get ProcessCameraProvider
     * 2. Create ImageAnalysis with STRATEGY_KEEP_ONLY_LATEST
     * 3. In analyzer: throttle to VIDEO_FRAME_INTERVAL_MS
     * 4. Convert ImageProxy to JPEG using ImageUtil
     * 5. Invoke onFrame with JPEG bytes
     */
    fun startCapture(lifecycleOwner: LifecycleOwner, onFrame: (ByteArray) -> Unit) {
        TODO("Implement CameraX frame capture — see ARCHITECTURE.md video pipeline")
    }

    fun stopCapture() {
        cameraProvider?.unbindAll()
    }
}
