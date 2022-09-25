package com.perpheads.files.data

import kotlinx.serialization.Serializable

@Serializable
data class StatisticsResponse(
    val totalStatistics: FileTotalStatistics,
    val userStatistics: List<FileUserStatistics>
)