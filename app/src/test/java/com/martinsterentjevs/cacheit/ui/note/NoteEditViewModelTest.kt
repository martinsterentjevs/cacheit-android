package com.martinsterentjevs.cacheit.ui.note

import app.cash.turbine.test
import com.martinsterentjevs.cacheit.data.note.FaceNote
import com.martinsterentjevs.cacheit.data.note.NoteFlowException
import com.martinsterentjevs.cacheit.data.note.NoteRepository
import com.martinsterentjevs.cacheit.data.note.NoteWriteResult
import com.martinsterentjevs.cacheit.network.note.NoteDto
import com.martinsterentjevs.cacheit.services.security.SecurityService
import com.martinsterentjevs.cacheit.testutil.AndroidLogRule
import com.martinsterentjevs.cacheit.testutil.MainDispatcherRule
import com.martinsterentjevs.cacheit.ui.common.PopupController
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

/**
 * IMPORTANT: startTtlCountdown() (triggered whenever a drawing-lock response carries a
 * non-null lockedAt) reads java.time.Instant.now() against the real wall clock, not an
 * injectable Clock - it cannot be driven by StandardTestDispatcher's virtual time. Its
 * `while (isActive)` loop only terminates once real time actually reaches the lock's TTL
 * (1 hour), so letting it start inside a runTest{} that calls advanceUntilIdle() will hang.
 * Every test below that touches acquireDrawingLock mocks lockedAt = null specifically to
 * avoid ever starting that loop. This is a genuine testability gap in the production code,
 * not something these tests work around by accident - flagging it rather than silently
 * routing past it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NoteEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val androidLogRule = AndroidLogRule()

    private lateinit var noteRepository: NoteRepository
    private lateinit var popupController: PopupController
    private lateinit var securityService: SecurityService
    private lateinit var viewModel: NoteEditViewModel

    @Before
    fun setUp() {
        noteRepository = mockk()
        popupController = mockk(relaxed = true)
        securityService = mockk()
        every { securityService.getAccountId() } returns "account-1"
        viewModel = NoteEditViewModel(noteRepository, popupController, securityService)
    }

    // ---- load(): Create mode ----

    @Test
    fun `load with null noteId enters Create mode with a blank note owned by the current account`() = runTest {
        viewModel.load(noteId = null)
        advanceUntilIdle()

        val state = viewModel.uiState.value as NoteEditUiState.Ready
        assertEquals(NoteEditMode.Create, state.mode)
        assertEquals("account-1", state.note.userId)
        assertEquals("", state.note.title)
        assertNull(state.preEditNote)
    }

    @Test
    fun `load with NEW_NOTE_ID enters Create mode the same as null`() = runTest {
        viewModel.load(noteId = "new")
        advanceUntilIdle()

        assertTrue((viewModel.uiState.value as NoteEditUiState.Ready).mode == NoteEditMode.Create)
    }

    @Test
    fun `load shows an error snackbar when the account id is unavailable in Create mode`() = runTest {
        every { securityService.getAccountId() } returns null

        viewModel.load(noteId = null)
        advanceUntilIdle()

        coVerify { popupController.show(any()) }
        // Loading never resolves to Ready when account id lookup fails
        assertTrue(viewModel.uiState.value is NoteEditUiState.Loading)
    }

    // ---- load(): existing note ----

    @Test
    fun `load with an existing cached note enters View mode without hitting the network`() = runTest {
        val cached = testFaceNote("note-1")
        coEvery { noteRepository.getLocalNote("note-1") } returns cached

        viewModel.load(noteId = "note-1")
        advanceUntilIdle()

        val state = viewModel.uiState.value as NoteEditUiState.Ready
        assertEquals(NoteEditMode.View, state.mode)
        assertEquals(cached, state.note)
        assertEquals(cached, state.preEditNote)
        coVerify(exactly = 0) { noteRepository.getNotes() }
    }

    @Test
    fun `load with an uncached note falls back to a sync and finds it`() = runTest {
        coEvery { noteRepository.getLocalNote("note-1") } returnsMany listOf(null, testFaceNote("note-1"))
        coEvery { noteRepository.getNotes() } returns mockk(relaxed = true)

        viewModel.load(noteId = "note-1")
        advanceUntilIdle()

        val state = viewModel.uiState.value as NoteEditUiState.Ready
        assertEquals(NoteEditMode.View, state.mode)
    }

    @Test
    fun `load surfaces NotFound without couldNotConfirm when the sync succeeds but the note genuinely does not exist`() = runTest {
        coEvery { noteRepository.getLocalNote("note-1") } returns null
        coEvery { noteRepository.getNotes() } returns mockk(relaxed = true)

        viewModel.load(noteId = "note-1")
        advanceUntilIdle()

        assertEquals(NoteEditUiState.NotFound(couldNotConfirm = false), viewModel.uiState.value)
    }

    @Test
    fun `load surfaces NotFound with couldNotConfirm when the confirming sync itself fails`() = runTest {
        coEvery { noteRepository.getLocalNote("note-1") } returns null
        coEvery { noteRepository.getNotes() } throws NoteFlowException("Couldn't sync your notes - try again")

        viewModel.load(noteId = "note-1")
        advanceUntilIdle()

        assertEquals(NoteEditUiState.NotFound(couldNotConfirm = true), viewModel.uiState.value)
    }

    @Test
    fun `a second load call while one is already in flight is a no-op`() = runTest {
        coEvery { noteRepository.getLocalNote("note-1") } coAnswers {
            delay(100.milliseconds)
            testFaceNote("note-1")
        }

        viewModel.load("note-1")
        viewModel.load("note-1")
        advanceUntilIdle()

        coVerify(exactly = 1) { noteRepository.getLocalNote("note-1") }
    }

    // ---- mode transitions ----

    @Test
    fun `enterTextEdit moves View to TextEdit`() = runTest {
        loadIntoView("note-1")

        viewModel.enterTextEdit()

        assertEquals(NoteEditMode.TextEdit, (viewModel.uiState.value as NoteEditUiState.Ready).mode)
    }

    @Test
    fun `enterTextEdit moves Create to TextEdit`() = runTest {
        viewModel.load(null)
        advanceUntilIdle()

        viewModel.enterTextEdit()

        assertEquals(NoteEditMode.TextEdit, (viewModel.uiState.value as NoteEditUiState.Ready).mode)
    }

    @Test
    fun `enterDrawingEdit is a no-op when already in DrawingEdit`() = runTest {
        enterDrawingEditOnExistingNote("note-1")

        viewModel.enterDrawingEdit()
        advanceUntilIdle()

        coVerify(exactly = 1) { noteRepository.acquireDrawingLock("note-1") } // still just the one from setup
    }

    @Test
    fun `enterDrawingEdit on a brand-new Create note stays fully local, no lock acquired`() = runTest {
        viewModel.load(null)
        advanceUntilIdle()

        viewModel.enterDrawingEdit()
        advanceUntilIdle()

        assertEquals(NoteEditMode.DrawingEdit, (viewModel.uiState.value as NoteEditUiState.Ready).mode)
        coVerify(exactly = 0) { noteRepository.acquireDrawingLock(any()) }
    }

    @Test
    fun `enterDrawingEdit on an existing note acquires the server-side lock`() = runTest {
        loadIntoView("note-1")
        coEvery { noteRepository.acquireDrawingLock("note-1") } returns testNoteDto("note-1", lockedAt = null)

        viewModel.enterDrawingEdit()
        advanceUntilIdle()

        val state = viewModel.uiState.value as NoteEditUiState.Ready
        assertEquals(NoteEditMode.DrawingEdit, state.mode)
        assertTrue(state.isDrawingLocked)
    }

    @Test
    fun `enterDrawingEdit shows an error snackbar and does not change mode when lock acquisition fails`() = runTest {
        loadIntoView("note-1")
        coEvery { noteRepository.acquireDrawingLock("note-1") } throws NoteFlowException("This drawing is being edited on another device")

        viewModel.enterDrawingEdit()
        advanceUntilIdle()

        val state = viewModel.uiState.value as NoteEditUiState.Ready
        assertEquals(NoteEditMode.View, state.mode)
        assertFalse(state.isDrawingLocked)
        coVerify { popupController.show(any()) }
    }

    // ---- field edits, gated by mode ----

    @Test
    fun `onTitleChanged updates the title in TextEdit but not in View`() = runTest {
        loadIntoView("note-1")

        viewModel.onTitleChanged("should be ignored")
        assertEquals("Title note-1", (viewModel.uiState.value as NoteEditUiState.Ready).note.title)

        viewModel.enterTextEdit()
        viewModel.onTitleChanged("New title")
        assertEquals("New title", (viewModel.uiState.value as NoteEditUiState.Ready).note.title)
    }

    @Test
    fun `onDrawingChanged updates the drawing in DrawingEdit but not in TextEdit`() = runTest {
        viewModel.load(null)
        advanceUntilIdle()
        viewModel.enterTextEdit()

        viewModel.onDrawingChanged("ignored-in-text-edit")
        assertNull((viewModel.uiState.value as NoteEditUiState.Ready).note.drawing)

        viewModel.enterDrawingEdit() // local-only, still Create/no preEditNote
        advanceUntilIdle()
        viewModel.onDrawingChanged("drawing-payload")
        assertEquals("drawing-payload", (viewModel.uiState.value as NoteEditUiState.Ready).note.drawing)
    }

    // ---- onBackPressed / exits ----

    @Test
    fun `onBackPressed in View sends a Dismiss event`() = runTest {
        loadIntoView("note-1")

        viewModel.events.test {
            viewModel.onBackPressed()
            assertEquals(NoteEditUiEvent.Dismiss, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onBackPressed in Create with a blank note dismisses without saving`() = runTest {
        viewModel.load(null)
        advanceUntilIdle()

        viewModel.onBackPressed()
        advanceUntilIdle()

        coVerify(exactly = 0) { noteRepository.addNote(any()) }
    }

    @Test
    fun `onBackPressed in Create with content saves via addNote and does not release a lock`() = runTest {
        viewModel.load(null)
        advanceUntilIdle()
        viewModel.onTitleChanged("Not blank")
        val note = (viewModel.uiState.value as NoteEditUiState.Ready).note
        coEvery { noteRepository.addNote(any()) } returns NoteWriteResult.Verified(note)

        viewModel.onBackPressed()
        advanceUntilIdle()

        coVerify { noteRepository.addNote(note) }
        coVerify(exactly = 0) { noteRepository.releaseDrawingLock(any()) }
        assertEquals(NoteEditMode.View, (viewModel.uiState.value as NoteEditUiState.Ready).mode)
    }

    @Test
    fun `onBackPressed in TextEdit saves via updateNote without releasing a lock`() = runTest {
        loadIntoView("note-1")
        viewModel.enterTextEdit()
        val edited = (viewModel.uiState.value as NoteEditUiState.Ready).note
        coEvery { noteRepository.updateNote("note-1", edited) } returns NoteWriteResult.Verified(edited)

        viewModel.onBackPressed()
        advanceUntilIdle()

        coVerify { noteRepository.updateNote("note-1", edited) }
        coVerify(exactly = 0) { noteRepository.releaseDrawingLock(any()) }
    }

    @Test
    fun `onBackPressed in DrawingEdit saves, releases the lock, and lands in View`() = runTest {
        enterDrawingEditOnExistingNote("note-1")
        val edited = (viewModel.uiState.value as NoteEditUiState.Ready).note
        coEvery { noteRepository.updateNote("note-1", edited) } returns NoteWriteResult.Verified(edited)
        coEvery { noteRepository.releaseDrawingLock("note-1") } returns Unit

        viewModel.onBackPressed()
        advanceUntilIdle()

        coVerify { noteRepository.updateNote("note-1", edited) }
        coVerify { noteRepository.releaseDrawingLock("note-1") }
        val state = viewModel.uiState.value as NoteEditUiState.Ready
        assertEquals(NoteEditMode.View, state.mode)
        assertFalse(state.isDrawingLocked)
    }

    // ---- save outcomes ----

    @Test
    fun `a verified save replaces both note and preEditNote and clears isSaving`() = runTest {
        loadIntoView("note-1")
        viewModel.enterTextEdit()
        viewModel.onTitleChanged("Edited")
        val edited = (viewModel.uiState.value as NoteEditUiState.Ready).note
        coEvery { noteRepository.updateNote("note-1", edited) } returns NoteWriteResult.Verified(edited)

        viewModel.exitTextEdit()
        advanceUntilIdle()

        val state = viewModel.uiState.value as NoteEditUiState.Ready
        assertEquals(edited, state.note)
        assertEquals(edited, state.preEditNote)
        assertFalse(state.isSaving)
    }

    @Test
    fun `an unverified save keeps the prior preEditNote and shows the unconfirmed-write snackbar`() = runTest {
        loadIntoView("note-1")
        viewModel.enterTextEdit()
        val original = (viewModel.uiState.value as NoteEditUiState.Ready).preEditNote
        coEvery { noteRepository.updateNote("note-1", any()) } returns NoteWriteResult.Unverified

        viewModel.exitTextEdit()
        advanceUntilIdle()

        val state = viewModel.uiState.value as NoteEditUiState.Ready
        assertEquals(original, state.preEditNote) // NOT replaced - write is unconfirmed, not failed
        assertEquals(NoteEditMode.View, state.mode)
        coVerify { popupController.show(any()) }
    }

    @Test
    fun `a failed save keeps the note in memory, allows retry, and shows the error snackbar`() = runTest {
        loadIntoView("note-1")
        viewModel.enterTextEdit()
        coEvery { noteRepository.updateNote("note-1", any()) } throws NoteFlowException("Couldn't sync your notes - try again")

        viewModel.exitTextEdit()
        advanceUntilIdle()

        val state = viewModel.uiState.value as NoteEditUiState.Ready
        assertEquals(NoteEditMode.TextEdit, state.mode) // NOT bounced to View on failure
        assertFalse(state.isSaving)
        coVerify { popupController.show(any()) }
    }

    @Test
    fun `a second exitTextEdit call while a save is already in flight is a no-op`() = runTest {
        loadIntoView("note-1")
        viewModel.enterTextEdit()
        coEvery { noteRepository.updateNote("note-1", any()) } coAnswers {
            delay(100.milliseconds)
            NoteWriteResult.Verified(secondArg())
        }

        viewModel.exitTextEdit()
        viewModel.exitTextEdit() // fired while isSaving is already true
        advanceUntilIdle()

        coVerify(exactly = 1) { noteRepository.updateNote("note-1", any()) }
    }

    // ---- helpers ----

    private fun TestScope.loadIntoView(noteId: String) {
        coEvery { noteRepository.getLocalNote(noteId) } returns testFaceNote(noteId)
        viewModel.load(noteId)
        advanceUntilIdle()
    }

    private fun testFaceNote(id: String, lockedByDeviceId: String? = null) = FaceNote(
        noteId = id,
        userId = "account-1",
        lastModifiedAt = Clock.System.now(),
        isDeleted = false,
        hasHistory = false,
        title = "Title $id",
        body = "Body $id",
        drawing = null,
        lockedByDeviceId = lockedByDeviceId,
        lockedAt = null,
    )

    private fun testNoteDto(id: String, lockedAt: String?) = NoteDto(
        noteId = id,
        userId = "account-1",
        lastModifiedAt = Clock.System.now(),
        isDeleted = false,
        hasHistory = false,
        encTitle = "enc-title",
        encBody = "enc-body",
        encDrawing = null,
        lockedByDeviceId = "account-1",
        lockedAt = lockedAt,
    )

    private fun TestScope.enterDrawingEditOnExistingNote(noteId: String) {
        loadIntoView(noteId)
        coEvery { noteRepository.acquireDrawingLock(noteId) } returns testNoteDto(noteId, lockedAt = null)
        viewModel.enterDrawingEdit()
        advanceUntilIdle()
    }
}