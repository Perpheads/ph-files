package com.perpheads.files.data

import kotlinx.serialization.*
@Serializable
data class ChangePasswordRequest(val existingPassword: String, val newPassword: String)