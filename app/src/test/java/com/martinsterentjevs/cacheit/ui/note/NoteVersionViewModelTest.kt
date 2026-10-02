package com.martinsterentjevs.cacheit.ui.note

import app.cash.turbine.test
import com.martinsterentjevs.cacheit.data.note.FaceNoteVersion
import com.martinsterentjevs.cacheit.data.note.NoteFlowException
import com.martinsterentjevs.cacheit.data.note.NoteRepository
import com.martinsterentjevs.cacheit.data.note.NoteWriteResult
import com.martinsterentjevs.cacheit.network.note.NoteVersionMeta
import com.martinsterentjevs.cacheit.testutil.MainDispatcherRule
import com.martinsterentjevs.cacheit.ui.common.PopupController
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class NoteVersionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var noteRepository: NoteRepository
    private lateinit var popupController: PopupController
    private lateinit var viewModel: NoteVersionViewModel

    @Before
    fun setUp() {
        noteRepository = mockk()
        popupController = mockk(relaxed = true)
        viewModel = NoteVersionViewModel(noteRepository, popupController)
    }

    @Test
    fun `load surfaces Content when the repository returns versions`() = runTest {
        coEvery { noteRepository.getVersionHistory("note-1") } returns listOf(testVersionMeta("v1"))

        viewModel.load("note-1")
        advanceUntilIdle()

        val state = viewModel.uiState.value as NoteVersionUiState.Content
        assertEquals(1, state.versions.size)
    }

    @Test
    fun `load surfaces Empty when there is no version history`() = runTest {
        coEvery { noteRepository.getVersionHistory("note-1") } returns emptyList()

        viewModel.load("note-1")
        advanceUntilIdle()

        assertEquals(NoteVersionUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `load surfaces Error with the exception's user-facing message`() = runTest {
        coEvery { noteRepository.getVersionHistory("note-1") } throws NoteFlowException("Couldn't load version history")

        viewModel.load("note-1")
        advanceUntilIdle()

        assertEquals(NoteVersionUiState.Error("Couldn't load version history"), viewModel.uiState.value)
    }

    @Test
    fun `a second load call while one is already in flight is a no-op`() = runTest {
        coEvery { noteRepository.getVersionHistory("note-1") } coAnswers {
            delay(100.milliseconds)
            listOf(testVersionMeta("v1"))
        }

        viewModel.load("note-1")
        viewModel.load("note-1")
        advanceUntilIdle()

        coVerify(exactly = 1) { noteRepository.getVersionHistory("note-1") }
    }

    @Test
    fun `onVersionTapped populates the preview on the existing Content state`() = runTest {
        loadContent("note-1")
        val preview = testFaceNoteVersion("v1")
        coEvery { noteRepository.getVersion("note-1", "v1") } returns preview

        viewModel.onVersionTapped("v1")
        advanceUntilIdle()

        val state = viewModel.uiState.value as NoteVersionUiState.Content
        assertEquals(preview, state.previewedVersion)
        assertEquals("v1", state.previewedVersionId)
    }

    @Test
    fun `onVersionTapped shows an error snackbar and leaves the preview untouched on failure`() = runTest {
        loadContent("note-1")
        coEvery { noteRepository.getVersion("note-1", "v1") } throws NoteFlowException("Couldn't load that version")

        viewModel.onVersionTapped("v1")
        advanceUntilIdle()

        val state = viewModel.uiState.value as NoteVersionUiState.Content
        assertNull(state.previewedVersion)
        verify { popupController.show(any()) }
    }

    @Test
    fun `dismissPreview clears the previewed version`() = runTest {
        loadContent("note-1")
        coEvery { noteRepository.getVersion("note-1", "v1") } returns testFaceNoteVersion("v1")
        viewModel.onVersionTapped("v1")
        advanceUntilIdle()

        viewModel.dismissPreview()

        val state = viewModel.uiState.value as NoteVersionUiState.Content
        assertNull(state.previewedVersion)
        assertNull(state.previewedVersionId)
    }

    @Test
    fun `restore sends RestoreSuccess and clears isRestoring on a verified result`() = runTest {
        loadContent("note-1")
        coEvery { noteRepository.restoreVersion("note-1", "v1") } returns NoteWriteResult.Verified(mockk(relaxed = true))

        viewModel.events.test {
            viewModel.restore("v1")
            assertEquals(NoteVersionEvent.RestoreSuccess, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        advanceUntilIdle()

        assertFalse((viewModel.uiState.value as NoteVersionUiState.Content).isRestoring)
    }

    @Test
    fun `restore shows the unconfirmed-write snackbar without sending RestoreSuccess on an unverified result`() = runTest {
        loadContent("note-1")
        coEvery { noteRepository.restoreVersion("note-1", "v1") } returns NoteWriteResult.Unverified

        viewModel.restore("v1")
        advanceUntilIdle()

        verify { popupController.show(any()) }
        assertFalse((viewModel.uiState.value as NoteVersionUiState.Content).isRestoring)
    }

    @Test
    fun `restore shows an error snackbar and clears isRestoring on failure`() = runTest {
        loadContent("note-1")
        coEvery { noteRepository.restoreVersion("note-1", "v1") } throws NoteFlowException("Restore failed - try again")

        viewModel.restore("v1")
        advanceUntilIdle()

        verify { popupController.show(any()) }
        assertFalse((viewModel.uiState.value as NoteVersionUiState.Content).isRestoring)
    }

    private fun kotlinx.coroutines.test.TestScope.loadContent(noteId: String) {
        coEvery { noteRepository.getVersionHistory(noteId) } returns listOf(testVersionMeta("v1"))
        viewModel.load(noteId)
        advanceUntilIdle()
    }

    private fun testVersionMeta(versionId: String) = NoteVersionMeta(
        versionId = versionId,
        noteId = "note-1",
        createdAt = "2026-01-01T00:00:00Z",
        deviceId = "device-1",
        isCurrent = false,
    )

    private fun testFaceNoteVersion(versionId: String) = FaceNoteVersion(
        versionId = versionId,
        title = "Title",
        body = "Body",
        drawing = null,
    )
}