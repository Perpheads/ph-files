package com.perpheads.files.data

import kotlinx.serialization.Serializable

@Serializable
data class FileTotalStatistics(
    val fileCount: Int,
    val storageUsed: Long
)