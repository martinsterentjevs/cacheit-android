package com.martinsterentjevs.cacheit.data.auth

import com.martinsterentjevs.cacheit.network.auth.AccountSessionResponse
import com.martinsterentjevs.cacheit.network.auth.AuthApi
import com.martinsterentjevs.cacheit.network.auth.SaltLookupResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.util.Base64

class AuthRepositoryImplTest {

    private lateinit var authApi: AuthApi
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setUp() {
        authApi = mockk()
        repository = AuthRepositoryImpl(authApi)
    }

    @Test
    fun `fetchSalt decodes the base64 salt from the server`() = runTest {
        val rawSalt = byteArrayOf(1, 2, 3, 4)
        coEvery { authApi.fetchSalt(any()) } returns SaltLookupResponse(kdfSalt = Base64.getEncoder().encodeToString(rawSalt))

        val result = repository.fetchSalt("user@cacheit.test")

        assertArrayEquals(rawSalt, result)
    }

    @Test
    fun `fetchSalt maps a generic HttpException to the reach-the-server message`() = runTest {
        coEvery { authApi.fetchSalt(any()) } throws httpException(500)

        try {
            repository.fetchSalt("user@cacheit.test")
            fail("Expected AuthFlowException")
        } catch (e: AuthFlowException) {
            assertEquals("Couldn't reach the server", e.userMessage)
        }
    }

    @Test
    fun `login maps the session response into an AuthSession`() = runTest {
        coEvery { authApi.login(any()) } returns AccountSessionResponse(
            userId = "account-1",
            deviceId = "device-1",
            accessToken = "access-token",
            refreshToken = "refresh-token",
            encMekEnvelope = "wrapped-mek",
        )

        val result = repository.login("user@cacheit.test", byteArrayOf(1, 2, 3), "device-1", "Pixel Test")

        assertEquals(AuthSession("account-1", "device-1", "access-token", "refresh-token", "wrapped-mek"), result)
    }

    @Test
    fun `login maps a 401 to the incorrect-credentials message`() = runTest {
        coEvery { authApi.login(any()) } throws httpException(401)

        try {
            repository.login("user@cacheit.test", byteArrayOf(1, 2, 3), "device-1", "Pixel Test")
            fail("Expected AuthFlowException")
        } catch (e: AuthFlowException) {
            assertEquals("Incorrect email/username or password", e.userMessage)
        }
    }

    @Test
    fun `login maps an IOException to the connectivity message`() = runTest {
        coEvery { authApi.login(any()) } throws IOException("no network")

        try {
            repository.login("user@cacheit.test", byteArrayOf(1, 2, 3), "device-1", "Pixel Test")
            fail("Expected AuthFlowException")
        } catch (e: AuthFlowException) {
            assertEquals("Can't reach the server. Check your connection.", e.userMessage)
        }
    }

    @Test
    fun `register maps a 409 to the already-registered message`() = runTest {
        coEvery { authApi.register(any()) } throws httpException(409)

        try {
            repository.register(
                name = "Test User",
                username = "user",
                email = "user@cacheit.test",
                salt = byteArrayOf(1),
                authHash = byteArrayOf(2),
                wrappedMek = "wrapped-mek",
                deviceId = "device-1",
                deviceName = "Pixel Test",
            )
            fail("Expected AuthFlowException")
        } catch (e: AuthFlowException) {
            assertEquals("That email or username is already registered", e.userMessage)
        }
    }

    @Test
    fun `logout maps a failure to the could-not-log-out message`() = runTest {
        coEvery { authApi.logout() } throws httpException(500)

        try {
            repository.logout()
            fail("Expected AuthFlowException")
        } catch (e: AuthFlowException) {
            assertEquals("Couldn't log out - try again", e.userMessage)
        }
    }

    private fun httpException(code: Int): HttpException =
        HttpException(Response.error<Any>(code, "".toResponseBody(null)))
}