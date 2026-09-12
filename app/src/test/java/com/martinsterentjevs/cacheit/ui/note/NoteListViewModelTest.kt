package com.martinsterentjevs.cacheit.ui.note

import com.martinsterentjevs.cacheit.R
import com.martinsterentjevs.cacheit.data.note.FaceNote
import com.martinsterentjevs.cacheit.data.note.NoteFlowException
import com.martinsterentjevs.cacheit.data.note.NoteRepository
import com.martinsterentjevs.cacheit.data.note.NotesChangeSignal
import com.martinsterentjevs.cacheit.data.note.NotesResult
import com.martinsterentjevs.cacheit.testutil.AndroidLogRule
import com.martinsterentjevs.cacheit.testutil.MainDispatcherRule
import com.martinsterentjevs.cacheit.ui.common.PopupController
import com.martinsterentjevs.cacheit.ui.common.UiEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

/**
 * refresh()'s failure path calls Log.e directly (see NoteListViewModel.refresh's catch
 * block) - AndroidLogRule stubs it out so that test doesn't throw before it ever reaches
 * the assertion.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NoteListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val androidLogRule = AndroidLogRule()

    private lateinit var noteRepository: NoteRepository
    private lateinit var popupController: PopupController
    private lateinit var notesChangeSignal: NotesChangeSignal
    private lateinit var viewModel: NoteListViewModel

    @Before
    fun setUp() {
        noteRepository = mockk()
        popupController = mockk(relaxed = true)
        notesChangeSignal = NotesChangeSignal() // simple in-memory SharedFlow wrapper, real instance is easiest here
        viewModel = NoteListViewModel(popupController, noteRepository, notesChangeSignal)
    }

    // ---- load() ----

    @Test
    fun `load populates Content when the repository returns notes`() = runTest {
        coEvery { noteRepository.getNotes() } returns NotesResult(
            notes = listOf(testNote("note-1")),
            failedCount = 0,
            isFromCache = false,
        )

        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is NoteListUiState.Content)
        state as NoteListUiState.Content
        assertEquals(1, state.notes.size)
        assertEquals(false, state.isFromCache)
        assertEquals(false, state.isRefreshing)
    }

    @Test
    fun `load surfaces Empty when the repository returns no notes`() = runTest {
        coEvery { noteRepository.getNotes() } returns NotesResult(emptyList(), failedCount = 0, isFromCache = false)

        viewModel.load()
        advanceUntilIdle()

        assertEquals(NoteListUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `load carries isFromCache through to Content so the list can show the stale-data treatment`() = runTest {
        coEvery { noteRepository.getNotes() } returns NotesResult(
            notes = listOf(testNote("note-1")),
            failedCount = 0,
            isFromCache = true,
        )

        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.uiState.value as NoteListUiState.Content
        assertTrue(state.isFromCache)
    }

    @Test
    fun `load shows a decrypt-failure snackbar when some notes failed to decrypt`() = runTest {
        coEvery { noteRepository.getNotes() } returns NotesResult(
            notes = listOf(testNote("note-1")),
            failedCount = 2,
            isFromCache = false,
        )

        viewModel.load()
        advanceUntilIdle()

        verify {
            popupController.show(
                UiEvent.Snackbar(messageId = R.string.note_decrypt_failure, formatArgs = listOf(2)),
            )
        }
    }

    @Test
    fun `load surfaces Error state with the exception's user-facing message`() = runTest {
        coEvery { noteRepository.getNotes() } throws NoteFlowException("Couldn't sync your notes - try again")

        viewModel.load()
        advanceUntilIdle()

        assertEquals(
            NoteListUiState.Error("Couldn't sync your notes - try again"),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `a second load call while one is already in flight is a no-op`() = runTest {
        coEvery { noteRepository.getNotes() } coAnswers {
            delay(100.milliseconds)
            NotesResult(listOf(testNote("note-1")), failedCount = 0, isFromCache = false)
        }

        viewModel.load()
        viewModel.load() // fired before the first has resolved - isSyncInFlight should block this one
        advanceUntilIdle()

        coVerify(exactly = 1) { noteRepository.getNotes() }
    }

    // ---- refresh() ----

    @Test
    fun `refresh does nothing when the current state is not Content`() = runTest {
        // uiState starts as Loading - refresh() should be a no-op until a successful load() lands Content
        viewModel.refresh()
        advanceUntilIdle()

        coVerify(exactly = 0) { noteRepository.getNotes() }
    }

    @Test
    fun `refresh replaces the note list on success and clears isRefreshing`() = runTest {
        coEvery { noteRepository.getNotes() } returns NotesResult(
            listOf(testNote("note-1")), failedCount = 0, isFromCache = false,
        )
        viewModel.load()
        advanceUntilIdle()

        coEvery { noteRepository.getNotes() } returns NotesResult(
            listOf(testNote("note-1"), testNote("note-2")), failedCount = 0, isFromCache = false,
        )
        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value as NoteListUiState.Content
        assertEquals(2, state.notes.size)
        assertEquals(false, state.isRefreshing)
    }

    @Test
    fun `refresh keeps the existing notes and shows an error snackbar on failure`() = runTest {
        coEvery { noteRepository.getNotes() } returns NotesResult(
            listOf(testNote("note-1")), failedCount = 0, isFromCache = false,
        )
        viewModel.load()
        advanceUntilIdle()

        coEvery { noteRepository.getNotes() } throws NoteFlowException("Refreshing failed, try again later")
        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value as NoteListUiState.Content
        assertEquals(1, state.notes.size) // stale data kept, not wiped out by the failed refresh
        assertEquals(false, state.isRefreshing)
        verify { popupController.show(UiEvent.Snackbar(messageId = R.string.note_list_refresh_error)) }
    }

    // ---- delete() ----

    @Test
    fun `delete removes the note from Content on success`() = runTest {
        coEvery { noteRepository.getNotes() } returns NotesResult(
            listOf(testNote("note-1"), testNote("note-2")), failedCount = 0, isFromCache = false,
        )
        viewModel.load()
        advanceUntilIdle()

        coEvery { noteRepository.deleteNote("note-1") } returns Unit
        viewModel.delete("note-1")
        advanceUntilIdle()

        val state = viewModel.uiState.value as NoteListUiState.Content
        assertEquals(listOf("note-2"), state.notes.map { it.noteId })
    }

    @Test
    fun `delete drops to Empty when it removes the last remaining note`() = runTest {
        coEvery { noteRepository.getNotes() } returns NotesResult(
            listOf(testNote("note-1")), failedCount = 0, isFromCache = false,
        )
        viewModel.load()
        advanceUntilIdle()

        coEvery { noteRepository.deleteNote("note-1") } returns Unit
        viewModel.delete("note-1")
        advanceUntilIdle()

        assertEquals(NoteListUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `delete keeps the note visible and shows an error snackbar on failure`() = runTest {
        coEvery { noteRepository.getNotes() } returns NotesResult(
            listOf(testNote("note-1")), failedCount = 0, isFromCache = false,
        )
        viewModel.load()
        advanceUntilIdle()

        coEvery { noteRepository.deleteNote("note-1") } throws NoteFlowException("Failed to delete the note")
        viewModel.delete("note-1")
        advanceUntilIdle()

        val state = viewModel.uiState.value as NoteListUiState.Content
        assertEquals(1, state.notes.size)
        verify { popupController.show(UiEvent.Snackbar(messageId = R.string.note_list_delete_error)) }
    }

    @Test
    fun `a second delete call while one is already in flight is a no-op`() = runTest {
        coEvery { noteRepository.getNotes() } returns NotesResult(
            listOf(testNote("note-1")), failedCount = 0, isFromCache = false,
        )
        viewModel.load()
        advanceUntilIdle()

        coEvery { noteRepository.deleteNote("note-1") } coAnswers {
            delay(100.milliseconds)
        }

        viewModel.delete("note-1")
        viewModel.delete("note-1") // fired before the first resolves - isDeleteInFlight should block this one
        advanceUntilIdle()

        coVerify(exactly = 1) { noteRepository.deleteNote("note-1") }
    }

    @Test
    fun `sync and mutation guards are independent - a delete is not blocked by an in-flight load`() = runTest {
        coEvery { noteRepository.getNotes() } coAnswers {
            delay(100.milliseconds)
            NotesResult(listOf(testNote("note-1")), failedCount = 0, isFromCache = false)
        }
        // Seed Content synchronously first so delete() has a Content state to operate on -
        // otherwise this test would just be exercising refresh()'s no-op-on-non-Content guard.
        coEvery { noteRepository.getNotes() } returns NotesResult(
            listOf(testNote("note-1")), failedCount = 0, isFromCache = false,
        )
        viewModel.load()
        advanceUntilIdle()

        coEvery { noteRepository.getNotes() } coAnswers {
            delay(100.milliseconds)
            NotesResult(listOf(testNote("note-1")), failedCount = 0, isFromCache = false)
        }
        coEvery { noteRepository.deleteNote("note-1") } returns Unit

        viewModel.refresh() // isSyncInFlight now true, in-flight for 100ms
        viewModel.delete("note-1") // should proceed anyway - separate guard
        advanceUntilIdle()

        coVerify(exactly = 1) { noteRepository.deleteNote("note-1") }
    }

    // ---- notesChangeSignal (background WS-nudge-triggered updates) ----

    @Test
    fun `a background change signal triggers load when nothing is on screen yet`() = runTest {
        coEvery { noteRepository.getNotes() } returns NotesResult(
            listOf(testNote("note-1")), failedCount = 0, isFromCache = false,
        )

        notesChangeSignal.notify()
        advanceUntilIdle()

        coVerify(exactly = 1) { noteRepository.getNotes() }
        assertTrue(viewModel.uiState.value is NoteListUiState.Content)
    }

    @Test
    fun `a background change signal triggers refresh rather than a full load when Content is already showing`() = runTest {
        coEvery { noteRepository.getNotes() } returns NotesResult(
            listOf(testNote("note-1")), failedCount = 0, isFromCache = false,
        )
        viewModel.load()
        advanceUntilIdle()

        coEvery { noteRepository.getNotes() } returns NotesResult(
            listOf(testNote("note-1"), testNote("note-2")), failedCount = 0, isFromCache = false,
        )
        notesChangeSignal.notify()
        advanceUntilIdle()

        val state = viewModel.uiState.value as NoteListUiState.Content
        assertEquals(2, state.notes.size)
        assertEquals(false, state.isRefreshing) // refresh() path, never shows the Loading screen
    }

    private fun testNote(id: String) = FaceNote(
        noteId = id,
        userId = "user-1",
        lastModifiedAt = Clock.System.now(),
        isDeleted = false,
        hasHistory = false,
        title = "Title $id",
        body = "Body $id",
        drawing = null,
        lockedByDeviceId = null,
        lockedAt = null,
    )
}