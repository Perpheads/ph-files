package com.perpheads.files.data

import kotlinx.datetime.Instant

import kotlinx.serialization.*
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

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
) {
    companion object {
        private val SIZE_UNITS = listOf("B", "kB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB")
    }
    fun humanReadableByteSize(): String {
        if (size <= 0) return  "0 B"
        val digitGroup = (log10(size.toDouble()) / 3).toInt()
        val unit = SIZE_UNITS.getOrNull(digitGroup) ?: "BIG"
        val amount = ((size / (1000.0.pow(digitGroup))) * 10).roundToInt() / 10.0
        return "$amount $unit"
    }
}