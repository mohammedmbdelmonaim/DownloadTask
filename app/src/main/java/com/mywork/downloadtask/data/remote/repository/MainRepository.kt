package com.mywork.downloadtask.data.remote.repository

import androidx.room.withTransaction
import com.mywork.downloadtask.data.locale.MediaDataBase
import com.mywork.downloadtask.data.locale.dao.MediaDao
import com.mywork.downloadtask.data.remote.api.ApiService
import com.mywork.downloadtask.data.remote.model.Media
import com.mywork.downloadtask.utils.networkBoundResource
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject

class MainRepository @Inject constructor(
    private val apiService: ApiService,
    private val dataBase: MediaDataBase,
    private val mediaDao: MediaDao
) {

    suspend fun getMedias(): List<Media> {
        return apiService.getMedias()
    }

    fun getMediasRX(): Single<List<Media>> {
        return apiService.getMediasRX()
    }

    //get medias from database then remote to solve delay response in future
    fun getMediasWithLocale() = networkBoundResource(
        query = {
            mediaDao.getAllMediaAsync()
        },
        fetch = {
            apiService.getMedias()
        },
        saveFetchResult = { medias ->
            dataBase.withTransaction {
                mediaDao.deleteAllMedia()
                mediaDao.insertAllMedia(medias)
            }
        }
    )
}