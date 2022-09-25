package com.perpheads.files.data

import kotlinx.serialization.*

@Serializable
data class AccountInfo(val username: String, val email: String)

@Serializable
data class AccountInfoV2(val username: String, val email: String, val admin: Boolean)

@Serializable
data class LoginResponse(val accountInfo: AccountInfo? = null, val error: String? = null)

@Serializable
data class LoginResponseV2(val accountInfo: AccountInfoV2? = null, val error: String? = null)