package com.martinsterentjevs.cacheit.network.websockets

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

sealed interface WsConnectionState {
    data object Disconnected : WsConnectionState
    data object Connecting : WsConnectionState
    data object Connected : WsConnectionState
    data object Subscribing : WsConnectionState
    data object Ready : WsConnectionState
    data class Failed(val error: Throwable) : WsConnectionState
}
data class StompFrame(
    val command:String,
    val headers: Map<String,String>,
    val body: String
)

interface CacheItWebSocketClient {
    val connectionState : StateFlow<WsConnectionState>
    val nudges: SharedFlow<WsNudgeDto>

    suspend fun connect()
    suspend fun disconnect()
}