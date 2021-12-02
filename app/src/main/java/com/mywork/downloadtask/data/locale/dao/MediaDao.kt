package com.mywork.downloadtask.data.locale.dao

import androidx.room.*
import com.mywork.downloadtask.data.remote.model.Media
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMedia(medias: List<Media>)

    @Update
    suspend fun updateMedia(media: Media)

    @Query("DELETE FROM medias")
    suspend fun deleteAllMedia()

    @Delete
    suspend fun deleteMedia(media: Media)

    @Query("SELECT * FROM medias")
    fun getAllMediaAsync(): Flow<List<Media>>

    @Query("SELECT * FROM medias WHERE name = :name")
    suspend fun getMedia(name: String): Media
}