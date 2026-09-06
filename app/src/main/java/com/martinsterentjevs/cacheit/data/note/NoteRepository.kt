package com.martinsterentjevs.cacheit.data.note

import android.util.Log
import com.martinsterentjevs.cacheit.data.note.local.NoteDao
import com.martinsterentjevs.cacheit.data.note.local.toDto
import com.martinsterentjevs.cacheit.data.note.local.toEntity
import com.martinsterentjevs.cacheit.network.note.NoteApi
import com.martinsterentjevs.cacheit.network.note.NoteDto
import com.martinsterentjevs.cacheit.network.note.NoteVersionDto
import com.martinsterentjevs.cacheit.network.note.NoteVersionMeta
import com.martinsterentjevs.cacheit.network.note.SyncRequestDto
import com.martinsterentjevs.cacheit.network.note.SyncResponseDto
import com.martinsterentjevs.cacheit.services.crypto.CryptoService
import com.martinsterentjevs.cacheit.services.crypto.NoteField
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Instant

private const val TAG = "NoteRepository"

/** Same shape as AuthFlowException - copy is already safe to show directly in a popup. */
class NoteFlowException(val userMessage: String) : Exception(userMessage)

/** In-memory, UI-facing note. Never persisted or sent over the wire directly - see ADR 0002. */
data class FaceNote(
    val noteId: String?,
    val userId: String,
    val lastModifiedAt: Instant,
    val isDeleted: Boolean,
    val hasHistory: Boolean,
    val title: String,
    val body: String?,
    val drawing: String?,
    val lockedByDeviceId: String?, // clear on drawing save
    val lockedAt: String?, // clear on drawing save
)

data class FaceNoteVersion(
    val versionId: String,
    val title: String,
    val body: String?,
    val drawing: String?,
)

/**
 * isFromCache is true when getNotes() couldn't reach the server and served the local cache
 * instead - lets the list screen show "showing saved notes, couldn't refresh" rather than
 * silently presenting stale data as if it were fresh.
 */
data class NotesResult(
    val notes: List<FaceNote>,
    val failedCount: Int,
    val isFromCache: Boolean,
)

/**
 * Result of addNote/updateNote per ADR 0002. Unverified means the server call already
 * succeeded but the response failed to redecrypt locally - the write is NOT treated as failed,
 * just unconfirmed. Caller reverts its own held state on Unverified (repository doesn't hand
 * back a fallback value - the ViewModel already has the pre-edit FaceNote for update, and has
 * nothing to add for create).
 */
sealed interface NoteWriteResult {
    data class Verified(val note: FaceNote) : NoteWriteResult
    data object Unverified : NoteWriteResult
}

interface NoteRepository {
    suspend fun getNotes(): NotesResult

    /** Local-cache-only read, no network call - see note-viewmodels-reference.md's "Local persistence" section. */
    suspend fun getLocalNote(noteId: String): FaceNote?
    suspend fun addNote(note: FaceNote): NoteWriteResult
    suspend fun updateNote(noteId: String, note: FaceNote): NoteWriteResult
    suspend fun deleteNote(noteId: String)
    suspend fun getSyncDelta(request: SyncRequestDto): SyncResponseDto
    suspend fun acquireDrawingLock(noteId: String): NoteDto
    suspend fun releaseDrawingLock(noteId: String)
    suspend fun getVersionHistory(noteId: String): List<NoteVersionMeta>
    suspend fun getVersion(noteId: String, versionId: String): FaceNoteVersion
    suspend fun restoreVersion(noteId: String, versionId: String): NoteWriteResult

    /** Wipes the local cache only - the in-app half of "clear-out". No server call. */
    suspend fun clearLocalCache()
}

