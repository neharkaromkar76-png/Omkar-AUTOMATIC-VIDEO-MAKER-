package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val videoUri: String,
    val durationMs: Long,
    val clipCount: Int,
    val keyframeMode: String,
    val defaultZoomScale: Float,
    val lastExportPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
