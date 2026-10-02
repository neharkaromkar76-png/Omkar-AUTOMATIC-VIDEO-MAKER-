package com.example.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.KeyframeInterpolation
import com.example.model.MotionType
import com.example.model.VideoClip
import com.example.ui.theme.KeyframeDot
import com.example.ui.theme.OmkarAccent
import com.example.ui.theme.OmkarPrimary
import com.example.ui.theme.OmkarSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun KeyframeEditorDialog(
    clip: VideoClip,
    onDismiss: () -> Unit,
    onSave: (startScale: Float, endScale: Float, interpolation: KeyframeInterpolation, motionType: MotionType) -> Unit
) {
    var startScale by remember { mutableFloatStateOf(clip.startKeyframe.scale) }
    var endScale by remember { mutableFloatStateOf(clip.endKeyframe.scale) }
    var selectedInterpolation by remember { mutableStateOf(clip.interpolation) }
    var selectedMotionType by remember { mutableStateOf(clip.motionType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OmkarSurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "Edit Keyframes (Clip #${clip.clipIndex + 1})",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Configure start & end zoom keyframe animations for this speech segment.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Start Scale
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Start Keyframe Scale", color = TextPrimary, fontSize = 13.sp)
                    Text(
                        "${(startScale * 100).toInt()}%",
                        color = OmkarAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Slider(
                    value = startScale,
                    onValueChange = { startScale = it },
                    valueRange = 1.0f..1.30f,
                    colors = SliderDefaults.colors(
                        thumbColor = KeyframeDot,
                        activeTrackColor = OmkarAccent
                    ),
                    modifier = Modifier.testTag("slider_start_scale")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // End Scale
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("End Keyframe Scale", color = TextPrimary, fontSize = 13.sp)
                    Text(
                        "${(endScale * 100).toInt()}%",
                        color = OmkarAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Slider(
                    value = endScale,
                    onValueChange = { endScale = it },
                    valueRange = 1.0f..1.30f,
                    colors = SliderDefaults.colors(
                        thumbColor = KeyframeDot,
                        activeTrackColor = OmkarAccent
                    ),
                    modifier = Modifier.testTag("slider_end_scale")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Motion Preset Shortcuts
                Text("Motion Direction", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = startScale < endScale,
                        onClick = {
                            startScale = 1.0f
                            endScale = 1.12f
                            selectedMotionType = MotionType.ZOOM_IN
                        },
                        label = { Text("Zoom In (100% → 112%)", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = startScale > endScale,
                        onClick = {
                            startScale = 1.12f
                            endScale = 1.0f
                            selectedMotionType = MotionType.ZOOM_OUT
                        },
                        label = { Text("Zoom Out", fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interpolation Curve
                Text("Interpolation Curve", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KeyframeInterpolation.values().forEach { interp ->
                        FilterChip(
                            selected = selectedInterpolation == interp,
                            onClick = { selectedInterpolation = interp },
                            label = { Text(interp.displayName, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(startScale, endScale, selectedInterpolation, selectedMotionType)
                },
                colors = ButtonDefaults.buttonColors(containerColor = OmkarPrimary),
                modifier = Modifier.testTag("btn_save_keyframe")
            ) {
                Text("Apply Keyframe", color = Color(0xFF00363D), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
