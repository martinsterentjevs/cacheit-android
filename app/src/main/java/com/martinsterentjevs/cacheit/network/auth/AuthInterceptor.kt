package com.martinsterentjevs.cacheit.network.auth

import com.martinsterentjevs.cacheit.services.security.SecurityService
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

// Attaches the current access token to every request except /auth/** (which doesn't have one yet). **/
@Singleton
class AuthInterceptor @Inject constructor(
private val securityService: SecurityService,
) : Interceptor {
override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
    val path = request.url.encodedPath
    // skips adding AccessToken for the register, login and refresh commands
    val skipToken =path.startsWith("/auth/") && path != "/auth/logout"
    if (skipToken) return chain.proceed(request)

    val token = securityService.getAccessToken() ?: return chain.proceed(request)
    val authorized = request.newBuilder()
        .header("Authorization", "Bearer $token")
        .build()
    return chain.proceed(authorized)
    }
}