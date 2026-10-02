package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.model.VideoMetadata
import com.example.ui.MainViewModel
import com.example.ui.components.AnalysisProgressDialog
import com.example.ui.components.ExportDialog
import com.example.ui.components.KeyframeEditorDialog
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OmkarVideoMakerApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun OmkarVideoMakerApp(viewModel: MainViewModel) {
    val selectedVideo by viewModel.selectedVideo.collectAsState()
    val analysisState by viewModel.analysisState.collectAsState()
    val timelineState by viewModel.timelineState.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    val showExportDialog by viewModel.showExportDialog.collectAsState()
    val editingClip by viewModel.editingClip.collectAsState()
    val savedProjects by viewModel.savedProjects.collectAsState()

    // Screen Routing:
    // If timeline has clips, show EditorScreen; otherwise show HomeScreen
    if (timelineState.clips.isNotEmpty()) {
        EditorScreen(
            state = timelineState,
            onBack = { viewModel.clearActiveProject() },
            onSeek = { viewModel.seekTo(it) },
            onTogglePlay = { viewModel.togglePlayback() },
            onSkipPrev = { viewModel.skipToPreviousClip() },
            onSkipNext = { viewModel.skipToNextClip() },
            onAddSplit = { viewModel.addSplitAtCurrentPlayhead() },
            onRemoveSplit = { viewModel.removeSplitAfter(it) },
            onMoveSplit = { idx, delta -> viewModel.moveSplitBoundary(idx, delta) },
            onSelectClip = { viewModel.selectClip(it) },
            onEditClipKeyframe = { viewModel.openKeyframeEditor(it) },
            onResetAutoEdit = { viewModel.resetAutomaticEdit() },
            onUndo = { viewModel.undo() },
            onRedo = { viewModel.redo() },
            onExportClick = { viewModel.openExportDialog() },
            onSwitchKeyframeMode = { viewModel.switchKeyframeMode(it) }
        )
    } else {
        HomeScreen(
            selectedVideo = selectedVideo,
            savedProjects = savedProjects,
            onSelectVideo = { uri, name -> viewModel.selectVideo(uri, name) },
            onLoadSample = { viewModel.loadSampleVideo() },
            onStartAutoEdit = { mode, scale, interp ->
                viewModel.startAutomaticEdit(mode, scale, interp)
            },
            onResumeProject = { project ->
                viewModel.selectVideo(Uri.parse(project.videoUri), project.title)
                viewModel.startAutomaticEdit(
                    keyframeMode = try {
                        com.example.model.KeyframeMode.valueOf(project.keyframeMode)
                    } catch (_: Exception) {
                        com.example.model.KeyframeMode.SIMPLE
                    },
                    zoomScale = project.defaultZoomScale
                )
            }
        )
    }

    // Modal Dialogs
    AnalysisProgressDialog(
        state = analysisState,
        onDismiss = { viewModel.dismissAnalysis() }
    )

    if (editingClip != null) {
        KeyframeEditorDialog(
            clip = editingClip!!,
            onDismiss = { viewModel.dismissKeyframeEditor() },
            onSave = { startScale, endScale, interp, motion ->
                viewModel.saveClipKeyframe(editingClip!!.id, startScale, endScale, interp, motion)
            }
        )
    }

    ExportDialog(
        isOpen = showExportDialog,
        exportState = exportState,
        onDismiss = { viewModel.dismissExportDialog() },
        onStartExport = { options -> viewModel.startExport(options) },
        onDismissProgress = { viewModel.dismissExportProgress() }
    )
}
