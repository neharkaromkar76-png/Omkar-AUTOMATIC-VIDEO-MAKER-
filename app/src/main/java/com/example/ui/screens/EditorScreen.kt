package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.KeyframeMode
import com.example.model.TimelineEditorState
import com.example.model.VideoClip
import com.example.ui.components.ClipItemCard
import com.example.ui.components.TimelineBar
import com.example.ui.components.VideoPlayerView
import com.example.ui.theme.KeyframeDot
import com.example.ui.theme.OmkarAccent
import com.example.ui.theme.OmkarDarkBackground
import com.example.ui.theme.OmkarPrimary
import com.example.ui.theme.OmkarSecondary
import com.example.ui.theme.OmkarSurface
import com.example.ui.theme.OmkarSurfaceBorder
import com.example.ui.theme.OmkarSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    state: TimelineEditorState,
    onBack: () -> Unit,
    onSeek: (Long) -> Unit,
    onTogglePlay: () -> Unit,
    onSkipPrev: () -> Unit,
    onSkipNext: () -> Unit,
    onAddSplit: () -> Unit,
    onRemoveSplit: (Int) -> Unit,
    onMoveSplit: (Int, Long) -> Unit,
    onSelectClip: (String) -> Unit,
    onEditClipKeyframe: (VideoClip) -> Unit,
    onResetAutoEdit: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onExportClick: () -> Unit,
    onSwitchKeyframeMode: (KeyframeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val videoMetadata = state.videoMetadata ?: return
    val currentClip = state.currentClip

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = videoMetadata.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = TextPrimary
                        )
                        Text(
                            text = "${state.clips.size} Speech Clips • Auto Keyframes",
                            fontSize = 11.sp,
                            color = OmkarPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_editor_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    // Reset Automatic Edit Button
                    IconButton(
                        onClick = onResetAutoEdit,
                        modifier = Modifier.testTag("btn_reset_auto_edit")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Automatic Edit",
                            tint = OmkarAccent
                        )
                    }

                    // Export Button
                    Button(
                        onClick = onExportClick,
                        colors = ButtonDefaults.buttonColors(containerColor = OmkarPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .height(36.dp)
                            .testTag("btn_export_video")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = Color(0xFF00363D),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "EXPORT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF00363D)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = OmkarSurface
                )
            )
        },
        containerColor = OmkarDarkBackground,
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Video Preview Area
            item {
                Spacer(modifier = Modifier.height(4.dp))
                val aspectRatio = remember(videoMetadata) {
                    val w = videoMetadata.width.toFloat()
                    val h = videoMetadata.height.toFloat()
                    if (w > 0 && h > 0) (w / h).coerceIn(0.56f, 1.78f) else 16f / 9f
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    VideoPlayerView(
                        videoMetadata = videoMetadata,
                        currentClip = currentClip,
                        currentScale = state.currentScale,
                        playheadMs = state.playheadMs,
                        isPlaying = state.isPlaying,
                        onSeekComplete = onSeek,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Timeline Controls Row: Play/Pause, Skip, Split, Undo/Redo
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = OmkarSurfaceElevated,
                    border = BorderStroke(1.dp, OmkarSurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Undo & Redo
                        Row {
                            IconButton(
                                onClick = onUndo,
                                enabled = state.canUndo,
                                modifier = Modifier.size(36.dp).testTag("btn_undo")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Undo,
                                    contentDescription = "Undo",
                                    tint = if (state.canUndo) TextPrimary else TextSecondary.copy(alpha = 0.4f)
                                )
                            }
                            IconButton(
                                onClick = onRedo,
                                enabled = state.canRedo,
                                modifier = Modifier.size(36.dp).testTag("btn_redo")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Redo,
                                    contentDescription = "Redo",
                                    tint = if (state.canRedo) TextPrimary else TextSecondary.copy(alpha = 0.4f)
                                )
                            }
                        }

                        // Playback: Prev, Play/Pause, Next
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onSkipPrev,
                                modifier = Modifier.size(40.dp).testTag("btn_skip_prev")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous Clip",
                                    tint = TextPrimary
                                )
                            }

                            FilledIconButton(
                                onClick = onTogglePlay,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = OmkarPrimary
                                ),
                                modifier = Modifier.size(48.dp).testTag("btn_toggle_play")
                            ) {
                                Icon(
                                    imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                                    tint = Color(0xFF00363D),
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            IconButton(
                                onClick = onSkipNext,
                                modifier = Modifier.size(40.dp).testTag("btn_skip_next")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next Clip",
                                    tint = TextPrimary
                                )
                            }
                        }

                        // Split at Playhead button
                        IconButton(
                            onClick = onAddSplit,
                            modifier = Modifier.size(40.dp).testTag("btn_add_split_playhead")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCut,
                                contentDescription = "Split at playhead",
                                tint = OmkarAccent
                            )
                        }
                    }
                }
            }

            // Multi-track Keyframe Timeline
            item {
                TimelineBar(
                    clips = state.clips,
                    totalDurationMs = state.totalDurationMs,
                    playheadMs = state.playheadMs,
                    selectedClipId = state.selectedClipId,
                    onSeek = onSeek,
                    onClipSelected = onSelectClip
                )
            }

            // Keyframe Motion Mode Switcher
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Motion Mode:",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.keyframeMode == KeyframeMode.SIMPLE,
                            onClick = { onSwitchKeyframeMode(KeyframeMode.SIMPLE) },
                            label = { Text("Simple (100%→Zoom)", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = state.keyframeMode == KeyframeMode.SMART,
                            onClick = { onSwitchKeyframeMode(KeyframeMode.SMART) },
                            label = { Text("Smart Motion", fontSize = 11.sp) }
                        )
                    }
                }
            }

            // Speech Segment Clips List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Speech Clips & Keyframes (${state.clips.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Tap to inspect or edit",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            // Clips List
            items(state.clips) { clip ->
                val isSelected = clip.id == state.selectedClipId
                val isLast = clip.clipIndex == state.clips.size - 1
                val isInside = clip.containsTime(state.playheadMs)

                ClipItemCard(
                    clip = clip,
                    isSelected = isSelected,
                    isLastClip = isLast,
                    isPlayheadInside = isInside,
                    onSelect = { onSelectClip(clip.id) },
                    onEditKeyframe = { onEditClipKeyframe(clip) },
                    onSplitAtPlayhead = onAddSplit,
                    onRemoveSplit = { onRemoveSplit(clip.clipIndex) },
                    onNudgeSplit = { deltaMs -> onMoveSplit(clip.clipIndex, deltaMs) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
