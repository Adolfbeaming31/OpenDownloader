package com.opendedownloader.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val fileName: String,
    val filePath: String,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val status: String, // PENDING, DOWNLOADING, PAUSED, COMPLETED, FAILED
    val sha256: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)
