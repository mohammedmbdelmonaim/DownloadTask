package com.mywork.downloadtask.ui.media

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mywork.downloadtask.data.remote.model.Media
import com.mywork.downloadtask.data.remote.repository.MainRepository
import com.mywork.downloadtask.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

@HiltViewModel
class MediaViewModel @Inject constructor(private val repository: MainRepository) : ViewModel() {
    private val mediasMutableLiveData = MutableLiveData<Resource<List<Media>>>()
    val mediasLiveData: LiveData<Resource<List<Media>>> get() = mediasMutableLiveData


    fun getMedias() =
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            val mediaFlow = repository.getMediasWithLocale()
            mediaFlow.collect { medias ->
                withContext(Dispatchers.Main) {
                    mediasMutableLiveData.value = medias
                }
            }
        }

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable is SocketException) {
            //very bad internet
            mediasMutableLiveData.postValue(Resource.Error(null, throwable.message!!))
        }

        if (throwable is SocketTimeoutException) {
            //timeout
            mediasMutableLiveData.postValue(Resource.Error(null, throwable.message!!))
        }

        if (throwable is HttpException) {
            // parse error body message from
            //throwable.response().errorBody()
            mediasMutableLiveData.postValue(Resource.Error(null, throwable.message!!))
        }

        if (throwable is UnknownHostException) {
            // probably no internet or your base url is wrong
            mediasMutableLiveData.postValue(Resource.Error(null, throwable.message!!))

        }
    }
//
    override fun onCleared() {
        super.onCleared()
        getMedias().cancel()
    }
}