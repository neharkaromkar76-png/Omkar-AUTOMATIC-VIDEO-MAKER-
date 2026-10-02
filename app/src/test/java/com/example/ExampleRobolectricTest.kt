package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.Keyframe
import com.example.model.KeyframeInterpolation
import com.example.model.VideoClip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("OMKAR AUTOMATIC VIDEO MAKER", appName)
    }

    @Test
    fun `test keyframe interpolation calculation`() {
        val clip = VideoClip(
            id = "test_clip_1",
            clipIndex = 0,
            startMs = 0L,
            endMs = 4000L,
            spokenText = "Test utterance",
            startKeyframe = Keyframe(timestampMs = 0L, scale = 1.0f),
            endKeyframe = Keyframe(timestampMs = 4000L, scale = 1.12f),
            interpolation = KeyframeInterpolation.LINEAR
        )

        // At start
        assertEquals(1.0f, clip.getScaleAt(0L), 0.001f)
        // At mid-point (2000ms = 50%)
        assertEquals(1.06f, clip.getScaleAt(2000L), 0.001f)
        // At end (4000ms = 100%)
        assertEquals(1.12f, clip.getScaleAt(4000L), 0.001f)
    }
}
