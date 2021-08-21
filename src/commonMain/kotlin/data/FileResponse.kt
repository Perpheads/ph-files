package com.perpheads.files.data

import kotlinx.datetime.Instant

import kotlinx.serialization.*
@Serializable
data class FileResponse(
    @SerialName("fileID")
    val fileId: Int,
    val link: String,
    val fileName: String,
    val mimeType: String,
    val uploadDate: Instant,
    val formattedUploadDate: String,
    val size: Int,
    val thumbnail: ByteArray?,
    val hasThumbnail: Boolean
)