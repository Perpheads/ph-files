package com.perpheads.files.data

import kotlinx.serialization.*

@Serializable
data class FileListResponse(
    val files: List<FileResponse>,
    val totalPages: Int,
    val currentPage: Int,
    val pageStart: Int,
    val pageEnd: Int
)