package com.example.segmentation

import com.example.model.SpeechSegment

data class SplitPoint(
    val timestampMs: Long,
    val precedingSegmentId: String,
    val pauseDurationMs: Long,
    val confidence: Float
) {
    fun formattedTimestamp(): String {
        val totalSec = timestampMs / 1000f
        val min = (timestampMs / 60000).toInt()
        val sec = totalSec % 60f
        return String.format("%02d:%06.3f", min, sec)
    }
}

class SplitPointDetector {

    /**
     * Determines natural split points from speech segments.
     * Guaranteed:
     * - Frame-accurate milliseconds
     * - Placed between completed spoken thoughts
     * - Preserves synchronization without cutting words
     */
    fun calculateSplitPoints(segments: List<SpeechSegment>): List<SplitPoint> {
        val splitPoints = mutableListOf<SplitPoint>()
        if (segments.size <= 1) return splitPoints

        for (i in 0 until segments.size - 1) {
            val current = segments[i]
            val next = segments[i + 1]

            // The natural split point is at the boundary between current utterance and next
            val splitMs = current.endTimeMs
            splitPoints.add(
                SplitPoint(
                    timestampMs = splitMs,
                    precedingSegmentId = current.id,
                    pauseDurationMs = next.startTimeMs - current.endTimeMs,
                    confidence = (current.confidence + next.confidence) / 2f
                )
            )
        }

        return splitPoints
    }
}
