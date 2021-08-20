package com.perpheads.files.data

import java.time.Instant


data class Cookie(
    val cookieId: String,
    val userId: Int,
    val cookie: String,
    val createDate: Instant
)