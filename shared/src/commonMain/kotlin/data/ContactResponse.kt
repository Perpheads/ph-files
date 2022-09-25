package com.perpheads.files.data

import kotlinx.serialization.Serializable

@Serializable
data class ContactResponse(
    val email: String
)