package com.martinsterentjevs.cacheit.network.note

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

// * Routes mirror cacheit-server's NoteController exactly - paths, methods, and body shapes.
// * No @Header(Authorization) params here: assumes AuthInterceptor attaches the access token
// * to every outgoing request the way it does for other authenticated endpoints. Confirm
// * AuthInterceptor's path matching actually covers /notes/** before relying on this - if it's
// * scoped narrower than "everything except /auth/**", these calls will 401.


interface NoteApi {

@GET("notes")
suspend fun getNotes(): List<NoteDto>

@POST("notes")
suspend fun addNote(@Body note: NoteDto): NoteDto

// noteId path variable mirrors server's routing; server currently resolves the target note
// from the body's own noteId field, not the path variable - kept for REST correctness anyway.
@PUT("notes/{noteId}")
suspend fun updateNote(
    @Path("noteId") noteId: String,
    @Body note: NoteDto,
): NoteDto

@DELETE("notes/{noteId}")
suspend fun deleteNote(@Path("noteId") noteId: String)

// POST, not GET+query-params - deviates from sync-protocol.md; see note-reference.md's
// "Spec follow-ups" on the server side for why.
@POST("notes/sync")
suspend fun getSyncDelta(@Body request: SyncRequestDto): SyncResponseDto

@POST("notes/{noteId}/lock")
suspend fun acquireDrawingLock(@Path("noteId") noteId: String): NoteDto

@DELETE("notes/{noteId}/lock")
suspend fun releaseDrawingLock(@Path("noteId") noteId: String)

@GET("notes/{noteId}/history")
suspend fun getVersionHistory(@Path("noteId") noteId: String): List<NoteVersionMeta>

@GET("notes/{noteId}/history/{versionId}")
suspend fun getVersion(
@Path("noteId") noteId: String,
@Path("versionId") versionId: String,
): NoteVersionDto

@POST("notes/{noteId}/restore/{versionId}")
suspend fun restoreVersion(
@Path("noteId") noteId: String,
@Path("versionId") versionId: String,
): NoteDto
}
