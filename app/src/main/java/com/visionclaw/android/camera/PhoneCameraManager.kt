package com.visionclaw.android.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.visionclaw.android.gemini.GeminiConfig
import com.visionclaw.android.util.ImageUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Captures frames from the phone's back camera using CameraX.
 *
 * Throttles to ~1fps, converts to JPEG at 50% quality.
 *
 * Implementation details:
 * - Binds ImageAnalysis use case with STRATEGY_KEEP_ONLY_LATEST
 * - Throttles frames to 1 per second
 * - Converts ImageProxy -> YUV -> JPEG bytes
 * - Emits JPEG via callback to GeminiLiveService
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
    private val executor = Executors.newSingleThreadExecutor()

    /**
     * Start camera preview and frame capture.
     *
     * @param lifecycleOwner Activity/Fragment lifecycle
     * @param surfaceProvider Optional Preview.SurfaceProvider for UI preview
     * @param onFrame Callback with JPEG bytes for each captured frame (~1fps)
     */
    fun startCapture(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider? = null,
        onFrame: (ByteArray) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                
                // ImageAnalysis
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()

                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastFrameTime >= GeminiConfig.VIDEO_FRAME_INTERVAL_MS) {
                        try {
                            val image = imageProxy.image
                            if (image != null) {
                                val jpegBytes = ImageUtil.imageToJpeg(image, GeminiConfig.VIDEO_JPEG_QUALITY)
                                onFrame(jpegBytes)
                                lastFrameTime = currentTime
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing frame: ${e.message}")
                        }
                    }
                    imageProxy.close()
                }

                // Preview
                var preview: Preview? = null
                if (surfaceProvider != null) {
                    preview = Preview.Builder().build()
                    preview.setSurfaceProvider(surfaceProvider)
                }

                // Select back camera
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider?.unbindAll()
                    
                    if (preview != null) {
                        cameraProvider?.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            imageAnalysis,
                            preview
                        )
                    } else {
                        cameraProvider?.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            imageAnalysis
                        )
                    }
                    Log.d(TAG, "CameraX started")
                } catch (exc: Exception) {
                    Log.e(TAG, "Use case binding failed", exc)
                }

            } catch (e: Exception) {
                Log.e(TAG, "CameraProvider failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stopCapture() {
        cameraProvider?.unbindAll()
        Log.d(TAG, "CameraX stopped")
    }
}
