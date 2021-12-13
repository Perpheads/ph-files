package com.perpheads.files.data

import kotlinx.serialization.*
@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
    val remember: Boolean
)