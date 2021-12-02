package com.mywork.downloadtask.di.module

import com.mywork.downloadtask.data.remote.api.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import retrofit2.Retrofit

@InstallIn(ActivityRetainedComponent::class)
@Module
class ApiServiceModule {
    @Provides
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}