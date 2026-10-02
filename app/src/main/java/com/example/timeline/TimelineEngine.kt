package com.example.timeline

import com.example.keyframe.KeyframeEngine
import com.example.model.Keyframe
import com.example.model.KeyframeInterpolation
import com.example.model.KeyframeMode
import com.example.model.MotionType
import com.example.model.TimelineEditorState
import com.example.model.VideoClip
import java.util.UUID

class TimelineEngine(
    private val keyframeEngine: KeyframeEngine = KeyframeEngine()
) {

    /**
     * Splits the active clip at [splitTimeMs].
     * Both resulting clips receive newly calculated keyframes!
     */
    fun addSplitAt(state: TimelineEditorState, splitTimeMs: Long): TimelineEditorState {
        val targetClip = state.clips.firstOrNull { it.containsTime(splitTimeMs) } ?: return state
        // Enforce minimal length to prevent micro-cuts
        if (splitTimeMs - targetClip.startMs < 400 || targetClip.endMs - splitTimeMs < 400) {
            return state
        }

        val pushUndo = pushHistory(state)
        val clipIndex = state.clips.indexOf(targetClip)

        val firstClip = targetClip.copy(
            id = UUID.randomUUID().toString(),
            endMs = splitTimeMs,
            startKeyframe = Keyframe(timestampMs = targetClip.startMs, scale = 1.0f),
            endKeyframe = Keyframe(timestampMs = splitTimeMs, scale = state.defaultZoomScale)
        )

        val secondClip = targetClip.copy(
            id = UUID.randomUUID().toString(),
            startMs = splitTimeMs,
            startKeyframe = Keyframe(timestampMs = splitTimeMs, scale = 1.0f),
            endKeyframe = Keyframe(timestampMs = targetClip.endMs, scale = state.defaultZoomScale)
        )

        val newClips = state.clips.toMutableList()
        newClips.removeAt(clipIndex)
        newClips.add(clipIndex, secondClip)
        newClips.add(clipIndex, firstClip)

        val reindexedClips = newClips.mapIndexed { idx, c -> c.copy(clipIndex = idx) }

        return pushUndo.copy(
            clips = reindexedClips,
            selectedClipId = firstClip.id,
            redoStack = emptyList()
        )
    }

    /**
     * Removes the split after [clipIndex] by merging it with the subsequent clip.
     */
    fun removeSplitAfter(state: TimelineEditorState, clipIndex: Int): TimelineEditorState {
        if (clipIndex < 0 || clipIndex >= state.clips.size - 1) return state

        val pushUndo = pushHistory(state)
        val current = state.clips[clipIndex]
        val next = state.clips[clipIndex + 1]

        val merged = current.copy(
            endMs = next.endMs,
            spokenText = "${current.spokenText} ${next.spokenText}".trim(),
            endKeyframe = Keyframe(timestampMs = next.endMs, scale = state.defaultZoomScale)
        )

        val newClips = state.clips.toMutableList()
        newClips.removeAt(clipIndex + 1)
        newClips[clipIndex] = merged

        val reindexed = newClips.mapIndexed { idx, c -> c.copy(clipIndex = idx) }

        return pushUndo.copy(
            clips = reindexed,
            selectedClipId = merged.id,
            redoStack = emptyList()
        )
    }

    /**
     * Shifts split boundary between clip at [clipIndex] and [clipIndex + 1] by [deltaMs].
     */
    fun moveSplitBoundary(state: TimelineEditorState, clipIndex: Int, deltaMs: Long): TimelineEditorState {
        if (clipIndex < 0 || clipIndex >= state.clips.size - 1) return state

        val current = state.clips[clipIndex]
        val next = state.clips[clipIndex + 1]
        val newBoundary = current.endMs + deltaMs

        if (newBoundary - current.startMs < 300 || next.endMs - newBoundary < 300) {
            return state
        }

        val pushUndo = pushHistory(state)
        val updatedCurrent = current.copy(
            endMs = newBoundary,
            endKeyframe = current.endKeyframe.copy(timestampMs = newBoundary)
        )
        val updatedNext = next.copy(
            startMs = newBoundary,
            startKeyframe = next.startKeyframe.copy(timestampMs = newBoundary)
        )

        val newClips = state.clips.toMutableList()
        newClips[clipIndex] = updatedCurrent
        newClips[clipIndex + 1] = updatedNext

        return pushUndo.copy(
            clips = newClips,
            redoStack = emptyList()
        )
    }

    /**
     * Updates keyframe settings on a specific clip.
     */
    fun updateClipKeyframe(
        state: TimelineEditorState,
        clipId: String,
        startScale: Float,
        endScale: Float,
        interpolation: KeyframeInterpolation,
        motionType: MotionType
    ): TimelineEditorState {
        val target = state.clips.firstOrNull { it.id == clipId } ?: return state
        val pushUndo = pushHistory(state)

        val updated = keyframeEngine.updateClipKeyframe(target, startScale, endScale, interpolation, motionType)
        val newClips = state.clips.map { if (it.id == clipId) updated else it }

        return pushUndo.copy(
            clips = newClips,
            redoStack = emptyList()
        )
    }

    /**
     * Re-applies automatic keyframe generation across all clips.
     */
    fun reapplyKeyframes(
        state: TimelineEditorState,
        mode: KeyframeMode,
        targetScale: Float,
        interpolation: KeyframeInterpolation
    ): TimelineEditorState {
        val pushUndo = pushHistory(state)
        val keyframed = keyframeEngine.applyAutomaticKeyframes(
            clips = state.clips,
            mode = mode,
            targetScale = targetScale,
            interpolation = interpolation
        )

        return pushUndo.copy(
            clips = keyframed,
            keyframeMode = mode,
            defaultZoomScale = targetScale,
            defaultInterpolation = interpolation,
            redoStack = emptyList()
        )
    }

    /**
     * Restores original AI-generated timeline splits and keyframes.
     */
    fun resetAutomaticEdit(state: TimelineEditorState): TimelineEditorState {
        if (state.originalAiClips.isEmpty()) return state
        val pushUndo = pushHistory(state)

        return pushUndo.copy(
            clips = state.originalAiClips,
            selectedClipId = state.originalAiClips.firstOrNull()?.id,
            redoStack = emptyList()
        )
    }

    fun undo(state: TimelineEditorState): TimelineEditorState {
        if (state.undoStack.isEmpty()) return state
        val previousClips = state.undoStack.last()
        val newUndoStack = state.undoStack.dropLast(1)
        val newRedoStack = state.redoStack + listOf(state.clips)

        return state.copy(
            clips = previousClips,
            undoStack = newUndoStack,
            redoStack = newRedoStack
        )
    }

    fun redo(state: TimelineEditorState): TimelineEditorState {
        if (state.redoStack.isEmpty()) return state
        val nextClips = state.redoStack.last()
        val newRedoStack = state.redoStack.dropLast(1)
        val newUndoStack = state.undoStack + listOf(state.clips)

        return state.copy(
            clips = nextClips,
            undoStack = newUndoStack,
            redoStack = newRedoStack
        )
    }

    private fun pushHistory(state: TimelineEditorState): TimelineEditorState {
        val maxHistory = 20
        val newUndo = (state.undoStack + listOf(state.clips)).takeLast(maxHistory)
        return state.copy(undoStack = newUndo)
    }
}
