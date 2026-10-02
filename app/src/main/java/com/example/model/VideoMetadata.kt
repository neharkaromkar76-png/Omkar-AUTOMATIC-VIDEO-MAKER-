package com.example.model

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

data class VideoMetadata(
    val uriString: String,
    val name: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val fps: Int = 30,
    val fileSizeBytes: Long = 0L,
    val aspectRatio: String = "16:9",
    val thumbnailBitmap: Bitmap? = null
) {
    val durationFormatted: String
        get() {
            val totalSec = durationMs / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            val ms = (durationMs % 1000) / 100
            return String.format("%02d:%02d.%d", min, sec, ms)
        }

    val resolutionFormatted: String
        get() = "${width}x${height}"

    val fileSizeFormatted: String
        get() {
            if (fileSizeBytes <= 0) return "Unknown"
            val mb = fileSizeBytes.toDouble() / (1024 * 1024)
            return String.format("%.1f MB", mb)
        }

    companion object {
        fun extractFromUri(context: Context, uri: Uri, fallbackName: String = "Selected Video"): VideoMetadata {
            val retriever = MediaMetadataRetriever()
            return try {
                retriever.setDataSource(context, uri)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                
                var width = widthStr?.toIntOrNull() ?: 1920
                var height = heightStr?.toIntOrNull() ?: 1080
                val rotation = rotationStr?.toIntOrNull() ?: 0
                
                if (rotation == 90 || rotation == 270) {
                    val temp = width
                    width = height
                    height = temp
                }
                
                val duration = durationStr?.toLongOrNull() ?: 0L
                val fps = 30 // typical standard
                
                val gcd = gcd(width, height)
                val aspect = if (gcd > 0) "${width / gcd}:${height / gcd}" else "16:9"
                
                var fileSize = 0L
                try {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        fileSize = pfd.statSize
                    }
                } catch (_: Exception) {}

                val thumb = try {
                    retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                } catch (_: Exception) {
                    null
                }

                VideoMetadata(
                    uriString = uri.toString(),
                    name = fallbackName,
                    durationMs = duration,
                    width = width,
                    height = height,
                    fps = fps,
                    fileSizeBytes = fileSize,
                    aspectRatio = aspect,
                    thumbnailBitmap = thumb
                )
            } catch (e: Exception) {
                VideoMetadata(
                    uriString = uri.toString(),
                    name = fallbackName,
                    durationMs = 12000L,
                    width = 1920,
                    height = 1080,
                    fps = 30,
                    aspectRatio = "16:9"
                )
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {}
            }
        }

        private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
    }
}
