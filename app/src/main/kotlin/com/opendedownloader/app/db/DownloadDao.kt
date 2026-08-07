package com.opendedownloader.app.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(download: DownloadEntity): Long

    @Update
    fun update(download: DownloadEntity)

    @Delete
    fun delete(download: DownloadEntity)

    @Query("SELECT * FROM downloads WHERE id = :id")
    fun getById(id: Long): DownloadEntity?

    @Query("SELECT * FROM downloads ORDER BY timestamp DESC")
    fun getAll(): List<DownloadEntity>

    @Query("SELECT * FROM downloads ORDER BY timestamp DESC")
    fun getAllLiveData(): LiveData<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE url = :url LIMIT 1")
    fun getByUrl(url: String): DownloadEntity?
}
