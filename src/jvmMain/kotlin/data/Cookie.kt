package com.perpheads.files.data

import java.time.Instant


data class Cookie(
    val cookieId: String,
    val userId: Int,
    val expiry: Instant
)