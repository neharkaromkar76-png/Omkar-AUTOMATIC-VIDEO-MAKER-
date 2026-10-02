package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AnalysisProgressState
import com.example.model.AnalysisStep
import com.example.ui.theme.KeyframeDot
import com.example.ui.theme.OmkarAccent
import com.example.ui.theme.OmkarError
import com.example.ui.theme.OmkarPrimary
import com.example.ui.theme.OmkarSuccess
import com.example.ui.theme.OmkarSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AnalysisProgressDialog(
    state: AnalysisProgressState,
    onDismiss: () -> Unit
) {
    if (!state.isAnalyzing && state.errorMessage == null) return

    val isError = state.errorMessage != null
    val currentStep = state.currentStep

    AlertDialog(
        onDismissRequest = { if (isError) onDismiss() },
        containerColor = OmkarSurfaceElevated,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = if (isError) OmkarError else OmkarPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isError) "Speech Analysis Notice" else "AI Automatic Speech Analysis",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isError) {
                    Text(
                        text = state.errorMessage ?: "No clear speech boundaries were detected. You can still use manual Split mode.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                } else {
                    Text(
                        text = "Analyzing spoken sentences, utterance cadence, and calculating natural split boundaries...",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .testTag("analysis_progress_bar"),
                        color = OmkarPrimary,
                        trackColor = Color(0xFF1E2A40)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = state.statusText,
                            color = OmkarAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${(state.progress * 100).toInt()}%",
                            color = OmkarPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Pipeline Step Checklist
                    val steps = listOf(
                        AnalysisStep.PREPARING_VIDEO to "Preparing video...",
                        AnalysisStep.EXTRACTING_AUDIO to "Extracting audio...",
                        AnalysisStep.ANALYZING_SPEECH to "Analyzing speech...",
                        AnalysisStep.DETECTING_WORDS to "Detecting words...",
                        AnalysisStep.UNDERSTANDING_BOUNDARIES to "Understanding sentence boundaries...",
                        AnalysisStep.FINDING_SPLIT_POINTS to "Finding split points...",
                        AnalysisStep.CREATING_CLIPS to "Creating clips...",
                        AnalysisStep.ADDING_KEYFRAMES to "Adding keyframes...",
                        AnalysisStep.PREPARING_PREVIEW to "Preparing preview..."
                    )

                    steps.forEach { (step, label) ->
                        val isDone = state.progress >= step.weight
                        val isCurrent = currentStep == step

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 3.dp)
                        ) {
                            if (isDone) {
                                Surface(
                                    shape = CircleShape,
                                    color = OmkarSuccess,
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            } else if (isCurrent) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = OmkarPrimary
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(Color(0xFF26334D), CircleShape)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = label,
                                fontSize = 12.sp,
                                color = if (isDone || isCurrent) TextPrimary else TextSecondary.copy(alpha = 0.6f),
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isError) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = OmkarPrimary),
                    modifier = Modifier.testTag("btn_dismiss_analysis_error")
                ) {
                    Text("OK", color = Color(0xFF00363D), fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}
