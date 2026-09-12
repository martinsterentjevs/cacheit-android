package com.martinsterentjevs.cacheit.network.account

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class DeviceSessionDto(
    val deviceId: String,
    val deviceName: String?,
    val lastSeenAt: Instant,
    val createdAt: Instant,
    val isCurrentDevice: Boolean
)
@Serializable
data class AccountProfileDto(
    val accountHolder: String,
    val username: String?,
    val email: String?
)