package com.example.tnutchatboxapp.account

// Data class for change password API request
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

// Data class for change email API request
data class ChangeEmailRequest(
    val newEmail: String
)

// Data class for verify email change API request
data class VerifyChangeEmailRequest(
    val newEmail: String,
    val verificationCode: String
)
