package com.martinsterentjevs.cacheit.note

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.martinsterentjevs.cacheit.R
import com.martinsterentjevs.cacheit.test.CacheItInstrumentedTestBase
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Path C - version restore, drawing to text-only (Scope_Of_PreMVP_Checklist.md, Testing >
 * Path C). Depends on Path B's mechanics (version list, non-current-row selection) plus the
 * NoteEditScreen redesign's exclusive text/drawing toggle - both already in place.
 *
 * The final assertion is scoped to what's actually checkable through Compose/UiAutomator: the
 * restored version's PREVIEW correctly shows no "has drawing" indicator before restoring
 * (this is the meaningful check - it proves the version being restored really is the
 * pre-drawing snapshot). Asserting the drawing canvas is visually empty AFTER restore isn't
 * practical here - Canvas-drawn pixels aren't exposed through the accessibility tree, so
 * there's no semantics-based way to assert "no strokes are visible". A screenshot-diff test
 * would be the right tool for that and is out of scope for this pass.
 */
@HiltAndroidTest
class PathCVersionRestoreDrawingToTextTest : CacheItInstrumentedTestBase() {

    @Test
    fun versionRestoreDrawingToTextOnly() {
        waitForAppToLoad()

        val identity = uniqueTestIdentity()
        val title = "Path C drawing note"
        val body = "Text-only before drawing is added."

        // ---- Register ----
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

        // ---- Step 1: create a text-only note, save ----
        composeRule.onNodeWithTag("note_list_create_fab").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("note_edit_title_field").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("note_edit_title_field").performTextInput(title)
        composeRule.onNodeWithTag("note_edit_body_field").performScrollTo().performClick().performTextInput(body)
        composeRule.onNodeWithTag("note_edit_save_button").performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithContentDescription("Edit note").fetchSemanticsNodes().isNotEmpty()
        }

        val backDescription = composeRule.activity.getString(R.string.navigation_back)

        // ---- Step 2: switch to drawing mode, draw something, save ----
        composeRule.onNodeWithContentDescription("Edit note").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("note_edit_title_field").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Switch to drawing").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("note_edit_drawing_canvas").fetchSemanticsNodes().isNotEmpty()
        }

        // Simulate a single stroke - down, a couple of moves, up. DrawingLayer uses a raw
        // pointerInput/awaitEachGesture handler rather than a high-level gesture detector, but
        // performTouchInput synthesizes real pointer events at the level Compose's input
        // pipeline consumes regardless, so this should reach it the same way a finger would.
        composeRule.onNodeWithTag("note_edit_drawing_canvas").performTouchInput {
            down(Offset(x = 50f, y = 50f))
            moveTo(Offset(x = 150f, y = 150f))
            moveTo(Offset(x = 250f, y = 100f))
            up()
        }

        composeRule.onNodeWithTag("note_edit_save_button").performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithContentDescription("Edit note").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithContentDescription(backDescription).performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("note_list_create_fab").fetchSemanticsNodes().isNotEmpty()
        }

        // ---- Step 3: open version history, restore the pre-drawing (text-only) version ----
        val currentVersionCaption = composeRule.activity.getString(R.string.note_version_current)
        composeRule.onAllNodesWithContentDescription(
            composeRule.activity.getString(R.string.note_view_history),
        )[0].performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("note_version_list_item").fetchSemanticsNodes().size >= 2
        }

        val olderVersionMatcher: SemanticsMatcher =
            hasTestTag("note_version_list_item") and !hasText(currentVersionCaption, substring = true)
        composeRule.onNode(olderVersionMatcher).performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("note_version_preview_title").fetchSemanticsNodes().isNotEmpty()
        }
        // The meaningful check for this path: confirm the version about to be restored is
        // genuinely the pre-drawing, text-only snapshot before committing to the restore.
        composeRule.onNodeWithText(body).assertExists()
        assertFalse(
            "Expected the pre-drawing version to have no drawing, but the preview shows one",
            composeRule.onAllNodesWithTag("note_version_preview_has_drawing").fetchSemanticsNodes().isNotEmpty(),
        )

        composeRule.onNodeWithTag("note_version_restore_confirm").performClick()

        // ---- Step 4: assert the note displays as text-only again ----
        // See class doc - scoped to "restore completed and the note is showing" rather than
        // asserting canvas pixel content, which isn't reachable through semantics.
        val restored = waitForOnScreenText(title)
        assertTrue("Note never reappeared in the list after restore", restored)
    }
}