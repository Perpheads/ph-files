package com.perpheads.files.data

data class User(
    val userId: Int,
    val name: String,
    val email: String,
    val password: String,
    val apiKey: String
)