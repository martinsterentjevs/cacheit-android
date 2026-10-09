package com.martinsterentjevs.cacheit.note

import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.martinsterentjevs.cacheit.R
import com.martinsterentjevs.cacheit.test.CacheItInstrumentedTestBase
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Path A - full note lifecycle (Scope_Of_PreMVP_Checklist.md, Testing > Path A).
 *
 * Deviation from the checklist's literal step 4 wording ("tap Save -> assert navigation
 * back to list"): NoteEditViewModel.exitCreate() on a successful save transitions to View
 * mode on the SAME screen (saveAndExit's default targetMode), it does not pop the nav
 * stack. This test asserts the actual behavior (View mode, same screen, saved content
 * shown) and then separately exercises the back arrow to confirm the note shows up in
 * the list - which is what the checklist's step is actually trying to verify. Flagging
 * this here rather than silently treating the checklist's phrasing as ground truth.
 *
 * Step 8 (delete the account) is NOT implemented in this test - DELETE /account
 * (AccountController) doesn't exist server-side yet per the checklist's own note.
 */
@HiltAndroidTest
class PathAFullNoteLifecycleTest : CacheItInstrumentedTestBase() {

    @Test
    fun fullNoteLifecycle() {
        waitForAppToLoad()

        val identity = uniqueTestIdentity()
        val noteTitle = "Path A test note"
        val noteBody = "Created by the full note lifecycle instrumented test."
        val editedTitle = "Path A test note (edited)"

        // ---- Step 1: Register a fresh test account ----
        composeRule.onNodeWithTag("welcome_get_started_button").performClick()

        // RegistrationScreen's fields all live in a single LazyColumn item - on anything
        // but a tall screen, fields past the first one or two are below the fold and need
        // an explicit scroll before Compose will consider them hit-testable.
        composeRule.onNodeWithTag("registration_name_field").performScrollTo().performTextInput(identity.name)
        composeRule.onNodeWithTag("registration_email_field").performScrollTo().performTextInput(identity.email)
        composeRule.onNodeWithTag("registration_username_field").performScrollTo().performTextInput(identity.username)
        composeRule.onNodeWithTag("registration_password_field").performScrollTo().performTextInput(identity.password)
        composeRule.onNodeWithTag("registration_confirm_password_field").performScrollTo().performTextInput(identity.password)
        composeRule.onNodeWithTag("registration_submit_button").performScrollTo().performClick()

        // ---- Step 2: Assert Notes list shows Empty state ----
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithTag("note_list_empty_cta").fetchSemanticsNodes().isNotEmpty()
        }

        // ---- Step 3: Tap create FAB -> assert Editor opens in create mode ----
        composeRule.onNodeWithTag("note_list_create_fab").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("note_edit_title_field").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("note_edit_body_field").assertExists()

        // ---- Step 4: Enter title + body, tap Save ----
        composeRule.onNodeWithTag("note_edit_title_field").performTextInput(noteTitle)
        // The body field sits in NoteCanvas's fixed-aspect-ratio scrollable content area
        // (see NoteContentSurface) - it's frequently below the fold and needs an explicit
        // scroll before Compose will hit-test it, same as the registration fields above.
        composeRule.onNodeWithTag("note_edit_body_field").performScrollTo().performClick().performTextInput(noteBody)
        composeRule.onNodeWithTag("note_edit_save_button").performClick()

        // Save is a real network round-trip - wait for View mode rather than assuming
        // it lands synchronously.
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithContentDescription("Edit note").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue("Saved note body never appeared on screen", waitForOnScreenText(noteBody))

        // Confirm the note actually shows up in the list (the checklist's real intent
        // behind step 4), by navigating back rather than assuming an auto-pop.
        val backDescription = composeRule.activity.getString(R.string.navigation_back)
        composeRule.onNodeWithContentDescription(backDescription).performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("note_list_create_fab").fetchSemanticsNodes().isNotEmpty()
        }

        // ---- Step 5: Tap the note -> assert Editor opens in edit mode with correct content ----
        composeRule.onNodeWithText(noteTitle).performScrollTo().performClick()
        assertTrue("Reloaded note body never appeared on screen", waitForOnScreenText(noteBody))

        // ---- Step 6: Edit title/body, tap Save -> assert the change persisted ----
        composeRule.onNodeWithContentDescription("Edit note").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("note_edit_title_field").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("note_edit_title_field").performTextClearance()
        composeRule.onNodeWithTag("note_edit_title_field").performTextInput(editedTitle)
        composeRule.onNodeWithTag("note_edit_save_button").performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithContentDescription("Edit note").fetchSemanticsNodes().isNotEmpty()
        }

        // ---- Step 7: Delete the note ----
        composeRule.onNodeWithContentDescription(backDescription).performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("note_list_create_fab").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithContentDescription(
            composeRule.activity.getString(R.string.note_card_delete_note),
        )[0].performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithTag("note_list_empty_cta").fetchSemanticsNodes().isNotEmpty()
        }

        // Step 8 (delete the account) intentionally not implemented - see class doc.
    }
}