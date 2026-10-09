package com.martinsterentjevs.cacheit.data.note.local

import androidx.room.Database
import androidx.room.RoomDatabase


/**
 * FLAG: if the project already has a Room database class (for anything else - device sessions,
 * settings, etc.), add NoteEntity/NoteDao to THAT class instead of creating a second database.
 * Room supports multiple DBs but there's rarely a good reason to split app state that way, and
 * two separate .db files means two separate places clear-out has to wipe.
 */
@Database(entities = [NoteEntity::class], version = 1, exportSchema = false)
abstract class CacheItDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}
