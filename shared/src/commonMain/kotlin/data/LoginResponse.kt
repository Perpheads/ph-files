package com.perpheads.files.data

import kotlinx.serialization.*

@Serializable
data class AccountInfo(val username: String, val email: String)

@Serializable
data class LoginResponse(val accountInfo: AccountInfo? = null, val error: String? = null)