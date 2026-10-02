package com.martinsterentjevs.cacheit.note

import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.martinsterentjevs.cacheit.test.CacheItInstrumentedTestBase
import com.martinsterentjevs.cacheit.test.SecondDeviceClient
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException

/**
 * Path E - two-device drawing-lock conflict (Scope_Of_PreMVP_Checklist.md, Testing > Path E).
 *
 * Per the checklist's own scope check: this only covers the drawing-lock-conflict half of
 * Path E (device 2's API call attempting to acquire a lock device 1 already holds -> 409).
 * Edit-propagation via pull-to-refresh/manual sync is effectively already covered by Path D's
 * shape (getNotes() sees device 1's write). Edit propagation via WebSocket-nudge-triggered
 * sync is explicitly out of scope here per the checklist ("that's WebSocket-issue territory
 * and shouldn't block this issue closing").
 *
 * Device 2 discovers the noteId via getNotes() rather than having it passed in some other
 * way - noteId is the one field the server holds unencrypted, so this doesn't require any
 * decryption on device 2's side (see SecondDeviceClient's class doc).
 */
@HiltAndroidTest
class PathEDrawingLockConflictTest : CacheItInstrumentedTestBase() {

    @Test
    fun secondDeviceCannotAcquireLockFirstDeviceHolds() {
        waitForAppToLoad()

        val identity = uniqueTestIdentity()
        val title = "Path E lock conflict note"

        // ---- Device 1 (real UI): register, create a note, save ----
        composeRule.onNodeWithTag("welcome_get_started_button").performClick()
        composeRule.onNodeWithTag("registration_name_field").performScrollTo().performTextInput(identity.name)
        composeRule.onNodeWithTag("registration_email_field").performScrollTo().performTextInput(identity.email)
        composeRule.onNodeWithTag("registration_username_field").performScrollTo().performTextInput(identity.username)
        composeRule.onNodeWithTag("registration_password_field").performScrollTo().performTextInput(identity.password)
        composeRule.onNodeWithTag("registration_confirm_password_field").performScrollTo().performTextInput(identity.password)
        composeRule.onNodeWithTag("registration_submit_button").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithTag("note_list_empty_cta").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("note_list_create_fab").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("note_edit_title_field").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("note_edit_title_field").performTextInput(title)
        composeRule.onNodeWithTag("note_edit_save_button").performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }

        // ---- Device 2: login, discover the noteId (content stays encrypted/unread) ----
        val secondDevice = SecondDeviceClient()
        val noteId = runBlocking {
            secondDevice.login(identity.email, identity.password)
            val notes = secondDevice.noteApi.getNotes()
            check(notes.size == 1) { "Expected exactly one note, found ${notes.size}" }
            notes.first().noteId
        }

        // ---- Device 1 (real UI): enter drawing mode, which acquires the server-side lock ----
        composeRule.onNodeWithContentDescription("Edit note").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("note_edit_title_field").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Switch to drawing").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("note_edit_drawing_canvas").fetchSemanticsNodes().isNotEmpty()
        }
        // enterDrawingEdit's server call is async relative to the mode switch appearing on
        // screen - the canvas rendering doesn't guarantee acquireDrawingLock has round-tripped
        // yet, so give it a moment before device 2 tries to acquire the same lock.
        composeRule.waitForIdle()

        // ---- Device 2: attempt to acquire the same lock -> expect 409 ----
        runBlocking {
            try {
                secondDevice.noteApi.acquireDrawingLock(noteId!!)
                fail("Expected a 409 Conflict - device 1 already holds the drawing lock")
            } catch (e: HttpException) {
                assertEquals(409, e.code())
            }
        }
    }
}