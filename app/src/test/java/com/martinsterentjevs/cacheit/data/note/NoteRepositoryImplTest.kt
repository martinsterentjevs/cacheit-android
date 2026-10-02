package com.martinsterentjevs.cacheit.data.note

import com.martinsterentjevs.cacheit.data.note.local.NoteDao
import com.martinsterentjevs.cacheit.data.note.local.NoteEntity
import com.martinsterentjevs.cacheit.data.note.local.toEntity
import com.martinsterentjevs.cacheit.network.note.NoteApi
import com.martinsterentjevs.cacheit.network.note.NoteDto
import com.martinsterentjevs.cacheit.services.crypto.CryptoService
import com.martinsterentjevs.cacheit.services.crypto.NoteField
import com.martinsterentjevs.cacheit.testutil.AndroidLogRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.util.UUID
import kotlin.time.Clock

class NoteRepositoryImplTest {

    @get:Rule
    val androidLogRule = AndroidLogRule()

    private lateinit var noteApi: NoteApi
    private lateinit var noteDao: NoteDao
    private lateinit var cryptoService: CryptoService
    private lateinit var repository: NoteRepositoryImpl

    private val noteId = UUID.randomUUID().toString()

    @Before
    fun setUp() {
        noteApi = mockk()
        noteDao = mockk(relaxed = true)
        cryptoService = mockk()
        repository = NoteRepositoryImpl(noteApi, noteDao, cryptoService)

        // Identity-ish decrypt/encrypt stubs unless a specific test overrides them.
        every { cryptoService.decryptField(any(), any(), any()) } answers { "decrypted-${secondArg<UUID>()}-${thirdArg<NoteField>()}" }
        every { cryptoService.encryptField(any(), any(), any()) } answers { "enc-${firstArg<String>()}" }
    }

    @Test
    fun `getNotes decrypts and caches fresh notes on success`() = runTest {
        val dto = testNoteDto(noteId)
        coEvery { noteApi.getNotes() } returns listOf(dto)

        val result = repository.getNotes()

        assertEquals(1, result.notes.size)
        assertEquals(0, result.failedCount)
        assertEquals(false, result.isFromCache)
        coVerify { noteDao.upsertAll(listOf(dto.toEntity())) }
    }

    @Test
    fun `getNotes falls back to the local cache without throwing when the server is unreachable`() = runTest {
        coEvery { noteApi.getNotes() } throws IOException("no network")
        coEvery { noteDao.getAll() } returns listOf(testNoteEntity(noteId))

        val result = repository.getNotes()

        assertTrue(result.isFromCache)
        assertEquals(1, result.notes.size)
    }

    @Test
    fun `getNotes surfaces NoteFlowException on a genuine server error rather than falling back to cache`() = runTest {
        coEvery { noteApi.getNotes() } throws httpException(500)

        try {
            repository.getNotes()
            org.junit.Assert.fail("Expected NoteFlowException")
        } catch (e: NoteFlowException) {
            assertEquals("Couldn't sync your notes - try again", e.userMessage)
        }
        coVerify(exactly = 0) { noteDao.getAll() }
    }

    @Test
    fun `getNotes counts decrypt failures without letting one bad note fail the whole call`() = runTest {
        val goodId = UUID.randomUUID().toString()
        val badId = UUID.randomUUID().toString()
        coEvery { noteApi.getNotes() } returns listOf(testNoteDto(goodId), testNoteDto(badId))
        every {
            cryptoService.decryptField(any(), UUID.fromString(badId), NoteField.TITLE)
        } throws RuntimeException("bad ciphertext")

        val result = repository.getNotes()

        assertEquals(1, result.notes.size)
        assertEquals(1, result.failedCount)
    }

    @Test
    fun `getLocalNote returns null without throwing when the cached note fails to decrypt`() = runTest {
        coEvery { noteDao.getById(noteId) } returns testNoteEntity(noteId)
        every { cryptoService.decryptField(any(), UUID.fromString(noteId), NoteField.TITLE) } throws RuntimeException("bad key")

        val result = repository.getLocalNote(noteId)

        assertNull(result)
    }

    @Test
    fun `getLocalNote returns null when there is nothing cached`() = runTest {
        coEvery { noteDao.getById(noteId) } returns null

        assertNull(repository.getLocalNote(noteId))
    }

