package com.opendedownloader.app.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface BookmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(bookmark: BookmarkEntity): Long

    @Update
    fun update(bookmark: BookmarkEntity)

    @Delete
    fun delete(bookmark: BookmarkEntity)

    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAll(): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllLiveData(): LiveData<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE url = :url LIMIT 1")
    fun getByUrl(url: String): BookmarkEntity?
}
