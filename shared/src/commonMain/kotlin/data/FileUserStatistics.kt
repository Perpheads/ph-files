package com.perpheads.files.data

import kotlinx.serialization.Serializable

@Serializable
data class FileUserStatistics(
    val name: String,
    val fileCount: Int,
    val storageUsed: Long
)