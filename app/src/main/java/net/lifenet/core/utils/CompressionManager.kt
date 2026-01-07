package net.lifenet.core.utils

import com.github.luben.zstd.Zstd
import android.util.Log

object CompressionManager {
    private const val TAG = "CompressionManager"
    private const val COMPRESSION_THRESHOLD = 64 // Don't compress small packets
    private const val MAGIC_BYTE: Byte = 0x5A // 'Z' for Zstd

    fun compress(data: ByteArray): ByteArray {
        if (data.size < COMPRESSION_THRESHOLD) return data

        return try {
            val compressed = Zstd.compress(data)
            // Add magic byte and original size for verification if needed
            val result = ByteArray(compressed.size + 1)
            result[0] = MAGIC_BYTE
            System.arraycopy(compressed, 0, result, 1, compressed.size)
            
            val ratio = (100f * (1f - (result.size.toFloat() / data.size.toFloat()))).toInt()
            Log.d(TAG, "Compressed: ${data.size} -> ${result.size} bytes (-$ratio%)")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Compression failed", e)
            data
        }
    }

    fun decompress(data: ByteArray): ByteArray {
        if (data.isEmpty() || data[0] != MAGIC_BYTE) return data

        return try {
            val compressedBody = ByteArray(data.size - 1)
            System.arraycopy(data, 1, compressedBody, 0, compressedBody.size)
            
            // Note: In a production app, you might want to store the original size
            // but Zstd.decompress bound is usually sufficient or use streaming.
            // For simple strings, we can estimate or use a large enough buffer.
            val decompressed = Zstd.decompress(compressedBody, 1024 * 64) // 64KB max for mess msgs
            
            // Trim to actual size if return value is smaller (Zstd returns long size)
            // But Zstd.decompress(byte[], long) returns byte[].
            decompressed
        } catch (e: Exception) {
            Log.e(TAG, "Decompression failed", e)
            data
        }
    }
}
