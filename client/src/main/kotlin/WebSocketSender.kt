package com.perpheads.files

import com.perpheads.files.data.*
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.dom.WebSocket
import org.w3c.files.File

class WebSocketSender(private val path: String, private val file: File) {
    private lateinit var socket: WebSocket
    private var sendJob: Job? = null
    private var tokenChannel = Channel<Unit>(Channel.UNLIMITED)
    private val chunkSize = 1000000
    var onProgress: (Long) -> Unit = {}
    var onError: (String) -> Unit = {}
    var onCompleted: () -> Unit = {}
    var onClosed: () -> Unit = {}
    var onLinkCreated: (String) -> Unit = {}

    fun close() {
        socket.close()
        sendJob?.cancel()
        tokenChannel.close()
    }

    private fun startSending() {
        onProgress(0)
        sendJob = MainScope().launch {
            var sent = 0
            while (sent < file.size.toInt()) {
                tokenChannel.receive()
                val end = (sent + chunkSize).coerceAtMost(file.size.toInt())
                socket.send(file.slice(sent, end))
                sent = end
                onProgress(sent.toLong())
            }
            socket.send(Json.encodeToString<ShareWebSocketMessage>(CompletedMessage))
            onCompleted()
            close()
        }
    }

    private fun getWebSocketURL(path: String): String {
        val location = window.location
        val protocol = if (window.location.protocol == "http:") "ws" else "wss"
        return "${protocol}://${location.host}$path"
    }

    fun open() {
        socket = WebSocket(getWebSocketURL(path))
        socket.onopen = {
            socket.send(Json.encodeToString(AnnounceMessage(file.name, file.size.toLong())))
        }
        socket.onerror = {
            console.log("An error occurred")
            onError("An error occurred")
        }
        socket.onmessage = {
            console.log("Received data: ${it.data}")
            when (val message = Json.decodeFromString<ShareWebSocketMessage>(it.data as String)) {
                is PullMessage -> {
                    for (i in 1..message.count) {
                        tokenChannel.trySend(Unit)
                    }
                    if (sendJob == null) {
                        startSending()
                    }
                }
                is LinkMessage -> {
                    onLinkCreated(message.link)
                }
            }
        }
        socket.onclose = {
            console.log("WebSocket closed")
            onClosed()
        }
    }
}