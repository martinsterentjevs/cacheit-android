package com.martinsterentjevs.cacheit.network.auth

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class SaltLookupRequest(
    val identifier: String
)

@Serializable
data class SaltLookupResponse(
    val kdfSalt: String
)

@Serializable
data class RegisterRequest(
    val accountHolder: String,
    val username: String,
    val email: String,
    val authHash: String,
    val deviceId: String,
    val deviceName: String,
    val encMekEnvelope: String,
    val kdfSalt: String
)

@Serializable
data class LoginRequest(
    val identifier: String,
    val authHash: String,
    val deviceId: String,
    val deviceName: String
)

@Serializable
data class AccountSessionResponse(
    val userId: String,
    val deviceId:String,
    val accessToken: String,
    val refreshToken: String,
    val encMekEnvelope: String
)

@Serializable
data class RefreshRequestDto(
    val deviceId: String,
    val refreshToken: String
)