package com.martinsterentjevs.cacheit.testutil

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * android.util.Log is a stub on the JVM unit-test classpath (real implementation
 * only exists on-device or under Robolectric) - every Log.* call throws
 * "not mocked. See http://g.co/androidstudio/not-mocked" unless stubbed first.
 *
 * Covers every overload actually in use across the codebase (NoteRepository,
 * BaseAuthViewModel, NoteEditViewModel, NoteListViewModel, the WS layer). Add a
 * new `every` line here if a class under test calls an overload not yet covered
 * - don't stub it locally in the individual test, this rule is the one place.
 *
 * Usage:
 *   @get:Rule
 *   val androidLogRule = AndroidLogRule()
 */
class AndroidLogRule : TestWatcher() {

    override fun starting(description: Description) {
        mockkStatic(Log::class)
        every { Log.v(any(), any<String>()) } returns 0
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.i(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<Throwable>()) } returns 0
        every { Log.w(any(), any<String>(), any<Throwable>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any<Throwable>()) } returns 0
    }

    override fun finished(description: Description) {
        unmockkStatic(Log::class)
    }
}