package com.example.model

enum class ExportResolution(val label: String, val width: Int, val height: Int) {
    ORIGINAL("Original", 0, 0),
    RES_1080P("1080p (Full HD)", 1920, 1080),
    RES_720P("720p (HD)", 1280, 720)
}

enum class ExportQuality(val label: String, val bitRateMultiplier: Float) {
    HIGH("High (Best Quality)", 1.5f),
    MEDIUM("Medium (Balanced)", 1.0f),
    LOW("Low (Small File Size)", 0.6f)
}

enum class ExportFps(val label: String, val fps: Int) {
    ORIGINAL("Original FPS", 0),
    FPS_30("30 FPS", 30),
    FPS_60("60 FPS", 60)
}

data class ExportOptions(
    val resolution: ExportResolution = ExportResolution.ORIGINAL,
    val quality: ExportQuality = ExportQuality.HIGH,
    val fps: ExportFps = ExportFps.ORIGINAL,
    val format: String = "MP4 (H.264)"
)

data class ExportProgressState(
    val isExporting: Boolean = false,
    val progress: Float = 0f,
    val currentStage: String = "",
    val outputFilePath: String? = null,
    val outputFileSizeBytes: Long = 0L,
    val error: String? = null,
    val isComplete: Boolean = false
)
