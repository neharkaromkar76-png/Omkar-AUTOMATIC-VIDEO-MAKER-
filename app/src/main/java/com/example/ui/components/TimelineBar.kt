package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VideoClip
import com.example.ui.theme.KeyframeDot
import com.example.ui.theme.OmkarAccent
import com.example.ui.theme.OmkarPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TimelineClipA
import com.example.ui.theme.TimelineClipB
import com.example.ui.theme.TimelinePlayhead
import com.example.ui.theme.TimelineTrackBg

@Composable
fun TimelineBar(
    clips: List<VideoClip>,
    totalDurationMs: Long,
    playheadMs: Long,
    selectedClipId: String?,
    onSeek: (Long) -> Unit,
    onClipSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val durationSafe = totalDurationMs.coerceAtLeast(1000L)
    val playheadFormatted = remember(playheadMs) {
        val min = (playheadMs / 60000).toInt()
        val sec = (playheadMs % 60000) / 1000f
        String.format("%02d:%05.2f", min, sec)
    }
    val totalFormatted = remember(durationSafe) {
        val min = (durationSafe / 60000).toInt()
        val sec = (durationSafe % 60000) / 1000f
        String.format("%02d:%05.2f", min, sec)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TimelineTrackBg, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // Timeline Header: Timestamps & Keyframe Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$playheadFormatted / $totalFormatted",
                color = OmkarPrimary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.testTag("timeline_timestamp")
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "● Keyframes (Start → End)",
                color = KeyframeDot,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Visual Multi-track Timeline Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF070B14))
                .pointerInput(totalDurationMs) {
                    detectTapGestures { offset ->
                        val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                        val seekTime = (ratio * durationSafe).toLong()
                        onSeek(seekTime)

                        // Check which clip was clicked
                        clips.firstOrNull { it.containsTime(seekTime) }?.let {
                            onClipSelected(it.id)
                        }
                    }
                }
                .pointerInput(totalDurationMs) {
                    detectDragGestures { change, _ ->
                        val ratio = (change.position.x / size.width).coerceIn(0f, 1f)
                        val seekTime = (ratio * durationSafe).toLong()
                        onSeek(seekTime)
                    }
                }
                .testTag("timeline_canvas_track")
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Draw clips
                for (clip in clips) {
                    val startX = (clip.startMs.toFloat() / durationSafe) * canvasWidth
                    val endX = (clip.endMs.toFloat() / durationSafe) * canvasWidth
                    val clipWidth = (endX - startX).coerceAtLeast(2f)

                    val isSelected = clip.id == selectedClipId
                    val clipColor = if (clip.clipIndex % 2 == 0) TimelineClipA else TimelineClipB

                    // Clip background
                    drawRect(
                        color = clipColor,
                        topLeft = Offset(startX, 0f),
                        size = Size(clipWidth, canvasHeight)
                    )

                    // Clip selection border
                    if (isSelected) {
                        drawRect(
                            color = OmkarPrimary,
                            topLeft = Offset(startX, 0f),
                            size = Size(clipWidth, canvasHeight),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                        )
                    }

                    // Split boundary line at clip start (if not 0)
                    if (startX > 0) {
                        drawLine(
                            color = OmkarAccent,
                            start = Offset(startX, 0f),
                            end = Offset(startX, canvasHeight),
                            strokeWidth = 2.dp.toPx()
                        )
                    }

                    // Draw keyframe visual track inside clip:
                    // ● ------------------- ●
                    val kfTrackY = canvasHeight * 0.72f
                    val dotRadius = 4.dp.toPx()

                    // Start Keyframe Dot
                    drawCircle(
                        color = KeyframeDot,
                        radius = dotRadius,
                        center = Offset(startX + dotRadius + 4f, kfTrackY)
                    )

                    // Connecting keyframe line
                    drawLine(
                        color = KeyframeDot.copy(alpha = 0.6f),
                        start = Offset(startX + dotRadius * 2 + 4f, kfTrackY),
                        end = Offset(endX - dotRadius * 2 - 4f, kfTrackY),
                        strokeWidth = 1.5.dp.toPx()
                    )

                    // End Keyframe Dot
                    drawCircle(
                        color = KeyframeDot,
                        radius = dotRadius,
                        center = Offset(endX - dotRadius - 4f, kfTrackY)
                    )
                }

                // Draw Playhead scrubber line
                val playheadX = (playheadMs.toFloat() / durationSafe) * canvasWidth
                drawLine(
                    color = TimelinePlayhead,
                    start = Offset(playheadX, 0f),
                    end = Offset(playheadX, canvasHeight),
                    strokeWidth = 2.5.dp.toPx()
                )

                // Playhead top marker triangle
                drawCircle(
                    color = TimelinePlayhead,
                    radius = 6.dp.toPx(),
                    center = Offset(playheadX, 6.dp.toPx())
                )
            }
        }
    }
}
