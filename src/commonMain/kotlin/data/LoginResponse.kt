package data

data class AccountInfo(val username: String, val email: String)

data class LoginResponse(val accountInfo: AccountInfo? = null, val error: String? = null)