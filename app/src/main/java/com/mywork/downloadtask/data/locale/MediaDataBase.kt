package com.mywork.downloadtask.data.locale

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mywork.downloadtask.data.locale.dao.MediaDao
import com.mywork.downloadtask.data.remote.model.Media

@Database(entities = [Media::class] , version = 3 , exportSchema = false)//exportSchema->make many versions in history
abstract class MediaDataBase : RoomDatabase(){
    abstract fun mediaDao(): MediaDao
}