package com.martinsterentjevs.cacheit.di

import com.martinsterentjevs.cacheit.network.websockets.CacheItWebSocketClient
import com.martinsterentjevs.cacheit.network.websockets.CacheItWsClient
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
@Module
@InstallIn(SingletonComponent::class)
abstract class WebSocketModule {
    @Binds
    @Singleton
    abstract fun bindWebSocketClient(impl: CacheItWsClient): CacheItWebSocketClient
}