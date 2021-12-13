package com.perpheads.files.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchRequest(
    val query: String,
    @SerialName("beforeID")
    val beforeId: Int? = null,
    val page: Int? = null,
    val entriesPerPage: Int
)