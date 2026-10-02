package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VideoClip
import com.example.ui.theme.KeyframeDot
import com.example.ui.theme.OmkarAccent
import com.example.ui.theme.OmkarPrimary
import com.example.ui.theme.OmkarSurfaceBorder
import com.example.ui.theme.OmkarSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ClipItemCard(
    clip: VideoClip,
    isSelected: Boolean,
    isLastClip: Boolean,
    isPlayheadInside: Boolean,
    onSelect: () -> Unit,
    onEditKeyframe: () -> Unit,
    onSplitAtPlayhead: () -> Unit,
    onRemoveSplit: () -> Unit,
    onNudgeSplit: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF16233B) else OmkarSurfaceElevated
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) OmkarPrimary else OmkarSurfaceBorder
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("clip_item_card_${clip.clipIndex}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Clip Index, Duration, and Keyframe Summary
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) OmkarPrimary else Color(0xFF26334D),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "#${clip.clipIndex + 1}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isSelected) Color(0xFF00363D) else Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = clip.formattedRange(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = OmkarAccent
                    )
                    Text(
                        text = String.format("Duration: %.2fs", clip.durationMs / 1000f),
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Edit Keyframe Button
                IconButton(
                    onClick = onEditKeyframe,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_edit_keyframe_${clip.clipIndex}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Edit Keyframe",
                        tint = KeyframeDot
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Utterance / Spoken Sentence Bubble
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF0D1424),
                border = BorderStroke(1.dp, Color(0xFF1E2C4A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Speech Segment",
                        tint = OmkarPrimary,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (clip.spokenText.isNotBlank()) clip.spokenText else "Spoken sentence boundary",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Keyframe Motion Pill & Values
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0x33FFB300)
                    ) {
                        Text(
                            text = "● ${clip.startKeyframe.scalePercentage}% → ● ${clip.endKeyframe.scalePercentage}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = OmkarAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = clip.interpolation.displayName,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                // Split & Merge Actions
                Row {
                    if (isPlayheadInside) {
                        IconButton(
                            onClick = onSplitAtPlayhead,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("btn_split_clip_${clip.clipIndex}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCut,
                                contentDescription = "Split At Playhead",
                                tint = OmkarPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (!isLastClip) {
                        IconButton(
                            onClick = onRemoveSplit,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("btn_remove_split_${clip.clipIndex}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallMerge,
                                contentDescription = "Merge With Next Clip",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Boundary Nudge Row for Fine-Tuning
            if (!isLastClip && isSelected) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Fine-tune split:",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    OutlinedButton(
                        onClick = { onNudgeSplit(-100L) },
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("-100ms", fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    OutlinedButton(
                        onClick = { onNudgeSplit(100L) },
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("+100ms", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
