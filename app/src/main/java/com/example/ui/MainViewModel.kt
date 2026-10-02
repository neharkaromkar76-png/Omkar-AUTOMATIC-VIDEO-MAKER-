package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SpeechAnalyzer
import com.example.data.AppDatabase
import com.example.data.ProjectEntity
import com.example.data.ProjectRepository
import com.example.export.ExportRenderer
import com.example.model.AnalysisProgressState
import com.example.model.ExportOptions
import com.example.model.ExportProgressState
import com.example.model.KeyframeInterpolation
import com.example.model.KeyframeMode
import com.example.model.MotionType
import com.example.model.TimelineEditorState
import com.example.model.VideoClip
import com.example.model.VideoMetadata
import com.example.sample.SampleVideoHelper
import com.example.timeline.TimelineEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val speechAnalyzer = SpeechAnalyzer(application)
    private val timelineEngine = TimelineEngine()
    private val exportRenderer = ExportRenderer(application)
    private val repository = ProjectRepository(AppDatabase.getDatabase(application).projectDao())

    // Selected video on Home Screen
    private val _selectedVideo = MutableStateFlow<VideoMetadata?>(null)
    val selectedVideo: StateFlow<VideoMetadata?> = _selectedVideo.asStateFlow()

    // Analysis Progress State
    private val _analysisState = MutableStateFlow(AnalysisProgressState())
    val analysisState: StateFlow<AnalysisProgressState> = _analysisState.asStateFlow()

    // Timeline Editor State
    private val _timelineState = MutableStateFlow(TimelineEditorState())
    val timelineState: StateFlow<TimelineEditorState> = _timelineState.asStateFlow()

    // Export Progress State
    private val _exportState = MutableStateFlow(ExportProgressState())
    val exportState: StateFlow<ExportProgressState> = _exportState.asStateFlow()

    // Export dialog open/close
    private val _showExportDialog = MutableStateFlow(false)
    val showExportDialog: StateFlow<Boolean> = _showExportDialog.asStateFlow()

    // Keyframe editor dialog open/close
    private val _editingClip = MutableStateFlow<VideoClip?>(null)
    val editingClip: StateFlow<VideoClip?> = _editingClip.asStateFlow()

    // Projects list from Room
    val savedProjects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Playback loop job
    private var playbackJob: Job? = null

    fun selectVideo(uri: Uri, name: String = "Imported Video") {
        viewModelScope.launch {
            val meta = VideoMetadata.extractFromUri(getApplication(), uri, name)
            _selectedVideo.value = meta
        }
    }

    fun loadSampleVideo() {
        viewModelScope.launch {
            _analysisState.value = AnalysisProgressState(
                isAnalyzing = true,
                statusText = "Loading built-in talking-head sample video..."
            )
            val uri = SampleVideoHelper.getOrCreateSampleVideo(getApplication())
            val meta = VideoMetadata.extractFromUri(
                getApplication(),
                uri,
                "Omkar Talking Head Sample"
            )
            _selectedVideo.value = meta
            _analysisState.value = AnalysisProgressState(isAnalyzing = false)
        }
    }

    fun startAutomaticEdit(
        keyframeMode: KeyframeMode = KeyframeMode.SIMPLE,
        zoomScale: Float = 1.12f,
        interpolation: KeyframeInterpolation = KeyframeInterpolation.EASE_IN_OUT
    ) {
        val video = _selectedVideo.value ?: return

        viewModelScope.launch {
            try {
                val clips = speechAnalyzer.analyzeAndAutoEdit(
                    videoMetadata = video,
                    keyframeMode = keyframeMode,
                    zoomScale = zoomScale,
                    interpolation = interpolation
                ) { progressState ->
                    _analysisState.value = progressState
                }

                _timelineState.value = TimelineEditorState(
                    videoMetadata = video,
                    clips = clips,
                    originalAiClips = clips,
                    selectedClipId = clips.firstOrNull()?.id,
                    playheadMs = 0L,
                    keyframeMode = keyframeMode,
                    defaultZoomScale = zoomScale,
                    defaultInterpolation = interpolation
                )

                // Save project record to Room
                repository.saveProject(
                    ProjectEntity(
                        title = video.name,
                        videoUri = video.uriString,
                        durationMs = video.durationMs,
                        clipCount = clips.size,
                        keyframeMode = keyframeMode.name,
                        defaultZoomScale = zoomScale
                    )
                )

            } catch (e: Exception) {
                _analysisState.value = AnalysisProgressState(
                    isAnalyzing = false,
                    errorMessage = e.localizedMessage ?: "Analysis failed"
                )
            }
        }
    }

    fun dismissAnalysis() {
        _analysisState.value = AnalysisProgressState(isAnalyzing = false)
    }

    // --- Timeline Control Actions ---

    fun selectClip(clipId: String) {
        val target = _timelineState.value.clips.firstOrNull { it.id == clipId } ?: return
        _timelineState.value = _timelineState.value.copy(
            selectedClipId = clipId,
            playheadMs = target.startMs
        )
    }

    fun seekTo(timeMs: Long) {
        val total = _timelineState.value.totalDurationMs
        val clamped = timeMs.coerceIn(0L, total)
        val clip = _timelineState.value.clips.firstOrNull { it.containsTime(clamped) }
        _timelineState.value = _timelineState.value.copy(
            playheadMs = clamped,
            selectedClipId = clip?.id ?: _timelineState.value.selectedClipId
        )
    }

    fun togglePlayback() {
        if (_timelineState.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        _timelineState.value = _timelineState.value.copy(isPlaying = true)
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            val total = _timelineState.value.totalDurationMs
            while (isActive && _timelineState.value.isPlaying) {
                delay(33) // ~30 fps tick
                val current = _timelineState.value.playheadMs
                val next = current + 33
                if (next >= total) {
                    seekTo(0)
                    pause()
                    break
                } else {
                    seekTo(next)
                }
            }
        }
    }

    fun pause() {
        playbackJob?.cancel()
        _timelineState.value = _timelineState.value.copy(isPlaying = false)
    }

    fun skipToPreviousClip() {
        val currentPlayhead = _timelineState.value.playheadMs
        val clips = _timelineState.value.clips
        val prevClip = clips.lastOrNull { it.startMs < currentPlayhead - 200 } ?: clips.firstOrNull()
        if (prevClip != null) {
            seekTo(prevClip.startMs)
        }
    }

    fun skipToNextClip() {
        val currentPlayhead = _timelineState.value.playheadMs
        val clips = _timelineState.value.clips
        val nextClip = clips.firstOrNull { it.startMs > currentPlayhead + 50 }
        if (nextClip != null) {
            seekTo(nextClip.startMs)
        }
    }

    // --- Manual Timeline Edits ---

    fun addSplitAtCurrentPlayhead() {
        val playhead = _timelineState.value.playheadMs
        _timelineState.value = timelineEngine.addSplitAt(_timelineState.value, playhead)
    }

    fun removeSplitAfter(clipIndex: Int) {
        _timelineState.value = timelineEngine.removeSplitAfter(_timelineState.value, clipIndex)
    }

    fun moveSplitBoundary(clipIndex: Int, deltaMs: Long) {
        _timelineState.value = timelineEngine.moveSplitBoundary(_timelineState.value, clipIndex, deltaMs)
    }

    fun resetAutomaticEdit() {
        _timelineState.value = timelineEngine.resetAutomaticEdit(_timelineState.value)
    }

    fun undo() {
        _timelineState.value = timelineEngine.undo(_timelineState.value)
    }

    fun redo() {
        _timelineState.value = timelineEngine.redo(_timelineState.value)
    }

    fun openKeyframeEditor(clip: VideoClip) {
        _editingClip.value = clip
    }

    fun dismissKeyframeEditor() {
        _editingClip.value = null
    }

    fun saveClipKeyframe(
        clipId: String,
        startScale: Float,
        endScale: Float,
        interpolation: KeyframeInterpolation,
        motionType: MotionType
    ) {
        _timelineState.value = timelineEngine.updateClipKeyframe(
            _timelineState.value,
            clipId,
            startScale,
            endScale,
            interpolation,
            motionType
        )
        _editingClip.value = null
    }

    fun switchKeyframeMode(mode: KeyframeMode) {
        val targetScale = _timelineState.value.defaultZoomScale
        val interp = _timelineState.value.defaultInterpolation
        _timelineState.value = timelineEngine.reapplyKeyframes(_timelineState.value, mode, targetScale, interp)
    }

    // --- Export Video Actions ---

    fun openExportDialog() {
        pause()
        _showExportDialog.value = true
    }

    fun dismissExportDialog() {
        _showExportDialog.value = false
    }

    fun startExport(options: ExportOptions) {
        val video = _timelineState.value.videoMetadata ?: return
        val clips = _timelineState.value.clips
        if (clips.isEmpty()) return

        pause()
        _showExportDialog.value = false

        viewModelScope.launch {
            try {
                val exportedFile = exportRenderer.exportVideo(
                    videoMetadata = video,
                    clips = clips,
                    options = options
                ) { state ->
                    _exportState.value = state
                }
            } catch (e: Exception) {
                _exportState.value = ExportProgressState(
                    isExporting = false,
                    error = e.localizedMessage ?: "Export failed"
                )
            }
        }
    }

    fun dismissExportProgress() {
        _exportState.value = ExportProgressState(isExporting = false)
    }

    fun clearActiveProject() {
        pause()
        _timelineState.value = TimelineEditorState()
    }
}
