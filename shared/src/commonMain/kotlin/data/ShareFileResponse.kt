package data

import kotlinx.serialization.Serializable

@Serializable
data class ShareFileResponse(val fileName: String, val size: Long)