package com.martinsterentjevs.cacheit.note

import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.martinsterentjevs.cacheit.test.CacheItInstrumentedTestBase
import com.martinsterentjevs.cacheit.test.SecondDeviceClient
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Path D - two-device sync visibility (Scope_Of_PreMVP_Checklist.md, Testing > Path D).
 *
 * "Device 2" is SecondDeviceClient (see its class doc) making raw API calls, NOT a second
 * emulator instance - per the checklist's own note, driving two real emulators is
 * disproportionate for this project.
 *
 * Content isn't decrypted or compared here - noteId is the only field the server holds
 * unencrypted, so "the note is visible to device 2" is checked via presence/count, matching
 * the checklist's "assert via API response, not UI" framing.
 */
@HiltAndroidTest
class PathDTwoDeviceSyncVisibilityTest : CacheItInstrumentedTestBase() {

    @Test
    fun secondDeviceSeesNoteCreatedByFirst() {
        waitForAppToLoad()

        val identity = uniqueTestIdentity()
        val title = "Path D sync test note"

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

        // ---- Device 2 (direct API call): login, GET /notes ----
        val secondDevice = SecondDeviceClient()
        val notes = runBlocking {
            secondDevice.login(identity.email, identity.password)
            secondDevice.noteApi.getNotes()
        }

        assertEquals("Expected exactly the one note device 1 created to be visible to device 2", 1, notes.size)
    }
}