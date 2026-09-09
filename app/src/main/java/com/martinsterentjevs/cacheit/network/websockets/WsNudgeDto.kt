package com.martinsterentjevs.cacheit.network.websockets

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
enum class WsNudgeType {
    NOTE_UPDATED, NOTE_DELETED, NOTE_RESTORED, NOTE_LOCK_ACQUIRED, NOTE_LOCK_RELEASED
}

@Serializable
data class WsNudgeDto(
    val type: WsNudgeType,
    val noteId: String,
    val lastModifiedAt: Instant? = null,
    val lockedByDeviceId: String? = null
)