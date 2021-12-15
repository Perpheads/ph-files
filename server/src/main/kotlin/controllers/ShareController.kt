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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.time.withTimeout
import kotlinx.coroutines.time.withTimeoutOrNull
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

private var openSessions: ConcurrentHashMap<String, ShareSession> = ConcurrentHashMap()
private var usersWithSessions: ConcurrentHashMap<Int, Unit> = ConcurrentHashMap()

class ShareSession(
    val fileName: String,
    val size: Long,
    private val startCompletable: CompletableDeferred<Unit>,
    private val channel: ReceiveChannel<ByteArray>
) {
    private val downloadStarted = AtomicBoolean(false)

    fun startDownload(): ReceiveChannel<ByteArray> {
        if (!downloadStarted.compareAndSet(false, true)) {
            throw ConflictException("Someone already started this download")
        }
        startCompletable.complete(Unit)
        return channel
    }
}

private object DownloadCancelledException : CancellationException()

private object UploadCancelledException : CancellationException()

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
            ContentDisposition.File.withParameter(ContentDisposition.Parameters.FileName, session.fileName).toString()
        )
        call.respondBytesWriter {
            try {
                for (arr in channel) {
                    this.writeFully(arr)
                }
            } catch (e: UploadCancelledException) {
                throw e
            } catch (e: Exception) {
                channel.cancel(DownloadCancelledException)
            }
        }
    }

    suspend fun selectCloseOrStart(
        completableDeferred: CompletableDeferred<Unit>,
        incoming: ReceiveChannel<*>
    ) {
        select<Unit> {
            completableDeferred.onAwait {
            }
            incoming.onReceiveCatching {
                it.exceptionOrNull()?.let { e -> throw e }
            }
        }
    }
    val validFileNameRegex = Regex("^[\\w\\-. ]+$")

    requireUser(AuthorizationType.COOKIE) {
        webSocket("/share/ws") {
            val userId = call.user().userId
            val logger = this.application.log
            logger.info("Opened share session for user $userId")
            val announceFrame = withTimeoutOrNull(Duration.ofSeconds(10)) {
                incoming.receive()
            }
            if (announceFrame == null) {
                val errorMessage =
                    Json.encodeToString<ShareWebSocketMessage>(ErrorMessage("Timeout waiting for announcement"))
                outgoing.send(Frame.Text(errorMessage))
                return@webSocket
            }
            val announceMessage = Json.decodeFromString<AnnounceMessage>(announceFrame.data.decodeToString())
            if (!validFileNameRegex.matches(announceMessage.fileName) || announceMessage.fileName.length !in 2..50) {
                logger.warn("User $userId attempted share session with invalid filename: ${announceMessage.fileName}")
                val errorMessage =
                    Json.encodeToString<ShareWebSocketMessage>(ErrorMessage("Invalid filename"))
                outgoing.send(Frame.Text(errorMessage))
                return@webSocket
            }


            logger.info("Received announce message: $announceMessage from user $userId")
            val completable = CompletableDeferred<Unit>()
            val windowSize = 10
            val arrayChannel = Channel<ByteArray>(windowSize) //Buffer up to 10 chunks, i.e. max 10 * 100kb
            val existedBefore = usersWithSessions.putIfAbsent(userId, Unit)
            if (existedBefore != null) {
                logger.warn("User $userId already had a share session open")
                val errorMessage =
                    Json.encodeToString<ShareWebSocketMessage>(ErrorMessage("Can only have one session open per user"))
                outgoing.send(Frame.Text(errorMessage))
                return@webSocket
            }
            val session = ShareSession(
                fileName = announceMessage.fileName,
                size = announceMessage.size,
                startCompletable = completable,
                channel = arrayChannel
            )
            val token = random.alphaNumeric(16)
            logger.info("Generated link: $token")
            if (openSessions.putIfAbsent(token, session) != null) {
                //Token was already used
                return@webSocket
            }
            try {
                outgoing.send(Frame.Text(Json.encodeToString<ShareWebSocketMessage>(LinkMessage(token))))
                logger.info("Waiting for user to open share link for user $userId")
                withTimeout(Duration.ofMinutes(10)) {
                    selectCloseOrStart(completable, incoming) //Wait for someone to start downloading
                }
                openSessions.remove(token)
                outgoing.send(Frame.Text(Json.encodeToString<ShareWebSocketMessage>(PullMessage(windowSize))))

                while (true) {
                    val dataFrame = withTimeout(Duration.ofSeconds(5)) {
                        incoming.receive()
                    }
                    if (dataFrame is Frame.Binary) {
                        val bytes = dataFrame.readBytes()
                        if (bytes.size > 1000000) return@webSocket
                        arrayChannel.send(bytes)
                        outgoing.send(Frame.Text(Json.encodeToString<ShareWebSocketMessage>(PullMessage(1))))
                    } else if (dataFrame is Frame.Text) {
                        val message = Json.decodeFromString<ShareWebSocketMessage>(dataFrame.readText())
                        if (message is CompletedMessage) {
                            outgoing.send(Frame.Text(Json.encodeToString<ShareWebSocketMessage>(CompletedMessage)))
                            logger.info("File upload complete for $token")
                            break
                        }
                    }
                }
            } catch (e: DownloadCancelledException) {
                logger.info("User cancelled download: $token")
                outgoing.send(Frame.Text(Json.encodeToString<ShareWebSocketMessage>(ErrorMessage("Download cancelled"))))
            } catch (ignored: CancellationException) {
                logger.info("User cancelled upload: $token")
                arrayChannel.cancel(UploadCancelledException)
                //WebSocket closed
            } catch (e: TimeoutException) {
                val errorMessage =
                    Json.encodeToString<ShareWebSocketMessage>(ErrorMessage("Timeout waiting for someone to download"))
                outgoing.send(Frame.Text(errorMessage))
            } finally {
                logger.info("Removing share session $token")
                usersWithSessions.remove(userId)
                openSessions.remove(token)
                arrayChannel.close()
            }
        }
    }
}