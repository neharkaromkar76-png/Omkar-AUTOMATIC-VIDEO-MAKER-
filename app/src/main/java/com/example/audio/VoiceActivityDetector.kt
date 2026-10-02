package com.example.audio

data class VoiceActivityFrame(
    val timestampMs: Long,
    val isVoiced: Boolean,
    val energy: Float
)

data class DetectedPause(
    val startMs: Long,
    val endMs: Long,
    val durationMs: Long,
    val lowestEnergyTimestampMs: Long,
    val isLikelySentenceBoundary: Boolean
)

class VoiceActivityDetector(
    private val silenceThreshold: Float = 0.12f,
    private val minSentencePauseDurationMs: Long = 520L,
    private val maxIntraSentencePauseDurationMs: Long = 450L
) {

    fun detectSpeechAndPauses(audioData: AudioAnalysisData): Pair<List<VoiceActivityFrame>, List<DetectedPause>> {
        val frames = mutableListOf<VoiceActivityFrame>()
        val energyList = audioData.rmsEnergyOverTime
        val windowMs = audioData.windowDurationMs

        // 1. Classify each window as voiced or unvoiced using adaptive threshold
        var smoothedEnergy = 0f
        val smoothingFactor = 0.3f

        for (i in energyList.indices) {
            val raw = energyList[i]
            smoothedEnergy = (smoothingFactor * raw) + ((1f - smoothingFactor) * smoothedEnergy)
            val timeMs = i * windowMs
            val isVoiced = smoothedEnergy > silenceThreshold
            frames.add(VoiceActivityFrame(timeMs, isVoiced, smoothedEnergy))
        }

        // 2. Identify pauses and determine whether they are intra-sentence pauses vs complete sentence boundaries
        val pauses = mutableListOf<DetectedPause>()
        var inPause = false
        var pauseStartMs = 0L
        var minEnergyInPause = Float.MAX_VALUE
        var minEnergyTimeMs = 0L

        for (frame in frames) {
            if (!frame.isVoiced) {
                if (!inPause) {
                    inPause = true
                    pauseStartMs = frame.timestampMs
                    minEnergyInPause = frame.energy
                    minEnergyTimeMs = frame.timestampMs
                } else {
                    if (frame.energy < minEnergyInPause) {
                        minEnergyInPause = frame.energy
                        minEnergyTimeMs = frame.timestampMs
                    }
                }
            } else {
                if (inPause) {
                    val pauseEndMs = frame.timestampMs
                    val pauseDuration = pauseEndMs - pauseStartMs

                    // Important Speech Rule:
                    // If pause is very short (< 450ms), person just took a breath or hesitated ("Guys... [pause] ...aaj hum")
                    // If pause >= 520ms, spoken thought is completed and next thought begins.
                    val isSentenceBoundary = pauseDuration >= minSentencePauseDurationMs

                    pauses.add(
                        DetectedPause(
                            startMs = pauseStartMs,
                            endMs = pauseEndMs,
                            durationMs = pauseDuration,
                            lowestEnergyTimestampMs = minEnergyTimeMs,
                            isLikelySentenceBoundary = isSentenceBoundary
                        )
                    )
                    inPause = false
                }
            }
        }

        return Pair(frames, pauses)
    }
}
