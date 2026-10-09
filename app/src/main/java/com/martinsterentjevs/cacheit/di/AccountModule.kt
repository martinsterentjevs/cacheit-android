package com.martinsterentjevs.cacheit.di

import com.martinsterentjevs.cacheit.data.account.AccountRepository
import com.martinsterentjevs.cacheit.data.account.AccountRepositoryImpl
import com.martinsterentjevs.cacheit.network.account.AccountApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AccountModule {
    @Provides
    @Singleton
    fun provideAccountRepository(accountApi: AccountApi): AccountRepository {
        return AccountRepositoryImpl(accountApi)
    }
}