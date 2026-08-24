package com.martinsterentjevs.cacheit.di

import android.content.Context
import androidx.room.Room
import com.martinsterentjevs.cacheit.data.note.local.CacheItDatabase
import com.martinsterentjevs.cacheit.data.note.local.NoteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CacheItDatabase =
        Room.databaseBuilder(context, CacheItDatabase::class.java, "cacheit.db")
            // MVP-acceptable: no migration path exists yet. Revisit before schema v2 - this
            // wipes the local cache (not the account) on any schema change, silently.
            .fallbackToDestructiveMigration(false)
            .build()

    @Provides
    fun provideNoteDao(database: CacheItDatabase): NoteDao = database.noteDao()
}
