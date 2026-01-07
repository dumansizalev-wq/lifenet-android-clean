package net.lifenet.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File

object MediaCompressor {

    /**
     * Compresses an image to a very small thumbnail (max 128px) and returns Base64 string.
     * Targeted for mesh transmission (< 5KB).
     */
    fun compressImageToBase64(context: Context, uri: Uri): String? {
        return try {
            val input = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inSampleSize = 4 // Initial downsample
            }
            val bitmap = BitmapFactory.decodeStream(input, null, options) ?: return null
            
            // Scaled to exactly 128px max dimension
            val scale = 128f / maxOf(bitmap.width, bitmap.height).coerceAtLeast(1)
            val scaledBitmap = Bitmap.createScaledBitmap(
                bitmap, 
                (bitmap.width * scale).toInt(), 
                (bitmap.height * scale).toInt(), 
                true
            )
            
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 30, outputStream) // Aggressive compression
            val bytes = outputStream.toByteArray()
            
            Base64.encodeToString(bytes, Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Reads an audio file and converts to Base64.
     * Assumes audio is already recorded in a low-bitrate format (AMR/3GP).
     */
    fun getAudioBase64(file: File): String? {
        return try {
            val bytes = file.readBytes()
            Base64.encodeToString(bytes, Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }
}
