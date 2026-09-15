package com.martinsterentjevs.cacheit.di.controllers

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class NoteAutosaveControllerTest {

    private val defaultDurationMs = 30_000L

    @Test
    fun `timer fires autosave after inactivity duration elapses`() = runTest {
        var saveCount = 0
        val controller = NoteAutosaveController(
            scope = this,
            getInactivityDurationMs = { defaultDurationMs },
            onAutosave = { saveCount++ }
        )

        controller.onEditInteraction()
        advanceTimeBy((defaultDurationMs + 100).milliseconds)

        assertEquals(1, saveCount)
    }

    @Test
    fun `interaction before timer fires resets the countdown`() = runTest {
        var saveCount = 0
        val controller = NoteAutosaveController(
            scope = this,
            getInactivityDurationMs = { defaultDurationMs },
            onAutosave = { saveCount++ }
        )

        controller.onEditInteraction()
        advanceTimeBy((defaultDurationMs - 5_000).milliseconds) // not yet due
        controller.onEditInteraction()           // resets clock
        advanceTimeBy((defaultDurationMs - 5_000).milliseconds) // would've fired under old clock, not new one

        assertEquals(0, saveCount)

        advanceTimeBy(10_000.milliseconds) // now past the reset duration
        assertEquals(1, saveCount)
    }

    @Test
    fun `screen off cancels pending timer and saves immediately`() = runTest {
        var saveCount = 0
        val controller = NoteAutosaveController(
            scope = this,
            getInactivityDurationMs = { defaultDurationMs },
            onAutosave = { saveCount++ }
        )

        controller.onEditInteraction()
        advanceTimeBy(5_000.milliseconds) // well before the timer would fire
        controller.onScreenOff()

        // onScreenOff launches a coroutine for the save — let it run.
        advanceTimeBy(1.milliseconds)
        assertEquals(1, saveCount)

        // Confirm the original timer job was actually cancelled, not just
        // raced — advancing past its original due time must not double-save.
        advanceTimeBy(defaultDurationMs.milliseconds)
        assertEquals(1, saveCount)
    }

    @Test
    fun `dispose cancels pending timer without saving`() = runTest {
        var saveCount = 0
        val controller = NoteAutosaveController(
            scope = this,
            getInactivityDurationMs = { defaultDurationMs },
            onAutosave = { saveCount++ }
        )

        controller.onEditInteraction()
        advanceTimeBy(5_000.milliseconds)
        controller.dispose()
        advanceTimeBy(defaultDurationMs.milliseconds)

        assertEquals(0, saveCount)
    }
}