package com.example.tnutchatboxapp.model

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("access_token")
    val accessToken: String?,
    @SerializedName("user_id")
    val userId: String?,
    @SerializedName("username")
    val username: String?,
    @SerializedName("email")
    val email: String?,
    @SerializedName("is_verified")
    val isVerified: Boolean?
)