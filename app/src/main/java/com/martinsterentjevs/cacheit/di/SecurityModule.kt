package com.martinsterentjevs.cacheit.di

import android.content.Context
import com.martinsterentjevs.cacheit.services.security.SecurityService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideSecurityService(@ApplicationContext context: Context): SecurityService =
        SecurityService(context)
}