package com.example.model

enum class KeyframeInterpolation(val displayName: String) {
    EASE_IN_OUT("Ease In Out"),
    EASE_IN("Ease In"),
    EASE_OUT("Ease Out"),
    LINEAR("Linear");

    fun interpolate(fraction: Float): Float {
        val t = fraction.coerceIn(0f, 1f)
        return when (this) {
            LINEAR -> t
            EASE_IN -> t * t
            EASE_OUT -> 1f - (1f - t) * (1f - t)
            EASE_IN_OUT -> {
                if (t < 0.5f) {
                    2f * t * t
                } else {
                    1f - (-2f * t + 2f) * (-2f * t + 2f) / 2f
                }
            }
        }
    }
}

enum class MotionType(val displayName: String) {
    ZOOM_IN("Zoom In (100% → 112%)"),
    ZOOM_OUT("Zoom Out (112% → 100%)"),
    SUBTLE_PAN("Subtle Focus Movement"),
    STATIC("Static Frame")
}

data class Keyframe(
    val timestampMs: Long,
    val scale: Float = 1.0f,
    val translationX: Float = 0f,
    val translationY: Float = 0f
) {
    val scalePercentage: Int
        get() = (scale * 100).toInt()
}
