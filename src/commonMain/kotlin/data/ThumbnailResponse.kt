package com.perpheads.files.data

import kotlinx.serialization.*

@Serializable
data class ThumbnailResponse(
    @SerialName("fileID")
    val fileId: Int,
    val thumbnail: String?
)