package com.example.model

enum class KeyframeMode(val displayName: String, val description: String) {
    SIMPLE(
        "Simple Mode",
        "Every clip receives standard Start (100%) to End (Zoom In) keyframes"
    ),
    SMART(
        "Smart Motion Mode",
        "AI alternates gentle Zoom In, Zoom Out, and subtle framing per sentence"
    )
}

data class TimelineEditorState(
    val videoMetadata: VideoMetadata? = null,
    val clips: List<VideoClip> = emptyList(),
    val originalAiClips: List<VideoClip> = emptyList(),
    val selectedClipId: String? = null,
    val playheadMs: Long = 0L,
    val isPlaying: Boolean = false,
    val keyframeMode: KeyframeMode = KeyframeMode.SIMPLE,
    val defaultZoomScale: Float = 1.12f,
    val defaultInterpolation: KeyframeInterpolation = KeyframeInterpolation.EASE_IN_OUT,
    val undoStack: List<List<VideoClip>> = emptyList(),
    val redoStack: List<List<VideoClip>> = emptyList()
) {
    val currentClip: VideoClip?
        get() = clips.firstOrNull { it.containsTime(playheadMs) } ?: clips.firstOrNull()

    val selectedClip: VideoClip?
        get() = clips.firstOrNull { it.id == selectedClipId } ?: currentClip

    val totalDurationMs: Long
        get() = videoMetadata?.durationMs ?: clips.lastOrNull()?.endMs ?: 0L

    val currentScale: Float
        get() = currentClip?.getScaleAt(playheadMs) ?: 1.0f

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    val canRedo: Boolean
        get() = redoStack.isNotEmpty()
}
