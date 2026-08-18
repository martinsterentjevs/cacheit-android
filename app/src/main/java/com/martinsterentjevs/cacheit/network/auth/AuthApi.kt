package com.martinsterentjevs.cacheit.network.auth

import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    /** Unauthenticated by design - real accounts get their stored salt, unknown identifiers get a deterministic fake. */
    @POST("auth/salt")
    suspend fun fetchSalt(@Body request: SaltLookupRequest): SaltLookupResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AccountSessionResponse

    /** Call only after fetchSalt — authHash must already be derived client-side before this request is built. */
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AccountSessionResponse

    /** Call for session refreshing  **/
    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequestDto): AccountSessionResponse
}