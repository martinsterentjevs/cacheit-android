package com.martinsterentjevs.cacheit.data.note.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert


/**
 * @Upsert requires Room 2.5+ - check libs.versions.toml before assuming this compiles as-is.
 * If the project's on an older Room version, swap to @Insert(onConflict = OnConflictStrategy.REPLACE).
 */
@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY lastModifiedAt DESC")
    suspend fun getAll(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE noteId = :noteId LIMIT 1")
    suspend fun getById(noteId: String): NoteEntity?

    @Upsert
    suspend fun upsert(note: NoteEntity)

    @Upsert
    suspend fun upsertAll(notes: List<NoteEntity>)

    @Query("DELETE FROM notes WHERE noteId = :noteId")
    suspend fun deleteById(noteId: String)

    @Query("DELETE FROM notes")
    suspend fun clearAll() // needed for the "clear-out" in-app wipe action
}
