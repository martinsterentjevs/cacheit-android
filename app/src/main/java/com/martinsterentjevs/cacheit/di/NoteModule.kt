package com.martinsterentjevs.cacheit.di

import com.martinsterentjevs.cacheit.data.note.NoteRepository
import com.martinsterentjevs.cacheit.data.note.NoteRepositoryImpl
import com.martinsterentjevs.cacheit.data.note.local.NoteDao
import com.martinsterentjevs.cacheit.network.note.NoteApi
import com.martinsterentjevs.cacheit.services.crypto.CryptoService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NoteModule {
    @Provides
    @Singleton
    fun provideNoteRepository(noteApi: NoteApi,noteDao: NoteDao,cryptoService: CryptoService): NoteRepository = NoteRepositoryImpl(noteApi,noteDao,cryptoService)
}