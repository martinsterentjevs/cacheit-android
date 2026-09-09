package com.martinsterentjevs.cacheit.network.auth

import com.martinsterentjevs.cacheit.services.security.SecurityService
import com.martinsterentjevs.cacheit.services.security.SessionExpiredSignal
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val securityService: SecurityService,
    private val authApiProvider: Provider<AuthApi>,
    private val sessionExpiredSignal: SessionExpiredSignal,
) : Authenticator {

    private val refreshMutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponseCount() >= 2) return null

        val failedAccessToken = response.request.header("Authorization")?.removePrefix("Bearer ")

        val newAccessToken = runBlocking {
            refreshMutex.withLock {
                val current = securityService.getAccessToken()
                if (current != null && current != failedAccessToken) {
                    current
                } else {
                    refreshToken()
                }
            }
        } ?: return null

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccessToken")
            .build()
    }

    private suspend fun refreshToken(): String? {
        val refreshToken = securityService.getRefreshToken() ?: return null
        val deviceId = securityService.getOrCreateDeviceId()
        return try {
            val session = authApiProvider.get().refresh(RefreshRequestDto(deviceId, refreshToken))
            securityService.setSession(session.userId, session.accessToken, session.refreshToken)
            session.accessToken
        } catch (e: Exception) {
            securityService.clearSession()
            securityService.clearSecureMek()
            sessionExpiredSignal.notify()
            null
        }
    }

    private fun Response.priorResponseCount(): Int {
        var count = 1
        var prior = priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}