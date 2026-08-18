package com.martinsterentjevs.cacheit.network.auth

import com.martinsterentjevs.cacheit.services.security.SecurityService
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val securityService: SecurityService,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        // Salt lookup, login, and register are the only unauthenticated calls —
        // everything else needs a token if one exists.
        val token = securityService.getAccessToken() ?: return chain.proceed(original)
        val authed = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(authed)
    }
}