package com.perpheads.files.data

import kotlinx.serialization.Serializable

@Serializable
data class CreateUserRequest(
    val username: String,
    val email: String,
    val password: String
) {
    fun isValidRequest(): Boolean {
        if (username.length !in 3..50) {
            return false
        }
        if (email.length !in 3..100) {
            return false
        }
        if (password.length !in 8..50) {
            return false
        }
        return true
    }
}