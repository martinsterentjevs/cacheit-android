package com.martinsterentjevs.cacheit.network.note

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Mirrors server-side NoteDto (cacheit-server: dtos/note/NoteDto.kt) field-for-field.
 * Server serializes UUID/Instant as plain ISO/string values over the wire (Jackson default
 * with jsr310 module) - kept as String here rather than guessing at a contextual serializer.
 * If AccountSessionResponse uses a different convention (e.g. @Contextual UUID), align this
 * to match it for consistency.
 */
@Serializable
data class NoteDto(
    val noteId: String?,
    val userId: String,
    val lastModifiedAt: Instant,
    val isDeleted: Boolean,
    val hasHistory:Boolean,
    val encTitle: String,
    val encBody: String?,
    val encDrawing: String?,
    val lockedByDeviceId: String?, // clear on drawing save
    val lockedAt: String?, // clear on drawing save
)
@Serializable
data class NoteVersionDto(
    val versionId: String,
    val encTitle: String,
    val encBody: String?,
    val encDrawing: String?,
)
@Serializable
data class NoteVersionMeta(
    val versionId: String,
    val noteId: String,
    val createdAt: String,
    val deviceId: String?, // nullable - a device may since be deleted
    val isCurrent: Boolean,
)
@Serializable
data class SyncManifestEntry(
    val noteId: String,
    val lastModifiedAt: String,
)
@Serializable
data class SyncRequestDto(
    val lastSyncedAt: String,
    val localNotes: List<SyncManifestEntry>,
)
@Serializable
data class SyncResponseDto(
    val updatedNotes: List<NoteDto>,
    val deletedIds: List<String>,
    val serverTime: String,
)
