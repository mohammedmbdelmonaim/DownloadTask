package com.mywork.downloadtask.di.module

import android.app.Application
import androidx.room.Room
import com.mywork.downloadtask.data.locale.MediaDataBase
import com.mywork.downloadtask.data.locale.dao.MediaDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
class DataBaseModule {
    @Provides
    @Singleton
    fun provideDB(application: Application?): MediaDataBase {
        return Room.databaseBuilder(application!!, MediaDataBase::class.java, "media_db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideDao(mediaDataBase: MediaDataBase): MediaDao {
        return mediaDataBase.mediaDao()
    }
}