internal class NoteRepositoryImpl @Inject constructor(
    private val noteApi: NoteApi,
    private val noteDao: NoteDao,
    private val cryptoService: CryptoService,
) : NoteRepository {

    override suspend fun getNotes(): NotesResult {
        val (dtos, isFromCache) = try {
            val fresh = noteApi.getNotes()
            noteDao.upsertAll(fresh.mapNotNull { dto -> dto.noteId?.let { dto.toEntity() } })
            fresh to false
        } catch (e: IOException) {
            // Specifically unreachable - not a real server error. Fall back to cache rather
            // than surfacing NoteFlowException, per "local storage is the fallback" decision.
            Log.w(TAG, "getNotes: server unreachable, serving local cache", e)
            noteDao.getAll().map { it.toDto() } to true
        } catch (_: HttpException) {
            // A real server-side error (4xx/5xx) is not "unreachable" - don't paper over it
            // with stale cache data, surface it the same as every other endpoint does.
            throw NoteFlowException("Couldn't sync your notes - try again")
        }

        var failedCount = 0
        val notes = dtos.mapNotNull { dto ->
            try {
                getDecryptedNote(dto)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decrypt note ${dto.noteId} (${e::class.simpleName})", e)
                failedCount++
                null
            }
        }
        return NotesResult(notes, failedCount, isFromCache)
    }

    override suspend fun getLocalNote(noteId: String): FaceNote? =
        noteDao.getById(noteId)?.toDto()?.let { dto ->
            try {
                getDecryptedNote(dto)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decrypt cached note $noteId (${e::class.simpleName})", e)
                null
            }
        }

    override suspend fun addNote(note: FaceNote): NoteWriteResult = noteCall {
        val created = noteApi.addNote(getEncryptedNote(note))
        created.noteId?.let { noteDao.upsert(created.toEntity()) }
        redecryptOrUnverified(created)
    }

    override suspend fun updateNote(noteId: String, note: FaceNote): NoteWriteResult = noteCall {
        val updated = noteApi.updateNote(noteId, getEncryptedNote(note))
        updated.noteId?.let { noteDao.upsert(updated.toEntity()) }
        redecryptOrUnverified(updated)
    }

    override suspend fun deleteNote(noteId: String) = noteCall {
        noteApi.deleteNote(noteId)
        noteDao.deleteById(noteId)
    }

    override suspend fun getSyncDelta(request: SyncRequestDto): SyncResponseDto =
        noteCall {
            val response = noteApi.getSyncDelta(request)
            noteDao.upsertAll(response.updatedNotes.mapNotNull { dto -> dto.noteId?.let { dto.toEntity() } })
            response.deletedIds.forEach { noteDao.deleteById(it) }
            response
        }

    override suspend fun acquireDrawingLock(noteId: String): NoteDto =
        noteCall(httpErrors = mapOf(409 to "This drawing is being edited on another device")) {
            noteApi.acquireDrawingLock(noteId)
        }

    override suspend fun releaseDrawingLock(noteId: String) =
        noteCall { noteApi.releaseDrawingLock(noteId) }

    override suspend fun getVersionHistory(noteId: String): List<NoteVersionMeta> =
        noteCall { noteApi.getVersionHistory(noteId) }

    override suspend fun getVersion(noteId: String, versionId: String): FaceNoteVersion =
        noteCall { getDecryptedVersion(UUID.fromString(noteId), noteApi.getVersion(noteId, versionId)) }

    override suspend fun restoreVersion(noteId: String, versionId: String): NoteWriteResult = noteCall {
        val restored = noteApi.restoreVersion(noteId, versionId)
        restored.noteId?.let { noteDao.upsert(restored.toEntity()) }
        redecryptOrUnverified(restored)
    }

    override suspend fun clearLocalCache() = noteDao.clearAll()

    /**
     * Redecrypts the server's response as a correctness check on the encrypt/decrypt round-trip
     * (see ADR 0002). Failure here means the write already succeeded server-side - it's
     * unconfirmed locally, not failed. No compensating rollback call; the next sync pull
     * reconciles whatever the server actually holds.
     */
    private fun redecryptOrUnverified(dto: NoteDto): NoteWriteResult = try {
        NoteWriteResult.Verified(getDecryptedNote(dto))
    } catch (e: Exception) {
        Log.e(TAG, "Write succeeded but redecrypt failed for note ${dto.noteId} (${e::class.simpleName})", e)
        NoteWriteResult.Unverified
    }

    /** Shared network-error -> user-facing-message mapping - same shape as AuthRepositoryImpl.authCall. */
    private suspend fun <T> noteCall(
        genericMessage: String = "Couldn't sync your notes - try again",
        httpErrors: Map<Int, String> = emptyMap(),
        block: suspend () -> T,
    ): T = try {
        block()
    } catch (e: HttpException) {
        throw NoteFlowException(httpErrors[e.code()] ?: genericMessage)
    } catch (_: IOException) {
        throw NoteFlowException("Can't reach the server. Check your connection.")
    }

    private fun getDecryptedNote(serverNote: NoteDto): FaceNote {
        val noteId = UUID.fromString(serverNote.noteId)
        return FaceNote(
            noteId = serverNote.noteId,
            userId = serverNote.userId,
            lastModifiedAt = serverNote.lastModifiedAt,
            isDeleted = serverNote.isDeleted,
            hasHistory = serverNote.hasHistory,
            title = cryptoService.decryptField(serverNote.encTitle, noteId, NoteField.TITLE),
            body = serverNote.encBody?.let { cryptoService.decryptField(it, noteId, NoteField.BODY) },
            drawing = serverNote.encDrawing?.let { cryptoService.decryptField(it, noteId, NoteField.ENC_DRAWING) },
            lockedByDeviceId = serverNote.lockedByDeviceId,
            lockedAt = serverNote.lockedAt,
        )
    }

    private fun getEncryptedNote(userNote: FaceNote): NoteDto {
        // A new note needs its client-generated noteId assigned before this is ever called -
        // see note-reference.md on client-generated note IDs. Fail loudly here rather than NPE.
        val noteId = UUID.fromString(
            requireNotNull(userNote.noteId) { "FaceNote.noteId must be assigned before encryption" },
        )
        return NoteDto(
            noteId = userNote.noteId,
            userId = userNote.userId,
            lastModifiedAt = userNote.lastModifiedAt,
            isDeleted = userNote.isDeleted,
            hasHistory = userNote.hasHistory,
            encTitle = cryptoService.encryptField(userNote.title, noteId, NoteField.TITLE),
            encBody = userNote.body?.let { cryptoService.encryptField(it, noteId, NoteField.BODY) },
            encDrawing = userNote.drawing?.let { cryptoService.encryptField(it, noteId, NoteField.ENC_DRAWING) },
            lockedByDeviceId = userNote.lockedByDeviceId,
            lockedAt = userNote.lockedAt,
        )
    }

    /** noteId is the PARENT note's id, passed in by the caller - NoteVersionDto carries only its own versionId. */
    private fun getDecryptedVersion(noteId: UUID, version: NoteVersionDto): FaceNoteVersion = FaceNoteVersion(
        versionId = version.versionId,
        title = cryptoService.decryptField(version.encTitle, noteId, NoteField.TITLE),
        body = version.encBody?.let { cryptoService.decryptField(it, noteId, NoteField.BODY) },
        drawing = version.encDrawing?.let { cryptoService.decryptField(it, noteId, NoteField.ENC_DRAWING) },
    )
}