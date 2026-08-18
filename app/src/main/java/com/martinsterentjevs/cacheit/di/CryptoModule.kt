package com.martinsterentjevs.cacheit.di

import com.martinsterentjevs.cacheit.services.crypto.CryptoService
import com.martinsterentjevs.cacheit.services.crypto.CryptoServiceImpl
import com.martinsterentjevs.cacheit.services.security.SecurityService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CryptoModule {

    @Provides
    @Singleton
    fun provideCryptoService(securityService: SecurityService): CryptoService =
        CryptoServiceImpl(securityService)
}