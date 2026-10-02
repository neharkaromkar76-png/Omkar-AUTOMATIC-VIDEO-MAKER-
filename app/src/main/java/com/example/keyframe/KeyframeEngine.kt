package com.example.keyframe

import com.example.model.Keyframe
import com.example.model.KeyframeInterpolation
import com.example.model.KeyframeMode
import com.example.model.MotionType
import com.example.model.VideoClip

class KeyframeEngine(
    private val defaultScale: Float = 1.12f,
    private val defaultInterpolation: KeyframeInterpolation = KeyframeInterpolation.EASE_IN_OUT
) {

    /**
     * Applies automatic keyframes to every clip according to selected [KeyframeMode].
     */
    fun applyAutomaticKeyframes(
        clips: List<VideoClip>,
        mode: KeyframeMode = KeyframeMode.SIMPLE,
        targetScale: Float = defaultScale,
        interpolation: KeyframeInterpolation = defaultInterpolation
    ): List<VideoClip> {
        return clips.mapIndexed { index, clip ->
            when (mode) {
                KeyframeMode.SIMPLE -> {
                    // MODE 1 - SIMPLE:
                    // Every clip starts at 100% and smoothly zooms into targetScale (e.g. 112%)
                    val startKf = Keyframe(timestampMs = clip.startMs, scale = 1.0f)
                    val endKf = Keyframe(timestampMs = clip.endMs, scale = targetScale)
                    clip.copy(
                        clipIndex = index,
                        startKeyframe = startKf,
                        endKeyframe = endKf,
                        interpolation = interpolation,
                        motionType = MotionType.ZOOM_IN
                    )
                }
                KeyframeMode.SMART -> {
                    // MODE 2 - SMART:
                    // AI alternates motion for professional engagement:
                    // - Alternates Zoom In, Zoom Out, or Subtle Focus based on clip rhythm & length
                    val motion: MotionType
                    val startScale: Float
                    val endScale: Float

                    when (index % 3) {
                        0 -> {
                            // Gentle Zoom In
                            motion = MotionType.ZOOM_IN
                            startScale = 1.0f
                            endScale = (targetScale).coerceIn(1.06f, 1.15f)
                        }
                        1 -> {
                            // Gentle Zoom Out (gives audience visual relief between sentences)
                            motion = MotionType.ZOOM_OUT
                            startScale = (targetScale).coerceIn(1.06f, 1.14f)
                            endScale = 1.0f
                        }
                        else -> {
                            // Subtle dynamic framing zoom
                            motion = MotionType.SUBTLE_PAN
                            startScale = 1.02f
                            endScale = (targetScale * 0.95f).coerceIn(1.05f, 1.12f)
                        }
                    }

                    val startKf = Keyframe(timestampMs = clip.startMs, scale = startScale)
                    val endKf = Keyframe(timestampMs = clip.endMs, scale = endScale)

                    clip.copy(
                        clipIndex = index,
                        startKeyframe = startKf,
                        endKeyframe = endKf,
                        interpolation = interpolation,
                        motionType = motion
                    )
                }
            }
        }
    }

    /**
     * Updates an individual clip with custom keyframe parameters.
     */
    fun updateClipKeyframe(
        clip: VideoClip,
        startScale: Float,
        endScale: Float,
        interpolation: KeyframeInterpolation,
        motionType: MotionType
    ): VideoClip {
        return clip.copy(
            startKeyframe = Keyframe(timestampMs = clip.startMs, scale = startScale),
            endKeyframe = Keyframe(timestampMs = clip.endMs, scale = endScale),
            interpolation = interpolation,
            motionType = motionType
        )
    }
}
