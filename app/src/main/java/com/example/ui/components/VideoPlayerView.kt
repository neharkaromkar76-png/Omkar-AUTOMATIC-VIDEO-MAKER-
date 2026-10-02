package com.example.ui.components

import android.graphics.Matrix
import android.media.MediaPlayer
import android.net.Uri
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.VideoClip
import com.example.model.VideoMetadata
import com.example.ui.theme.OmkarAccent
import com.example.ui.theme.OmkarPrimary

@Composable
fun VideoPlayerView(
    videoMetadata: VideoMetadata,
    currentClip: VideoClip?,
    currentScale: Float,
    playheadMs: Long,
    isPlaying: Boolean,
    onSeekComplete: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uri = remember(videoMetadata.uriString) { Uri.parse(videoMetadata.uriString) }

    val mediaPlayer = remember {
        MediaPlayer().apply {
            try {
                setDataSource(context, uri)
                isLooping = true
                prepareAsync()
            } catch (_: Exception) {}
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer.stop()
                mediaPlayer.release()
            } catch (_: Exception) {}
        }
    }

    // Playback synchronization with timeline playhead
    LaunchedEffect(isPlaying) {
        try {
            if (isPlaying) {
                if (!mediaPlayer.isPlaying) mediaPlayer.start()
            } else {
                if (mediaPlayer.isPlaying) mediaPlayer.pause()
            }
        } catch (_: Exception) {}
    }

    // Sync playhead seek when user scrubs or skips
    LaunchedEffect(playheadMs) {
        try {
            val currentPos = mediaPlayer.currentPosition.toLong()
            if (kotlin.math.abs(currentPos - playheadMs) > 250) {
                mediaPlayer.seekTo(playheadMs.toInt())
            }
        } catch (_: Exception) {}
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Video Surface rendered with Compose Hardware graphicsLayer scale
        // This gives real-time keyframe animation smoothly at 60fps!
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .graphicsLayer {
                    // Center-crop protection: Scale smoothly from keyframe interpolation
                    scaleX = currentScale
                    scaleY = currentScale
                    clip = true
                },
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    SurfaceView(ctx).apply {
                        holder.addCallback(object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {
                                try {
                                    mediaPlayer.setDisplay(holder)
                                } catch (_: Exception) {}
                            }

                            override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {}

                            override fun surfaceDestroyed(holder: SurfaceHolder) {
                                try {
                                    mediaPlayer.setDisplay(null)
                                } catch (_: Exception) {}
                            }
                        })
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("video_player_surface")
            )
        }

        // Live Scale & Keyframe Overlay Badges
        Surface(
            color = Color(0xCC0B0F19),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Text(
                text = "ZOOM: ${(currentScale * 100).toInt()}%",
                color = OmkarAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        if (currentClip != null) {
            Surface(
                color = Color(0xCC0B0F19),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                Text(
                    text = "CLIP #${currentClip.clipIndex + 1} (${currentClip.motionType.displayName.split(" ").first()})",
                    color = OmkarPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
