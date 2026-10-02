package com.example.audio

data class TimedWord(
    val word: String,
    val startMs: Long,
    val endMs: Long,
    val confidence: Float,
    val hasTerminalPunctuation: Boolean = false
)

class WordTimestampAnalyzer {

    /**
     * Extracts word rhythm and estimates word-level boundaries from voice activity bursts.
     */
    fun analyzeWordBoundaries(
        frames: List<VoiceActivityFrame>,
        pauses: List<DetectedPause>,
        totalDurationMs: Long
    ): List<TimedWord> {
        val words = mutableListOf<TimedWord>()
        var wordStartMs = -1L

        for (i in frames.indices) {
            val f = frames[i]
            if (f.isVoiced) {
                if (wordStartMs < 0) {
                    wordStartMs = f.timestampMs
                }
            } else {
                if (wordStartMs >= 0) {
                    val wordEndMs = f.timestampMs
                    val wordDur = wordEndMs - wordStartMs
                    if (wordDur >= 120L) { // Meaningful spoken word duration
                        // Check if immediately followed by a sentence boundary pause
                        val followedBySentencePause = pauses.any {
                            it.isLikelySentenceBoundary && kotlin.math.abs(it.startMs - wordEndMs) <= 100L
                        }
                        words.add(
                            TimedWord(
                                word = "word_${words.size + 1}",
                                startMs = wordStartMs,
                                endMs = wordEndMs,
                                confidence = 0.94f,
                                hasTerminalPunctuation = followedBySentencePause
                            )
                        )
                    }
                    wordStartMs = -1L
                }
            }
        }

        return words
    }
}
