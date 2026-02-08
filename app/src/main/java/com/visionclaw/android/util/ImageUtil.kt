package com.visionclaw.android.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Image conversion utilities.
 */
object ImageUtil {

    /**
     * Convert a camera Image (YUV_420_888) to JPEG bytes.
     */
    fun imageToJpeg(image: Image, quality: Int = 50): ByteArray {
        require(image.format == ImageFormat.YUV_420_888) { "Invalid image format" }

        val width = image.width
        val height = image.height

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val numPixels = (width * height * 1.5).toInt()
        val nv21 = ByteArray(numPixels)

        var idY = 0
        var idUV = width * height

        // Y plane
        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride

        if (yPixelStride == 1) {
            // Bulk copy if no gaps
            if (yRowStride == width) {
                 yBuffer.get(nv21, 0, width * height)
                 idY = width * height
            } else {
                 for (row in 0 until height) {
                     yBuffer.position(row * yRowStride)
                     yBuffer.get(nv21, idY, width)
                     idY += width
                 }
            }
        } else {
             for (row in 0 until height) {
                 for (col in 0 until width) {
                     nv21[idY++] = yBuffer.get(row * yRowStride + col * yPixelStride)
                 }
             }
        }

        // UV planes (NV21 expects V first, then U)
        val uvRowStride = uPlane.rowStride // stride is same for u and v
        val uvPixelStride = uPlane.pixelStride
        val uvWidth = width / 2
        val uvHeight = height / 2

        for (row in 0 until uvHeight) {
            for (col in 0 until uvWidth) {
                val vIndex = row * uvRowStride + col * uvPixelStride
                val uIndex = row * uvRowStride + col * uvPixelStride
                
                // V first
                nv21[idUV++] = vBuffer.get(vIndex)
                // Then U
                nv21[idUV++] = uBuffer.get(uIndex)
            }
        }

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), quality, out)
        return out.toByteArray()
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
