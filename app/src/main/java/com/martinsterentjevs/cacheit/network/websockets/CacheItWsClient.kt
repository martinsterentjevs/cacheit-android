package com.martinsterentjevs.cacheit.network.websockets

import android.util.Log
import com.martinsterentjevs.cacheit.BuildConfig
import com.martinsterentjevs.cacheit.services.security.SecurityService
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.SerializationException
import com.martinsterentjevs.cacheit.network.cacheItJson
import java.net.URL

class CacheItWsClient @Inject constructor(private val securityService: SecurityService): CacheItWebSocketClient {
    override val connectionState: StateFlow<WsConnectionState>
        field = MutableStateFlow<WsConnectionState>(
            WsConnectionState.Disconnected
        )
    private val _nudges = MutableSharedFlow<WsNudgeDto>(extraBufferCapacity = 16)
    override val nudges: SharedFlow<WsNudgeDto> = _nudges.asSharedFlow()
    private val client = HttpClient(CIO) { install(WebSockets.Plugin) }
    private val serverUrl = URL(BuildConfig.SERVER_BASE_URL)
    private val host = serverUrl.host
    private val port = if (serverUrl.port != -1) serverUrl.port else if (serverUrl.protocol == "https") 443 else 80
    private val path = "/ws/sync"
    override suspend fun connect() {
        connectionState.value = WsConnectionState.Connecting

        try {
            val token = securityService.getAccessToken()
            if (token.isNullOrBlank()) {
                connectionState.value = WsConnectionState.Failed(IllegalStateException("No access token found"))
                return
            }

            Log.d("WS", "Connecting to $host:$port$path")

            client.webSocket(
                host = host,
                port = port,
                path = path,
                request = {
                    header("Authorization", "Bearer $token")
                    parameter("access_token", token)
                }
            ) {
                send(
                    Frame.Text(
                        stompConnectFrame(
                            accessToken = token
                        )
                    )
                )

                val connectedFrame = incoming.receive()

                if (connectedFrame !is Frame.Text) {
                    connectionState.value =
                        WsConnectionState.Failed(
                            IllegalStateException("Expected STOMP CONNECTED frame")
                        )
                    return@webSocket
                }

                val connectedText = connectedFrame.readText()

                if (!connectedText.startsWith("CONNECTED")) {
                    connectionState.value =
                        WsConnectionState.Failed(
                            IllegalStateException(
                                "STOMP connection rejected: $connectedText"
                            )
                        )
                    return@webSocket
                }

                connectionState.value = WsConnectionState.Connected
                Log.d("WS", "STOMP CONNECTED received")
                connectionState.value = WsConnectionState.Subscribing

                send(Frame.Text(stompSubscribeFrame()))

                val receiptFrame = incoming.receive()

                if (receiptFrame !is Frame.Text) {
                    connectionState.value =
                        WsConnectionState.Failed(
                            IllegalStateException("Expected STOMP RECEIPT frame")
                        )
                    return@webSocket
                }

                val receiptText = receiptFrame.readText()

                if (!receiptText.startsWith("RECEIPT")) {
                    connectionState.value =
                        WsConnectionState.Failed(
                            IllegalStateException(
                                "STOMP subscription rejected: $receiptText"
                            )
                        )
                    return@webSocket
                }

                connectionState.value = WsConnectionState.Ready

                // Keep the WebSocket session alive.
                for (frame in incoming) {
                    if (frame !is Frame.Text) {
                        continue
                    }

                    val stompFrame = try {
                        StompFrameParser.parse(frame.readText())
                    } catch (e: IllegalArgumentException) {
                        connectionState.value =
                            WsConnectionState.Failed(e)

                        continue
                    }

                    when (stompFrame.command) {
                        "MESSAGE" -> {
                            try {
                                val nudge = cacheItJson.decodeFromString<WsNudgeDto>(stompFrame.body)
                                _nudges.emit(nudge)
                            } catch (e: SerializationException) {
                                Log.e("WS", "Failed to parse nudge payload: ${stompFrame.body}", e)
                            }
                        }

                        "ERROR" -> {
                            connectionState.value =
                                WsConnectionState.Failed(
                                    IllegalStateException(
                                        "STOMP error: ${stompFrame.body}"
                                    )
                                )
                        }

                        else -> {
                            println("STOMP ${stompFrame.command}")
                        }
                    }
                }
            }

            connectionState.value = WsConnectionState.Disconnected

        } catch (e: Throwable) {
            connectionState.value = WsConnectionState.Failed(e)
            Log.e("WebSocket Error: ","Failed",e)
        }
    }

    override suspend fun disconnect() {
        connectionState.value = WsConnectionState.Disconnected
        // Deliberately does NOT call client.close() - that permanently kills the shared
        // HttpClient and would break every future connect() call. Socket teardown happens
        // via structured concurrency when WsSessionManager cancels the coroutine running
        // connect().
    }
    private fun stompConnectFrame(accessToken: String): String =
        buildString {
            append("CONNECT\n")
            append("accept-version:1.2\n")
            append("Authorization:Bearer $accessToken\n")
            append("\n")
            append('\u0000')
        }

    private fun stompSubscribeFrame(): String =
        buildString {
            append("SUBSCRIBE\n")
            append("id:cacheit-nudges\n")
            append("destination:/user/queue/nudges\n")
            append("ack:auto\n")
            append("receipt:cacheit-nudge-subscription\n")
            append("\n")
            append('\u0000')
        }
}