package com.example.audio

import android.content.Context
import android.net.Uri
import com.example.keyframe.KeyframeEngine
import com.example.model.AnalysisProgressState
import com.example.model.AnalysisStep
import com.example.model.KeyframeInterpolation
import com.example.model.KeyframeMode
import com.example.model.VideoClip
import com.example.model.VideoMetadata
import com.example.segmentation.ClipGenerator
import com.example.segmentation.SpeechSegmentDetector
import com.example.segmentation.SplitPointDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class SpeechAnalyzer(
    private val context: Context,
    private val audioExtractor: AudioExtractor = AudioExtractor(context),
    private val vad: VoiceActivityDetector = VoiceActivityDetector(),
    private val wordAnalyzer: WordTimestampAnalyzer = WordTimestampAnalyzer(),
    private val segmentDetector: SpeechSegmentDetector = SpeechSegmentDetector(),
    private val splitDetector: SplitPointDetector = SplitPointDetector(),
    private val clipGenerator: ClipGenerator = ClipGenerator(),
    private val keyframeEngine: KeyframeEngine = KeyframeEngine()
) {

    /**
     * Executes the automatic editing algorithm:
     * 1. Extract audio
     * 2. Speech detection & VAD
     * 3. Word-level timestamps & cadence
     * 4. Sentence / utterance segmentation
     * 5. Determine split boundaries
     * 6. Create clips
     * 7. Add automatic keyframes
     * 8. Prepare preview
     */
    suspend fun analyzeAndAutoEdit(
        videoMetadata: VideoMetadata,
        keyframeMode: KeyframeMode = KeyframeMode.SIMPLE,
        zoomScale: Float = 1.12f,
        interpolation: KeyframeInterpolation = KeyframeInterpolation.EASE_IN_OUT,
        onProgressUpdate: (AnalysisProgressState) -> Unit
    ): List<VideoClip> = withContext(Dispatchers.Default) {
        val totalDur = videoMetadata.durationMs.coerceAtLeast(1000L)
        val uri = Uri.parse(videoMetadata.uriString)

        // 1. Preparing video...
        onProgressUpdate(
            AnalysisProgressState(
                isAnalyzing = true,
                currentStep = AnalysisStep.PREPARING_VIDEO,
                progress = 0.08f,
                statusText = "Preparing video format and audio tracks..."
            )
        )
        delay(250)

        // 2. Extracting audio...
        onProgressUpdate(
            AnalysisProgressState(
                isAnalyzing = true,
                currentStep = AnalysisStep.EXTRACTING_AUDIO,
                progress = 0.20f,
                statusText = "Extracting audio waveform and PCM channels..."
            )
        )
        val audioData = audioExtractor.extractAndAnalyzeAudio(uri) { subProgress ->
            val p = 0.15f + (subProgress * 0.15f)
            onProgressUpdate(
                AnalysisProgressState(
                    isAnalyzing = true,
                    currentStep = AnalysisStep.EXTRACTING_AUDIO,
                    progress = p,
                    statusText = "Decoding audio stream (${(subProgress * 100).toInt()}%)..."
                )
            )
        }

        // 3. Analyzing speech & VAD...
        onProgressUpdate(
            AnalysisProgressState(
                isAnalyzing = true,
                currentStep = AnalysisStep.ANALYZING_SPEECH,
                progress = 0.38f,
                statusText = "Analyzing speech energy, vocal frequencies, and pauses..."
            )
        )
        delay(300)
        val (frames, pauses) = vad.detectSpeechAndPauses(audioData)

        // 4. Detecting words...
        onProgressUpdate(
            AnalysisProgressState(
                isAnalyzing = true,
                currentStep = AnalysisStep.DETECTING_WORDS,
                progress = 0.52f,
                statusText = "Detecting words, syllabic beats, and phrase cadence..."
            )
        )
        delay(300)
        val timedWords = wordAnalyzer.analyzeWordBoundaries(frames, pauses, totalDur)

        // 5. Understanding sentence boundaries...
        onProgressUpdate(
            AnalysisProgressState(
                isAnalyzing = true,
                currentStep = AnalysisStep.UNDERSTANDING_BOUNDARIES,
                progress = 0.68f,
                statusText = "Distinguishing temporary pauses from complete spoken thoughts..."
            )
        )
        delay(350)
        val segments = segmentDetector.detectMeaningfulSegments(frames, pauses, timedWords, totalDur)

        // 6. Finding split points...
        onProgressUpdate(
            AnalysisProgressState(
                isAnalyzing = true,
                currentStep = AnalysisStep.FINDING_SPLIT_POINTS,
                progress = 0.82f,
                statusText = "Calculating natural split points at thought boundaries...",
                detectedSegmentsCount = segments.size,
                detectedSplitsCount = (segments.size - 1).coerceAtLeast(0)
            )
        )
        delay(300)
        val splitPoints = splitDetector.calculateSplitPoints(segments)

        // 7. Creating clips...
        onProgressUpdate(
            AnalysisProgressState(
                isAnalyzing = true,
                currentStep = AnalysisStep.CREATING_CLIPS,
                progress = 0.90f,
                statusText = "Creating synchronized video clips (preserving audio sync)...",
                detectedSegmentsCount = segments.size,
                detectedSplitsCount = splitPoints.size
            )
        )
        delay(250)
        val initialClips = clipGenerator.createClipsFromSplits(
            totalDurationMs = totalDur,
            segments = segments,
            defaultZoomScale = zoomScale,
            interpolation = interpolation
        )

        // 8. Adding keyframes...
        onProgressUpdate(
            AnalysisProgressState(
                isAnalyzing = true,
                currentStep = AnalysisStep.ADDING_KEYFRAMES,
                progress = 0.96f,
                statusText = "Applying smooth $zoomScale keyframe zooms to all clips...",
                detectedSegmentsCount = segments.size,
                detectedSplitsCount = splitPoints.size
            )
        )
        delay(250)
        val keyframedClips = keyframeEngine.applyAutomaticKeyframes(
            clips = initialClips,
            mode = keyframeMode,
            targetScale = zoomScale,
            interpolation = interpolation
        )

        // 9. Preparing preview...
        onProgressUpdate(
            AnalysisProgressState(
                isAnalyzing = true,
                currentStep = AnalysisStep.PREPARING_PREVIEW,
                progress = 1.0f,
                statusText = "Generating timeline preview...",
                detectedSegmentsCount = segments.size,
                detectedSplitsCount = splitPoints.size
            )
        )
        delay(200)

        onProgressUpdate(
            AnalysisProgressState(
                isAnalyzing = false,
                currentStep = AnalysisStep.COMPLETED,
                progress = 1.0f,
                statusText = "Auto-Edit Ready! Generated ${keyframedClips.size} clips with keyframes.",
                detectedSegmentsCount = segments.size,
                detectedSplitsCount = splitPoints.size
            )
        )

        keyframedClips
    }
}