    @Test
    fun `addNote encrypts, writes, caches, and returns the decrypted note on a verified round-trip`() = runTest {
        val note = testFaceNote(noteId)
        val created = testNoteDto(noteId)
        coEvery { noteApi.addNote(any()) } returns created

        val result = repository.addNote(note)

        assertTrue(result is NoteWriteResult.Verified)
        coVerify { noteDao.upsert(created.toEntity()) }
    }

    @Test
    fun `addNote returns Unverified rather than throwing when the post-write redecrypt fails`() = runTest {
        val note = testFaceNote(noteId)
        val created = testNoteDto(noteId)
        coEvery { noteApi.addNote(any()) } returns created
        every { cryptoService.decryptField(any(), UUID.fromString(noteId), NoteField.TITLE) } throws RuntimeException("redecrypt failed")

        val result = repository.addNote(note)

        assertEquals(NoteWriteResult.Unverified, result)
        // The write already succeeded server-side - caching the entity must still happen.
        coVerify { noteDao.upsert(created.toEntity()) }
    }

    @Test
    fun `addNote maps an HttpException to the generic sync-failure message`() = runTest {
        coEvery { noteApi.addNote(any()) } throws httpException(500)

        try {
            repository.addNote(testFaceNote(noteId))
            org.junit.Assert.fail("Expected NoteFlowException")
        } catch (e: NoteFlowException) {
            assertEquals("Couldn't sync your notes - try again", e.userMessage)
        }
    }

    @Test
    fun `deleteNote maps an IOException to the connectivity message`() = runTest {
        coEvery { noteApi.deleteNote(noteId) } throws IOException("no network")

        try {
            repository.deleteNote(noteId)
            org.junit.Assert.fail("Expected NoteFlowException")
        } catch (e: NoteFlowException) {
            assertEquals("Can't reach the server. Check your connection.", e.userMessage)
        }
        coVerify(exactly = 0) { noteDao.deleteById(any()) }
    }

    @Test
    fun `acquireDrawingLock maps 409 to the already-being-edited message`() = runTest {
        coEvery { noteApi.acquireDrawingLock(noteId) } throws httpException(409)

        try {
            repository.acquireDrawingLock(noteId)
            org.junit.Assert.fail("Expected NoteFlowException")
        } catch (e: NoteFlowException) {
            assertEquals("This drawing is being edited on another device", e.userMessage)
        }
    }

    @Test
    fun `acquireDrawingLock falls back to the generic message for an unmapped status code`() = runTest {
        coEvery { noteApi.acquireDrawingLock(noteId) } throws httpException(500)

        try {
            repository.acquireDrawingLock(noteId)
            org.junit.Assert.fail("Expected NoteFlowException")
        } catch (e: NoteFlowException) {
            assertEquals("Couldn't sync your notes - try again", e.userMessage)
        }
    }

    @Test
    fun `clearLocalCache delegates straight to the dao`() = runTest {
        repository.clearLocalCache()

        coVerify { noteDao.clearAll() }
    }

    // ---- helpers ----

    private fun httpException(code: Int): HttpException =
        HttpException(Response.error<Any>(code, "".toResponseBody(null)))

    private fun testNoteDto(id: String) = NoteDto(
        noteId = id,
        userId = "user-1",
        lastModifiedAt = Clock.System.now(),
        isDeleted = false,
        hasHistory = false,
        encTitle = "enc-title",
        encBody = "enc-body",
        encDrawing = null,
        lockedByDeviceId = null,
        lockedAt = null,
    )

    private fun testNoteEntity(id: String) = NoteEntity(
        noteId = id,
        userId = "user-1",
        lastModifiedAt = Clock.System.now().toString(),
        isDeleted = false,
        hasHistory = false,
        encTitle = "enc-title",
        encBody = "enc-body",
        encDrawing = null,
        lockedByDeviceId = null,
        lockedAt = null,
    )

    private fun testFaceNote(id: String) = FaceNote(
        noteId = id,
        userId = "user-1",
        lastModifiedAt = Clock.System.now(),
        isDeleted = false,
        hasHistory = false,
        title = "Title",
        body = "Body",
        drawing = null,
        lockedByDeviceId = null,
        lockedAt = null,
    )
}