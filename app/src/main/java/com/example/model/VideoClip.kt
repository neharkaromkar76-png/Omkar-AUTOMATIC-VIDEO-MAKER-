package com.example.model

data class VideoClip(
    val id: String,
    val clipIndex: Int,
    val startMs: Long,
    val endMs: Long,
    val spokenText: String = "",
    val confidence: Float = 0.92f,
    val startKeyframe: Keyframe = Keyframe(timestampMs = startMs, scale = 1.0f),
    val endKeyframe: Keyframe = Keyframe(timestampMs = endMs, scale = 1.12f),
    val interpolation: KeyframeInterpolation = KeyframeInterpolation.EASE_IN_OUT,
    val motionType: MotionType = MotionType.ZOOM_IN
) {
    val durationMs: Long
        get() = (endMs - startMs).coerceAtLeast(1L)

    fun containsTime(timeMs: Long): Boolean {
        return timeMs in startMs until endMs
    }

    /**
     * Calculates the animated zoom scale at [currentTimeMs] using configured interpolation.
     */
    fun getScaleAt(currentTimeMs: Long): Float {
        if (currentTimeMs <= startMs) return startKeyframe.scale
        if (currentTimeMs >= endMs) return endKeyframe.scale
        
        val fraction = (currentTimeMs - startMs).toFloat() / durationMs.toFloat()
        val easedFraction = interpolation.interpolate(fraction)
        
        return startKeyframe.scale + easedFraction * (endKeyframe.scale - startKeyframe.scale)
    }

    fun formattedRange(): String {
        val sMin = (startMs / 60000)
        val sSec = (startMs % 60000) / 1000f
        val eMin = (endMs / 60000)
        val eSec = (endMs % 60000) / 1000f
        return String.format("%02d:%05.2f → %02d:%05.2f", sMin, sSec, eMin, eSec)
    }
}
