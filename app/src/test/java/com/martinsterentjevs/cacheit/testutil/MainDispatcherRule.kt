package com.martinsterentjevs.cacheit.testutil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Every ViewModel here launches work via viewModelScope, which resolves to
 * Dispatchers.Main. Unit tests run on the JVM with no real Android Main looper,
 * so without this rule any viewModelScope.launch throws
 * "Module with the Main dispatcher had failed to initialize".
 *
 * Usage:
 *   @get:Rule
 *   val mainDispatcherRule = MainDispatcherRule()
 *
 * Then drive coroutines from a `runTest { }` block. StandardTestDispatcher means
 * launched coroutines don't run until the test explicitly yields (e.g. the
 * runTest block finishing, or an explicit advanceUntilIdle()) - this is what lets
 * single-flight-guard tests fire two calls back-to-back before either completes.
 */
@ExperimentalCoroutinesApi
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}