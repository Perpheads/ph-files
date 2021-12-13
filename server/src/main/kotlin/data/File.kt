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
)