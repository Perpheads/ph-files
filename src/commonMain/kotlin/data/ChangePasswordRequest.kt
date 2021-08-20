package data

data class ChangePasswordRequest(val existingPassword: String, val newPassword: String)