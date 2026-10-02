package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProjectEntity
import com.example.model.KeyframeInterpolation
import com.example.model.KeyframeMode
import com.example.model.VideoMetadata
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

@Composable
fun HomeScreen(
    selectedVideo: VideoMetadata?,
    savedProjects: List<ProjectEntity>,
    onSelectVideo: (Uri, String) -> Unit,
    onLoadSample: () -> Unit,
    onStartAutoEdit: (KeyframeMode, Float, KeyframeInterpolation) -> Unit,
    onResumeProject: (ProjectEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onSelectVideo(uri, "My Video")
        }
    }

    var keyframeMode by remember { mutableStateOf(KeyframeMode.SIMPLE) }
    var zoomScale by remember { mutableFloatStateOf(1.12f) }
    var selectedInterpolation by remember { mutableStateOf(KeyframeInterpolation.EASE_IN_OUT) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(OmkarDarkBackground)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Premium Hero Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x2600E5FF),
                    border = BorderStroke(1.dp, Color(0x4D00E5FF)),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = OmkarPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SPEECH-AWARE AI VIDEO EDITOR",
                            color = OmkarPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Text(
                    text = "OMKAR",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = OmkarPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "AUTOMATIC VIDEO MAKER",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Speech Utterance Detection → Natural Thought Splits → Smooth Keyframes",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }

        // Video Import Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = OmkarSurfaceElevated),
                border = BorderStroke(1.dp, OmkarSurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (selectedVideo == null) {
                        // Empty State: Select Button + Try Sample Button
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0x3300E5FF), Color(0x0000E5FF))
                                    ),
                                    shape = CircleShape
                                )
                                .border(1.dp, OmkarPrimary.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = null,
                                tint = OmkarPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Select ONE Video",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Upload a talking-head video or reel to automatically detect sentences and animate keyframes.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                pickVideoLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OmkarPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_select_video")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoFile,
                                contentDescription = null,
                                tint = Color(0xFF00363D)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SELECT VIDEO",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF00363D)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onLoadSample,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, OmkarAccent.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_load_sample_video")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = OmkarAccent
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TRY SAMPLE TALKING-HEAD VIDEO",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = OmkarAccent
                            )
                        }
                    } else {
                        // Video Selected State: Details & Metadata
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Thumbnail Preview
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedVideo.thumbnailBitmap != null) {
                                    Image(
                                        bitmap = selectedVideo.thumbnailBitmap.asImageBitmap(),
                                        contentDescription = "Video Thumbnail",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Movie,
                                        contentDescription = null,
                                        tint = OmkarPrimary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Metadata Grid
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedVideo.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row {
                                    Text(
                                        text = "Duration: ",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = selectedVideo.durationFormatted,
                                        color = OmkarAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    text = "Resolution: ${selectedVideo.resolutionFormatted} (${selectedVideo.aspectRatio})",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "FPS: ${selectedVideo.fps} • Size: ${selectedVideo.fileSizeFormatted}",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Change video button
                        OutlinedButton(
                            onClick = {
                                pickVideoLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                        ) {
                            Text("Choose Different Video", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }

        // Analysis Settings (Configurable)
        if (selectedVideo != null) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = OmkarSurfaceElevated),
                    border = BorderStroke(1.dp, OmkarSurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = OmkarAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Automatic Edit Settings",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Keyframe Mode Selection (Simple vs Smart)
                        Text(
                            text = "Keyframe Mode",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = keyframeMode == KeyframeMode.SIMPLE,
                                onClick = { keyframeMode = KeyframeMode.SIMPLE },
                                label = { Text("Mode 1: Simple (100% → Zoom)", fontSize = 11.sp) },
                                modifier = Modifier.testTag("chip_mode_simple")
                            )
                            FilterChip(
                                selected = keyframeMode == KeyframeMode.SMART,
                                onClick = { keyframeMode = KeyframeMode.SMART },
                                label = { Text("Mode 2: Smart Motion", fontSize = 11.sp) },
                                modifier = Modifier.testTag("chip_mode_smart")
                            )
                        }

                        Text(
                            text = keyframeMode.description,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )

                        // Target Zoom Scale Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Automatic Zoom Amount",
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "${(zoomScale * 100).toInt()}%",
                                color = KeyframeDot,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Slider(
                            value = zoomScale,
                            onValueChange = { zoomScale = it },
                            valueRange = 1.05f..1.25f,
                            colors = SliderDefaults.colors(
                                thumbColor = KeyframeDot,
                                activeTrackColor = OmkarAccent
                            ),
                            modifier = Modifier.testTag("slider_home_zoom")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Interpolation Curve
                        Text(
                            text = "Keyframe Interpolation",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            KeyframeInterpolation.values().forEach { interp ->
                                FilterChip(
                                    selected = selectedInterpolation == interp,
                                    onClick = { selectedInterpolation = interp },
                                    label = { Text(interp.displayName, fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // Big [ AUTOMATIC EDIT ] Action Button
            item {
                Button(
                    onClick = {
                        onStartAutoEdit(keyframeMode, zoomScale, selectedInterpolation)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OmkarPrimary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("btn_automatic_edit")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF00363D),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "AUTOMATIC EDIT",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp,
                        color = Color(0xFF00363D)
                    )
                }
            }
        }

        // Recent Projects Section (Room Database)
        if (savedProjects.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Recent Projects",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
            }

            items(savedProjects) { project ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141C2E)),
                    border = BorderStroke(1.dp, OmkarSurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = OmkarSecondary.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = OmkarSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = project.title,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = TextPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = "${project.clipCount} Clips • Keyframes ${(project.defaultZoomScale * 100).toInt()}%",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        OutlinedButton(
                            onClick = { onResumeProject(project) },
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Open", fontSize = 11.sp, color = OmkarPrimary)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
