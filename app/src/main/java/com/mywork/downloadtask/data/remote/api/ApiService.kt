package com.mywork.downloadtask.data.remote.api
import com.mywork.downloadtask.data.remote.model.Media
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET

interface ApiService {
// coroutine
    @GET("movies")
    suspend fun getMedias(): List<Media>

    //rxjava
    @GET("movies")
    fun getMediasRX(): Single<List<Media>>
}