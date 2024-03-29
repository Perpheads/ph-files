package com.perpheads.files

import com.perpheads.files.data.*
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import web.file.File
import websockets.WebSocket

class WebSocketSender(private val path: String, private val file: File, private val scope: CoroutineScope) {
    private lateinit var socket: WebSocket
    private var sendJob: Job? = null
    private var tokenChannel = Channel<Unit>(Channel.UNLIMITED)
    private val chunkSize = 1000000
    var onProgress: (Long) -> Unit = {}
    var onError: (String) -> Unit = {}
    var onCompleted: () -> Unit = {}
    var onClosed: () -> Unit = {}
    var onLinkCreated: (String) -> Unit = {}

    var finalCallbackCalled = false

    fun close() {
        socket.close()
        sendJob?.cancel()
        tokenChannel.close()
    }

    private fun startSending() {
        onProgress(0)
        sendJob = scope.launch {
            var sent = 0.0
            while (sent < file.size) {
                tokenChannel.receive()
                val end = (sent + chunkSize).coerceAtMost(file.size)
                socket.send(file.slice(sent, end))
                sent = end
                if (!finalCallbackCalled) {
                    onProgress(sent.toLong())
                }
            }
            socket.send(Json.encodeToString<ShareWebSocketMessage>(CompletedMessage))
        }
    }

    private fun getWebSocketURL(path: String): String {
        val protocol = if (window.location.protocol == "http:") "ws" else "wss"
        return "${protocol}://${ApiClient.host}$path"
    }

    fun open() {
        socket = WebSocket(getWebSocketURL(path))
        socket.onopen = {
            socket.send(Json.encodeToString(AnnounceMessage(file.name, file.size.toLong())))
        }
        socket.onerror = {
            if (!finalCallbackCalled) {
                finalCallbackCalled = true
                onError("An unknown error occurred")
            }
        }
        socket.onmessage = {
            when (val message = Json.decodeFromString<ShareWebSocketMessage>(it.data as String)) {
                is PullMessage -> {
                    for (i in 1..message.count) {
                        tokenChannel.trySend(Unit)
                    }
                    if (sendJob == null) {
                        startSending()
                    }
                }
                is ErrorMessage -> {
                    if (!finalCallbackCalled) {
                        finalCallbackCalled = true
                        onError(message.error)
                    }
                }
                is LinkMessage -> {
                    if (!finalCallbackCalled) {
                        onLinkCreated(message.link)
                    }
                }
                is CompletedMessage -> {
                    if (!finalCallbackCalled) {
                        finalCallbackCalled = true
                        onCompleted()
                    }
                    close()
                }
                else -> {}
            }
        }
        socket.onclose = {
            if (!finalCallbackCalled) {
                finalCallbackCalled = true
                onClosed()
            }
        }
    }
}