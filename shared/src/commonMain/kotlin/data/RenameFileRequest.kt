package com.perpheads.files.data

import kotlinx.serialization.*

@Serializable
data class RenameFileRequest(val newName: String)
