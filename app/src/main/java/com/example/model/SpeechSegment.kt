package com.example.model

data class SpeechSegment(
    val id: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
    val confidence: Float = 0.95f,
    val pauseAfterMs: Long = 0L,
    val isSentenceBoundary: Boolean = true
) {
    val durationMs: Long
        get() = endTimeMs - startTimeMs

    fun formattedTimeSpan(): String {
        val startSec = startTimeMs / 1000f
        val endSec = endTimeMs / 1000f
        return String.format("%.2fs – %.2fs", startSec, endSec)
    }
}
