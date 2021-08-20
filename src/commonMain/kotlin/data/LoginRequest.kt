package data

data class LoginRequest(
    val username: String,
    val password: String,
    val remember: Boolean
)