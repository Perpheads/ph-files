package com.perpheads.files.data

import java.time.Instant
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

data class File(
    val fileId: Int,
    val link: String,
    val fileName: String,
    val mimeType: String,
    val userId: Int?,
    val uploadDate: Instant,
    val size: Int,
    val thumbnail: ByteArray?,
    val md5: String?
) {
    companion object {
        private val SIZE_UNITS = listOf("B", "kB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB")
    }
    fun humanReadableByteSize(): String {
        if (size <= 0) return  "0 B"
        val digitGroup = (log10(size.toDouble()) / 10).toInt()
        val unit = SIZE_UNITS.getOrNull(digitGroup) ?: "BIG"
        val amount = ((size / (1024.0.pow(digitGroup))) * 10).roundToInt() / 10.0
        return "$amount $unit"
    }


}