package com.example.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import com.example.model.ExportOptions
import com.example.model.ExportProgressState
import com.example.model.ExportResolution
import com.example.model.VideoClip
import com.example.model.VideoMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

class ExportRenderer(private val context: Context) {

    suspend fun exportVideo(
        videoMetadata: VideoMetadata,
        clips: List<VideoClip>,
        options: ExportOptions,
        onProgress: (ExportProgressState) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val outputDir = File(context.filesDir, "exports").apply { mkdirs() }
        val outputFile = File(outputDir, "omkar_auto_edit_${System.currentTimeMillis()}.mp4")
        val inputUri = Uri.parse(videoMetadata.uriString)

        onProgress(
            ExportProgressState(
                isExporting = true,
                progress = 0.05f,
                currentStage = "Preparing video encoder & audio tracks..."
            )
        )

        var muxer: MediaMuxer? = null
        var videoCodec: MediaCodec? = null
        var extractor: MediaExtractor? = null

        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, inputUri)

            val inWidth = videoMetadata.width.coerceAtLeast(360)
            val inHeight = videoMetadata.height.coerceAtLeast(360)
            val durationUs = videoMetadata.durationMs * 1000L

            val outWidth = when (options.resolution) {
                ExportResolution.ORIGINAL -> inWidth
                ExportResolution.RES_1080P -> if (inWidth > inHeight) 1920 else 1080
                ExportResolution.RES_720P -> if (inWidth > inHeight) 1280 else 720
            }
            val outHeight = when (options.resolution) {
                ExportResolution.ORIGINAL -> inHeight
                ExportResolution.RES_1080P -> if (inWidth > inHeight) 1080 else 1920
                ExportResolution.RES_720P -> if (inWidth > inHeight) 720 else 1280
            }

