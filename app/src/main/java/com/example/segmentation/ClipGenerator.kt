package com.example.segmentation

import com.example.model.Keyframe
import com.example.model.KeyframeInterpolation
import com.example.model.MotionType
import com.example.model.SpeechSegment
import com.example.model.VideoClip
import java.util.UUID

class ClipGenerator {

    /**
     * Splits video into contiguous, non-overlapping clips based on split boundaries.
     * Guarantees 100% time coverage without audio gaps or duplicated frames.
     */
    fun createClipsFromSplits(
        totalDurationMs: Long,
        segments: List<SpeechSegment>,
        defaultZoomScale: Float = 1.12f,
        interpolation: KeyframeInterpolation = KeyframeInterpolation.EASE_IN_OUT
    ): List<VideoClip> {
        val clips = mutableListOf<VideoClip>()
        if (segments.isEmpty()) {
            clips.add(
                VideoClip(
                    id = UUID.randomUUID().toString(),
                    clipIndex = 0,
                    startMs = 0L,
                    endMs = totalDurationMs,
                    spokenText = "Entire video segment",
                    confidence = 0.95f,
                    startKeyframe = Keyframe(timestampMs = 0L, scale = 1.0f),
                    endKeyframe = Keyframe(timestampMs = totalDurationMs, scale = defaultZoomScale),
                    interpolation = interpolation,
                    motionType = MotionType.ZOOM_IN
                )
            )
            return clips
        }

        var prevEndMs = 0L
        for (i in segments.indices) {
            val segment = segments[i]
            val clipStartMs = prevEndMs
            val clipEndMs = if (i == segments.size - 1) totalDurationMs else segment.endTimeMs

            if (clipEndMs > clipStartMs) {
                clips.add(
                    VideoClip(
                        id = UUID.randomUUID().toString(),
                        clipIndex = clips.size,
                        startMs = clipStartMs,
                        endMs = clipEndMs,
                        spokenText = segment.text,
                        confidence = segment.confidence,
                        startKeyframe = Keyframe(timestampMs = clipStartMs, scale = 1.0f),
                        endKeyframe = Keyframe(timestampMs = clipEndMs, scale = defaultZoomScale),
                        interpolation = interpolation,
                        motionType = MotionType.ZOOM_IN
                    )
                )
                prevEndMs = clipEndMs
            }
        }

        return clips
    }
}
