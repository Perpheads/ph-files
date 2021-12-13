package com.perpheads.files.data
import kotlinx.serialization.*

@Serializable
data class ThumbnailRequest(
    @SerialName("fileIDs")
    val fileIds: List<Int>
)