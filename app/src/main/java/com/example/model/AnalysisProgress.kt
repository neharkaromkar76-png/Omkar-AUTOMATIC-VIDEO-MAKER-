package com.example.model

enum class AnalysisStep(val title: String, val weight: Float) {
    PREPARING_VIDEO("Preparing video...", 0.10f),
    EXTRACTING_AUDIO("Extracting audio...", 0.20f),
    ANALYZING_SPEECH("Analyzing speech...", 0.35f),
    DETECTING_WORDS("Detecting words...", 0.50f),
    UNDERSTANDING_BOUNDARIES("Understanding sentence boundaries...", 0.65f),
    FINDING_SPLIT_POINTS("Finding split points...", 0.80f),
    CREATING_CLIPS("Creating clips...", 0.90f),
    ADDING_KEYFRAMES("Adding keyframes...", 0.95f),
    PREPARING_PREVIEW("Preparing preview...", 1.0f),
    COMPLETED("Analysis complete!", 1.0f),
    FAILED("Analysis failed", 0f)
}

data class AnalysisProgressState(
    val isAnalyzing: Boolean = false,
    val currentStep: AnalysisStep = AnalysisStep.PREPARING_VIDEO,
    val progress: Float = 0f,
    val statusText: String = "",
    val detectedSegmentsCount: Int = 0,
    val detectedSplitsCount: Int = 0,
    val errorMessage: String? = null
)