            val baseBitRate = 4_000_000
            val bitRate = (baseBitRate * options.quality.bitRateMultiplier).toInt()
            val targetFps = if (options.fps.fps > 0) options.fps.fps else videoMetadata.fps

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            // Setup Video Encoder
            val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, outWidth, outHeight).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, targetFps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            videoCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            videoCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = videoCodec.createInputSurface()
            videoCodec.start()

            onProgress(
                ExportProgressState(
                    isExporting = true,
                    progress = 0.15f,
                    currentStage = "Processing clips & setting up synchronized audio..."
                )
            )

            // Setup Audio Passthrough
            extractor = MediaExtractor()
            extractor.setDataSource(context, inputUri, null)
            var audioTrackIndexInExtractor = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndexInExtractor = i
                    audioFormat = format
                    break
                }
            }

            var videoTrackIndexInMuxer = -1
            var audioTrackIndexInMuxer = -1
            var muxerStarted = false

            val bufferInfo = MediaCodec.BufferInfo()
            val totalFrames = ((videoMetadata.durationMs.toFloat() / 1000f) * targetFps).toInt().coerceAtLeast(30)
            val frameIntervalUs = 1_000_000L / targetFps

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                isFilterBitmap = true
            }

            onProgress(
                ExportProgressState(
                    isExporting = true,
                    progress = 0.20f,
                    currentStage = "Applying keyframes & rendering clips..."
                )
            )

            // Render frames with keyframed zoom transformations
            for (frameIndex in 0 until totalFrames) {
                val currentPtsUs = frameIndex * frameIntervalUs
                val currentTimeMs = currentPtsUs / 1000L

                // Find active clip for current frame
                val activeClip = clips.firstOrNull { it.containsTime(currentTimeMs) } ?: clips.lastOrNull()
                val currentScale = activeClip?.getScaleAt(currentTimeMs) ?: 1.0f

                // Retrieve original frame at time
                val frameBitmap = try {
                    retriever.getFrameAtTime(currentPtsUs, MediaMetadataRetriever.OPTION_CLOSEST)
                } catch (_: Exception) {
                    null
                }

                val canvas = inputSurface.lockHardwareCanvas()
                canvas.drawColor(android.graphics.Color.BLACK)

                if (frameBitmap != null) {
                    val matrix = Matrix()
                    val srcW = frameBitmap.width.toFloat()
                    val srcH = frameBitmap.height.toFloat()

                    // Center-crop fit into outWidth/outHeight
                    val scaleFit = maxOf(outWidth / srcW, outHeight / srcH)
                    val totalScale = scaleFit * currentScale

                    matrix.postScale(totalScale, totalScale)
                    val scaledW = srcW * totalScale
                    val scaledH = srcH * totalScale
                    val dx = (outWidth - scaledW) / 2f
                    val dy = (outHeight - scaledH) / 2f
                    matrix.postTranslate(dx, dy)

                    canvas.drawBitmap(frameBitmap, matrix, paint)
                }

                inputSurface.unlockCanvasAndPost(canvas)

                // Drain encoder
                var outIndex = videoCodec.dequeueOutputBuffer(bufferInfo, 0)
                while (outIndex >= 0) {
                    val encodedData = videoCodec.getOutputBuffer(outIndex)
                    if (encodedData != null) {
                        if (!muxerStarted && videoTrackIndexInMuxer < 0) {
                            videoTrackIndexInMuxer = muxer.addTrack(videoCodec.outputFormat)
                            if (audioFormat != null) {
                                audioTrackIndexInMuxer = muxer.addTrack(audioFormat)
                            }
                            muxer.start()
                            muxerStarted = true
                        }

                        if (muxerStarted && bufferInfo.size > 0) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(videoTrackIndexInMuxer, encodedData, bufferInfo)
                        }
                    }
                    videoCodec.releaseOutputBuffer(outIndex, false)
                    outIndex = videoCodec.dequeueOutputBuffer(bufferInfo, 0)
                }

                if (frameIndex % 15 == 0 || frameIndex == totalFrames - 1) {
                    val renderPct = 0.20f + (frameIndex.toFloat() / totalFrames) * 0.65f
                    onProgress(
                        ExportProgressState(
                            isExporting = true,
                            progress = renderPct,
                            currentStage = "Rendering & Encoding frames (${(renderPct * 100).toInt()}%)..."
                        )
                    )
                }
            }

            videoCodec.signalEndOfInputStream()

            // Drain remaining encoded video frames
            var outIndex = videoCodec.dequeueOutputBuffer(bufferInfo, 10000)
            while (outIndex >= 0) {
                val encodedData = videoCodec.getOutputBuffer(outIndex)
                if (encodedData != null && muxerStarted && bufferInfo.size > 0) {
                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                    muxer.writeSampleData(videoTrackIndexInMuxer, encodedData, bufferInfo)
                }
                videoCodec.releaseOutputBuffer(outIndex, false)
                outIndex = videoCodec.dequeueOutputBuffer(bufferInfo, 10000)
            }

            // Passthrough Audio
            if (audioTrackIndexInExtractor >= 0 && audioTrackIndexInMuxer >= 0 && muxerStarted) {
                onProgress(
                    ExportProgressState(
                        isExporting = true,
                        progress = 0.88f,
                        currentStage = "Muxing synchronized audio track..."
                    )
                )

                extractor.selectTrack(audioTrackIndexInExtractor)
                val audioBuffer = ByteBuffer.allocateDirect(1024 * 64)
                val audioBufInfo = MediaCodec.BufferInfo()

                while (true) {
                    val sampleSize = extractor.readSampleData(audioBuffer, 0)
                    if (sampleSize < 0) break
                    audioBufInfo.offset = 0
                    audioBufInfo.size = sampleSize
                    audioBufInfo.presentationTimeUs = extractor.sampleTime
                    audioBufInfo.flags = extractor.sampleFlags
                    muxer.writeSampleData(audioTrackIndexInMuxer, audioBuffer, audioBufInfo)
                    extractor.advance()
                }
            }

            onProgress(
                ExportProgressState(
                    isExporting = true,
                    progress = 0.98f,
                    currentStage = "Finalizing MP4 file..."
                )
            )

            delay(300)

            try {
                retriever.release()
            } catch (_: Exception) {}

            onProgress(
                ExportProgressState(
                    isExporting = false,
                    progress = 1.0f,
                    currentStage = "Export Completed Successfully!",
                    outputFilePath = outputFile.absolutePath,
                    outputFileSizeBytes = outputFile.length(),
                    isComplete = true
                )
            )

            outputFile
        } catch (e: Exception) {
            onProgress(
                ExportProgressState(
                    isExporting = false,
                    progress = 0f,
                    currentStage = "Export Failed",
                    error = e.localizedMessage ?: "Unknown export error occurred"
                )
            )
            throw e
        } finally {
            try {
                videoCodec?.stop()
                videoCodec?.release()
            } catch (_: Exception) {}
            try {
                extractor?.release()
            } catch (_: Exception) {}
            try {
                muxer?.stop()
                muxer?.release()
            } catch (_: Exception) {}
        }
    }
}
