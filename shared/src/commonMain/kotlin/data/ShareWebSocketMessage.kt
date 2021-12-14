package com.perpheads.files.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ShareWebSocketMessage


@Serializable
@SerialName("pull")
data class PullMessage(val count: Int): ShareWebSocketMessage()

@Serializable
@SerialName("link")
data class LinkMessage(val link: String): ShareWebSocketMessage()

@Serializable
@SerialName("error")
data class ErrorMessage(val error: String): ShareWebSocketMessage()

@Serializable
data class AnnounceMessage(val fileName: String, val size: Long)

@Serializable
@SerialName("completed")
object CompletedMessage : ShareWebSocketMessage()