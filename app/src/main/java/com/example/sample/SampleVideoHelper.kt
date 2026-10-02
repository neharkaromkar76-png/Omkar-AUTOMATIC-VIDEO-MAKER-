package com.example.sample

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

object SampleVideoHelper {

    private const val SAMPLE_FILE_NAME = "omkar_talking_head_demo.mp4"

    suspend fun getOrCreateSampleVideo(context: Context): Uri = withContext(Dispatchers.IO) {
        val sampleFile = File(context.filesDir, SAMPLE_FILE_NAME)
        if (sampleFile.exists() && sampleFile.length() > 20000) {
            return@withContext Uri.fromFile(sampleFile)
        }

        try {
            generateSyntheticTalkingHeadVideo(sampleFile)
            Uri.fromFile(sampleFile)
        } catch (e: Exception) {
            // Fallback: If hardware encoder fails on emulator, create minimal mp4 or placeholder
            createFallbackSampleFile(context, sampleFile)
            Uri.fromFile(sampleFile)
        }
    }

    private fun generateSyntheticTalkingHeadVideo(outputFile: File) {
        val width = 720
        val height = 1280 // 9:16 portrait talking-head reel
        val frameRate = 30
        val durationSeconds = 16
        val totalFrames = frameRate * durationSeconds
        val bitRate = 2_500_000

        var muxer: MediaMuxer? = null
        var videoCodec: MediaCodec? = null
        var audioCodec: MediaCodec? = null

        try {
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            // Setup Video Format
            val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            videoCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            videoCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = videoCodec.createInputSurface()
            videoCodec.start()

            // Setup Audio Format (AAC)
            val audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 1).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, 64000)
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            }
            audioCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            audioCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            audioCodec.start()

            var videoTrackIndex = -1
            var audioTrackIndex = -1
            var muxerStarted = false

            // Draw video frames
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 34f
                textAlign = Paint.Align.CENTER
            }
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(0, 229, 255)
                textSize = 42f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }

            val bufferInfo = MediaCodec.BufferInfo()

            for (frame in 0 until totalFrames) {
                val timeMs = (frame * 1000L) / frameRate
                val canvas = inputSurface.lockHardwareCanvas()

                // Background gradient
                canvas.drawColor(Color.rgb(15, 23, 42))

                // Avatar / Speaker circle with pulse
                val cx = width / 2f
                val cy = height * 0.40f
                val isSpeaking = isSpeakerActive(timeMs)
                val pulse = if (isSpeaking) (sin(frame * 0.4).toFloat() * 12f) else 0f

                paint.color = Color.rgb(30, 41, 59)
                canvas.drawCircle(cx, cy, 140f + pulse, paint)

                paint.color = if (isSpeaking) Color.rgb(0, 229, 255) else Color.rgb(100, 116, 139)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 8f
                canvas.drawCircle(cx, cy, 140f + pulse, paint)
                paint.style = Paint.Style.FILL

                // Avatar face
                paint.color = Color.rgb(255, 179, 0)
                canvas.drawCircle(cx, cy - 20, 60f, paint) // head
                val bodyRect = RectF(cx - 90, cy + 30, cx + 90, cy + 140)
                canvas.drawRoundRect(bodyRect, 30f, 30f, paint)

                // Current Sentence text overlay
                val sentence = getSentenceForTime(timeMs)
                canvas.drawText("OMKAR AUTO VIDEO MAKER", cx, 120f, titlePaint)
                canvas.drawText("Reference Sample: Talking Head", cx, 175f, textPaint)

                // Speech bubble card
                val bubbleRect = RectF(60f, height * 0.65f, width - 60f, height * 0.85f)
                paint.color = Color.rgb(26, 34, 56)
                canvas.drawRoundRect(bubbleRect, 24f, 24f, paint)
                paint.color = Color.rgb(0, 229, 255)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                canvas.drawRoundRect(bubbleRect, 24f, 24f, paint)
                paint.style = Paint.Style.FILL

                // Text inside bubble
                textPaint.textSize = 28f
                textPaint.color = Color.rgb(148, 163, 184)
                canvas.drawText("SPOKEN UTTERANCE:", cx, height * 0.70f, textPaint)
                textPaint.textSize = 32f
                textPaint.color = Color.WHITE
                canvas.drawText(sentence, cx, height * 0.76f, textPaint)

                // Time counter
                textPaint.textSize = 26f
                textPaint.color = Color.rgb(255, 179, 0)
                val timeStr = String.format("Playhead: %02d:%05.2fs", timeMs / 60000, (timeMs % 60000) / 1000f)
                canvas.drawText(timeStr, cx, height * 0.82f, textPaint)

                inputSurface.unlockCanvasAndPost(canvas)

                // Drain video encoder
                var outIndex = videoCodec.dequeueOutputBuffer(bufferInfo, 0)
                while (outIndex >= 0) {
                    val encodedData = videoCodec.getOutputBuffer(outIndex)
                    if (encodedData != null) {
                        if (!muxerStarted && videoTrackIndex < 0) {
                            videoTrackIndex = muxer.addTrack(videoCodec.outputFormat)
                            audioTrackIndex = muxer.addTrack(audioCodec.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                        if (muxerStarted && bufferInfo.size > 0) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                        }
                    }
                    videoCodec.releaseOutputBuffer(outIndex, false)
                    outIndex = videoCodec.dequeueOutputBuffer(bufferInfo, 0)
                }
            }

            videoCodec.signalEndOfInputStream()

            // Drain remaining video
            var outIndex = videoCodec.dequeueOutputBuffer(bufferInfo, 10000)
            while (outIndex >= 0) {
                val encodedData = videoCodec.getOutputBuffer(outIndex)
                if (encodedData != null && muxerStarted && bufferInfo.size > 0) {
                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                    muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                }
                videoCodec.releaseOutputBuffer(outIndex, false)
                outIndex = videoCodec.dequeueOutputBuffer(bufferInfo, 10000)
            }

            // Encode audio sine waves with pauses matching speech
            encodeAudioSamples(audioCodec, muxer, audioTrackIndex, durationSeconds)

        } finally {
            try {
                videoCodec?.stop()
                videoCodec?.release()
            } catch (_: Exception) {}
            try {
                audioCodec?.stop()
                audioCodec?.release()
            } catch (_: Exception) {}
            try {
                muxer?.stop()
                muxer?.release()
            } catch (_: Exception) {}
        }
    }

    private fun isSpeakerActive(timeMs: Long): Boolean {
        // Active speaking intervals:
        // Sentence 1: 0 - 3400ms
        // Pause: 3400 - 4100ms
        // Sentence 2: 4100 - 7600ms
        // Pause: 7600 - 8300ms
        // Sentence 3: 8300 - 11900ms
        // Pause: 11900 - 12600ms
        // Sentence 4: 12600 - 16000ms
        return when {
            timeMs in 0..3400 -> true
            timeMs in 4100..7600 -> true
            timeMs in 8300..11900 -> true
            timeMs in 12600..16000 -> true
            else -> false
        }
    }

    private fun getSentenceForTime(timeMs: Long): String {
        return when {
            timeMs < 3400 -> "\"Toh guys, aaj hum ek important topic ki baat karenge.\""
            timeMs < 4100 -> "[Sentence Complete • Natural Thought Boundary]"
            timeMs < 7600 -> "\"Sabse pehle ye samajhna zaroori hai...\""
            timeMs < 8300 -> "[Sentence Complete • Natural Thought Boundary]"
            timeMs < 11900 -> "\"AI detects completion and splits at boundary.\""
            timeMs < 12600 -> "[Sentence Complete • Natural Thought Boundary]"
            else -> "\"Aur har clip par smooth zoom keyframe lagta hai!\""
        }
    }

    private fun encodeAudioSamples(codec: MediaCodec, muxer: MediaMuxer, trackIndex: Int, durationSeconds: Int) {
        if (trackIndex < 0) return
        val sampleRate = 44100
        val totalSamples = sampleRate * durationSeconds
        val bufferInfo = MediaCodec.BufferInfo()
        val chunkSamples = 2048

        var sampleOffset = 0
        while (sampleOffset < totalSamples) {
            val inIdx = codec.dequeueInputBuffer(10000)
            if (inIdx >= 0) {
                val inBuf = codec.getInputBuffer(inIdx) ?: break
                inBuf.clear()
                inBuf.order(ByteOrder.LITTLE_ENDIAN)

                val toWrite = minOf(chunkSamples, totalSamples - sampleOffset)
                for (s in 0 until toWrite) {
                    val currentSample = sampleOffset + s
                    val timeMs = (currentSample * 1000L) / sampleRate
                    val active = isSpeakerActive(timeMs)
                    val sampleVal = if (active) {
                        (sin(2.0 * Math.PI * 440.0 * currentSample / sampleRate) * 12000.0).toInt().toShort()
                    } else {
                        0.toShort()
                    }
                    inBuf.putShort(sampleVal)
                }

                val pts = (sampleOffset * 1_000_000L) / sampleRate
                val isEOS = (sampleOffset + toWrite >= totalSamples)
                val flags = if (isEOS) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                codec.queueInputBuffer(inIdx, 0, toWrite * 2, pts, flags)
                sampleOffset += toWrite
            }

            var outIdx = codec.dequeueOutputBuffer(bufferInfo, 0)
            while (outIdx >= 0) {
                val outBuf = codec.getOutputBuffer(outIdx)
                if (outBuf != null && bufferInfo.size > 0) {
                    outBuf.position(bufferInfo.offset)
                    outBuf.limit(bufferInfo.offset + bufferInfo.size)
                    muxer.writeSampleData(trackIndex, outBuf, bufferInfo)
                }
                codec.releaseOutputBuffer(outIdx, false)
                outIdx = codec.dequeueOutputBuffer(bufferInfo, 0)
            }
        }
    }

    private fun createFallbackSampleFile(context: Context, targetFile: File) {
        targetFile.writeBytes(ByteArray(1024))
    }
}
