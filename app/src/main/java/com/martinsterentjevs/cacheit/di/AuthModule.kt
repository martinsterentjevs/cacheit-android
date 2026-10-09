package com.martinsterentjevs.cacheit.di

import com.martinsterentjevs.cacheit.data.auth.AuthRepository
import com.martinsterentjevs.cacheit.data.auth.AuthRepositoryImpl
import com.martinsterentjevs.cacheit.network.auth.AuthApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideAuthRepository(authApi: AuthApi): AuthRepository = AuthRepositoryImpl(authApi)
}