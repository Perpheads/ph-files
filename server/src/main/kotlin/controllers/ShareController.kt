package com.perpheads.files.controllers

import com.perpheads.files.*
import com.perpheads.files.data.*
import data.ShareFileResponse
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.utils.io.*
import io.ktor.websocket.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.html.InputType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

private var openSessions: ConcurrentHashMap<String, ShareSession> = ConcurrentHashMap()

class ShareSession(
    val fileName: String,
    val size: Long,
    private val startCompletable: CompletableDeferred<Unit>,
    private val channel: ReceiveChannel<ByteArray>
) {
    private val downloadStarted = AtomicBoolean(false)

    suspend fun startDownload(): ReceiveChannel<ByteArray> {
        if (!downloadStarted.compareAndSet(false, true)) {
            throw ConflictException("Someone already started this download")
        }
        startCompletable.complete(Unit)
        return channel
    }
}


fun Route.shareRoutes() {
    val random = SecureRandom()
    get("/share/{token}") {
        val token = call.parameters["token"] ?: throw BadRequestException("")
        val session = openSessions[token] ?: throw NotFoundException("")
        call.respond(ShareFileResponse(session.fileName, session.size))
    }

    get("/share/{token}/download") {
        val token = call.parameters["token"] ?: throw BadRequestException("")
        val session = openSessions[token] ?: throw NotFoundException("")
        val channel = session.startDownload()
        call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Inline.withParameter(ContentDisposition.Parameters.FileName, session.fileName).toString()
        )
        call.respondBytesWriter {
            channel.consumeEach {
                this.writeFully(it)
            }
        }
    }

    requireUser(AuthorizationType.COOKIE) {
        webSocket("/share/ws") {
            val logger = this.application.log
            logger.info("Opened share session")
            val announceFrame = this.incoming.receive()
            val announceMessage = Json.decodeFromString<AnnounceMessage>(announceFrame.data.decodeToString())
            logger.info("Received announce message: $announceMessage")
            val completable = CompletableDeferred<Unit>()
            val windowSize = 100
            val arrayChannel = Channel<ByteArray>(windowSize) //Buffer up to 10 chunks, i.e. max 10 * 100kb
            val token = random.alphaNumeric(16)
            logger.info("Generated link: $token")
            val session = ShareSession(
                fileName = announceMessage.fileName,
                size = announceMessage.size,
                startCompletable = completable,
                channel = arrayChannel
            )
            try {
                openSessions[token] = session
                outgoing.send(Frame.Text(Json.encodeToString<ShareWebSocketMessage>(LinkMessage(token))))
                logger.info("Waiting for user to open share link")
                completable.await() //Wait for someone to start downloading
                openSessions.remove(token)
                outgoing.send(Frame.Text(Json.encodeToString<ShareWebSocketMessage>(PullMessage(windowSize))))

                while (true) {
                    val dataFrame = incoming.receive()
                    if (dataFrame is Frame.Binary) {
                        val bytes = dataFrame.readBytes()
                        if (bytes.size > 1000000) return@webSocket
                        arrayChannel.send(bytes)
                        outgoing.send(Frame.Text(Json.encodeToString<ShareWebSocketMessage>(PullMessage(1))))
                    } else if (dataFrame is Frame.Text) {
                        val message = Json.decodeFromString<ShareWebSocketMessage>(dataFrame.readText())
                        if (message is CompletedMessage) {
                            break
                        }
                    }
                }
            } finally {
                openSessions.remove(token)
                arrayChannel.close()
            }
        }
    }
}