package com.martinsterentjevs.cacheit.data.auth

import com.martinsterentjevs.cacheit.network.auth.AccountSessionResponse
import com.martinsterentjevs.cacheit.network.auth.AuthApi
import com.martinsterentjevs.cacheit.network.auth.LoginRequest
import com.martinsterentjevs.cacheit.network.auth.RegisterRequest
import com.martinsterentjevs.cacheit.network.auth.SaltLookupRequest
import retrofit2.HttpException
import java.io.IOException
import java.util.Base64
import javax.inject.Inject

/** Thrown with copy that's already safe to show directly in a popup - never leak raw exception text to the UI. */
class AuthFlowException(val userMessage: String) : Exception(userMessage)

data class AuthSession(
    val accountId: String,
    val deviceId: String,
    val accessToken: String,
    val refreshToken: String,
    val wrappedMek: String, // Base64 envelope - caller unwraps with the locally-derived MUK
)

interface AuthRepository {
    suspend fun fetchSalt(identifier: String): ByteArray
    suspend fun login(identifier: String, authHash: ByteArray, deviceId: String,deviceName: String): AuthSession
    suspend fun register(
        name: String,
        username: String,
        email: String,
        salt: ByteArray,
        authHash: ByteArray,
        wrappedMek: String,
        deviceId: String,
        deviceName: String
    ): AuthSession
    suspend fun logout()
}

internal class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
) : AuthRepository {

    override suspend fun fetchSalt(identifier: String): ByteArray = authCall("Couldn't reach the server") {
        Base64.getDecoder().decode(authApi.fetchSalt(SaltLookupRequest(identifier)).kdfSalt)
    }

    override suspend fun login(identifier: String, authHash: ByteArray, deviceId: String,deviceName: String): AuthSession =
        authCall("Login failed - try again", httpErrors = mapOf(401 to "Incorrect email/username or password")) {
            authApi.login(
                LoginRequest(identifier = identifier, authHash = authHash.toBase64(), deviceId = deviceId, deviceName = deviceName),
            ).toSession()
        }

    override suspend fun register(
        name: String,
        username: String,
        email: String,
        salt: ByteArray,
        authHash: ByteArray,
        wrappedMek: String,
        deviceId:String,
        deviceName: String
    ): AuthSession =
        authCall("Registration failed - try again", httpErrors = mapOf(409 to "That email or username is already registered")) {
            authApi.register(
                RegisterRequest(
                    accountHolder = name,
                    username = username,
                    email = email,
                    kdfSalt = salt.toBase64(),
                    authHash = authHash.toBase64(),
                    encMekEnvelope = wrappedMek,
                    deviceId = deviceId,
                    deviceName = deviceName
                ),
            ).toSession()
        }

    /** Shared network-error → user-facing-message mapping so each call site doesn't repeat the try/catch shape. */
    private suspend fun <T> authCall(
        genericMessage: String,
        httpErrors: Map<Int, String> = emptyMap(),
        block: suspend () -> T,
    ): T = try {
        block()
    } catch (e: HttpException) {
        throw AuthFlowException(httpErrors[e.code()] ?: genericMessage)
    } catch (e: IOException) {
        throw AuthFlowException("Can't reach the server. Check your connection.")
    }

    private fun ByteArray.toBase64(): String = Base64.getEncoder().encodeToString(this)

    override suspend fun logout() = authCall("Couldn't log out - try again") { authApi.logout() }
    private fun AccountSessionResponse.toSession() = AuthSession(userId,
        deviceId = deviceId, accessToken, refreshToken, encMekEnvelope)
}