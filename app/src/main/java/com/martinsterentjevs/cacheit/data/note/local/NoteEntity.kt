package com.martinsterentjevs.cacheit.data.note.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.martinsterentjevs.cacheit.network.note.NoteDto

/**
 * Local cache row - encrypted fields only, matching NoteDto exactly. Never stores decrypted
 * content; the cache is at-rest encrypted the same way the server is, per ADR 0002 and the
 * "local storage is encrypted at rest" constraint that ADR operates under.
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val noteId: String,
    val userId: String,
    val lastModifiedAt: String,
    val isDeleted: Boolean,
    val hasHistory: Boolean,
    val encTitle: String,
    val encBody: String?,
    val encDrawing: String?,
    val lockedByDeviceId: String?,
    val lockedAt: String?,
)

fun NoteDto.toEntity() = NoteEntity(
    noteId = requireNotNull(noteId) { "Cannot cache a note with no noteId" },
    userId = userId,
    lastModifiedAt = lastModifiedAt.toString(),
    isDeleted = isDeleted,
    hasHistory = hasHistory,
    encTitle = encTitle,
    encBody = encBody,
    encDrawing = encDrawing,
    lockedByDeviceId = lockedByDeviceId,
    lockedAt = lockedAt,
)

fun NoteEntity.toDto() = NoteDto(
    noteId = noteId,
    userId = userId,
    lastModifiedAt = kotlin.time.Instant.parse(lastModifiedAt),
    isDeleted = isDeleted,
    hasHistory = hasHistory,
    encTitle = encTitle,
    encBody = encBody,
    encDrawing = encDrawing,
    lockedByDeviceId = lockedByDeviceId,
    lockedAt = lockedAt,
)
