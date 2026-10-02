package com.martinsterentjevs.cacheit.note

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
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.martinsterentjevs.cacheit.R
import com.martinsterentjevs.cacheit.test.CacheItInstrumentedTestBase
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Path B - version restore, text only (Scope_Of_PreMVP_Checklist.md, Testing > Path B).
 *
 * The version list (NoteVersionScreen) only shows a timestamp per row, never the note's
 * title/body - there's no way to pick "the version with title A" by row text. Instead this
 * finds the row WITHOUT the "Current version" caption (there are exactly two versions after
 * step 2: the current one and one older one) and previews it to confirm it's actually the
 * older ("A") state before restoring - a stronger check than trusting row order, which isn't
 * documented anywhere as newest-first or oldest-first.
 */
@HiltAndroidTest
class PathBVersionRestoreTextOnlyTest : CacheItInstrumentedTestBase() {

    @Test
    fun versionRestoreTextOnly() {
        waitForAppToLoad()

        val identity = uniqueTestIdentity()
        val titleA = "Path B version A"
        val titleB = "Path B version B"

        // ---- Register (same flow as Path A) ----
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

        // ---- Step 1: create a note (title "A"), save -> 1 version exists ----
        composeRule.onNodeWithTag("note_list_create_fab").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("note_edit_title_field").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("note_edit_title_field").performTextInput(titleA)
        composeRule.onNodeWithTag("note_edit_save_button").performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithContentDescription("Edit note").fetchSemanticsNodes().isNotEmpty()
        }

        val backDescription = composeRule.activity.getString(R.string.navigation_back)

        // ---- Step 2: edit title to "B", save -> 2 versions exist, hasHistory true ----
        composeRule.onNodeWithContentDescription("Edit note").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("note_edit_title_field").fetchSemanticsNodes().isNotEmpty()
        }
        // Cursor lands at the end of existing text on focus - clear first so the field
        // ends up as exactly titleB rather than titleA with titleB appended.
        composeRule.onNodeWithTag("note_edit_title_field").performTextClearance()
        composeRule.onNodeWithTag("note_edit_title_field").performTextInput(titleB)
        composeRule.onNodeWithTag("note_edit_save_button").performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithContentDescription("Edit note").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithContentDescription(backDescription).performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("note_list_create_fab").fetchSemanticsNodes().isNotEmpty()
        }

        // ---- Step 3: open version history, select the non-current (older, "A") version ----
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

        // Confirm the preview really is the "A" state before restoring - don't just trust
        // "the non-current one must be A" without checking.
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("note_version_preview_title").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(titleA).assertExists()

        composeRule.onNodeWithTag("note_version_restore_confirm").performClick()

        // ---- Step 4: assert displayed title reverts to "A" ----
        // onRestored pops all the way back to NoteList (see NavHost), skipping back through
        // NoteEditScreen - so the check happens on the list, not by reopening the editor.
        // Title uses UiAutomator rather than Compose semantics matching Path A's reasoning:
        // not proven here to be plain Compose Text vs. something markdown-rendered, and
        // UiAutomator works either way.
        val restored = waitForOnScreenText(titleA)
        assertTrue("Note title never reverted to '$titleA' after restore", restored)
    }
}