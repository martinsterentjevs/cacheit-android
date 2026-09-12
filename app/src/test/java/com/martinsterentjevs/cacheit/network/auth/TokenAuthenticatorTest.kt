package com.martinsterentjevs.cacheit.network.auth

import com.martinsterentjevs.cacheit.services.security.SecurityService
import com.martinsterentjevs.cacheit.services.security.SessionExpiredSignal
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.milliseconds

/**
 * Covers the refresh-on-401 flow: single-flight guard (a concurrent caller that
 * already refreshed shouldn't trigger a second refresh), the priorResponseCount
 * give-up-after-2-retries check, and session teardown on refresh failure.
 *
 * authenticate() is a blocking Authenticator override that wraps suspend calls in
 * runBlocking internally - tests call it directly from a plain @Test method, no
 * runTest/MainDispatcherRule needed here (that's for viewModelScope-based code).
 */
class TokenAuthenticatorTest {

    private lateinit var securityService: SecurityService
    private lateinit var authApi: AuthApi
    private lateinit var sessionExpiredSignal: SessionExpiredSignal
    private lateinit var authenticator: TokenAuthenticator

    @Before
    fun setUp() {
        securityService = mockk(relaxed = true)
        authApi = mockk()
        sessionExpiredSignal = mockk(relaxed = true)
        authenticator = TokenAuthenticator(
            securityService = securityService,
            authApiProvider = { authApi },
            sessionExpiredSignal = sessionExpiredSignal,
        )
    }

    @Test
    fun `returns the currently cached token without refreshing when it already differs from the failed token`() {
        // Simulates a concurrent request that failed on an old token while another
        // caller already refreshed - the fresh token is already sitting in SecurityService.
        every { securityService.getAccessToken() } returns "freshToken"

        val response = buildUnauthorizedResponse(failedToken = "staleToken")
        val result = authenticator.authenticate(null, response)

        assertEquals("Bearer freshToken", result?.header("Authorization"))
        coVerify(exactly = 0) { authApi.refresh(any()) }
    }

    @Test
    fun `refreshes when the cached token matches the token that just failed`() {
        every { securityService.getAccessToken() } returns "expiredToken"
        every { securityService.getRefreshToken() } returns "refreshToken123"
        every { securityService.getOrCreateDeviceId() } returns "device-1"
        coEvery { authApi.refresh(RefreshRequestDto("device-1", "refreshToken123")) } returns
                AccountSessionResponse(
                    userId = "user-1",
                    deviceId = "device-1",
                    accessToken = "brandNewToken",
                    refreshToken = "brandNewRefreshToken",
                    encMekEnvelope = "envelope",
                )

        val response = buildUnauthorizedResponse(failedToken = "expiredToken")
        val result = authenticator.authenticate(null, response)

        assertEquals("Bearer brandNewToken", result?.header("Authorization"))
        verify { securityService.setSession("user-1", "brandNewToken", "brandNewRefreshToken") }
    }

    @Test
    fun `refreshes when there is no cached token at all`() {
        every { securityService.getAccessToken() } returns null
        every { securityService.getRefreshToken() } returns "refreshToken123"
        every { securityService.getOrCreateDeviceId() } returns "device-1"
        coEvery { authApi.refresh(any()) } returns
                AccountSessionResponse("user-1", "device-1", "newToken", "newRefreshToken", "envelope")

        val response = buildUnauthorizedResponse(failedToken = "someOldToken")
        val result = authenticator.authenticate(null, response)

        assertEquals("Bearer newToken", result?.header("Authorization"))
    }

    @Test
    fun `clears the session and signals expiry when the refresh call itself fails`() {
        every { securityService.getAccessToken() } returns "expiredToken"
        every { securityService.getRefreshToken() } returns "refreshToken123"
        every { securityService.getOrCreateDeviceId() } returns "device-1"
        coEvery { authApi.refresh(any()) } throws RuntimeException("refresh token rejected by server")

        val response = buildUnauthorizedResponse(failedToken = "expiredToken")
        val result = authenticator.authenticate(null, response)

        assertNull(result)
        verify { securityService.clearSession() }
        verify { securityService.clearSecureMek() }
        coVerify { sessionExpiredSignal.notify() }
    }

    @Test
    fun `returns null without attempting a refresh when there is no stored refresh token`() {
        every { securityService.getAccessToken() } returns "expiredToken"
        every { securityService.getRefreshToken() } returns null

        val response = buildUnauthorizedResponse(failedToken = "expiredToken")
        val result = authenticator.authenticate(null, response)

        assertNull(result)
        coVerify(exactly = 0) { authApi.refresh(any()) }
    }

    @Test
    fun `gives up after two prior retries on the same request chain`() {
        every { securityService.getAccessToken() } returns "expiredToken"

        val original = buildUnauthorizedResponse(failedToken = "expiredToken")
        // One priorResponse already makes priorResponseCount() == 2, which is the give-up threshold.
        val retried = buildUnauthorizedResponse(failedToken = "expiredToken", previousResponse = original)

        val result = authenticator.authenticate(null, retried)

        assertNull(result)
        verify(exactly = 0) { securityService.getRefreshToken() }
    }

    @Test
    fun `concurrent callers on the same expired token only trigger one refresh`() {
        // A mutable "server" of sorts so the mock behaves statefully across threads -
        // getAccessToken() reflects whatever the most recent setSession() wrote, which is
        // what makes the single-flight guard's "already refreshed by someone else" branch
        // actually reachable under real concurrency.
        val storedAccessToken = AtomicReference("expiredToken")
        val refreshCallCount = AtomicInteger(0)

        every { securityService.getAccessToken() } answers { storedAccessToken.get() }
        every { securityService.getRefreshToken() } returns "refreshToken123"
        every { securityService.getOrCreateDeviceId() } returns "device-1"
        every { securityService.setSession(any(), any(), any()) } answers {
            storedAccessToken.set(secondArg())
        }
        coEvery { authApi.refresh(any()) } coAnswers {
            refreshCallCount.incrementAndGet()
            // Small delay so both threads are genuinely racing to acquire the mutex.
            kotlinx.coroutines.delay(50.milliseconds)
            AccountSessionResponse("user-1", "device-1", "refreshedToken", "newRefreshToken", "envelope")
        }

        val response1 = buildUnauthorizedResponse(failedToken = "expiredToken")
        val response2 = buildUnauthorizedResponse(failedToken = "expiredToken")

        val thread1 = Thread { authenticator.authenticate(null, response1) }
        val thread2 = Thread { authenticator.authenticate(null, response2) }
        thread1.start()
        thread2.start()
        thread1.join()
        thread2.join()

        assertEquals(1, refreshCallCount.get())
    }

    private fun buildUnauthorizedResponse(failedToken: String, previousResponse: Response? = null): Response {
        val request = Request.Builder()
            .url("https://cacheit.test/notes")
            .header("Authorization", "Bearer $failedToken")
            .build()

        val builder = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")

        if (previousResponse != null) {
            builder.priorResponse(previousResponse)
        }

        return builder.build()
    }
}