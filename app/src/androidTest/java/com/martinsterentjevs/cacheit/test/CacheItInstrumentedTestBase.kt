package com.martinsterentjevs.cacheit.test

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.martinsterentjevs.cacheit.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Base for Compose instrumented tests (Paths A, B, C) and the UI-assertion half of the
 * two-device paths (D, E). Rule order matters: Hilt must inject before MainActivity launches.
 *
 * Uses the v2 testing package (androidx.compose.ui.test.junit4.v2), not the deprecated v1
 * createAndroidComposeRule/AndroidComposeTestRule. NOTE: the v2 APIs are alpha per Google's
 * own migration guide ("subject to change") - flagging that trade-off explicitly since it's a
 * real stability risk to take on, not just a mechanical import swap.
 *
 * The v1 -> v2 switch is NOT cosmetic: v1 ran composition on UnconfinedTestDispatcher
 * (coroutines execute immediately, inline), v2 runs it on StandardTestDispatcher (coroutines
 * are queued and only run when the test explicitly advances/idles). This is almost certainly
 * why tests were crashing before the app had a chance to load - MainActivity launch, Hilt
 * injection, and SessionCheckViewModel's routing decision are all coroutine-driven, and
 * nothing runs until something pumps the dispatcher. waitForAppToLoad() below is that pump -
 * call it as the first line of every test body, not just an occasional convenience.
 *
 * Deliberately does NOT try to seed a session before the Activity launches (e.g. to skip
 * past Welcome/Registration) - AndroidComposeTestRule launches the Activity as part of rule
 * application, before @Before or the test body run, so there's no hook to inject a session
 * first without switching to a manual ActivityScenario.launch() flow. For the paths this
 * currently covers, registration/login IS one of the steps under test anyway (see Path A
 * step 1), so this isn't a real limitation yet - flagging it here in case a future path
 * needs to start from an already-authenticated state.
 *
 * No teardown/account-deletion here: DELETE /account (AccountController) isn't implemented
 * server-side yet (see mvp-checklist), so test accounts accumulate on whatever backend this
 * runs against. Fine for a local docker-compose stack you tear down anyway - worth revisiting
 * once AccountController exists.
 */
@RunWith(AndroidJUnit4::class)
abstract class CacheItInstrumentedTestBase {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val clearStateRule = object : org.junit.rules.ExternalResource() {
        override fun before() {
            val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
            context.getSharedPreferences("cacheit_local_store", android.content.Context.MODE_PRIVATE)
                .edit().clear().commit()
            context.deleteDatabase("cacheit.db")
        }
    }

    @get:Rule(order = 2)
    val composeRule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity> =
        createAndroidComposeRule<MainActivity>()

    private val uiDevice: UiDevice by lazy {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Before
    fun setUpHilt() {
        hiltRule.inject()
    }

    /**
     * Waits until the app has actually rendered its first real screen (Welcome if there's no
     * session, NoteList if there is) before letting the test body touch anything.
     *
     * Call this as the first line of every test body. Two things are happening here, not one:
     * waitForIdle() explicitly pumps the StandardTestDispatcher-backed clock so queued
     * coroutines (Hilt injection completing, SessionCheckViewModel's routing decision) actually
     * get a chance to run - v2's own migration guide calls this out as the standard fix for
     * "assertion fails immediately because the data is still in a Loading state". waitUntil()
     * on top of that is a belt-and-suspenders wait for the specific tag to exist, since a single
     * waitForIdle() call isn't guaranteed to span a real network-backed transition end to end.
     */
    protected fun waitForAppToLoad(timeoutMillis: Long = 15_000) {
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis) {
            composeRule.onAllNodesWithTag("welcome_get_started_button").fetchSemanticsNodes().isNotEmpty() ||
                    composeRule.onAllNodesWithTag("note_list_create_fab").fetchSemanticsNodes().isNotEmpty() ||
                    composeRule.onAllNodesWithTag("note_list_empty_cta").fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** A fresh, collision-free identity for each test run - registration is real, hits the real backend. */
    protected fun uniqueTestIdentity(): TestIdentity {
        val suffix = UUID.randomUUID().toString().take(8)
        return TestIdentity(
            name = "Test User $suffix",
            username = "test_$suffix",
            email = "test-$suffix@cacheit.test",
            password = "TestPassword123!",
        )
    }

    /**
     * Waits for [text] to appear anywhere in the real on-screen accessibility tree, via
     * UiAutomator rather than Compose semantics.
     *
     * Needed for any content rendered through MarkdownText (dev.jeziellago:compose-markdown),
     * which wraps a plain Android TextView in an AndroidView (Markwon-based, confirmed against
     * the library's own source) - Compose's semantics tree does not automatically surface text
     * from an embedded AndroidView, so composeRule.onNodeWithText(...) will not find it. This is
     * only a concern for the rendered (non-editing) note body in View/DrawingEdit mode - the
     * title bar and the editing-mode body field (BasicTextField) are native Compose and are
     * reachable through normal semantics matching.
     *
     * Returns without throwing if the text never appears - callers should assert on the result
     * or follow with a Compose-side assertion that would fail anyway if the screen never updated.
     */
    protected fun waitForOnScreenText(text: String, timeoutMillis: Long = 15_000): Boolean =
        uiDevice.wait(Until.hasObject(By.textContains(text)), timeoutMillis)
}

data class TestIdentity(
    val name: String,
    val username: String,
    val email: String,
    val password: String,
)