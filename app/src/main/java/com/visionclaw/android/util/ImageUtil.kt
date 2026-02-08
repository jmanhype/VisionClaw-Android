package com.visionclaw.android.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.media.Image
import java.io.ByteArrayOutputStream

/**
 * Image conversion utilities.
 *
 * TODO: Implement image format conversions:
 * - ImageProxy (YUV_420_888) -> Bitmap -> JPEG bytes
 * - Resize to reasonable dimensions before JPEG encoding
 * - Use configurable JPEG quality (default 50%)
 */
object ImageUtil {

    /**
     * Convert a camera Image (YUV_420_888) to JPEG bytes.
     *
     * TODO: Implement YUV -> JPEG conversion
     */
    fun imageToJpeg(image: Image, quality: Int = 50): ByteArray {
        TODO("Convert YUV_420_888 Image to JPEG bytes")
    }

    /**
     * Convert Bitmap to JPEG bytes.
     */
    fun bitmapToJpeg(bitmap: Bitmap, quality: Int = 50): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }
}
