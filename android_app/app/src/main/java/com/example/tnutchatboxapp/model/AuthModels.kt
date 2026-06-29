package com.example.tnutchatboxapp.model

import com.google.gson.annotations.SerializedName

// Auth Models
data class UserRegistration(
    val username: String,
    val email: String,
    val password: String,
    @SerializedName("confirm_password")
    val confirmPassword: String
)

data class UserLogin(
    val username: String,
    val password: String
)

data class EmailVerification(
    val email: String,
    @SerializedName("verification_code")
    val verificationCode: String
)

data class PasswordReset(
    val email: String
)

data class PasswordResetVerification(
    val email: String,
    @SerializedName("verification_code")
    val verificationCode: String
)

data class PasswordChange(
    @SerializedName("current_password")
    val currentPassword: String,
    @SerializedName("new_password")
    val newPassword: String,
    @SerializedName("confirm_new_password")
    val confirmNewPassword: String
)

data class EmailChange(
    @SerializedName("new_email")
    val newEmail: String,
    val password: String
)

data class GoogleAuth(
    @SerializedName("id_token")
    val idToken: String
)

data class GoogleAuthManual(
    val email: String,
    val username: String,
    val password: String,
    @SerializedName("confirm_password")
    val confirmPassword: String
)

// Response Models

data class ApiResponse(
    val message: String,
    val detail: String? = null
)

data class ErrorResponse(
    val detail: String
)

data class VerificationCodeResponse(
    val message: String,
    @SerializedName("email_sent")
    val emailSent: Boolean
)

data class UserProfile(
    @SerializedName("user_id")
    val userId: Int,
    val username: String,
    val email: String,
    @SerializedName("created_at")
    val createdAt: String
)
