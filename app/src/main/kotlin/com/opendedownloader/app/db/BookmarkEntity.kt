package com.opendedownloader.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
