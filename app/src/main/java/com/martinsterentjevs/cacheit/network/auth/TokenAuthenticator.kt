package com.martinsterentjevs.cacheit.network.auth

import com.martinsterentjevs.cacheit.services.security.SecurityService
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

/**
 * Handles 401s from any authenticated endpoint by refreshing the access token
 * and retrying once. The mutex is the single-flight guard: if five requests
 * 401 at once, only the first one actually calls /auth/refresh - the other
 * four block on the lock, then read the token SecurityService already has
 * once it's released, instead of each firing their own refresh.
 *
 * authApiProvider is a Provider, not a direct AuthApi, to break the
 * dependency cycle: AuthApi is built from a Retrofit that's built from an
 * OkHttpClient that needs this Authenticator to exist first.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val securityService: SecurityService,
    private val authApiProvider: Provider<AuthApi>,
) : Authenticator {

    private val refreshMutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Never loop forever - if the retried request also 401s, give up.
        if (response.priorResponseCount() >= 2) return null

        val failedAccessToken = response.request.header("Authorization")?.removePrefix("Bearer ")

        val newAccessToken = runBlocking {
            refreshMutex.withLock {
                // Another request may have already refreshed while we waited for the lock -
                // if the cached token has moved on from the one that just failed, use it
                // as-is instead of refreshing again.
                val current = securityService.getAccessToken()
                if (current != null && current != failedAccessToken) {
                    current
                } else {
                    refreshToken()
                }
            }
        } ?: return null // refresh failed - fall through to a real 401, don't loop

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