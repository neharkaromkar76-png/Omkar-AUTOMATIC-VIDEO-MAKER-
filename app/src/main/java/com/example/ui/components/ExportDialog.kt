package com.example.ui.components

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.model.ExportFps
import com.example.model.ExportOptions
import com.example.model.ExportProgressState
import com.example.model.ExportQuality
import com.example.model.ExportResolution
import com.example.ui.theme.OmkarAccent
import com.example.ui.theme.OmkarPrimary
import com.example.ui.theme.OmkarSuccess
import com.example.ui.theme.OmkarSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File

@Composable
fun ExportDialog(
    isOpen: Boolean,
    exportState: ExportProgressState,
    onDismiss: () -> Unit,
    onStartExport: (ExportOptions) -> Unit,
    onDismissProgress: () -> Unit
) {
    val context = LocalContext.current
    var selectedRes by remember { mutableStateOf(ExportResolution.ORIGINAL) }
    var selectedQuality by remember { mutableStateOf(ExportQuality.HIGH) }
    var selectedFps by remember { mutableStateOf(ExportFps.ORIGINAL) }

    // If export is currently running or completed, show the progress dialog
    if (exportState.isExporting || exportState.isComplete || exportState.error != null) {
        AlertDialog(
            onDismissRequest = {
                if (!exportState.isExporting) onDismissProgress()
            },
            containerColor = OmkarSurfaceElevated,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (exportState.isComplete) Icons.Default.CheckCircle else Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = if (exportState.isComplete) OmkarSuccess else OmkarPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (exportState.isComplete) "Export Complete!" else "Exporting Video...",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (exportState.error != null) {
                        Text(
                            text = "Export failed: ${exportState.error}",
                            color = Color(0xFFFF5252),
                            fontSize = 13.sp
                        )
                    } else if (exportState.isComplete) {
                        Text(
                            text = "Finished video with speech splits and keyframe animations is ready.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val mb = exportState.outputFileSizeBytes / (1024f * 1024f)
                        Text(
                            text = String.format("File size: %.1f MB • Format: MP4 (H.264)", mb),
                            color = OmkarAccent,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    } else {
                        Text(
                            text = exportState.currentStage,
                            color = OmkarAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        LinearProgressIndicator(
                            progress = { exportState.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = OmkarPrimary,
                            trackColor = Color(0xFF1E2A40)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${(exportState.progress * 100).toInt()}%",
                            color = OmkarPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                if (exportState.isComplete && exportState.outputFilePath != null) {
                    Row {
                        Button(
                            onClick = {
                                try {
                                    val file = File(exportState.outputFilePath)
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "video/mp4"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
                                } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OmkarPrimary),
                            modifier = Modifier.testTag("btn_share_exported_video")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share", color = Color(0xFF00363D), fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = onDismissProgress,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26334D))
                        ) {
                            Text("Done", color = Color.White)
                        }
                    }
                } else if (exportState.error != null) {
                    Button(onClick = onDismissProgress) {
                        Text("Close")
                    }
                }
            }
        )
    }

    // Config Dialog before export starts
    if (isOpen && !exportState.isExporting) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = OmkarSurfaceElevated,
            shape = RoundedCornerShape(18.dp),
            title = {
                Text(
                    text = "Export Finished Video",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Render keyframed zooms and export complete synchronized MP4 video.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Resolution
                    Text("Resolution", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ExportResolution.values().forEach { res ->
                            FilterChip(
                                selected = selectedRes == res,
                                onClick = { selectedRes = res },
                                label = { Text(res.label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quality
                    Text("Quality & Bitrate", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ExportQuality.values().forEach { q ->
                            FilterChip(
                                selected = selectedQuality == q,
                                onClick = { selectedQuality = q },
                                label = { Text(q.label.split(" ").first(), fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // FPS
                    Text("Frame Rate", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ExportFps.values().forEach { f ->
                            FilterChip(
                                selected = selectedFps == f,
                                onClick = { selectedFps = f },
                                label = { Text(f.label, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onStartExport(
                            ExportOptions(
                                resolution = selectedRes,
                                quality = selectedQuality,
                                fps = selectedFps
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OmkarPrimary),
                    modifier = Modifier.testTag("btn_confirm_export")
                ) {
                    Text("Start Export", color = Color(0xFF00363D), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
